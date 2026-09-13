package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.RecordingEntity
import com.example.ui.screens.RecordingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class RecordingsGalleryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleRecordings = listOf(
        RecordingEntity(
            id = 101L,
            title = "PUBG Epic Victory",
            filePath = "/sdcard/pubg.mp4",
            durationMs = 65000L,
            fileSizeBytes = 25 * 1024 * 1024L,
            resolution = "1080p FHD",
            format = "MP4",
            fps = 60,
            bitrateMbps = 16,
            audioSource = "Mic + System Audio"
        ),
        RecordingEntity(
            id = 102L,
            title = "Tutorial On Jetpack Compose",
            filePath = "/sdcard/compose.mkv",
            durationMs = 120000L,
            fileSizeBytes = 40 * 1024 * 1024L,
            resolution = "2K QHD",
            format = "MKV",
            fps = 30,
            bitrateMbps = 24,
            audioSource = "Microphone"
        )
    )

    @Test
    fun galleryGrid_displaysVideosAndThumbnails() {
        var playedRecording: RecordingEntity? = null
        var deletedRecording: RecordingEntity? = null
        var sharedRecording: RecordingEntity? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                RecordingsScreen(
                    recordings = sampleRecordings,
                    onPlay = { playedRecording = it },
                    onTrim = {},
                    onBackup = {},
                    onDelete = { deletedRecording = it },
                    onShare = { sharedRecording = it },
                    cloudSyncInProgressId = null,
                    cloudSyncProgress = 0f
                )
            }
        }

        // Verify gallery grid layout is displayed
        composeTestRule.onNodeWithTag("gallery_grid_view").assertIsDisplayed()

        // Verify video item exists in grid
        composeTestRule.onNodeWithText("PUBG Epic Victory").assertExists()

        // Verify play button triggers onPlay
        composeTestRule.onNodeWithTag("play_button_101").performClick()
        assertEquals(101L, playedRecording?.id)

        // Verify share button triggers onShare
        composeTestRule.onNodeWithTag("share_button_101").performClick()
        assertEquals(101L, sharedRecording?.id)

        // Verify delete button opens confirmation dialog and confirms
        composeTestRule.onNodeWithTag("delete_button_101").performClick()
        composeTestRule.onNodeWithTag("confirm_delete_button").performClick()
        assertEquals(101L, deletedRecording?.id)
    }

    @Test
    fun galleryGrid_viewModeToggle_switchesToListAndBack() {
        composeTestRule.setContent {
            MyApplicationTheme {
                RecordingsScreen(
                    recordings = sampleRecordings,
                    onPlay = {},
                    onTrim = {},
                    onBackup = {},
                    onDelete = {},
                    onShare = {},
                    cloudSyncInProgressId = null,
                    cloudSyncProgress = 0f
                )
            }
        }

        // Initially in Grid view
        composeTestRule.onNodeWithTag("gallery_grid_view").assertIsDisplayed()

        // Switch to List view
        composeTestRule.onNodeWithTag("view_mode_list_button").performClick()
        composeTestRule.onNodeWithTag("gallery_list_view").assertIsDisplayed()

        // Switch back to Grid view
        composeTestRule.onNodeWithTag("view_mode_grid_button").performClick()
        composeTestRule.onNodeWithTag("gallery_grid_view").assertIsDisplayed()
    }

    @Test
    fun galleryGrid_downloadButton_triggersCallback() {
        var downloadedRecording: RecordingEntity? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                RecordingsScreen(
                    recordings = sampleRecordings,
                    onPlay = {},
                    onTrim = {},
                    onBackup = {},
                    onDelete = {},
                    onShare = {},
                    onDownload = { downloadedRecording = it },
                    cloudSyncInProgressId = null,
                    cloudSyncProgress = 0f
                )
            }
        }

        // Click download button on first recording
        composeTestRule.onNodeWithTag("download_button_101").assertIsDisplayed().performClick()
        assertEquals(101L, downloadedRecording?.id)
    }
}
