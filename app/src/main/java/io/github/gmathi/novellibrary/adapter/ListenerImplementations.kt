package io.github.gmathi.novellibrary.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.github.gmathi.novellibrary.fragment.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelSection
import io.github.gmathi.novellibrary.model.database.TranslatorSource
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.network.HostNames

//region Fragment Page Listeners

//endregion

//region Fragment State Page Listeners

class NavPageListener : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment {
        return when (position) {
            0 -> SearchUrlFragment.newInstance("https://www.novelupdates.com/series-ranking/?rank=popmonth")
            1 -> SearchUrlFragment.newInstance("https://www.novelupdates.com/series-ranking/?rank=popular")
            else -> SearchUrlFragment.newInstance("https://www.novelupdates.com/series-ranking/?rank=sixmonths")
        }
    }
}

class SearchResultsListener(private val searchTerms: String, private val tabNames: ArrayList<String>) : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment {
        if (position >= tabNames.size) return SearchTermFragment.newInstance(searchTerms, HostNames.WLN_UPDATES)
        return when (tabNames[position]) {
            "Novel-Updates" -> SearchTermFragment.newInstance(searchTerms, HostNames.NOVEL_UPDATES)
            "RoyalRoad" -> SearchTermFragment.newInstance(searchTerms, HostNames.ROYAL_ROAD)
            "NovelFull" -> SearchTermFragment.newInstance(searchTerms, HostNames.NOVEL_FULL)
            "ScribbleHub" -> SearchTermFragment.newInstance(searchTerms, HostNames.SCRIBBLE_HUB)
            "LNMTL" -> SearchTermFragment.newInstance(searchTerms, HostNames.LNMTL)
            "Neovel" -> SearchTermFragment.newInstance(searchTerms, HostNames.NEOVEL)
            else -> SearchTermFragment.newInstance(searchTerms, HostNames.WLN_UPDATES)
        }
    }
}


class WebPageFragmentPageListener(val novel: Novel, val webPages: List<WebPage>) : GenericFragmentStatePagerAdapter.Listener {

    override fun getFragmentForItem(position: Int): Fragment {
        return WebPageDBFragment.newInstance(novel.id, webPages[position])
    }
}

class LibraryPageListener(private val novelSections: ArrayList<NovelSection>) : GenericFragmentStatePagerAdapter.Listener {
    private var currentFragment = HashMap<Int, Fragment>()
    
    fun getCurrentFragment(position: Int) = currentFragment[position]

    override fun getFragmentForItem(position: Int): Fragment {
        val result = LibraryFragment.newInstance(novelSections[position].id)
        currentFragment[position] = result
        return result
    }
}

class ChaptersPageListener(private val novel: Novel, private val translatorSources: ArrayList<TranslatorSource>) : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment {
        return ChaptersFragment.newInstance(novel, translatorSources[position].id)
    }
}
//endregion



