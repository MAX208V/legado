package io.legado.app.ui.book.source.manage

import android.os.Parcelable
import androidx.room.Ignore
import io.legado.app.data.entities.BookSourcePart
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * 书源列表项模型，支持树形结构：分组 → 域名子文件夹 → 书源
 */
sealed class BookSourceListItem : Parcelable {

    /**
     * 域名子文件夹标题
     */
    @Parcelize
    data class DomainHeader(
        val domainKey: String,
        val sourceCount: Int,
        val enabledCount: Int,
        @Ignore
        @IgnoredOnParcel
        val isExpanded: Boolean = true
    ) : BookSourceListItem()

    /**
     * 单条书源
     */
    @Parcelize
    data class SourceItem(
        val source: BookSourcePart
    ) : BookSourceListItem()
}
