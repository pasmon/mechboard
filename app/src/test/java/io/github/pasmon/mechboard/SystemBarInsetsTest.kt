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
        // The list is kept clear either by placing its container between the bars
        // or by padding a container that fills the window; both must end up the same.
        val listTop = settings.topInWindow() + settings.paddingTop
        val listBottom = settings.topInWindow() + settings.height - settings.paddingBottom
        assertEquals("action bar starts below the status bar", STATUS_BAR, actionBar.topInWindow())
        assertEquals("list starts below the action bar", STATUS_BAR + actionBar.height, listTop)
        assertEquals("list ends above the navigation bar", HEIGHT - NAVIGATION_BAR, listBottom)
    }

    @Test
    fun `settings list pads itself by the bars when the action bar overlays it`() {
        // On Android 15 and later the window draws edge to edge: the content fills
        // the window and is told how much of it the system and action bars cover.
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        val settings = activity.findViewById<View>(R.id.settings_container)
        val covered = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, STATUS_BAR + ACTION_BAR, 0, 0))
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, NAVIGATION_BAR))
            .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(CUTOUT, 0, 0, 0))
            .build()
        ViewCompat.dispatchApplyWindowInsets(settings, covered)

        assertEquals(STATUS_BAR + ACTION_BAR, settings.paddingTop)
        assertEquals(NAVIGATION_BAR, settings.paddingBottom)
        assertEquals(CUTOUT, settings.paddingLeft)
        assertEquals(0, settings.paddingRight)
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

    @Test
    fun `keyboard pads itself by its own navigation buttons, which count as a caption bar`() {
        val service = Robolectric.setupService(MechboardService::class.java)
        val root = service.onCreateInputView()
        // With gesture navigation the system draws the switch and hide buttons
        // inside the keyboard window and reports them as caption bar insets.
        val imeNavigationBar = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.captionBar(), Insets.of(0, 0, 0, NAVIGATION_BAR))
            .build()
        ViewCompat.dispatchApplyWindowInsets(root, imeNavigationBar)

        assertEquals(NAVIGATION_BAR, root.paddingBottom)
    }

    @Test
    fun `a new keyboard view asks for the insets once attached`() {
        // A window sends insets only when they change or a view asks for them, and
        // the keyboard view is replaced (e.g. on a theme change) while the window
        // keeps its insets. Without asking, the new view would never be padded.
        val activity = Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        val parent = RequestCountingFrame(activity)
        activity.setContentView(parent)
        val service = Robolectric.setupService(MechboardService::class.java)
        val root = service.onCreateInputView()
        val before = parent.requests

        parent.addView(root)

        assertEquals(before + 1, parent.requests)
    }

    /** Counts the requests for insets that reach it from its children. */
    private class RequestCountingFrame(context: android.content.Context) : android.widget.FrameLayout(context) {
        var requests = 0

        @Deprecated("Deprecated in Java")
        override fun requestFitSystemWindows() {
            requests++
            @Suppress("DEPRECATION")
            super.requestFitSystemWindows()
        }
    }

    private companion object {
        const val WIDTH = 1080
        const val HEIGHT = 2400
        const val STATUS_BAR = 80
        const val ACTION_BAR = 150
        const val NAVIGATION_BAR = 120
        const val CUTOUT = 90
    }
}
