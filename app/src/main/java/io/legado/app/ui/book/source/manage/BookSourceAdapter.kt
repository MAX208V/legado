package io.legado.app.ui.book.source.manage

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.databinding.ItemBookSourceBinding
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.model.Debug
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.buildMainHandler
import io.legado.app.utils.gone
import io.legado.app.utils.invisible
import io.legado.app.utils.startActivity
import io.legado.app.utils.visible
import java.util.Collections

/**
 * 书源列表适配器，支持两种列表项类型：
 * - DomainHeader：域名子文件夹标题
 * - SourceItem：单条书源
 */
class BookSourceAdapter(
    context: Context,
    private val callBack: CallBack,
    private val recyclerView: RecyclerView
) : RecyclerAdapter<BookSourceListItem, ItemBookSourceBinding>(context),
    ItemTouchCallback.Callback {

    private val selected = linkedSetOf<BookSourcePart>()
    private val finalMessageRegex = Regex("成功|失败")
    private val handler = buildMainHandler()
    private val expandedDomains = hashSetOf<String>()

    val selection: List<BookSourcePart>
        get() {
            return getItems().filterIsInstance<BookSourceListItem.SourceItem>()
                .map { it.source }
                .filter { selected.contains(it) }
        }

    val diffItemCallback = object : DiffUtil.ItemCallback<BookSourceListItem>() {

        override fun areItemsTheSame(oldItem: BookSourceListItem, newItem: BookSourceListItem): Boolean {
            return when {
                oldItem is BookSourceListItem.DomainHeader && newItem is BookSourceListItem.DomainHeader ->
                    oldItem.domainKey == newItem.domainKey
                oldItem is BookSourceListItem.SourceItem && newItem is BookSourceListItem.SourceItem ->
                    oldItem.source.bookSourceUrl == newItem.source.bookSourceUrl
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: BookSourceListItem, newItem: BookSourceListItem): Boolean {
            return when {
                oldItem is BookSourceListItem.DomainHeader && newItem is BookSourceListItem.DomainHeader ->
                    oldItem.sourceCount == newItem.sourceCount
                            && oldItem.enabledCount == newItem.enabledCount
                oldItem is BookSourceListItem.SourceItem && newItem is BookSourceListItem.SourceItem ->
                    oldItem.source.bookSourceName == newItem.source.bookSourceName
                            && oldItem.source.bookSourceGroup == newItem.source.bookSourceGroup
                            && oldItem.source.enabled == newItem.source.enabled
                            && oldItem.source.enabledExplore == newItem.source.enabledExplore
                            && oldItem.source.hasExploreUrl == newItem.source.hasExploreUrl
                else -> false
            }
        }

        override fun getChangePayload(oldItem: BookSourceListItem, newItem: BookSourceListItem): Any? {
            val payload = Bundle()
            when {
                oldItem is BookSourceListItem.SourceItem && newItem is BookSourceListItem.SourceItem -> {
                    if (oldItem.source.bookSourceName != newItem.source.bookSourceName
                        || oldItem.source.bookSourceGroup != newItem.source.bookSourceGroup
                    ) {
                        payload.putBoolean("upName", true)
                    }
                    if (oldItem.source.enabled != newItem.source.enabled) {
                        payload.putBoolean("enabled", newItem.source.enabled)
                    }
                    if (oldItem.source.enabledExplore != newItem.source.enabledExplore ||
                        oldItem.source.hasExploreUrl != newItem.source.hasExploreUrl
                    ) {
                        payload.putBoolean("upExplore", true)
                    }
                }
                oldItem is BookSourceListItem.DomainHeader && newItem is BookSourceListItem.DomainHeader -> {
                    payload.putBoolean("upDomain", true)
                }
            }
            if (payload.isEmpty) return null
            return payload
        }

    }

    override fun getViewBinding(parent: ViewGroup): ItemBookSourceBinding {
        return ItemBookSourceBinding.inflate(inflater, parent, false)
    }

    override fun getItemViewType(item: BookSourceListItem, position: Int): Int {
        return when (item) {
            is BookSourceListItem.DomainHeader -> VIEW_TYPE_DOMAIN_HEADER
            is BookSourceListItem.SourceItem -> VIEW_TYPE_SOURCE_ITEM
        }
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBookSourceBinding,
        item: BookSourceListItem,
        payloads: MutableList<Any>
    ) {
        when (item) {
            is BookSourceListItem.DomainHeader -> convertDomainHeader(binding, item, payloads)
            is BookSourceListItem.SourceItem -> convertSourceItem(holder, binding, item, payloads)
        }
    }

    private fun convertDomainHeader(
        binding: ItemBookSourceBinding,
        item: BookSourceListItem.DomainHeader,
        payloads: MutableList<Any>
    ) = binding.run {
        if (payloads.isEmpty()) {
            root.setBackgroundColor(ColorUtils.withAlpha(context.backgroundColor, 0.5f))
            tvHostText.text = item.domainKey
            val info = "[${item.sourceCount}个源, ${item.enabledCount}个启用]"
            tvHostText.append(" $info")
            tvHostText.visible()
            // 隐藏其他元素
            cbBookSource.gone()
            swtEnabled.gone()
            ivEdit.gone()
            ivMenuMore.gone()
            ivExplore.gone()
            ivDebugText.gone()
            ivProgressBar.gone()
        }
    }

    private fun convertSourceItem(
        holder: ItemViewHolder,
        binding: ItemBookSourceBinding,
        item: BookSourceListItem.SourceItem,
        payloads: MutableList<Any>
    ) = binding.run {
        val source = item.source
        if (payloads.isEmpty()) {
            root.setBackgroundColor(ColorUtils.withAlpha(context.backgroundColor, 0.5f))
            tvHostText.gone()
            cbBookSource.visible()
            swtEnabled.visible()
            ivEdit.visible()
            ivMenuMore.visible()
            cbBookSource.text = source.getDisPlayNameGroup()
            swtEnabled.isChecked = source.enabled
            cbBookSource.isChecked = selected.contains(source)
            upCheckSourceMessage(binding, source)
            upShowExplore(ivExplore, source)
        } else {
            for (i in payloads.indices) {
                val bundle = payloads[i] as Bundle
                bundle.keySet().forEach {
                    when (it) {
                        "enabled" -> swtEnabled.isChecked = bundle.getBoolean("enabled")
                        "upName" -> cbBookSource.text = source.getDisPlayNameGroup()
                        "upExplore" -> upShowExplore(ivExplore, source)
                        "selected" -> cbBookSource.isChecked = selected.contains(source)
                        "checkSourceMessage" -> upCheckSourceMessage(binding, source)
                    }
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBookSourceBinding) {
        getItem(holder.layoutPosition)?.let { item ->
            when (item) {
                is BookSourceListItem.DomainHeader -> {
                    binding.tvHostText.setOnClickListener {
                        toggleDomain(item.domainKey)
                    }
                    binding.tvHostText.setOnLongClickListener {
                        callBack.onDomainLongClick(item.domainKey)
                        true
                    }
                }
                is BookSourceListItem.SourceItem -> {
                    val source = item.source
                    binding.swtEnabled.setOnUserCheckedChangeListener { checked ->
                        source.enabled = checked
                        callBack.enable(checked, source)
                    }
                    binding.cbBookSource.setOnUserCheckedChangeListener { checked ->
                        if (checked) {
                            selected.add(source)
                        } else {
                            selected.remove(source)
                        }
                        callBack.upCountView()
                    }
                    binding.ivEdit.setOnClickListener {
                        callBack.edit(source)
                    }
                    binding.ivMenuMore.setOnClickListener {
                        showMenu(binding.ivMenuMore, source)
                    }
                }
            }
        }
    }

    /**
     * 展开/折叠域名组
     */
    fun toggleDomain(domainKey: String) {
        if (expandedDomains.contains(domainKey)) {
            expandedDomains.remove(domainKey)
        } else {
            expandedDomains.add(domainKey)
        }
        callBack.onDomainToggleChanged()
    }

    fun isDomainExpanded(domainKey: String): Boolean {
        return expandedDomains.contains(domainKey)
    }

    fun expandAllDomains() {
        getItems().forEach {
            if (it is BookSourceListItem.DomainHeader) {
                expandedDomains.add(it.domainKey)
            }
        }
        callBack.onDomainToggleChanged()
    }

    fun collapseAllDomains() {
        expandedDomains.clear()
        callBack.onDomainToggleChanged()
    }

    override fun onCurrentListChanged() {
        callBack.upCountView()
    }

    private fun showMenu(view: View, source: BookSourcePart) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.book_source_item)
        popupMenu.menu.findItem(R.id.menu_top).isVisible = callBack.sort == BookSourceSort.Default
        popupMenu.menu.findItem(R.id.menu_bottom).isVisible =
            callBack.sort == BookSourceSort.Default
        val qyMenu = popupMenu.menu.findItem(R.id.menu_enable_explore)
        if (!source.hasExploreUrl) {
            qyMenu.isVisible = false
        } else {
            if (source.enabledExplore) {
                qyMenu.setTitle(R.string.disable_explore)
            } else {
                qyMenu.setTitle(R.string.enable_explore)
            }
        }
        val loginMenu = popupMenu.menu.findItem(R.id.menu_login)
        loginMenu.isVisible = source.hasLoginUrl
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_top -> callBack.toTop(source)
                R.id.menu_bottom -> callBack.toBottom(source)
                R.id.menu_login -> context.startActivity<SourceLoginActivity> {
                    putExtra("type", "bookSource")
                    putExtra("key", source.bookSourceUrl)
                }
                R.id.menu_search -> callBack.searchBook(source)
                R.id.menu_debug_source -> callBack.debug(source)
                R.id.menu_del -> {
                    callBack.del(source)
                    selected.remove(source)
                }
                R.id.menu_enable_explore -> {
                    callBack.enableExplore(!source.enabledExplore, source)
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun upShowExplore(iv: ImageView, source: BookSourcePart) {
        when {
            !source.hasExploreUrl -> {
                iv.invisible()
            }
            source.enabledExplore -> {
                iv.setColorFilter(Color.GREEN)
                iv.visible()
                iv.contentDescription = context.getString(R.string.tag_explore_enabled)
            }
            else -> {
                iv.setColorFilter(Color.RED)
                iv.visible()
                iv.contentDescription = context.getString(R.string.tag_explore_disabled)
            }
        }
    }

    private fun upCheckSourceMessage(
        binding: ItemBookSourceBinding,
        item: BookSourcePart
    ) = binding.run {
        val msg = Debug.debugMessageMap[item.bookSourceUrl] ?: ""
        ivDebugText.text = msg
        val isEmpty = msg.isEmpty()
        var isFinalMessage = msg.contains(finalMessageRegex)
        if (!Debug.isChecking && !isFinalMessage) {
            Debug.updateFinalMessage(item.bookSourceUrl, "校验失败")
            ivDebugText.text = Debug.debugMessageMap[item.bookSourceUrl] ?: ""
            isFinalMessage = true
        }
        ivDebugText.visibility =
            if (!isEmpty) View.VISIBLE else View.GONE
        ivProgressBar.visibility =
            if (isFinalMessage || isEmpty || !Debug.isChecking) View.GONE else View.VISIBLE
    }

    // ===== 选择相关 =====

    fun selectAll() {
        getItems().filterIsInstance<BookSourceListItem.SourceItem>().forEach {
            selected.add(it.source)
        }
        notifyItemRangeChanged(0, itemCount, bundleOf(Pair("selected", null)))
        callBack.upCountView()
    }

    fun revertSelection() {
        getItems().filterIsInstance<BookSourceListItem.SourceItem>().forEach {
            if (selected.contains(it.source)) {
                selected.remove(it.source)
            } else {
                selected.add(it.source)
            }
        }
        notifyItemRangeChanged(0, itemCount, bundleOf(Pair("selected", null)))
        callBack.upCountView()
    }

    fun checkSelectedInterval() {
        val selectedPosition = linkedSetOf<Int>()
        getItems().forEachIndexed { index, item ->
            if (item is BookSourceListItem.SourceItem && selected.contains(item.source)) {
                selectedPosition.add(index)
            }
        }
        if (selectedPosition.isEmpty()) return
        val minPosition = Collections.min(selectedPosition)
        val maxPosition = Collections.max(selectedPosition)
        val itemCount = maxPosition - minPosition + 1
        for (i in minPosition..maxPosition) {
            (getItem(i) as? BookSourceListItem.SourceItem)?.let {
                selected.add(it.source)
            }
        }
        notifyItemRangeChanged(minPosition, itemCount, bundleOf(Pair("selected", null)))
        callBack.upCountView()
    }

    /**
     * 选择整个域名组下的所有书源
     */
    fun selectDomain(domainKey: String) {
        getItems().forEach {
            if (it is BookSourceListItem.SourceItem && it.source.domainKey == domainKey) {
                selected.add(it.source)
            }
        }
        notifyItemRangeChanged(0, itemCount, bundleOf(Pair("selected", null)))
        callBack.upCountView()
    }

    /**
     * 获取域名组下已选中的书源个数
     */
    fun getSelectedCountForDomain(domainKey: String): Int {
        return selected.count { it.domainKey == domainKey }
    }

    // ===== 拖拽排序 =====

    override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
        val srcItem = getItem(srcPosition) as? BookSourceListItem.SourceItem ?: return false
        val targetItem = getItem(targetPosition) as? BookSourceListItem.SourceItem ?: return false
        val srcSource = srcItem.source
        val targetSource = targetItem.source
        val srcOrder = srcSource.customOrder
        srcSource.customOrder = targetSource.customOrder
        targetSource.customOrder = srcOrder
        movedItems.add(srcSource)
        movedItems.add(targetSource)
        swapItem(srcPosition, targetPosition)
        return true
    }

    private val movedItems = hashSetOf<BookSourcePart>()

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            val sortNumberSet = hashSetOf<Int>()
            movedItems.forEach {
                sortNumberSet.add(it.customOrder)
            }
            if (movedItems.size > sortNumberSet.size) {
                callBack.upOrder(getItems().filterIsInstance<BookSourceListItem.SourceItem>()
                    .mapIndexed { index, item ->
                        item.source.customOrder = if (callBack.sortAscending) index else -index
                        item.source
                    })
            } else {
                callBack.upOrder(movedItems.toList())
            }
            movedItems.clear()
        }
    }

    val dragSelectCallback: DragSelectTouchHelper.Callback =
        object : DragSelectTouchHelper.AdvanceCallback<BookSourceListItem>(Mode.ToggleAndReverse) {
            override fun currentSelectedId(): MutableSet<BookSourceListItem> {
                return getItems().filterIsInstance<BookSourceListItem.SourceItem>()
                    .filter { selected.contains(it.source) }
                    .toMutableSet()
            }

            override fun getItemId(position: Int): BookSourceListItem {
                return getItem(position)!!
            }

            override fun updateSelectState(position: Int, isSelected: Boolean): Boolean {
                val item = getItem(position)
                if (item is BookSourceListItem.SourceItem) {
                    if (isSelected) {
                        selected.add(item.source)
                    } else {
                        selected.remove(item.source)
                    }
                    notifyItemChanged(position, bundleOf(Pair("selected", null)))
                    callBack.upCountView()
                    return true
                }
                return false
            }
        }

    interface CallBack {
        val sort: BookSourceSort
        val sortAscending: Boolean
        fun del(bookSource: BookSourcePart)
        fun edit(bookSource: BookSourcePart)
        fun toTop(bookSource: BookSourcePart)
        fun toBottom(bookSource: BookSourcePart)
        fun searchBook(bookSource: BookSourcePart)
        fun debug(bookSource: BookSourcePart)
        fun upOrder(items: List<BookSourcePart>)
        fun enable(enable: Boolean, bookSource: BookSourcePart)
        fun enableExplore(enable: Boolean, bookSource: BookSourcePart)
        fun upCountView()
        fun onDomainToggleChanged()
        fun onDomainLongClick(domainKey: String)
    }

    companion object {
        private const val VIEW_TYPE_DOMAIN_HEADER = 1
        private const val VIEW_TYPE_SOURCE_ITEM = 2
    }
}
