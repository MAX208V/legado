package io.legado.app.ui.book.changesource

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.SearchBook
import io.legado.app.databinding.ItemChangeSourceBinding
import io.legado.app.help.ado.app.lib.dialogs.alert
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.gone
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import splitties.init.appCtx
import splitties.views.onLongClick

class ChangeBookSourceAdapter(
    context: Context,
    val viewModel: ChangeBookSourceViewModel,
    val callBack: CallBack
) : RecyclerAdapter<ChangeSourceGroupItem, ItemChangeSourceBinding>(context) {

    private val collapsedGroups = hashSetOf<String>()

    val diffItemCallback = object : DiffUtil.ItemCallback<ChangeSourceGroupItem>() {
        override fun areItemsTheSame(oldItem: ChangeSourceGroupItem, newItem: ChangeSourceGroupItem): Boolean {
            return when {
                oldItem is ChangeSourceGroupItem.GroupHeader && newItem is ChangeSourceGroupItem.GroupHeader ->
                    oldItem.domainKey == newItem.domainKey
                oldItem is ChangeSourceGroupItem.SourceItem && newItem is ChangeSourceGroupItem.SourceItem ->
                    oldItem.searchBook.bookUrl == newItem.searchBook.bookUrl
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ChangeSourceGroupItem, newItem: ChangeGroupSourceHeaderItem Change newGroup.searchName == newItem.searchBook.originName &&
                    oldItem.searchBook.getDisplayLastChapterTitle() == newItem.searchBook.getDisplayLastChapterTitle()
                else -> false
            }
        }
    }

    override fun getViewBinding(parent: ViewGroup): ItemChangeSourceBinding {
        return ItemChangeSourceBinding.inflate(inflater, parent, false)
    }

    override fun getItemViewType(item: ChangeSourceGroupItem, position: Int): Int {
        return when (item) {
            is ChangeSourceGroupItem.GroupHeader -> VIEW_TYPE_GROUP_HEADER
            is ChangeSourceGroupItem.SourceItem -> VIEW_TYPE_SOURCE_ITEM
        }
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemChangeSourceBinding,
        item: ChangeSourceGroupItem,
        payloads: MutableList<Any>
    ) {
        when (item) {
            is ChangeSourceGroupItem.GroupHeader -> convertGroupHeader(binding, item)
            is ChangeSourceGroupItem.SourceItem -> convertSourceItem(binding, item)
        }
      : ItemChangeSourceBinding, item: ChangeSourceGroupItem.GroupHeader) {
        binding.apply {
            val arrow = if (isGroupExpanded(item.domainKey)) "\u25BC" else "\u25B6"
            tvOrigin.text = "$arrow ${item.displayName} (${item.versionCount}个版本)"
            tv.text
            tvOrigin.visible()
            tvAuthor.gone()
Count.gone()
            tvRespondTime.gone()
        }
    }

    private fun convertSourceItem(binding: ItemChangeSourceBinding, item: ChangeSourceGroupItem.SourceItem) {
        val book = item.searchBook
        binding.apply {
            if (item.parentDomainKey != null && !item.isFirstInGroup) {
                tvOrigin.text = "   \u2514 ${book.originName}"
                ivGood.gone()
                ivBad.gone()
            } else {
                tvOrigin.text = book.originName
                ivGood.visible()
                ivBad.visible()
            }
            tvOrigin.setTextColor(appCtx.getCompatColor(R.color.primaryText))
            tvOrigin.textSize = 14f
            tvAuthor.text = book.author
            tvAuthor.visible()
            tvLast.text = book.getDisplayLastChapterTitle()
            tvLast.visible()

            if (callBack.oldBookUrl == book.bookUrl) {
                ivChecked.visible()
            } else {
                ivChecked.invisible()
            }

            val score = callBack.getBookScore(book)
            if (score > 0) {
                ivBad.gone()
                DrawableCompat.setTint(ivGood.drawable, appCtx.getCompatColor(R.color.md_red_A200))
                DrawableCompat.setTint(ivBad.drawable, appCtx.getCompatColor(R.color.md_blue_100))
            } else if (score < 0) {
                ivGood.gone()
                DrawableCompat.getCompatColor(R.color.md_red_100))
                DrawableCompat.setTint(ivBad.drawable, appCtx.getCompatColor(R.color.md_blue_A200))
            } else {
                DrawableCompat.setTint(ivGood.drawable, appCtx.getCompatColor(R.color.md_red_100))
                DrawableCompat.setTint(ivBad.drawable, appCtx.getCompatColor(R.color.md_blue_100))
            }

            if (AppConfig.changeSourceLoadWordCount && !book.chapterWordCountText.isNullOrBlank()) {
                tvCurrentChapterWordCount.visible()
            } else {
                tvCurrentChapterWordCount.gone()
            }
            if (AppConfig.changeSourceLoadWordCount && book.respondTime >= 0) {
                tvRespondTime.visible()
            } else {
                tvRespondTime.gone()
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemChangeSourceBinding) {
        getItem(holder.layoutPosition)?.let { item ->
            when (item) {
                is ChangeSourceGroupItem.GroupHeader -> {
                    binding.root.setOnClickListener {
                        toggleGroup(item.domainKey)
                    }
                }
                is ChangeSourceGroupItem.SourceItem -> {
                    val book = item.searchBook
                    if (item.parentDomainKey == null || item.isFirstInGroup) {
                        binding.ivGood.setOnClickListener {
                            if (binding.ivBad.isVisible) {
                                DrawableCompat.setTint(binding.ivGood.drawable, appCtx.getCompatColor(R.color.md_red_A200))
                                binding.ivBad.gone()
                                callBack.setBookScore(book, 1)
                            } else {
                                DrawableCompat.setTint(binding.ivGood.drawable, appCtx.getCompatColor(R.color.md_red_100))
                                binding.ivBad.visible()
                                callBack.setBookScore(book, 0)
                            }
                        }
                        binding.ivBad.setOnClickListener {
                            if (binding.ivGood.isVisible) {
                                DrawableCompat.setTint(binding.ivBad.drawable, appCtx.getCompatColor(R.color.md_blue_A200))
                                binding.ivGood.gone()
                                callBack.setBookScore(book, -1)
                            } else {
                                DrawableCompat.setTint(binding.ivBad.drawable, appCtx.getCompatColor(R.color.md_blue_100))
                                binding.ivGood.visible()
                                callBack.setBookScore(book, 0)
                            }
                        }
                    }
                    holder.itemView.setOnClickListener {
                        if (book.bookUrl != callBack.oldBookUrl) {
                            val altSources = item.parentDomainKey?.let { viewModel.getAlternativeSources(it) }
                            if (altSources != null && altSources.isNotEmpty(book, altSources)
                            } else {
                                }
.onLongClick {
                        showMenu(holder.itemView, book)
                    }
                }
            }
        }
    }

    fun ( fun isGroupExpanded(domainKey: String): Boolean {
        return !collapsedGroups.contains(domainKey)
    }

    private fun showMenu(view: View, searchBook: SearchBook) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.change_source_item)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_top_source -> callBack.topSource(searchBook)
                R.id.menu_bottom_source -> callBack.bottomSource(searchBook)
                R.id.menu_edit_source -> callBack.editSource(searchBookisableBook context set))
Source(searchBook) }
                }
            }
            true
        }
        popupMenu.show()
    }

    interface CallBack {
        val oldBookUrl: String?
        fun changeTo(searchBook: SearchBook)
        fun changeToWithVersions(searchBook: SearchBook, alternativeVersions: List<SearchBook>)
        fun topSource(searchBook: SearchBook)
        fun bottomSource(searchBook: SearchBook)
        fun editSource(searchBook: SearchBook)
        fun disableSource(searchBook: SearchBook)
        fun deleteSource(searchBook: SearchBook)
        fun setBookScore(searchBook: SearchBook, score: Int)
        fun getBookScore(searchBook: SearchBook): Int
        fun onGroupToggleChanged()
    }

    companion object {
        private const val VIEW_TYPE_GROUP_HEADER = 1
        private const val VIEW_TYPE_SOURCE_ITEM = 2
    }
}
