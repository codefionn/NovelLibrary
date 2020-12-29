package io.github.gmathi.novellibrary.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.MotionEventCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import com.afollestad.materialdialogs.MaterialDialog
import com.google.firebase.analytics.ktx.logEvent
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extensions.FAC
import io.github.gmathi.novellibrary.extensions.showEmpty
import io.github.gmathi.novellibrary.extensions.showLoading
import io.github.gmathi.novellibrary.model.database.NovelSection
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.SimpleItemTouchHelperCallback
import io.github.gmathi.novellibrary.util.view.SimpleItemTouchListener
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_novel_sections.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_novel_section.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NovelSectionsActivity : BaseActivity(), GenericAdapter.Listener<NovelSection>, SimpleItemTouchListener {

    lateinit var adapter: GenericAdapter<NovelSection>
    private lateinit var touchHelper: ItemTouchHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_novel_sections)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
        progressLayout.showLoading()
        setData()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(ArrayList(), R.layout.listitem_novel_section, this)
        val callback = SimpleItemTouchHelperCallback(this)
        touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(recyclerView)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        swipeRefreshLayout.isEnabled = false
    }

    private fun setData() {
        updateOrderIds()
        val novelSections = dbHelper.getAllNovelSections()
        adapter.updateData(ArrayList(novelSections))
        if (swipeRefreshLayout != null && progressLayout != null) {
            swipeRefreshLayout.isRefreshing = false
            progressLayout.showContent()
        }
        if (novelSections.isEmpty())
            progressLayout.showEmpty(
                resId = R.drawable.ic_info_white_vector,
                isLottieAnimation = false,
                emptyText = getString(R.string.novel_section_empty_message),
                buttonText = getString(R.string.add_novel_section),
                onClickListener = View.OnClickListener {
                    addNovelSection()
                })
    }

    private fun updateOrderIds() {
        if (adapter.items.isNotEmpty())
            for (i in 0 until adapter.items.size) {
                dbHelper.updateNovelSectionOrderId(adapter.items[i].id, i.toLong())
            }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun bind(item: NovelSection, itemView: View, position: Int) {
        itemView.novelSectionTitle.text = item.name

        itemView.reorderButton.setOnTouchListener { _, event ->
            @Suppress("DEPRECATION")
            if (MotionEventCompat.getActionMasked(event) ==
                MotionEvent.ACTION_DOWN
            ) {
                touchHelper.startDrag(recyclerView.getChildViewHolder(itemView))
            }
            false
        }

        itemView.popMenu.setOnClickListener {
            val popup = PopupMenu(this@NovelSectionsActivity, it)
            popup.menuInflater.inflate(R.menu.menu_popup_novel_section, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_novel_section_rename -> {
                        onItemRename(position)
                        true
                    }
                    R.id.action_novel_section_remove -> {
                        onItemRemove(position)
                        true
                    }
                    else -> {
                        true
                    }
                }
            }
            popup.show()
        }

        itemView.setBackgroundColor(
            if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
            else ContextCompat.getColor(this, android.R.color.transparent)
        )
    }

    override fun onItemClick(item: NovelSection, position: Int) {
        //Do Nothing
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_novel_section, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        else if (item.itemId == R.id.action_add_novel_section) addNovelSection()
        return super.onOptionsItemSelected(item)
    }

    override fun onItemMove(source: Int, target: Int) {
        adapter.onItemMove(source, target)
    }

    override fun onItemDismiss(viewHolderPosition: Int) {
        onItemRemove(viewHolderPosition)
    }

    private fun onItemRemove(position: Int) {
        MaterialDialog.Builder(this)
            .title(getString(R.string.confirm_remove))
            .content(getString(R.string.confirm_remove_description_novel_section))
            .positiveText(R.string.remove)
            .negativeText(R.string.cancel)
            .onPositive { dialog, _ ->
                run {
                    val novelSection = adapter.items[position]
                    if (novelSection.id != -1L) {
                        dbHelper.getAllNovels(novelSection.id).forEach {
                            dbHelper.updateNovelSectionId(it.id, -1L)
                            NovelSync.getInstance(it)?.applyAsync(lifecycleScope) { sync -> if (dataCenter.getSyncAddNovels(sync.host)) sync.updateNovel(it, null) }
                        }
                        dbHelper.deleteNovelSection(novelSection.id)
                    }
                    setData()
                    launchFirebase {
                        firebaseAnalytics.await().logEvent(FAC.Event.REMOVE_NOVEL_SECTION) {
                            param(FAC.Param.NOVEL_SECTION_NAME, novelSection.name ?: "N/A")
                        }
                    }
                    dialog.dismiss()
                }
            }
            .onNegative { dialog, _ ->
                run {
                    adapter.notifyDataSetChanged()
                    dialog.dismiss()
                }
            }
            .show()
    }

    private fun onItemRename(position: Int) {
        val novelSection = adapter.items[position]
        MaterialDialog.Builder(this)
            .title(getString(R.string.rename))
            .input(getString(R.string.novel_section_name), novelSection.name) { dialog, input ->
                val newName = input.trim().toString()
                if (newName.isNotBlank() && dbHelper.getNovelSection(newName) == null) {
                    dbHelper.updateNovelSectionName(novelSection.id, newName)
                    setData()
                    val oldName = novelSection.name
                    lifecycleScope.launch {
                        NovelSync.getAllInstances().forEach {
                            withContext(Dispatchers.IO) { if (dataCenter.getSyncAddNovels(it.host)) it.renameSection(novelSection, oldName, newName) }
                        }
                    }
                    launchFirebase {
                        firebaseAnalytics.await().logEvent(FAC.Event.RENAME_NOVEL_SECTION) {
                            param(FAC.Param.NOVEL_SECTION_NAME, newName)
                        }
                    }
                } else {
                    MaterialDialog.Builder(this@NovelSectionsActivity).content(getString(R.string.novel_section_name_error)).show()
                }
                dialog.dismiss()
            }.show()
    }

    private fun addNovelSection() {
        MaterialDialog.Builder(this)
            .title(getString(R.string.add_novel_section_title))
            .content(getString(R.string.add_novel_section_description))
            .input(getString(R.string.novel_section_name), "") { dialog, input ->
                val name = input.trim().toString()
                if (name.isNotBlank() && dbHelper.getNovelSection(name) == null) {
                    dbHelper.createNovelSection(name)
                    setData()
                    launchFirebase {
                        firebaseAnalytics.await().logEvent(FAC.Event.ADD_NOVEL_SECTION) {
                            param(FAC.Param.NOVEL_SECTION_NAME, name)
                        }
                    }
                } else {
                    MaterialDialog.Builder(this@NovelSectionsActivity).content(getString(R.string.novel_section_name_error)).show()
                }
                dialog.dismiss()
            }.show()
    }

    override fun onPause() {
        super.onPause()
        updateOrderIds()
    }

}
