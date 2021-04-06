package de.xikolo.testing.instrumented.unit

import de.xikolo.download.DownloadIdentifier
import de.xikolo.download.DownloadItem
import de.xikolo.testing.instrumented.mocking.base.BaseTest

abstract class DownloadItemTest<T : DownloadItem<F, I>,
    F, I : DownloadIdentifier> : BaseTest() {

    /*@Rule
    @JvmField
    var activityTestRule =
        ActivityTestRule(MainActivity::class.java, false, true)

    @Rule
    @JvmField
    var permissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    abstract var testDownloadItem: T
    abstract var testDownloadItemNotDownloadable: T

    @Before
    fun deleteItem() {
        testDownloadItem.delete(activityTestRule.activity)
    }

    @Test
    fun testIsDownloadable() {
        assertTrue(testDownloadItem.isDownloadable)
        assertFalse(testDownloadItemNotDownloadable.isDownloadable)
    }

    @Test
    fun testStateBeforeDownload() {
        testDownloadItem.isDownloadRunning {
            assertFalse(it)

            assertNull(testDownloadItem.download)
            assertFalse(testDownloadItem.downloadExists)
        }
    }

    @Test
    fun testStartDownload() {
        var started = false
        var completed = false
        testDownloadItem.stateListener = object : DownloadItem.StateListener {
            override fun onStarted() {
                started = true
            }

            override fun onCompleted() {
                completed = true
            }

            override fun onDeleted() {}
        }

        testDownloadItem.start(activityTestRule.activity) {
            assertNotNull(it)

            assertTrue(started)
            testDownloadItem.isDownloadRunning {
                assertTrue(it)

                testDownloadItem.start(activityTestRule.activity) {
                    assertNull(it)

                    waitWhile({ !completed })

                    assertTrue(testDownloadItem.downloadExists)
                    assertNotNull(testDownloadItem.download)
                }
            }
        }
    }

    @Test
    fun testCancelDownload() {
        testDownloadItem.start(activityTestRule.activity) {
            assertNotNull(it)

            testDownloadItem.isDownloadRunning {
                assertTrue(it)

                testDownloadItem.cancel(activityTestRule.activity) {
                    assertTrue(it)

                    testDownloadItem.isDownloadRunning {
                        assertFalse(it)

                        assertFalse(testDownloadItem.downloadExists)
                        assertNull(testDownloadItem.download)
                    }
                }
            }
        }
    }

    @Test
    fun testDeleteDownload() {
        var deleted = true
        var completed = false
        testDownloadItem.stateListener = object : DownloadItem.StateListener {
            override fun onStarted() {}

            override fun onCompleted() {
                completed = true
            }

            override fun onDeleted() {
                deleted = true
            }
        }

        testDownloadItem.delete(activityTestRule.activity) {
            assertFalse(it)

            testDownloadItem.start(activityTestRule.activity) {
                assertNotNull(it)

                waitWhile({ !completed })

                assertTrue(testDownloadItem.downloadExists)

                testDownloadItem.delete(activityTestRule.activity) {
                    assertTrue(it)

                    assertTrue(deleted)
                    assertFalse(testDownloadItem.downloadExists)
                    assertNull(testDownloadItem.download)
                }
            }
        }
    }

    protected fun waitWhile(condition: () -> Boolean, timeout: Long = 300000) {
        var waited = 0
        while (condition()) {
            Thread.sleep(100)
            waited += 100
            if (waited > timeout) {
                throw Exception()
            }
        }
    }*/
}
