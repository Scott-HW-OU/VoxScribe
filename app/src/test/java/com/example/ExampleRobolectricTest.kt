package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ActionItem
import com.example.data.local.CallRecordingEntity
import com.example.data.remote.ExportFieldSelection
import com.example.data.remote.ExportFormatOption
import com.example.data.remote.ProductivityExportManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VoxScribe", appName)
    }

    @Test
    fun `formatCustomExport respects format and selected data fields`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val exportManager = ProductivityExportManager(context)

        val entity = CallRecordingEntity(
            id = 42L,
            contactName = "Elena Vance",
            phoneNumber = "+1-415-890-4312",
            callDirection = "INCOMING",
            timestamp = 1790408000000L,
            durationSeconds = 185,
            audioFilePath = null,
            consentJurisdiction = "Two-Party Consent",
            audibleNoticePlayed = true,
            consentScriptUsed = "This call is recorded.",
            consentTimestamp = 1790408000000L,
            transcript = "[00:02] Speaker 1: Let's finalize the October 15th launch.",
            executiveSummary = "Agreed on October 15th launch and Q4 seat expansion.",
            keyPointsJson = CallRecordingEntity.encodeStringList(listOf("15% volume discount")),
            decisionsMadeJson = CallRecordingEntity.encodeStringList(listOf("Expand to 200 seats")),
            importantDatesJson = CallRecordingEntity.encodeStringList(listOf("October 15th deadline")),
            actionItemsJson = CallRecordingEntity.encodeActionItems(
                listOf(ActionItem(task = "Send revised MSA", owner = "Scott", priority = "High"))
            ),
            keywordTagsJson = CallRecordingEntity.encodeStringList(listOf("Q4Budget", "ContractRenewal")),
            summaryLength = "DETAILED",
            geminiAccountEmail = "scott.harveywhittle@ou.ac.uk"
        )

        // Test CSV export with Transcript excluded
        val csvOutput = exportManager.formatCustomExport(
            recording = entity,
            format = ExportFormatOption.CSV,
            fields = ExportFieldSelection(includeTranscript = false, includeTags = true)
        )
        assertTrue(csvOutput.contains("Q4Budget"))
        assertTrue(csvOutput.contains("Elena Vance"))
        assertFalse(csvOutput.contains("Transcript"))

        // Test Markdown export with Decisions & Important Dates included
        val mdOutput = exportManager.formatCustomExport(
            recording = entity,
            format = ExportFormatOption.MARKDOWN,
            fields = ExportFieldSelection(includeDecisionsAndDates = true)
        )
        assertTrue(mdOutput.contains("Expand to 200 seats"))
        assertTrue(mdOutput.contains("October 15th deadline"))
    }
}
