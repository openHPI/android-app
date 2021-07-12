package de.xikolo.controllers.helper

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import de.xikolo.R
import de.xikolo.controllers.dialogs.MobileDownloadDialog
import de.xikolo.controllers.dialogs.ModuleDownloadDialog
import de.xikolo.controllers.dialogs.ModuleDownloadDialogAutoBundle
import de.xikolo.controllers.dialogs.ProgressDialogIndeterminate
import de.xikolo.controllers.dialogs.ProgressDialogIndeterminateAutoBundle
import de.xikolo.download.DownloadItem
import de.xikolo.download.DownloadStatus
import de.xikolo.models.Course
import de.xikolo.models.DownloadAsset
import de.xikolo.models.Item
import de.xikolo.models.Section
import de.xikolo.models.dao.VideoDao
import de.xikolo.network.jobs.ListItemsWithContentForSectionJob
import de.xikolo.network.jobs.base.NetworkCode
import de.xikolo.network.jobs.base.NetworkState
import de.xikolo.network.jobs.base.NetworkStateLiveData
import de.xikolo.storages.ApplicationPreferences
import de.xikolo.utils.LanalyticsUtil
import de.xikolo.utils.extensions.ConnectivityType
import de.xikolo.utils.extensions.connectivityType
import de.xikolo.utils.extensions.isOnline
import de.xikolo.utils.extensions.showToast

class SectionDownloadHelper(private val activity: FragmentActivity) {

    fun initSectionDownloads(course: Course, section: Section) {
        val listDialog = ModuleDownloadDialogAutoBundle.builder(section.title).build()

        listDialog.listener = object : ModuleDownloadDialog.ItemSelectionListener {

            override fun onSelected(dialog: DialogFragment, video: Boolean, slides: Boolean) {
                val appPreferences = ApplicationPreferences()
                if (video || slides) {
                    if (activity.isOnline) {
                        if (activity.connectivityType === ConnectivityType.CELLULAR && appPreferences.isDownloadNetworkLimitedOnMobile) {
                            val permissionDialog = MobileDownloadDialog()

                            permissionDialog.listener =
                                object : MobileDownloadDialog.MobileDownloadGrantedListener {

                                    override fun onGranted(dialog: DialogFragment) {
                                        appPreferences.isDownloadNetworkLimitedOnMobile = false
                                        startSectionDownloads(course, section, video, slides)
                                    }
                                }
                            permissionDialog.show(
                                activity.supportFragmentManager,
                                MobileDownloadDialog.TAG
                            )
                        } else {
                            startSectionDownloads(course, section, video, slides)
                        }
                    } else {
                        activity.showToast(R.string.toast_no_network)
                    }
                }
            }
        }

        listDialog.show(activity.supportFragmentManager, ModuleDownloadDialog.TAG)
    }

    private fun startSectionDownloads(
        course: Course,
        section: Section,
        video: Boolean,
        slides: Boolean
    ) {
        LanalyticsUtil.trackDownloadedSection(section.id, course.id, video, false, slides)

        val dialog = ProgressDialogIndeterminateAutoBundle.builder().build()
        dialog.show(activity.supportFragmentManager, ProgressDialogIndeterminate.TAG)

        val itemRequestNetworkState = NetworkStateLiveData()
        itemRequestNetworkState.observeForever(object : Observer<NetworkState> {
            override fun onChanged(networkState: NetworkState) {
                if (networkState.code === NetworkCode.STARTED) {
                    return
                }

                if (networkState.code === NetworkCode.SUCCESS) {
                    dialog.dismissAllowingStateLoss()
                    for (item in section.accessibleItems) {
                        if (item.contentType == Item.TYPE_VIDEO) {
                            if (video) {
                                startDownload(
                                    DownloadAsset.Course.Item.VideoHLS(
                                        item,
                                        VideoDao.Unmanaged.find(item.contentId)!!,
                                        ApplicationPreferences().videoDownloadQuality
                                    )
                                )
                            }

                            if (slides) {
                                startDownload(
                                    DownloadAsset.Course.Item.Slides(
                                        item,
                                        VideoDao.Unmanaged.find(item.contentId)!!
                                    )
                                )
                            }
                        }
                    }
                } else {
                    dialog.dismiss()
                }

                itemRequestNetworkState.removeObserver(this)
            }
        })
        ListItemsWithContentForSectionJob(section.id, itemRequestNetworkState, false).run()
    }

    private fun startDownload(item: DownloadItem<*, *>) {
        if (item.status.state == DownloadStatus.State.DELETED) {
            item.start(activity)
        }
    }
}
