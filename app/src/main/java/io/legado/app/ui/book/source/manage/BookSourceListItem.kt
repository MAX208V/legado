package io.legado.app.ui.book.source.manage

import io.legado.app.data.entities.BookSourcePart

/**
 * 书源列表项模型，支持树形结构：分组 → 域名子文件夹 → 书源
 */
sealed class BookSourceListItem {

    /**
     * 域名子文件夹标题
     */
    data class DomainHeader(
        val domainKey: String,
        val sourceCount: Int,
        val enabledCount: Int,
        val isExpanded: Boolean = true
    ) : BookSourceListItem()

    /**
     * 单条书源
     */
    data class SourceItem(
        val source: BookSourcePart
    ) : BookSourceListItem()
}
