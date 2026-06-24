package io.legado.app.ui.book.changesource

import io.legado.app.data.entities.SearchBook

sealed class ChangeSourceGroupItem {

    data class GroupHeader(
        val domainKey: String,
        val displayName: String,
        val versionCount: Int,
        val isExpanded: Boolean = true
    ) : ChangeSourceGroupItem()

    data class SourceItem(
        val searchBook: SearchBook,
        val isFirstInGroup: Boolean = false,
        val parentDomainKey: String? = null
    ) : ChangeSourceGroupItem()
}
