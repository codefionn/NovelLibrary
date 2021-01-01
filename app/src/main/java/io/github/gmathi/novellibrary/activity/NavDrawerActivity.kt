package io.github.gmathi.novellibrary.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.databinding.ActivityNavDrawerBinding
import io.github.gmathi.novellibrary.fragment.LibraryPagerFragment
import io.github.gmathi.novellibrary.fragment.SearchFragment
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.network.CloudFlareByPasser
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.system.*
import org.cryse.widget.persistentsearch.PersistentSearchView


class NavDrawerActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var snackBar: Snackbar? = null
    private var currentNavId: Int = R.id.nav_search

    private var cloudFlareLoadingDialog: MaterialDialog? = null

    lateinit var binding: ActivityNavDrawerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = io.github.gmathi.novellibrary.databinding.ActivityNavDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.navigationView.setNavigationItemSelectedListener(this)

        //Initialize custom logging
        currentNavId = if (dataCenter.loadLibraryScreen) R.id.nav_library else R.id.nav_search

        if (intent.hasExtra("currentNavId"))
            currentNavId = intent.getIntExtra("currentNavId", currentNavId)

        if (savedInstanceState != null && savedInstanceState.containsKey("currentNavId")) {
            currentNavId = savedInstanceState.getInt("currentNavId")
        }

        snackBar = Snackbar.make(binding.appBarNavDrawer.navFragmentContainer, getString(R.string.app_exit), Snackbar.LENGTH_SHORT)

        if (Utils.isConnectedToNetwork(this)) {
            checkForCloudFlare()
        } else {
            checkIntentForNotificationData()
            loadFragment(currentNavId)
            showWhatsNewDialog()
        }
        currentNavId = if (dataCenter.loadLibraryScreen) R.id.nav_library else R.id.nav_search

        if (intent.hasExtra("showDownloads")) {
            intent.removeExtra("showDownloads")
            startNovelDownloadsActivity()
        }

    }

    private fun showWhatsNewDialog() {
        if (dataCenter.appVersionCode < BuildConfig.VERSION_CODE) {
            MaterialDialog.Builder(this)
                .title("\uD83C\uDF89 What's New 0.16.beta!")
                .content(

                        "✨️ New Source - \"Neovel\"\n" +
                                "✨️ Add your own selector query for websites not yet discovered/supported. (Refer to \"Jsoup Selectors\" for more info)\n" +
                                "✨ Detailed Sync Status so you know the progress!\n" +
                            "✨ Add support for <abbr> and footnotes Keep IDs by default\n" +
//                            "⚠️ Fix - No Novels/Chapters loading issue due to ssl errors\n" +
//                            "⚠️ Fix - ActiveTranslations will work in reader mode\n" +
//                            "⚠️ Fix - Read Aloud bug for Wuxia novels\n" +
                            "\uD83D\uDEE0️ Other major/minor bug fixes.\n" +
//                            "\uD83D\uDEE0 Support for 3 more translation sites in reader mode.\n" +
//                            "\uD83D\uDEE0 Discord link updated.\n" +
//                                    "\uD83D\uDEE0 Bug Fixes for Recommendations not showing\n" +
//                            "✨ Read Aloud moved to bottom in the reader settings.\n" +
//                                    "✨ Added Hidden Buttons to unlock some hidden functionality!" +
//                            "\uD83D\uDEE0  Experimental - Added Hard Reset to Novel Popup menu to reset the novel." +
                            ""
                )
                .positiveText("Ok")
                .onPositive { dialog, _ -> dialog.dismiss() }
                .show()
            dataCenter.appVersionCode = BuildConfig.VERSION_CODE
        }
    }

    private fun checkForCloudFlare() {

        cloudFlareLoadingDialog = Utils
            .dialogBuilder(this@NavDrawerActivity, content = "If this is taking too long, You can skip and goto \"Settings\" -> \"CloudFlare Check\" to make the app work.", isProgress = true)
            .cancelable(false)
            .negativeText("Skip")
            .onNegative { _, _ ->
                loadFragment(currentNavId)
                showWhatsNewDialog()
                checkIntentForNotificationData()
            }
            .build()

        cloudFlareLoadingDialog?.show()

        CloudFlareByPasser.check(this, "novelupdates.com") { state ->

            if (!isDestroyed) {
                if (state == CloudFlareByPasser.State.CREATED || state == CloudFlareByPasser.State.UNNEEDED) {
                    if (cloudFlareLoadingDialog?.isShowing == true) {
                        loadFragment(currentNavId)
                        showWhatsNewDialog()
                        checkIntentForNotificationData()
                        cloudFlareLoadingDialog?.dismiss()
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            val existingSearchFrag = supportFragmentManager.findFragmentByTag(SearchFragment::class.toString())
            if (existingSearchFrag != null) {
                val searchView = existingSearchFrag.view?.findViewById<PersistentSearchView>(R.id.searchView)
                if (searchView != null && (searchView.isEditing || searchView.isSearching)) {
                    (existingSearchFrag as SearchFragment).closeSearch()
                    return
                }
            }

            if (snackBar != null && snackBar!!.isShown)
                finish()
            else {
                if (snackBar == null) snackBar = Snackbar.make(binding.appBarNavDrawer.navFragmentContainer, getString(R.string.app_exit), Snackbar.LENGTH_SHORT)
                snackBar?.show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        loadFragment(item.itemId)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun loadFragment(id: Int) {
        currentNavId = id
        when (id) {
            R.id.nav_library -> {
                replaceFragment(LibraryPagerFragment(), LibraryPagerFragment::class.toString())
            }
            R.id.nav_search -> {
                replaceFragment(SearchFragment(), SearchFragment::class.toString())
            }
            R.id.nav_downloads -> {
                startNovelDownloadsActivity()
                //replaceFragment(DownloadFragment(), DownloadFragment::class.toString())
            }
            R.id.nav_settings -> {
                startSettingsActivity()
            }
            R.id.nav_recently_viewed -> {
                startRecentlyViewedNovelsActivity()
            }
            R.id.nav_recently_updated -> {
                startRecentlyUpdatedNovelsActivity()
            }
            R.id.nav_discord_link -> {
                openInBrowser("https://discord.gg/cPMxEVn")
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.commit(true) {
            replace(binding.appBarNavDrawer.navFragmentContainer.id, fragment, tag)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            addToBackStack(tag)
        }
    }

    fun setToolbar(toolbar: Toolbar?) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_white_vector)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            requestCode == Constants.OPEN_FIREBASE_AUTH_UI -> {
            }
            resultCode == Constants.OPEN_DOWNLOADS_RES_CODE -> loadFragment(R.id.nav_downloads)
            requestCode == Constants.IWV_ACT_REQ_CODE -> checkIntentForNotificationData()
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkIntentForNotificationData() {
        if (intent.extras != null && intent.extras!!.containsKey("novel")) {
            val novel = intent.extras!!.getSerializable("novel") as? Novel
            novel?.let {
                intent.extras!!.remove("novel")
                startChaptersActivity(novel)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentNavId", currentNavId)
    }

    override fun onDestroy() {
        super.onDestroy()
    }


}
