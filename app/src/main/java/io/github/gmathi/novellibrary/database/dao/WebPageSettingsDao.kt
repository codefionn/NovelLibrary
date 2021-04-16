package io.github.gmathi.novellibrary.database.dao

import androidx.room.*
import com.google.gson.Gson
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.util.Constants
import java.util.*

@Dao
interface WebPageSettingsDao {
    @Insert
    fun insertAll(vararg webPageSettings: WebPageSettings)
    
    @Insert
    fun insert(webPageSettings: WebPageSettings)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(webPageSettings: WebPageSettings)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertOrIgnore(webPageSettings: WebPageSettings)

    @Update
    fun update(webPageSettings: WebPageSettings)
    
    @Delete
    fun delete(webPageSettings: WebPageSettings)

    @Query("DELETE FROM web_page_settings WHERE novel_id = :novelId")
    fun deleteByNovelId(novelId: Long)

    @Query("DELETE FROM web_page_settings WHERE url = :url")
    fun deleteByUrl(url: String)

    @Query("SELECT * FROM web_page_settings WHERE url = :url")
    fun findOneByUrl(url: String): WebPageSettings?
    
    @Query("SELECT * FROM web_page_settings WHERE redirect_url = :redirectUrl")
    fun findOneByRedirectUrl(redirectUrl: String): WebPageSettings?
    
    @Query("SELECT * FROM web_page_settings WHERE novel_id = :novelId")
    fun findByNovelId(novelId: Long): List<WebPageSettings>
    
    @Query("SELECT * FROM web_page_settings")
    fun getAll(): List<WebPageSettings>

    @Query("UPDATE web_page_settings SET is_read = :isRead WHERE url = :url")
    fun updateReadStatus(url: String, isRead: Int)

    @Query("UPDATE web_page_settings SET metadata = :metadata WHERE url = :url")
    fun updateMetadata(url: String, metadata: String)

    @Transaction
    fun updateWebPageSettingsReadStatus(webPageSettings: WebPageSettings) {
        if (!webPageSettings.isRead) {
            webPageSettings.metadata.remove(Constants.MetaDataKeys.SCROLL_POSITION)
            updateMetadata(webPageSettings.url, Gson().toJson(webPageSettings.metadata))
        }
        else {
            webPageSettings.metadata[Constants.MetaDataKeys.LAST_READ_DATE] = Date().toString()
            updateMetadata(webPageSettings.url, Gson().toJson(webPageSettings.metadata))
        }

        updateReadStatus(webPageSettings.url, if (webPageSettings.isRead) 1 else 0)
    }
}