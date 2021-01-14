package de.xikolo.controllers.downloads

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import de.xikolo.App
import de.xikolo.R
import de.xikolo.utils.MetaSectionList
import de.xikolo.utils.extensions.asFormattedFileSize

class DownloadsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val TAG: String = DownloadsAdapter::class.java.simpleName
        private const val ITEM_VIEW_TYPE_HEADER = 0
        private const val ITEM_VIEW_TYPE_ITEM = 1
    }

    private val sectionList: MetaSectionList<String, Any, List<DownloadCategory>> =
        MetaSectionList()

    fun addItem(header: String, downloadCategory: List<DownloadCategory>) {
        if (downloadCategory.isNotEmpty()) {
            sectionList.add(header, downloadCategory)
            notifyDataSetChanged()
        }
    }

    fun clear() {
        sectionList.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = sectionList.size

    override fun getItemViewType(position: Int): Int {
        return if (sectionList.isHeader(position)) {
            ITEM_VIEW_TYPE_HEADER
        } else {
            ITEM_VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_header_secondary, parent, false)
            view.isEnabled = false
            view.setOnClickListener(null)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_download_list, parent, false)
            view.isEnabled = false
            view.setOnClickListener(null)
            FolderViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.title.text = sectionList.get(position) as String
        } else {
            val downloadCategory = sectionList.get(position) as DownloadCategory

            val viewHolder = holder as FolderViewHolder
            viewHolder.textTitle.text = downloadCategory.title.replace("_".toRegex(), " ")
            viewHolder.textButtonDelete.setOnClickListener {
                downloadCategory.onDelete()
            }

            viewHolder.textSubTitle.text =
                when {
                    downloadCategory.itemCount < 0 -> {
                        viewHolder.textButtonDelete.visibility = View.VISIBLE
                        downloadCategory.size.asFormattedFileSize
                    }
                    downloadCategory.itemCount == 0 -> {
                        viewHolder.textButtonDelete.visibility = View.GONE
                        downloadCategory.itemCount.toString() + " " +
                            App.instance.getString(R.string.files)
                    }
                    else -> {
                        viewHolder.textButtonDelete.visibility = View.VISIBLE
                        downloadCategory.itemCount.toString() + " " +
                            App.instance.getString(R.string.files) +
                            if (downloadCategory.size > 0) {
                                ": " + downloadCategory.size.asFormattedFileSize
                            } else ""
                    }
                }

            if (position == itemCount - 1 || sectionList.isHeader(position + 1)) {
                viewHolder.viewDivider.visibility = View.INVISIBLE
            } else {
                viewHolder.viewDivider.visibility = View.VISIBLE
            }
        }
    }

    data class DownloadCategory(
        val title: String,
        val size: Long,
        val itemCount: Int,
        val onDelete: () -> Unit
    )

    internal class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        @BindView(R.id.textTitle)
        lateinit var textTitle: TextView

        @BindView(R.id.textSubTitle)
        lateinit var textSubTitle: TextView

        @BindView(R.id.buttonDelete)
        lateinit var textButtonDelete: TextView

        @BindView(R.id.divider)
        lateinit var viewDivider: View

        init {
            ButterKnife.bind(this, view)
        }

    }

    internal class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        @BindView(R.id.textHeader)
        lateinit var title: TextView

        init {
            ButterKnife.bind(this, view)
        }

    }

}
