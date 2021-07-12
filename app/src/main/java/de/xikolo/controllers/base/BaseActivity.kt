package de.xikolo.controllers.base

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import butterknife.ButterKnife
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.google.android.gms.cast.framework.IntroductoryOverlay
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.yatatsu.autobundle.AutoBundle
import de.xikolo.App
import de.xikolo.R
import de.xikolo.extensions.observe
import de.xikolo.utils.NotificationUtil
import de.xikolo.utils.extensions.hasPlayServices

abstract class BaseActivity : AppCompatActivity(), CastStateListener {

    protected var actionBar: ActionBar? = null

    protected var toolbar: Toolbar? = null

    protected var appBar: AppBarLayout? = null

    private var castContext: CastContext? = null

    private var drawerLayout: DrawerLayout? = null

    private var contentLayout: CoordinatorLayout? = null

    private var mediaRouteMenuItem: MenuItem? = null

    private var offlineModeToolbar: Boolean = false

    private var overlay: IntroductoryOverlay? = null

    private var translucentActionbar = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            // restore
            AutoBundle.bind(this, savedInstanceState)
        } else {
            AutoBundle.bind(this)
        }

        offlineModeToolbar = true

        try {
            if (this.hasPlayServices) {
                castContext = CastContext.getSharedInstance(this)
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }

        if (overlay == null) {
            showOverlay()
        }

        handleIntent(intent)

        App.instance.state.connectivity.observe(this) {
            this.onConnectivityChange(it)
        }
    }

    override fun setContentView(@LayoutRes layoutResID: Int) {
        super.setContentView(layoutResID)
        ButterKnife.bind(this, findViewById<View>(android.R.id.content))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        try {
            if (this.hasPlayServices) {
                castContext = CastContext.getSharedInstance(this)
                menuInflater.inflate(R.menu.cast, menu)
                mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(
                    applicationContext,
                    menu,
                    R.id.media_route_menu_item)
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }

        return true
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        setIntent(intent)
        AutoBundle.bind(this)

        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()

        if (castContext != null) {
            castContext?.addCastStateListener(this)
            setupCastMiniController()
        }
    }

    override fun onPause() {
        super.onPause()
        App.instance.syncCookieSyncManager()
    }

    override fun onStop() {
        super.onStop()

        castContext?.removeCastStateListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        AutoBundle.pack(this, outState)
    }

    private fun showOverlay() {
        overlay?.remove()
        if (mediaRouteMenuItem?.isVisible == true) {
            Handler().postDelayed({
                if (mediaRouteMenuItem != null && mediaRouteMenuItem?.isVisible == true) {
                    overlay = IntroductoryOverlay.Builder(this@BaseActivity, mediaRouteMenuItem)
                        .setTitleText(R.string.intro_overlay_text)
                        .setSingleTime()
                        .setOnOverlayDismissedListener { overlay = null }
                        .build()
                    overlay?.show()
                }
            }, 1000)
        }
    }

    override fun onCastStateChanged(newState: Int) {
        if (newState != CastState.NO_DEVICES_AVAILABLE) {
            showOverlay()
        }
    }

    @JvmOverloads
    protected fun setupActionBar(translucentActionbar: Boolean = false) {
        this.translucentActionbar = translucentActionbar
        val tb = findViewById<Toolbar>(R.id.toolbar)
        if (tb != null) {
            setSupportActionBar(tb)
        }

        toolbar = tb

        actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeButtonEnabled(true)

        drawerLayout = findViewById(R.id.drawer_layout)
        contentLayout = findViewById(R.id.contentLayout)
        appBar = findViewById(R.id.appbar)
        setColorScheme(R.color.toolbar, R.color.statusbar)
    }

    private fun setupCastMiniController(): Boolean {
        return if (this.hasPlayServices && findViewById<View>(R.id.miniControllerContainer) != null) {
            var container = findViewById<View>(R.id.miniControllerContainer)
            val parent = container.parent as ViewGroup
            val index = parent.indexOfChild(container)

            parent.removeView(container)
            container = layoutInflater.inflate(R.layout.container_mini_controller, parent, false)
            parent.addView(container, index)

            true
        } else {
            false
        }
    }

    protected fun setActionBarElevation(elevation: Float) {
        actionBar?.elevation = elevation
    }

    protected fun setAppBarExpanded(expanded: Boolean) {
        appBar?.setExpanded(expanded, false)
    }

    fun setScrollingBehavior(hide: Boolean) {
        toolbar?.layoutParams = (toolbar?.layoutParams as? AppBarLayout.LayoutParams)?.apply {
            scrollFlags = SCROLL_FLAG_ENTER_ALWAYS or SCROLL_FLAG_SNAP or if (hide) SCROLL_FLAG_SCROLL else 0
        }
    }

    protected fun enableOfflineModeToolbar(enable: Boolean) {
        this.offlineModeToolbar = enable
    }

    protected fun setColorScheme(toolbarColor: Int, statusbarColor: Int) {
        if (!translucentActionbar) {
            toolbar?.let { toolbar ->
                toolbar.setBackgroundColor(ContextCompat.getColor(this, toolbarColor))

                drawerLayout?.setStatusBarBackgroundColor(ContextCompat.getColor(this, toolbarColor))
                if (drawerLayout == null) {
                    window.statusBarColor = ContextCompat.getColor(this, statusbarColor)
                }

                contentLayout?.setBackgroundColor(ContextCompat.getColor(this, toolbarColor))
            }
        }
    }

    open fun onConnectivityChange(isOnline: Boolean) {
        if (offlineModeToolbar) {
            toolbar?.let {
                if (isOnline) {
                    it.subtitle = ""
                    setColorScheme(R.color.toolbar, R.color.statusbar)
                } else {
                    it.subtitle = getString(R.string.offline_mode)
                    setColorScheme(R.color.toolbar_offline, R.color.statusbar_offline)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            App.instance.state.permission.of(requestCode).granted()
        } else {
            App.instance.state.permission.of(requestCode).denied()
        }
    }

    protected fun enableCastMediaRouterButton(enable: Boolean) {
        mediaRouteMenuItem?.isVisible = enable
        invalidateOptionsMenu()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent != null) {
            NotificationUtil.getInstance(this).deleteDownloadNotificationsFromIntent(intent)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return if (this.hasPlayServices && castContext != null) {
            castContext?.onDispatchVolumeKeyEventBeforeJellyBean(event) == true || super.dispatchKeyEvent(event)
        } else {
            super.dispatchKeyEvent(event)
        }
    }

}
