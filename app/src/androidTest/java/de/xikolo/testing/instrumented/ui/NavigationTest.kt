package de.xikolo.testing.instrumented.ui


import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import de.xikolo.R
import de.xikolo.controllers.main.MainActivity
import de.xikolo.testing.instrumented.mocking.base.BaseMockedTest
import de.xikolo.testing.instrumented.ui.helper.AssertionHelper
import de.xikolo.testing.instrumented.ui.helper.NavigationHelper
import de.xikolo.testing.instrumented.ui.helper.NavigationHelper.WAIT_UI_ANIMATION
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test

@LargeTest
class NavigationTest : BaseMockedTest() {

    @Rule
    @JvmField
    var activityTestRule = ActivityTestRule(MainActivity::class.java)

    /**
     * Tests the behavior of the navigation drawer.
     * It consists of opening and closing via the hamburger menu and via swipe gestures.
     */
    @Test
    fun navigationTest() {
        NavigationHelper.openNavigation(context)

        pressBack()

        NavigationHelper.openNavigation(context)

        val drawerLayout = onView(
            allOf(
                withId(R.id.drawer_layout),
                isDisplayed()
            )
        )

        drawerLayout.perform(swipeLeft())
        Thread.sleep(WAIT_UI_ANIMATION)

        AssertionHelper.assertMainShown(context)
    }

}
