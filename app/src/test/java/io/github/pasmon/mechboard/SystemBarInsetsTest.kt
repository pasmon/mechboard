package io.github.pasmon.mechboard

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * The app targets an Android version on which windows draw behind the status
 * and navigation bars. These tests check that content stays out from under them.
 */
@RunWith(RobolectricTestRunner::class)
class SystemBarInsetsTest {

    private val insets = WindowInsetsCompat.Builder()
        .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, STATUS_BAR, 0, 0))
        .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, NAVIGATION_BAR))
        .build()

    private fun layout(view: View) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, WIDTH, HEIGHT)
    }

    private fun View.topInWindow(): Int = IntArray(2).also(::getLocationInWindow)[1]

    @Test
    fun `settings screen keeps its action bar and list clear of the system bars`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        val decor = activity.window.decorView
        ViewCompat.dispatchApplyWindowInsets(decor, insets)
        layout(decor)

        val actionBar = activity.findViewById<View>(androidx.appcompat.R.id.action_bar_container)
        val settings = activity.findViewById<View>(R.id.settings_container)
        assertEquals("action bar starts below the status bar", STATUS_BAR, actionBar.topInWindow())
        assertEquals("list starts below the action bar", STATUS_BAR + actionBar.height, settings.topInWindow())
        assertEquals(
            "list ends above the navigation bar",
            HEIGHT - NAVIGATION_BAR, settings.topInWindow() + settings.height
        )
    }

    @Test
    fun `keyboard pads itself by the navigation bar but not the status bar`() {
        val service = Robolectric.setupService(MechboardService::class.java)
        val root = service.onCreateInputView()
        ViewCompat.dispatchApplyWindowInsets(root, insets)

        assertEquals(NAVIGATION_BAR, root.paddingBottom)
        assertEquals(0, root.paddingTop)
        assertEquals(0, root.paddingLeft)
        assertEquals(0, root.paddingRight)
    }

    @Test
    fun `keyboard pads its sides for a navigation bar or cutout on the side`() {
        val service = Robolectric.setupService(MechboardService::class.java)
        val root = service.onCreateInputView()
        val landscape = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, NAVIGATION_BAR, 0))
            .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(CUTOUT, 0, 0, 0))
            .build()
        ViewCompat.dispatchApplyWindowInsets(root, landscape)

        assertEquals(CUTOUT, root.paddingLeft)
        assertEquals(NAVIGATION_BAR, root.paddingRight)
        assertEquals(0, root.paddingBottom)
    }

    private companion object {
        const val WIDTH = 1080
        const val HEIGHT = 2400
        const val STATUS_BAR = 80
        const val NAVIGATION_BAR = 120
        const val CUTOUT = 90
    }
}
