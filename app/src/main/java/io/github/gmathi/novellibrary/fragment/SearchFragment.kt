package io.github.gmathi.novellibrary.fragment

import android.animation.Animator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.adapter.NavPageListener
import io.github.gmathi.novellibrary.adapter.SearchResultsListener
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.databinding.ActivityImportLibraryBinding
import io.github.gmathi.novellibrary.databinding.ActivityNavDrawerBinding
import io.github.gmathi.novellibrary.databinding.FragmentSearchBinding
import io.github.gmathi.novellibrary.extensions.FAC
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.view.SimpleAnimationListener
import io.github.gmathi.novellibrary.util.view.SuggestionsBuilder
import io.github.gmathi.novellibrary.util.addToNovelSearchHistory
import io.github.gmathi.novellibrary.util.system.hideSoftKeyboard
import org.cryse.widget.persistentsearch.PersistentSearchView
import org.cryse.widget.persistentsearch.SearchItem


class SearchFragment : BaseFragment() {

    lateinit var adapter: GenericAdapter<Novel>
    var searchMode: Boolean = false
    private var searchTerm: String? = null

    private lateinit var binding: FragmentSearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false) ?: return null
        binding = FragmentSearchBinding.bind(view)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setSearchView()

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("searchTerm"))
                searchTerm = savedInstanceState.getString("searchTerm")
            if (savedInstanceState.containsKey("searchMode"))
                searchMode = savedInstanceState.getBoolean("searchMode")
        }

        if (searchMode && searchTerm != null)
            searchNovels(searchTerm!!)
        else
            setViewPager()
    }

    private fun setViewPager() {
        while (childFragmentManager.backStackEntryCount > 0)
            childFragmentManager.popBackStack()
        searchTerm = null
        searchMode = false
        val titles = resources.getStringArray(R.array.search_tab_titles)
        val navPageAdapter = GenericFragmentStatePagerAdapter(childFragmentManager, titles, titles.size, NavPageListener())
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.adapter = navPageAdapter
        binding.tabStrip.setViewPager(binding.viewPager)
    }

    private fun setSearchView() {
        //searchView.setHomeButtonVisibility(View.GONE)
        binding.searchView.setHomeButtonListener {
            hideSoftKeyboard()
            if (activity != null && activity is NavDrawerActivity) {
                (requireActivity() as NavDrawerActivity).binding.drawerLayout?.openDrawer(GravityCompat.START)
            }
        }

        binding.searchView.setSuggestionBuilder(SuggestionsBuilder(dataCenter.loadNovelSearchHistory()))
        binding.searchView.setSearchListener(object : PersistentSearchView.SearchListener {

            override fun onSearch(query: String?) {
                query?.addToNovelSearchHistory()
                if (query != null) {
                    searchNovels(query)
                    binding.searchView.setSuggestionBuilder(SuggestionsBuilder(dataCenter.loadNovelSearchHistory()))
                }
            }

            override fun onSearchEditOpened() {
                binding.searchViewBgTint.visibility = View.VISIBLE
                binding.searchViewBgTint
                    .animate()
                    .alpha(1.0f)
                    .setDuration(300)
                    .setListener(SimpleAnimationListener())
                    .start()
            }

            override fun onSearchEditClosed() {
                binding.searchViewBgTint
                    .animate()
                    .alpha(0.0f)
                    .setDuration(300)
                    .setListener(object : SimpleAnimationListener() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            binding.searchViewBgTint.visibility = View.GONE
                        }
                    })
                    .start()
            }

            override fun onSearchExit() {
                if (searchMode)
                    setViewPager()

            }

            override fun onSearchCleared() {
                //Toast.makeText(context, "onSearchCleared", Toast.LENGTH_SHORT).show()
            }

            override fun onSearchTermChanged(term: String?) {
                //Toast.makeText(context, "Search Exited", Toast.LENGTH_SHORT).show()
            }

            override fun onSuggestion(searchItem: SearchItem?): Boolean {
                return true
            }

            override fun onSearchEditBackPressed(): Boolean {
                //Toast.makeText(context, "onSearchEditBackPressed", Toast.LENGTH_SHORT).show()
                if (binding.searchView.searchOpen) {
                    binding.searchView.closeSearch()
                    return true
                }
                return false
            }
        })
    }


    private fun searchNovels(searchTerm: String) {
        while (childFragmentManager.backStackEntryCount > 0)
            childFragmentManager.popBackStack()
        searchMode = true
        this.searchTerm = searchTerm

        val titles = ArrayList<String>()
        titles.add("Novel-Updates")
        if (!dataCenter.lockRoyalRoad || dataCenter.isDeveloper)
            titles.add("RoyalRoad")
        if (!dataCenter.lockNovelFull || dataCenter.isDeveloper)
            titles.add("NovelFull")
        if (!dataCenter.lockScribble || dataCenter.isDeveloper)
            titles.add("ScribbleHub")
        titles.add("WLN-Updates")
        titles.add("LNMTL")
        titles.add("Neovel")

        val searchPageAdapter: GenericFragmentStatePagerAdapter
        searchPageAdapter = GenericFragmentStatePagerAdapter(childFragmentManager, titles.toTypedArray(), titles.size, SearchResultsListener(searchTerm, titles))

        binding.viewPager.offscreenPageLimit = 2
        binding.viewPager.adapter = searchPageAdapter
        binding.tabStrip.setViewPager(binding.viewPager)
    }

    fun closeSearch() {
        binding.searchView.closeSearch()
        setViewPager()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("searchMode", searchMode)
        if (searchTerm != null) outState.putString("searchTerm", searchTerm)
    }

}
