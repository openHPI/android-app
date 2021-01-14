package de.xikolo.controllers.downloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.xikolo.App
import de.xikolo.R
import de.xikolo.config.Feature
import de.xikolo.controllers.dialogs.ConfirmDeleteDialog
import de.xikolo.controllers.dialogs.ConfirmDeleteDialogAutoBundle
import de.xikolo.controllers.helper.NetworkStateHelper
import de.xikolo.download.DownloadIdentifier
import de.xikolo.download.DownloadStatus
import de.xikolo.download.Downloaders
import de.xikolo.extensions.observe
import de.xikolo.managers.PermissionManager
import de.xikolo.models.dao.CourseDao
import de.xikolo.storages.ApplicationPreferences
import de.xikolo.utils.extensions.internalStorage
import de.xikolo.utils.extensions.preferredStorage
import de.xikolo.utils.extensions.sdcardStorage
import de.xikolo.utils.extensions.showToast

class DownloadsFragment :
    Fragment(), SwipeRefreshLayout.OnRefreshListener, NetworkStateHelper.NetworkStateOwner {

    companion object {
        val TAG: String = DownloadsFragment::class.java.simpleName

        const val CATEGORY_DOCUMENTS = "documents"
        const val CATEGORY_CERTIFICATES = "certificates"
    }

    private var adapter: DownloadsAdapter? = null

    private var permissionManager: PermissionManager? = null

    override lateinit var networkStateHelper: NetworkStateHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let { activity ->
            permissionManager = PermissionManager(activity)
            adapter = DownloadsAdapter()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val layout = inflater.inflate(R.layout.fragment_downloads, container, false)

        val recyclerView = layout.findViewById<RecyclerView>(R.id.content_view)
        val layoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        networkStateHelper = NetworkStateHelper(activity, layout, this)

        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        App.instance.state.permission.of(PermissionManager.REQUEST_CODE_WRITE_EXTERNAL_STORAGE)
            .observe(viewLifecycleOwner) {
                fetchItems()
            }
    }

    override fun onStart() {
        super.onStart()
        fetchItems()
    }

    override fun onRefresh() {
        fetchItems()
        networkStateHelper.hideAnyProgress()
    }

    private fun buildItems(
        internalStorageDownloads: Map<DownloadIdentifier, Pair<DownloadStatus, String?>>,
        sdcardStorageDownloads: Map<DownloadIdentifier, Pair<DownloadStatus, String?>>?
    ) {
        var internalAddition = ""
        var sdcardAddition = ""

        if (activity?.sdcardStorage != null) {
            if (activity?.preferredStorage == activity?.sdcardStorage) {
                sdcardAddition = " " + getString(R.string.settings_storage_addition)
            } else {
                internalAddition = " " + getString(R.string.settings_storage_addition)
            }
        }

        adapter?.addItem(
            getString(R.string.overall),
            mutableListOf(
                buildSummary(
                    internalStorageDownloads,
                    getString(R.string.settings_title_storage_internal) + internalAddition
                )
            ).apply {
                if (sdcardStorageDownloads != null) {
                    add(
                        buildSummary(
                            sdcardStorageDownloads,
                            getString(R.string.settings_title_storage_external) + sdcardAddition
                        )
                    )
                }
            }
        )

        if (Feature.enabled("documents")) {
            adapter?.addItem(
                getString(R.string.tab_documents),
                buildCategories(
                    internalStorageDownloads,
                    { it == CATEGORY_DOCUMENTS },
                    { getString(R.string.settings_title_storage_internal) + internalAddition }
                ).toMutableList()
                    .apply {
                        if (sdcardStorageDownloads != null) {
                            addAll(
                                buildCategories(
                                    sdcardStorageDownloads,
                                    { it == CATEGORY_DOCUMENTS },
                                    {
                                        getString(R.string.settings_title_storage_external) +
                                            sdcardAddition
                                    }
                                )
                            )
                        }
                    }
            )
        }

        adapter?.addItem(
            getString(R.string.tab_certificates),
            buildCategories(
                internalStorageDownloads,
                { it == CATEGORY_CERTIFICATES },
                { getString(R.string.settings_title_storage_internal) + internalAddition }
            ).toMutableList()
                .apply {
                    if (sdcardStorageDownloads != null) {
                        addAll(
                            buildCategories(
                                sdcardStorageDownloads,
                                { it == CATEGORY_CERTIFICATES },
                                {
                                    getString(R.string.settings_title_storage_external) +
                                        sdcardAddition
                                }
                            )
                        )
                    }
                }
        )

        adapter?.addItem(
            getString(R.string.courses) + if (sdcardStorageDownloads != null) {
                " (" + getString(R.string.settings_title_storage_internal) + ")"
            } else "",
            buildCategories(
                internalStorageDownloads,
                { it != CATEGORY_CERTIFICATES && it != CATEGORY_DOCUMENTS },
                { CourseDao.Unmanaged.find(it)?.title ?: "" }
            )
        )

        if (sdcardStorageDownloads != null) {
            adapter?.addItem(
                getString(R.string.courses) +
                    " (" + getString(R.string.settings_title_storage_external) + ")",
                buildCategories(
                    sdcardStorageDownloads,
                    { it != CATEGORY_CERTIFICATES && it != CATEGORY_DOCUMENTS },
                    { CourseDao.Unmanaged.find(it)?.title ?: "" }
                )
            )
        }

        showContent()
    }

    private fun buildSummary(
        downloads: Map<DownloadIdentifier, Pair<DownloadStatus, String?>>,
        title: String
    ): DownloadsAdapter.DownloadCategory {
        return DownloadsAdapter.DownloadCategory(
            title,
            downloads.values.sumOf {
                it.first.takeIf { it.state == DownloadStatus.State.DOWNLOADED }?.totalBytes ?: 0L
            },
            -1 // hide number of files because of ExoPlayer Cache
        ) { deleteDownloads(downloads.keys) }
    }

    private fun buildCategories(
        downloads: Map<DownloadIdentifier, Pair<DownloadStatus, String?>>,
        categoryFilter: (String) -> Boolean,
        titleSelector: (String?) -> String
    ): List<DownloadsAdapter.DownloadCategory> {
        val downloadCategories: MutableList<DownloadsAdapter.DownloadCategory> =
            mutableListOf()
        downloads
            .entries
            .filter {
                it.value.first.state == DownloadStatus.State.DOWNLOADED &&
                    it.value.second?.takeIf { categoryFilter(it) } != null
            }
            .groupBy { it.value.second }
            .mapValues { it.value.map { Pair(it.key, it.value.first) } }
            .forEach {
                downloadCategories.add(
                    DownloadsAdapter.DownloadCategory(
                        titleSelector(it.key),
                        it.value.sumOf { it.second.totalBytes ?: 0L },
                        it.value.count()
                    ) { deleteDownloads(it.value.map { it.first }) }
                )
            }
        return downloadCategories
    }

    private fun deleteDownloads(downloads: Collection<DownloadIdentifier>) {
        fun executeDelete() {
            showAnyProgress()
            Downloaders.deleteDownloads(downloads) {
                if (!it) {
                    showToast(R.string.error_plain)
                }
                fetchItems()
                hideAnyProgress()
            }
        }

        val appPreferences = ApplicationPreferences()
        if (appPreferences.confirmBeforeDeleting) {
            val dialog =
                ConfirmDeleteDialogAutoBundle.builder(true).build()
            dialog.listener =
                object : ConfirmDeleteDialog.Listener {
                    override fun onDialogPositiveClick(
                        dialog: DialogFragment
                    ) {
                        executeDelete()
                    }

                    override fun onDialogPositiveAndAlwaysClick(
                        dialog: DialogFragment
                    ) {
                        appPreferences.confirmBeforeDeleting = false
                        executeDelete()
                    }
                }
            dialog.show(
                requireActivity().supportFragmentManager,
                ConfirmDeleteDialog.TAG
            )
        } else {
            executeDelete()
        }
    }

    private fun fetchItems() {
        activity?.let { activity ->
            adapter?.clear()
            if (permissionManager?.requestPermission(
                    PermissionManager.WRITE_EXTERNAL_STORAGE
                ) == 1
            ) {
                Downloaders.getDownloads(activity.internalStorage) {
                    val internalStorageDownloads = it

                    activity.sdcardStorage?.let { sdcardStorage ->
                        Downloaders.getDownloads(sdcardStorage) { sdcardStorageDownloads ->
                            buildItems(internalStorageDownloads, sdcardStorageDownloads)
                        }
                    } ?: buildItems(internalStorageDownloads, null)
                }
            } else {
                networkStateHelper.setMessageTitle(R.string.dialog_title_permissions)
                networkStateHelper.setMessageSummary(R.string.dialog_permissions)
                networkStateHelper.setMessageOnClickListener(View.OnClickListener {
                    PermissionManager.startAppInfo(activity)
                })
                networkStateHelper.showMessage()
            }
        }
    }
}
