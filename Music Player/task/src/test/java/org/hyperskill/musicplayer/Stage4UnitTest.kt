package org.hyperskill.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Button
import androidx.fragment.app.FragmentContainerView
import androidx.recyclerview.widget.RecyclerView
import org.hyperskill.musicplayer.internals.CustomMediaPlayerShadow
import org.hyperskill.musicplayer.internals.CustomShadowAsyncDifferConfig
import org.hyperskill.musicplayer.internals.CustomShadowCountDownTimer
import org.hyperskill.musicplayer.internals.FakeContentProvider
import org.hyperskill.musicplayer.internals.MusicPlayerUnitTests
import org.hyperskill.musicplayer.internals.SongFake
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration

// version 1.4
@Config(shadows = [CustomMediaPlayerShadow::class, CustomShadowCountDownTimer::class, CustomShadowAsyncDifferConfig::class])
@RunWith(RobolectricTestRunner::class)
class Stage4UnitTest : MusicPlayerUnitTests<MainActivity>(MainActivity::class.java) {


    companion object {
        const val expectedRequestCode = 1
    }

    private val mainButtonSearch by lazy {
        val view = activity.findViewByString<Button>("mainButtonSearch")

        val expectedText = "search"
        val actualText = view.text.toString().lowercase()
        Assert.assertEquals("wrong text for mainButtonSearch", expectedText, actualText)

        view
    }

    private val mainSongList by lazy {
        activity.findViewByString<RecyclerView>("mainSongList")
    }

    private val mainFragmentContainer by lazy {
        activity.findViewByString<FragmentContainerView>("mainFragmentContainer")
    }

    @Test
    fun testPermissionRequestGranted() {
        val fakeSongResult = SongFakeRepository.fakeSongData.dropLast(3)

        setupContentProvider(fakeSongResult)
        CustomMediaPlayerShadow.setFakeSong(fakeSongResult.first())

        testActivity {
            mainButtonSearch
            mainSongList

            mainButtonSearch.clickAndRun()
            assertRequestPermissions(listOf(Manifest.permission.READ_EXTERNAL_STORAGE))

            // grant permissions and invoke listener
            shadowActivity.grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
            activity.onRequestPermissionsResult(
                    expectedRequestCode,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    intArrayOf(PackageManager.PERMISSION_GRANTED)
            )
            shadowLooper.idleFor(Duration.ofSeconds(3))
            //


            mainSongList.assertListItems(fakeSongResult) { itemViewSupplier, position, song ->
                assertSongItem(
                        "After permission granted the list should load with song files data.",
                        itemViewSupplier(), song
                )
            }
        }
    }

    @Test
    fun testListStateOnPermissionRequestDenied() {
        val fakeSongResult = SongFakeRepository.fakeSongData
        setupContentProvider(fakeSongResult)

        testActivity {


            mainButtonSearch
            mainSongList

            FakeContentProvider.hasPermissionToReadExternalStorage = false
            mainButtonSearch.clickAndRun()

            assertRequestPermissions(listOf(Manifest.permission.READ_EXTERNAL_STORAGE))

            shadowActivity.denyPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
            activity.onRequestPermissionsResult(
                    expectedRequestCode,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    intArrayOf(PackageManager.PERMISSION_DENIED)
            )
            shadowLooper.idleFor(Duration.ofSeconds(3))

            mainSongList.assertListItems(listOf<SongFake>()) { _, _, _ -> /*implicitSizeAssertion*/ }
        }
    }


    @Test
    fun testToastShowsOnPermissionRequestDenied() {
        val fakeSongResult = SongFakeRepository.fakeSongData
        setupContentProvider(fakeSongResult)

        testActivity {

            mainButtonSearch
            mainSongList

            FakeContentProvider.hasPermissionToReadExternalStorage = false
            mainButtonSearch.clickAndRun()

            assertRequestPermissions(listOf(Manifest.permission.READ_EXTERNAL_STORAGE))

            shadowActivity.denyPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
            activity.onRequestPermissionsResult(
                    expectedRequestCode,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    intArrayOf(PackageManager.PERMISSION_DENIED)
            )
            shadowLooper.idleFor(Duration.ofSeconds(3))

            assertLastToastMessageEquals(
                    errorMessage = "On permission denial a Toast with warning message",
                    expectedMessage = "Songs cannot be loaded without permission"
            )
        }
    }

    @Test
    fun testPermissionRequestAgainGranted() {
        val fakeSongResult = SongFakeRepository.fakeSongData

        setupContentProvider(fakeSongResult)
        CustomMediaPlayerShadow.setFakeSong(fakeSongResult.first())

        testActivity {
            mainButtonSearch
            mainSongList

            FakeContentProvider.hasPermissionToReadExternalStorage = false
            mainButtonSearch.clickAndRun()

            assertRequestPermissions(listOf(Manifest.permission.READ_EXTERNAL_STORAGE))

            shadowActivity.denyPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
            activity.onRequestPermissionsResult(
                    expectedRequestCode,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    intArrayOf(PackageManager.PERMISSION_DENIED)
            )
            shadowLooper.runToEndOfTasks()

            mainSongList.assertListItems(listOf<SongFake>()) { _ , _ , _ -> /*implicitSizeAssertion*/ }

            FakeContentProvider.hasPermissionToReadExternalStorage = true
            mainButtonSearch.clickAndRun()
            assertRequestPermissions(listOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            shadowActivity.grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
            activity.onRequestPermissionsResult(
                    expectedRequestCode,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    intArrayOf(PackageManager.PERMISSION_GRANTED)
            )
            shadowLooper.runToEndOfTasks()

            mainSongList.assertListItems(fakeSongResult) { itemViewSupplier, position, song ->
                assertSongItem(
                        "After permission is granted songs should be loaded into mainSongList. Song",
                        itemViewSupplier(), song
                )

            }
        }
    }

    @Test
    fun testMusicFilesRetrievalAllFiles() {
        shadowActivity.grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
        val fakeSongResult = SongFakeRepository.fakeSongData
        setupContentProvider(fakeSongResult)
        CustomMediaPlayerShadow.setFakeSong(fakeSongResult.first())

        testActivity {
            mainButtonSearch
            mainSongList

            mainButtonSearch.clickAndRun()
            mainSongList.assertListItems(fakeSongResult) { itemViewSupplier, position, song ->
                assertSongItem(
                        "mainSongList content should be songs found on external storage. Song",
                        itemViewSupplier(), song
                )
            }
        }
    }

    @Test
    fun testMusicFilesRetrievalNoFiles() {
        shadowActivity.grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
        val fakeSongResult = listOf<SongFake>()
        setupContentProvider(fakeSongResult)

        testActivity {
            mainButtonSearch
            mainSongList
            mainSongList.assertListItems(fakeSongResult) { _, _, _ -> /*implicitSizeAssertion*/ }
        }
    }

    @After
    fun tearDown() {
        FakeContentProvider.hasPermissionToReadExternalStorage = true
    }
}