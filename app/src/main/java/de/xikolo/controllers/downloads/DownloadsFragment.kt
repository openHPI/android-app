package de.xikolo.controllers.downloads

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.xikolo.R
import de.xikolo.config.FeatureToggle
import de.xikolo.controllers.dialogs.ConfirmDeleteDialog
import de.xikolo.controllers.helper.LoadingStateHelper
import de.xikolo.events.PermissionDeniedEvent
import de.xikolo.events.PermissionGrantedEvent
import de.xikolo.managers.DownloadManager
import de.xikolo.managers.PermissionManager
import de.xikolo.storages.ApplicationPreferences
import de.xikolo.utils.FileUtil
import de.xikolo.utils.StorageUtil
import de.xikolo.utils.ToastUtil
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.util.*

class DownloadsFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, DownloadsAdapter.OnDeleteButtonClickedListener {

    companion object {
        val TAG: String = DownloadsFragment::class.java.simpleName
    }

    private var adapter: DownloadsAdapter? = null

    private var downloadManager: DownloadManager? = null

    private var permissionManager: PermissionManager? = null

    private var notificationController: LoadingStateHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let { activity ->
            downloadManager = DownloadManager(activity)
            permissionManager = PermissionManager(activity)
            adapter = DownloadsAdapter(this)

            EventBus.getDefault().register(this)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val layout = inflater.inflate(R.layout.fragment_downloads, container, false)

        val recyclerView = layout.findViewById<RecyclerView>(R.id.content_view)
        val layoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        notificationController = LoadingStateHelper(activity, layout, this)

        return layout
    }

    override fun onStart() {
        super.onStart()
        fetchItems()
    }

    override fun onRefresh() {
        fetchItems()
        notificationController?.hideProgress()
    }

    @Subscribe
    fun onPermissionGrantedEvent(permissionGrantedEvent: PermissionGrantedEvent) {
        if (permissionGrantedEvent.requestCode == PermissionManager.REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            fetchItems()
        }
    }

    @Subscribe
    fun onPermissionDeniedEvent(permissionDeniedEvent: PermissionDeniedEvent) {
        if (permissionDeniedEvent.requestCode == PermissionManager.REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            fetchItems()
        }
    }

    private fun fetchItems() {
        activity?.let { activity ->
            adapter?.clear()
            if (permissionManager?.requestPermission(PermissionManager.WRITE_EXTERNAL_STORAGE) == 1) {
                notificationController?.showContentView()

                // total items

                var internalAddition = ""
                var sdcardAddition = ""

                val sdcardStorageAvailable = StorageUtil.getSdcardStorage(activity) != null

                if (sdcardStorageAvailable) {
                    if (StorageUtil.getStorage(activity) == StorageUtil.getSdcardStorage(activity)) {
                        sdcardAddition = " " + getString(R.string.settings_storage_addition)
                    } else {
                        internalAddition = " " + getString(R.string.settings_storage_addition)
                    }
                }

                fun buildTotalItem(appFolder: String, suffix: String): DownloadsAdapter.FolderItem {
                    // clean up the storage before fetching items
                    StorageUtil.cleanStorage(File(appFolder))

                     return DownloadsAdapter.FolderItem(
                        getString(R.string.settings_title_storage_internal) + suffix,
                        appFolder
                    )
                }

                var list: MutableList<DownloadsAdapter.FolderItem> = ArrayList()

                list.add(buildTotalItem(
                    FileUtil.createStorageFolderPath(StorageUtil.getInternalStorage(activity)),
                    internalAddition
                ))

                StorageUtil.getSdcardStorage(activity)?.let { sdcardStorage ->
                    list.add(buildTotalItem(
                        FileUtil.createStorageFolderPath(sdcardStorage),
                        sdcardAddition
                    ))
                }

                adapter?.addItem(getString(R.string.overall), list)

                // documents

                if (FeatureToggle.documents()) {
                    list = ArrayList()

                    list.add(buildTotalItem(
                        StorageUtil.getInternalStorage(activity).absolutePath + File.separator + "Documents",
                        internalAddition
                    ))

                    StorageUtil.getSdcardStorage(activity)?.let { sdcardStorage ->
                        list.add(buildTotalItem(
                            sdcardStorage.absolutePath + File.separator + "Documents",
                            sdcardAddition
                        ))
                    }

                    adapter?.addItem(getString(R.string.tab_documents), list)
                }

                // course folders

                fun buildCourseItems(storage: File): List<DownloadsAdapter.FolderItem> {
                    val folders = downloadManager?.getFoldersWithDownloads(
                        File(storage.absolutePath + File.separator + "Courses")
                    )
                    val folderList: MutableList<DownloadsAdapter.FolderItem> = ArrayList()
                    if (folders?.isNotEmpty() == true) {
                        for (folder in folders) {
                            val name = try {
                                folder.substring(
                                    folder.lastIndexOf(File.separator) + 1,
                                    folder.lastIndexOf("_")
                                )
                            } catch (e: Exception) {
                                folder
                            }

                            val item = DownloadsAdapter.FolderItem(name, folder)
                            folderList.add(item)
                        }
                    }
                    return folderList
                }

                val internalCourseTitle = if (sdcardStorageAvailable) {
                    getString(R.string.courses) + " (" + getString(R.string.settings_title_storage_internal) + ")"
                } else {
                    getString(R.string.courses)
                }
                adapter?.addItem(
                    internalCourseTitle,
                    buildCourseItems(StorageUtil.getInternalStorage(activity))
                )

                StorageUtil.getSdcardStorage(activity)?.let { sdcardStorage ->
                    val sdcardCourseTitle = if (sdcardStorageAvailable) {
                        getString(R.string.courses) + " (" + getString(R.string.settings_title_storage_external) + ")"
                    } else {
                        getString(R.string.courses)
                    }
                    adapter?.addItem(
                        sdcardCourseTitle,
                        buildCourseItems(sdcardStorage)
                    )
                }
            } else {
                notificationController?.setMessageTitle(R.string.dialog_title_permissions)
                notificationController?.setMessageSummary(R.string.dialog_permissions)
                notificationController?.setMessageOnClickListener {
                    PermissionManager.startAppInfo(activity)
                }
                notificationController?.showMessage()
            }
        }
    }

    override fun onDeleteButtonClicked(item: DownloadsAdapter.FolderItem) {
        activity?.let { activity ->
            val appPreferences = ApplicationPreferences()

            if (appPreferences.confirmBeforeDeleting) {
                val dialog = ConfirmDeleteDialog.getInstance(true)
                dialog.setConfirmDeleteDialogListener(object :
                    ConfirmDeleteDialog.ConfirmDeleteDialogListener {
                    override fun onDialogPositiveClick(dialog: DialogFragment) {
                        deleteFolder(item)
                    }

                    override fun onDialogPositiveAndAlwaysClick(dialog: DialogFragment) {
                        appPreferences.confirmBeforeDeleting = false
                        deleteFolder(item)
                    }
                })
                dialog.show(activity.supportFragmentManager, ConfirmDeleteDialog.TAG)
            } else {
                deleteFolder(item)
            }
        }
    }

    private fun deleteFolder(item: DownloadsAdapter.FolderItem) {
        val dir = File(item.path)

        if (dir.exists()) {
            FileUtil.delete(dir)
        } else {
            ToastUtil.show(R.string.error)
        }

        fetchItems()
    }

}
