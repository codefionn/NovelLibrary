package io.github.gmathi.novellibrary.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.UiThread
import android.view.MenuItem
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.databinding.ActivityNovelDownloadsBinding
import io.github.gmathi.novellibrary.databinding.ListitemDownloadQueueOldBinding
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.util.system.startDownloadNovelService
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.other.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.service.download.DownloadListener
import io.github.gmathi.novellibrary.service.download.DownloadNovelService
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.getGlideUrl
import io.github.gmathi.novellibrary.util.setDefaultsNoAnimation


class NovelDownloadsActivity : BaseActivity(), GenericAdapter.Listener<String>, DownloadListener {

    companion object {
        private const val TAG = "NovelDownloadsActivity"
    }

    private var downloadNovelService: DownloadNovelService? = null
    private var isServiceConnected: Boolean = false
    
    private lateinit var binding: ActivityNovelDownloadsBinding

    /** Defines callbacks for service binding, passed to bindService()  */
    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as DownloadNovelService.DownloadNovelBinder
            downloadNovelService = binder.getService()
            downloadNovelService?.downloadListener = this@NovelDownloadsActivity
            isServiceConnected = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            isServiceConnected = false
            downloadNovelService?.downloadListener = null
        }
    }

    lateinit var adapter: GenericAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNovelDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.fab.hide()
        setRecyclerView()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = dbHelper.getDownloadNovelNames() as ArrayList<String>, layoutResId = R.layout.listitem_download_queue_old, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaultsNoAnimation(adapter)
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: String, itemView: View, position: Int) {
        val binding = ListitemDownloadQueueOldBinding.bind(itemView)
        val novel = dbHelper.getNovel(item)
        if (!novel?.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                    .load(novel!!.imageUrl!!.getGlideUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .into(binding.novelImageView)
        }
        binding.novelTitleTextView.text = item
        //val downloadedPages = dbHelper.getDownloadedChapterCount(novel!!.id)

        if (dbHelper.hasDownloadsInQueue(item) && Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME)) {
            binding.playPauseImage.setImageResource(R.drawable.ic_pause_white_vector)
            binding.playPauseImage.tag = Download.STATUS_IN_QUEUE
            //val remainingDownloadsCount = dbHelper.getRemainingDownloadsCountForNovel(item)
            binding.novelProgressText.text = "Downloading: retrieving status…"
        } else {
            binding.playPauseImage.setImageResource(R.drawable.ic_play_arrow_white_vector)
            binding.playPauseImage.tag = Download.STATUS_PAUSED
            binding.novelProgressText.text = getString(R.string.download_paused)
        }

        binding.playPauseImage.setOnClickListener {
            when (binding.playPauseImage.tag) {
                Download.STATUS_PAUSED -> {
                    binding.playPauseImage.setImageResource(R.drawable.ic_pause_white_vector)
                    binding.playPauseImage.tag = Download.STATUS_IN_QUEUE
                    dbHelper.updateDownloadStatusNovelName(Download.STATUS_IN_QUEUE, item)
                    if (Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME)) {
                        downloadNovelService?.handleNovelDownload(item, DownloadNovelService.ACTION_START)
                    } else {
                        startDownloadNovelService(item)
                        bindService()
                    }
                }
                Download.STATUS_IN_QUEUE -> {
                    binding.playPauseImage.setImageResource(R.drawable.ic_play_arrow_white_vector)
                    binding.playPauseImage.tag = Download.STATUS_PAUSED
                    dbHelper.updateDownloadStatusNovelName(Download.STATUS_PAUSED, item)
                    binding.novelProgressText.text = getString(R.string.download_paused)
                    if (Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME))
                        downloadNovelService?.handleNovelDownload(item, DownloadNovelService.ACTION_PAUSE)
                }
            }
            //adapter.notifyDataSetChanged()
        }

        binding.deleteButton.setOnClickListener {
            confirmDeleteDialog(item)
        }
    }

    //Invoke it using --> notifyItemRangeChanged(positionStart, itemCount, payload);
    @SuppressLint("SetTextI18n")
    override fun bind(item: String, itemView: View, position: Int, payloads: MutableList<Any>?) {
        val binding = ListitemDownloadQueueOldBinding.bind(itemView)
        if (payloads == null || payloads.size == 0)
            bind(item, itemView, position)
        else {

            //Update the cells partially
            val downloadEvent = payloads[0]

            if (downloadEvent is DownloadWebPageEvent) {
                if (downloadEvent.download.novelName == item && binding.playPauseImage.tag != Download.STATUS_PAUSED) {
                    val remainingDownloadsCount = dbHelper.getRemainingDownloadsCountForNovel(item)
                    binding.novelProgressText.text = "Remaining: $remainingDownloadsCount, Current: ${downloadEvent.download.chapter}"
                }
            } else if (downloadEvent is DownloadNovelEvent) {
                if (downloadEvent.novelName == item) {
                    @Suppress("NON_EXHAUSTIVE_WHEN")
                    when (downloadEvent.type) {
                        EventType.PAUSED -> {
                            binding.novelProgressText.text = getString(R.string.download_paused)
                            binding.playPauseImage.setImageResource(R.drawable.ic_play_arrow_white_vector)
                            binding.playPauseImage.tag = Download.STATUS_PAUSED
                        }
                        EventType.INSERT -> {
                            binding.playPauseImage.setImageResource(R.drawable.ic_pause_white_vector)
                            binding.playPauseImage.tag = Download.STATUS_IN_QUEUE
                            val remainingDownloadsCount = dbHelper.getRemainingDownloadsCountForNovel(item)
                            binding.novelProgressText.text = "${getString(R.string.download_in_queue)} - Remaining: $remainingDownloadsCount"
                        }
                        EventType.RUNNING -> {
                            binding.playPauseImage.setImageResource(R.drawable.ic_pause_white_vector)
                            binding.playPauseImage.tag = Download.STATUS_IN_QUEUE
                            binding.novelProgressText.text = "Collecting Novel Information…"
                        }
                    }

                }
            }
        }
    }

    override fun onItemClick(item: String, position: Int) {
        //Do Nothing
    }

    private fun confirmDeleteDialog(novelName: String) {
        MaterialDialog(this).show {
            title(R.string.confirm_remove)
            message(R.string.confirm_remove_download_description)
            positiveButton(R.string.remove) { dialog ->
                this@NovelDownloadsActivity.run {
                    dbHelper.deleteDownloads(novelName)
                    adapter.removeItem(novelName)
                    if (isServiceConnected && Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME))
                        downloadNovelService?.handleNovelDownload(novelName, DownloadNovelService.ACTION_REMOVE)
                    dialog.dismiss()
                }
            }
            negativeButton(R.string.cancel) { dialog ->
                this@NovelDownloadsActivity.run {
                    dialog.dismiss()
                }
            }

            lifecycleOwner(this@NovelDownloadsActivity)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }


    @UiThread
    override fun handleEvent(downloadWebPageEvent: DownloadWebPageEvent) {
        binding.contentRecyclerView.recyclerView.post {
            //if (downloadWebPageEvent.type == EventType.COMPLETE) {
            val index = adapter.items.indexOf(downloadWebPageEvent.download.novelName)
            adapter.notifyItemRangeChanged(index, 1, downloadWebPageEvent)
            //}
        }
    }

    @UiThread
    override fun handleEvent(downloadNovelEvent: DownloadNovelEvent) {
        binding.contentRecyclerView.recyclerView.post {
            when (downloadNovelEvent.type) {
                EventType.INSERT, EventType.RUNNING, EventType.PAUSED -> {
                    val index = adapter.items.indexOf(downloadNovelEvent.novelName)
                    adapter.notifyItemRangeChanged(index, 1, downloadNovelEvent)
                }
                EventType.DELETE -> {
                    adapter.removeItem(downloadNovelEvent.novelName)
                    if (isServiceConnected && Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME))
                        downloadNovelService?.handleNovelDownload(downloadNovelEvent.novelName, DownloadNovelService.ACTION_REMOVE)
                }
                else -> {
                    //Do Nothing
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind to LocalService
        val isServiceRunning = Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME)
        if (isServiceRunning) {
            bindService()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isServiceConnected) {
            unbindService(mConnection)
            isServiceConnected = false
            downloadNovelService?.downloadListener = null
        }
    }

    private fun bindService() {
        Intent(this, DownloadNovelService::class.java).also { intent ->
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        }
    }


}
