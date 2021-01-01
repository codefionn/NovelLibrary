package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.afollestad.materialdialogs.MaterialDialog
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.getAllNovelSections
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.databinding.ActivitySettingsBinding
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleWidgetBinding
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.util.system.startSyncLoginActivity
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import java.util.ArrayList

class SyncSettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {
        const val POSITION_ENABLE = 0
        const val POSITION_STATUS = 1
        const val POSITION_LOG_IN = 2
        const val POSITION_ADD_NOVELS = 3
        const val POSITION_DELETE_NOVELS = 4
        const val POSITION_BOOKMARKS = 5
        const val POSITION_MAKE_SYNC = 6
//        const val POSITION_FETCH_NOVELS = 7
//        const val POSITION_FETCH_BOOKMARKS = 8
//        const val POSITION_FETCH_SECTIONS = 9
//        const val POSITION_MAKE_FETCH = 10
        const val POSITION_FORGET = 7 // 11
    }

    lateinit var adapter: GenericAdapter<String>
    private lateinit var settingsItems: ArrayList<String>
    private lateinit var settingsItemsDescriptions: ArrayList<String>

    private lateinit var novelSync: NovelSync
    
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sync = NovelSync.getInstance(intent.getStringExtra("url")!!, true)
        if (sync == null) {
            finish()
            return
        }
        novelSync = sync
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }

    private fun setRecyclerView() {
        settingsItems = ArrayList(resources.getStringArray(R.array.sync_options).asList())
        settingsItemsDescriptions = ArrayList(resources.getStringArray(R.array.sync_options_descriptions).asList())
        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_title_subtitle_widget, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleWidgetBinding.bind(itemView)
        itemBinding.blackOverlay.visibility = View.INVISIBLE
        itemBinding.widgetChevron.visibility = View.INVISIBLE
        itemBinding.widgetSwitch.visibility = View.INVISIBLE
        itemBinding.currentValue.visibility = View.INVISIBLE

        itemBinding.title.applyFont(assets).text = item
        itemBinding.subtitle.applyFont(assets).text = settingsItemsDescriptions[position]

        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))

        itemBinding.widgetSwitch.setOnCheckedChangeListener(null)
        when(position) {
            POSITION_ENABLE -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.getSyncEnabled(novelSync.host)
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, isChecked ->
                    dataCenter.setSyncEnabled(novelSync.host, isChecked)
//                    if (isChecked) {
//                        // TODO: Ask to perform full sync if logged in
//                    }
                }
            }
            POSITION_STATUS -> {
                itemBinding.widgetChevron.visibility = View.VISIBLE
                if (novelSync.loggedIn()) {
                    itemBinding.widgetChevron.setImageResource(R.drawable.ic_check_circle_white_vector)
                    itemBinding.widgetChevron.imageTintList = ContextCompat.getColorStateList(this, R.color.colorStateGreen)
                    itemBinding.subtitle.text = getString(R.string.logged_in)
                } else {
                    itemBinding.widgetChevron.setImageResource(R.drawable.ic_warning_white_vector)
                    itemBinding.widgetChevron.imageTintList = ContextCompat.getColorStateList(this, R.color.colorStateOrange)
                    itemBinding.subtitle.text = getString(R.string.not_logged_in)
                }
            }
            POSITION_LOG_IN -> {
                itemBinding.widgetChevron.visibility = View.VISIBLE
            }
            POSITION_ADD_NOVELS -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.getSyncAddNovels(novelSync.host)
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, isChecked -> dataCenter.setSyncAddNovels(novelSync.host, isChecked) }
            }
            POSITION_DELETE_NOVELS -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.getSyncDeleteNovels(novelSync.host)
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, isChecked -> dataCenter.setSyncDeleteNovels(novelSync.host, isChecked) }
            }
            POSITION_BOOKMARKS -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.getSyncBookmarks(novelSync.host)
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, isChecked -> dataCenter.setSyncBookmarks(novelSync.host, isChecked) }
            }
            POSITION_MAKE_SYNC -> {
                itemBinding.widgetChevron.visibility = View.VISIBLE
                itemBinding.widgetChevron.setImageResource(R.drawable.ic_sync_white_vector)
            }
            POSITION_FORGET -> {
                itemBinding.widgetChevron.visibility = View.VISIBLE
                itemBinding.widgetChevron.setImageResource(R.drawable.ic_delete_white_vector)
            }
        }
    }

    override fun onItemClick(item: String, position: Int) {
        when (position) {
            POSITION_LOG_IN -> startSyncLoginActivity(novelSync.getLoginURL(), novelSync.getCookieLookupRegex())
            POSITION_MAKE_SYNC -> {
                if (novelSync.loggedIn()) {
                    MaterialDialog.Builder(this)
                        .title(R.string.confirm_full_sync)
                        .content(R.string.confirm_full_sync_description)
                        .positiveText(R.string.okay)
                        .negativeText(R.string.cancel)
                        .onPositive { _, _ -> makeFullSync() }
                        .show()
                }
            }
            POSITION_FORGET -> {
                if (novelSync.loggedIn()) {
                    MaterialDialog.Builder(this)
                        .title(R.string.confirm_forget_cookies)
                        .content(R.string.confirm_forget_cookies_description)
                        .positiveText(R.string.okay)
                        .negativeText(R.string.cancel)
                        .autoDismiss(true)
                        .onPositive { _, _ ->
                            novelSync.forget()
                            adapter.notifyDataSetChanged()
                        }
                        .show()
                }
            }
        }
    }

    private fun makeFullSync() {
        val sections = dbHelper.getAllNovelSections()
        val novels = dbHelper.getAllNovels().filter { it.url.contains(novelSync.host) }
        val dialog = MaterialDialog.Builder(this)
            .title(R.string.sync_in_progress)
            .content(R.string.please_wait)
            .progress(true, 1)
            .show()

        var counter = 0
        val total = novels.count()
        novelSync.batchAdd(novels, sections) { novelName ->
            if (counter == total) {
                dialog.dismiss()
            } else {
                dialog.setTitle(getString(R.string.sync_batch_progress_title, ++counter, total))
                dialog.setContent(getString(R.string.sync_batch_progress, novelName))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}