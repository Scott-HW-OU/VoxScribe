package com.example.data.remote

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.local.CallRecordingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

enum class ExportFormatOption(val id: String, val label: String, val extension: String, val mimeType: String) {
    MARKDOWN("MARKDOWN", "Markdown (.md)", "md", "text/markdown"),
    PLAIN_TEXT("PLAIN_TEXT", "Plain Text (.txt)", "txt", "text/plain"),
    CSV("CSV", "CSV Spreadsheet (.csv)", "csv", "text/csv"),
    ENEX("ENEX", "Evernote XML (.enex)", "enex", "application/xml")
}

enum class ExportTargetApp(val id: String, val label: String, val packageName: String?) {
    NOTION("NOTION", "Notion", "notion.id"),
    EVERNOTE("EVERNOTE", "Evernote", "com.evernote"),
    ONENOTE("ONENOTE", "Microsoft OneNote", "com.microsoft.office.onenote"),
    GOOGLE_KEEP("GOOGLE_KEEP", "Google Keep", "com.google.android.keep"),
    UNIVERSAL_SHARE("UNIVERSAL_SHARE", "Save / Share File", null)
}

data class ExportFieldSelection(
    val includeDateTime: Boolean = true,
    val includeParticipants: Boolean = true,
    val includeCompliance: Boolean = true,
    val includeSummary: Boolean = true,
    val includeDecisionsAndDates: Boolean = true,
    val includeActionItems: Boolean = true,
    val includeTags: Boolean = true,
    val includeTranscript: Boolean = true
)

sealed class ExportResult {
    data class Success(val message: String, val shareIntent: Intent? = null) : ExportResult()
    data class Error(val errorMessage: String) : ExportResult()
}

class ProductivityExportManager(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Generates a formatted string preview (Markdown, Plain Text, CSV, or ENEX) using the user's exact
     * selected data fields (`ExportFieldSelection`).
     */
    fun formatCustomExport(
        recording: CallRecordingEntity,
        format: ExportFormatOption,
        fields: ExportFieldSelection,
        notebookName: String = "VoxScribe Call Summaries"
    ): String {
        return when (format) {
            ExportFormatOption.MARKDOWN -> formatMarkdownWithFields(recording, fields)
            ExportFormatOption.PLAIN_TEXT -> formatPlainTextWithFields(recording, fields)
            ExportFormatOption.CSV -> formatCsvWithFields(recording, fields)
            ExportFormatOption.ENEX -> buildEvernoteEnexContent(recording, fields, notebookName)
        }
    }

    /**
     * Executes an advanced export to Notion, Evernote, OneNote, Google Keep, or universal file share
     * honoring the user's chosen `ExportFormatOption` and `ExportFieldSelection`.
     */
    suspend fun executeCustomExport(
        recording: CallRecordingEntity,
        targetApp: ExportTargetApp,
        format: ExportFormatOption,
        fields: ExportFieldSelection,
        notionDatabaseId: String,
        evernoteNotebookName: String
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val formattedContent = formatCustomExport(recording, format, fields, evernoteNotebookName)
            copyToClipboard("${targetApp.label} Export - ${recording.contactName}", formattedContent)

            // Write export file in cacheDir/exports so it can be attached via FileProvider
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeContact = recording.contactName.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifEmpty { "call" }
            val exportFile = File(exportDir, "voxscribe_${safeContact}_${recording.id}.${format.extension}")
            exportFile.writeText(formattedContent)
            val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportFile)

            when (targetApp) {
                ExportTargetApp.NOTION -> {
                    val apiKey = BuildConfig.NOTION_API_KEY
                    if (apiKey.isNotBlank() && apiKey != "MY_NOTION_API_KEY" && notionDatabaseId.isNotBlank()) {
                        runCatching {
                            val requestJson = buildNotionPagePayload(recording, notionDatabaseId.trim(), fields)
                            val request = Request.Builder()
                                .url("https://api.notion.com/v1/pages")
                                .addHeader("Authorization", "Bearer $apiKey")
                                .addHeader("Notion-Version", "2022-06-28")
                                .addHeader("Content-Type", "application/json")
                                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                                .build()
                            httpClient.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    return@withContext ExportResult.Success(
                                        message = "Created Notion Page via Notion API & copied ${format.label} to clipboard!"
                                    )
                                }
                            }
                        }
                    }
                    val shareIntent = buildTargetedOrChooserIntent(
                        targetPackage = ExportTargetApp.NOTION.packageName,
                        subject = "Call Summary: ${recording.contactName}",
                        bodyText = formattedContent,
                        fileUri = fileUri,
                        mimeType = format.mimeType,
                        chooserTitle = "Export to Notion"
                    )
                    ExportResult.Success(
                        message = "Copied ${format.label} to clipboard & ready for Notion!",
                        shareIntent = shareIntent
                    )
                }

                ExportTargetApp.EVERNOTE -> {
                    val enexFile = if (format == ExportFormatOption.ENEX) {
                        exportFile
                    } else {
                        File(exportDir, "voxscribe_${safeContact}_${recording.id}.enex").apply {
                            writeText(buildEvernoteEnexContent(recording, fields, evernoteNotebookName))
                        }
                    }
                    val enexUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", enexFile)

                    val evernoteNativeIntent = Intent("com.evernote.action.CREATE_NEW_NOTE").apply {
                        putExtra(Intent.EXTRA_TITLE, "Call Summary: ${recording.contactName}")
                        putExtra(Intent.EXTRA_TEXT, formattedContent)
                        if (fields.includeTags) {
                            putStringArrayListExtra("TAG_NAME_LIST", ArrayList(recording.keywordTagsList()))
                        }
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    if (evernoteNativeIntent.resolveActivity(context.packageManager) != null) {
                        return@withContext ExportResult.Success(
                            message = "Launching Evernote with selected fields & keyword tags!",
                            shareIntent = evernoteNativeIntent
                        )
                    }

                    val shareIntent = buildTargetedOrChooserIntent(
                        targetPackage = ExportTargetApp.EVERNOTE.packageName,
                        subject = "[$evernoteNotebookName] Call with ${recording.contactName}",
                        bodyText = formattedContent,
                        fileUri = enexUri,
                        mimeType = "text/plain",
                        chooserTitle = "Export to Evernote"
                    )
                    ExportResult.Success(
                        message = "Generated Evernote note (${format.label} + .enex) & copied to clipboard!",
                        shareIntent = shareIntent
                    )
                }

                ExportTargetApp.ONENOTE -> {
                    val shareIntent = buildTargetedOrChooserIntent(
                        targetPackage = ExportTargetApp.ONENOTE.packageName,
                        subject = "OneNote Call Summary: ${recording.contactName}",
                        bodyText = formattedContent,
                        fileUri = fileUri,
                        mimeType = "text/plain",
                        chooserTitle = "Send Call Summary to Microsoft OneNote"
                    )
                    ExportResult.Success(
                        message = "Prepared OneNote export (${format.label}) & copied to clipboard!",
                        shareIntent = shareIntent
                    )
                }

                ExportTargetApp.GOOGLE_KEEP -> {
                    val keepText = formatPlainTextWithFields(recording, fields)
                    copyToClipboard("Google Keep Note - ${recording.contactName}", keepText)
                    val shareIntent = buildTargetedOrChooserIntent(
                        targetPackage = ExportTargetApp.GOOGLE_KEEP.packageName,
                        subject = "Call: ${recording.contactName}",
                        bodyText = keepText,
                        fileUri = null,
                        mimeType = "text/plain",
                        chooserTitle = "Save Call Note to Google Keep"
                    )
                    ExportResult.Success(
                        message = "Formatted checklist note for Google Keep & copied to clipboard!",
                        shareIntent = shareIntent
                    )
                }

                ExportTargetApp.UNIVERSAL_SHARE -> {
                    val shareIntent = buildTargetedOrChooserIntent(
                        targetPackage = null,
                        subject = "VoxScribe Export: ${recording.contactName} (.${format.extension})",
                        bodyText = formattedContent,
                        fileUri = fileUri,
                        mimeType = format.mimeType,
                        chooserTitle = "Export .${format.extension} File"
                    )
                    ExportResult.Success(
                        message = "Generated ${exportFile.name} (${format.label}) & copied to clipboard!",
                        shareIntent = shareIntent
                    )
                }
            }
        } catch (e: Exception) {
            ExportResult.Error("Export failed: ${e.localizedMessage}")
        }
    }

    suspend fun exportToNotion(
        recording: CallRecordingEntity,
        notionDatabaseId: String
    ): ExportResult = executeCustomExport(
        recording = recording,
        targetApp = ExportTargetApp.NOTION,
        format = ExportFormatOption.MARKDOWN,
        fields = ExportFieldSelection(),
        notionDatabaseId = notionDatabaseId,
        evernoteNotebookName = "VoxScribe Call Summaries"
    )

    suspend fun exportToEvernote(
        recording: CallRecordingEntity,
        notebookName: String
    ): ExportResult = executeCustomExport(
        recording = recording,
        targetApp = ExportTargetApp.EVERNOTE,
        format = ExportFormatOption.ENEX,
        fields = ExportFieldSelection(),
        notionDatabaseId = "",
        evernoteNotebookName = notebookName
    )

    fun formatNotionMarkdown(recording: CallRecordingEntity): String =
        formatMarkdownWithFields(recording, ExportFieldSelection())

    private fun formatMarkdownWithFields(
        recording: CallRecordingEntity,
        fields: ExportFieldSelection
    ): String {
        val dateStr = formatReadableDate(recording.timestamp)
        return buildString {
            appendLine("# 📞 Call Summary: ${recording.contactName}")
            if (fields.includeParticipants || fields.includeDateTime) {
                val metaParts = mutableListOf<String>()
                if (fields.includeParticipants) {
                    metaParts.add("**Participant:** ${recording.contactName} (${recording.phoneNumber})")
                    metaParts.add("**Direction:** ${recording.callDirection}")
                }
                if (fields.includeDateTime) {
                    metaParts.add("**Date & Time:** $dateStr")
                    metaParts.add("**Duration:** ${formatDuration(recording.durationSeconds)}")
                }
                appendLine("> ${metaParts.joinToString(" | ")}")
            }
            if (fields.includeCompliance) {
                appendLine(
                    "> **🛡️ Privacy Compliance:** ${recording.consentJurisdiction} (${
                        if (recording.audibleNoticePlayed) "Audible TTS Notice Played" else "Consent Verified"
                    })"
                )
            }
            appendLine()

            if (fields.includeTags) {
                val tagsLine = recording.keywordTagsList().joinToString(" ") { "#$it" }
                if (tagsLine.isNotBlank()) {
                    appendLine("**Keyword Tags:** $tagsLine")
                    appendLine()
                }
            }

            if (fields.includeSummary) {
                appendLine("## ✨ AI Executive Summary (${recording.summaryStyle} · ${recording.summaryLength})")
                appendLine(recording.executiveSummary)
                appendLine()
                val keyPoints = recording.keyPointsList()
                if (keyPoints.isNotEmpty()) {
                    appendLine("### 🎯 Key Takeaways")
                    keyPoints.forEach { appendLine("- $it") }
                    appendLine()
                }
            }

            if (fields.includeDecisionsAndDates) {
                val decisions = recording.decisionsMadeList()
                if (decisions.isNotEmpty()) {
                    appendLine("## 🤝 Decisions Made")
                    decisions.forEach { appendLine("- $it") }
                    appendLine()
                }
                val dates = recording.importantDatesList()
                if (dates.isNotEmpty()) {
                    appendLine("## 📅 Important Dates & Deadlines")
                    dates.forEach { appendLine("- 📌 $it") }
                    appendLine()
                }
            }

            if (fields.includeActionItems) {
                val actionItems = recording.actionItemsList()
                if (actionItems.isNotEmpty()) {
                    appendLine("## ✅ Action Items")
                    actionItems.forEach { item ->
                        val box = if (item.completed) "[x]" else "[ ]"
                        appendLine("- $box **${item.task}** *(Owner: ${item.owner} · Priority: ${item.priority})*")
                    }
                    appendLine()
                }
            }

            if (fields.includeTranscript) {
                appendLine("## 📝 Call Transcript")
                appendLine("```text")
                appendLine(recording.transcript)
                appendLine("```")
            }
        }
    }

    private fun formatPlainTextWithFields(
        recording: CallRecordingEntity,
        fields: ExportFieldSelection
    ): String {
        val dateStr = formatReadableDate(recording.timestamp)
        return buildString {
            appendLine("CALL SUMMARY: ${recording.contactName.uppercase()}")
            appendLine("=".repeat(44))
            if (fields.includeParticipants) {
                appendLine("Participant : ${recording.contactName} (${recording.phoneNumber})")
                appendLine("Call Type   : ${recording.callDirection}")
            }
            if (fields.includeDateTime) {
                appendLine("Date & Time : $dateStr")
                appendLine("Duration    : ${formatDuration(recording.durationSeconds)}")
            }
            if (fields.includeCompliance) {
                appendLine("Compliance  : ${recording.consentJurisdiction} (Audible Alert: ${if (recording.audibleNoticePlayed) "Yes" else "Verified"})")
            }
            if (fields.includeTags) {
                val tags = recording.keywordTagsList().joinToString(" ") { "#$it" }
                if (tags.isNotBlank()) {
                    appendLine("Tags        : $tags")
                }
            }
            appendLine()

            if (fields.includeSummary) {
                appendLine("AI EXECUTIVE SUMMARY (${recording.summaryLength})")
                appendLine("-".repeat(28))
                appendLine(recording.executiveSummary)
                appendLine()
                val points = recording.keyPointsList()
                if (points.isNotEmpty()) {
                    appendLine("KEY TAKEAWAYS:")
                    points.forEach { appendLine("• $it") }
                    appendLine()
                }
            }

            if (fields.includeDecisionsAndDates) {
                val decisions = recording.decisionsMadeList()
                if (decisions.isNotEmpty()) {
                    appendLine("DECISIONS MADE:")
                    decisions.forEach { appendLine("• $it") }
                    appendLine()
                }
                val dates = recording.importantDatesList()
                if (dates.isNotEmpty()) {
                    appendLine("IMPORTANT DATES & DEADLINES:")
                    dates.forEach { appendLine("• $it") }
                    appendLine()
                }
            }

            if (fields.includeActionItems) {
                val actions = recording.actionItemsList()
                if (actions.isNotEmpty()) {
                    appendLine("ACTION ITEMS:")
                    actions.forEach { item ->
                        val mark = if (item.completed) "[DONE]" else "[TODO]"
                        appendLine("$mark ${item.task} (Owner: ${item.owner}, Priority: ${item.priority})")
                    }
                    appendLine()
                }
            }

            if (fields.includeTranscript) {
                appendLine("FULL TRANSCRIPT:")
                appendLine("-".repeat(28))
                appendLine(recording.transcript)
            }
        }
    }

    private fun formatCsvWithFields(
        recording: CallRecordingEntity,
        fields: ExportFieldSelection
    ): String {
        val headers = mutableListOf<String>()
        val values = mutableListOf<String>()

        if (fields.includeDateTime) {
            headers.add("Date_Time")
            values.add(formatReadableDate(recording.timestamp))
            headers.add("Duration_Seconds")
            values.add(recording.durationSeconds.toString())
        }
        if (fields.includeParticipants) {
            headers.add("Contact_Name")
            values.add(recording.contactName)
            headers.add("Phone_Number")
            values.add(recording.phoneNumber)
            headers.add("Call_Direction")
            values.add(recording.callDirection)
        }
        if (fields.includeCompliance) {
            headers.add("Compliance_Jurisdiction")
            values.add(recording.consentJurisdiction)
            headers.add("Audible_Alert_Played")
            values.add(recording.audibleNoticePlayed.toString())
        }
        if (fields.includeSummary) {
            headers.add("Summary_Length")
            values.add(recording.summaryLength)
            headers.add("Executive_Summary")
            values.add(recording.executiveSummary)
            headers.add("Key_Takeaways")
            values.add(recording.keyPointsList().joinToString(" | "))
        }
        if (fields.includeDecisionsAndDates) {
            headers.add("Decisions_Made")
            values.add(recording.decisionsMadeList().joinToString(" | "))
            headers.add("Important_Dates")
            values.add(recording.importantDatesList().joinToString(" | "))
        }
        if (fields.includeActionItems) {
            headers.add("Action_Items")
            values.add(
                recording.actionItemsList().joinToString(" | ") {
                    "${if (it.completed) "[x]" else "[ ]"} ${it.task} (${it.owner} - ${it.priority})"
                }
            )
        }
        if (fields.includeTags) {
            headers.add("Keyword_Tags")
            values.add(recording.keywordTagsList().joinToString(", ") { "#$it" })
        }
        if (fields.includeTranscript) {
            headers.add("Transcript")
            values.add(recording.transcript)
        }

        val headerRow = headers.joinToString(",") { escapeCsv(it) }
        val valueRow = values.joinToString(",") { escapeCsv(it) }
        return "$headerRow\n$valueRow\n"
    }

    private fun buildEvernoteEnexContent(
        recording: CallRecordingEntity,
        fields: ExportFieldSelection,
        notebookName: String
    ): String {
        val enexDateFmt = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val createdDate = enexDateFmt.format(Date(recording.timestamp))
        val title = escapeXml("Call with ${recording.contactName} ($notebookName)")

        val bodyHtml = buildString {
            if (fields.includeParticipants || fields.includeDateTime) {
                append("<h2>Call: ${escapeXml(recording.contactName)} (${escapeXml(recording.phoneNumber)})</h2>")
                if (fields.includeDateTime) {
                    append("<p><b>Date:</b> ${escapeXml(formatReadableDate(recording.timestamp))} | <b>Duration:</b> ${recording.durationSeconds}s</p>")
                }
            }
            if (fields.includeCompliance) {
                append("<p><b>Privacy Compliance:</b> ${escapeXml(recording.consentJurisdiction)} | <b>Audible Alert Played:</b> ${recording.audibleNoticePlayed}</p>")
            }
            append("<hr/>")
            if (fields.includeSummary) {
                append("<h3>AI Executive Summary (${escapeXml(recording.summaryLength)})</h3>")
                append("<p>${escapeXml(recording.executiveSummary)}</p>")
                val keyPoints = recording.keyPointsList()
                if (keyPoints.isNotEmpty()) {
                    append("<h4>Key Takeaways</h4><ul>")
                    keyPoints.forEach { append("<li>${escapeXml(it)}</li>") }
                    append("</ul>")
                }
            }
            if (fields.includeDecisionsAndDates) {
                val decisions = recording.decisionsMadeList()
                if (decisions.isNotEmpty()) {
                    append("<h3>Decisions Made</h3><ul>")
                    decisions.forEach { append("<li>${escapeXml(it)}</li>") }
                    append("</ul>")
                }
                val dates = recording.importantDatesList()
                if (dates.isNotEmpty()) {
                    append("<h3>Important Dates &amp; Deadlines</h3><ul>")
                    dates.forEach { append("<li>${escapeXml(it)}</li>") }
                    append("</ul>")
                }
            }
            if (fields.includeActionItems) {
                val actions = recording.actionItemsList()
                if (actions.isNotEmpty()) {
                    append("<h3>Action Items</h3><ul>")
                    actions.forEach {
                        append("<li><en-todo checked=\"${it.completed}\"/> <b>${escapeXml(it.task)}</b> (${escapeXml(it.owner)} - ${escapeXml(it.priority)})</li>")
                    }
                    append("</ul>")
                }
            }
            if (fields.includeTranscript) {
                append("<h3>Full Call Transcript</h3>")
                append("<div>${escapeXml(recording.transcript).replace("\n", "<br/>")}</div>")
            }
        }

        val tagsXml = if (fields.includeTags) {
            recording.keywordTagsList().joinToString("\n") { "    <tag>${escapeXml(it)}</tag>" }
        } else {
            ""
        }

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE en-export SYSTEM "http://xml.evernote.com/pub/evernote-export3.dtd">
            <en-export export-date="$createdDate" application="VoxScribe" version="1.0">
              <note>
                <title>$title</title>
                <content>
                  <![CDATA[<?xml version="1.0" encoding="UTF-8" standalone="no"?>
                  <!DOCTYPE en-note SYSTEM "http://xml.evernote.com/pub/enml2.dtd">
                  <en-note>
                    $bodyHtml
                  </en-note>
                  ]]>
                </content>
                <created>$createdDate</created>
            $tagsXml
              </note>
            </en-export>
        """.trimIndent()
    }

    private fun buildNotionPagePayload(
        recording: CallRecordingEntity,
        databaseId: String,
        fields: ExportFieldSelection
    ) = buildJsonObject {
        putJsonObject("parent") {
            put("database_id", databaseId)
        }
        putJsonObject("properties") {
            putJsonObject("Name") {
                putJsonArray("title") {
                    add(
                        buildJsonObject {
                            putJsonObject("text") {
                                put("content", "Call with ${recording.contactName} (${recording.phoneNumber})")
                            }
                        }
                    )
                }
            }
        }
        put("children", buildNotionChildrenBlocks(recording, fields))
    }

    private fun buildNotionChildrenBlocks(
        recording: CallRecordingEntity,
        fields: ExportFieldSelection
    ): JsonArray = buildJsonArray {
        if (fields.includeCompliance || fields.includeTags) {
            add(
                buildJsonObject {
                    put("object", "block")
                    put("type", "callout")
                    putJsonObject("callout") {
                        putJsonArray("rich_text") {
                            add(
                                buildJsonObject {
                                    put("type", "text")
                                    putJsonObject("text") {
                                        put(
                                            "content",
                                            "Compliance: ${recording.consentJurisdiction} | Tags: ${
                                                recording.keywordTagsList().joinToString(", ") { "#$it" }
                                            }"
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
        if (fields.includeSummary) {
            add(
                buildJsonObject {
                    put("object", "block")
                    put("type", "paragraph")
                    putJsonObject("paragraph") {
                        putJsonArray("rich_text") {
                            add(
                                buildJsonObject {
                                    put("type", "text")
                                    putJsonObject("text") {
                                        put("content", recording.executiveSummary.take(1900))
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
        if (fields.includeActionItems) {
            recording.actionItemsList().forEach { item ->
                add(
                    buildJsonObject {
                        put("object", "block")
                        put("type", "to_do")
                        putJsonObject("to_do") {
                            put("checked", JsonPrimitive(item.completed))
                            putJsonArray("rich_text") {
                                add(
                                    buildJsonObject {
                                        put("type", "text")
                                        putJsonObject("text") {
                                            put("content", "${item.task} (${item.owner} - ${item.priority})")
                                        }
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    private fun buildTargetedOrChooserIntent(
        targetPackage: String?,
        subject: String,
        bodyText: String,
        fileUri: android.net.Uri?,
        mimeType: String,
        chooserTitle: String
    ): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TITLE, subject)
            putExtra(Intent.EXTRA_TEXT, bodyText)
            if (fileUri != null) {
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        if (!targetPackage.isNullOrBlank()) {
            val targeted = Intent(sendIntent).apply {
                setPackage(targetPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (targeted.resolveActivity(context.packageManager) != null) {
                return targeted
            }
        }

        return Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun formatReadableDate(timestamp: Long): String {
        val fmt = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        return fmt.format(Date(timestamp))
    }

    private fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    private fun escapeCsv(value: String): String {
        val sanitized = value.replace("\"", "\"\"").replace("\r\n", " ").replace("\n", " ")
        return "\"$sanitized\""
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
