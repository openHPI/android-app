package de.xikolo.testing.instrumented.unit.download

import android.Manifest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import de.xikolo.controllers.downloads.DownloadsActivity
import de.xikolo.testing.instrumented.mocking.base.BaseTest
import org.junit.Rule

// Parallel test execution needs to be disabled for all downloading tests, because it can occur
// that @Before deleteAllDownloads is called while another test is still being executed.
abstract class BaseDownloadTest : BaseTest() {

    @Rule
    @JvmField
    var activityTestRule =
        ActivityTestRule(DownloadsActivity::class.java, false, true)

    @Rule
    @JvmField
    var permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

}
