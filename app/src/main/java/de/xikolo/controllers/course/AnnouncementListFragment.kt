package de.xikolo.controllers.course

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.yatatsu.autobundle.AutoBundleField
import de.xikolo.R
import de.xikolo.controllers.announcement.AnnouncementActivityAutoBundle
import de.xikolo.controllers.base.ViewModelFragment
import de.xikolo.controllers.main.AnnouncementListAdapter
import de.xikolo.extensions.observe
import de.xikolo.models.Announcement
import de.xikolo.utils.LanalyticsUtil
import de.xikolo.viewmodels.main.AnnouncementListViewModel

class AnnouncementListFragment : ViewModelFragment<AnnouncementListViewModel>() {

    companion object {
        val TAG: String = AnnouncementListFragment::class.java.simpleName
    }

    @AutoBundleField
    internal lateinit var courseId: String

    @BindView(R.id.content_view)
    internal lateinit var recyclerView: RecyclerView

    private lateinit var announcementListAdapter: AnnouncementListAdapter

    override val layoutResource = R.layout.fragment_announcement_list

    override fun createViewModel(): AnnouncementListViewModel {
        return AnnouncementListViewModel(courseId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        announcementListAdapter = AnnouncementListAdapter({ announcementId -> openAnnouncement(announcementId) }, false)

        val layoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = layoutManager
        val dividerItemDecoration = DividerItemDecoration(recyclerView.context, layoutManager.orientation)
        recyclerView.addItemDecoration(dividerItemDecoration)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = announcementListAdapter

        viewModel.announcements
            .observe(viewLifecycleOwner) {
                showAnnouncementList(it)
            }
    }

    private fun showAnnouncementList(announcements: List<Announcement>) {
        if (announcements.isEmpty()) {
            showEmptyMessage(R.string.empty_message_course_announcements_title)
        } else {
            showContent()
            announcementListAdapter.update(announcements)
        }
    }

    private fun openAnnouncement(announcementId: String) {
        val intent = AnnouncementActivityAutoBundle.builder(announcementId, false).build(activity!!)
        startActivity(intent)
        LanalyticsUtil.trackVisitedAnnouncementDetail(announcementId)
    }
}
