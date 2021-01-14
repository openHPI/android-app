package de.xikolo.controllers.channels

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.app.NavUtils
import com.yatatsu.autobundle.AutoBundleField
import de.xikolo.R
import de.xikolo.config.GlideApp
import de.xikolo.controllers.base.CollapsingToolbarViewModelActivity
import de.xikolo.controllers.dialogs.CreateTicketDialog
import de.xikolo.controllers.dialogs.CreateTicketDialogAutoBundle
import de.xikolo.extensions.observe
import de.xikolo.models.Channel
import de.xikolo.utils.extensions.shareCourseLink
import de.xikolo.viewmodels.channel.ChannelViewModel

class ChannelDetailsActivity : CollapsingToolbarViewModelActivity<ChannelViewModel>() {

    companion object {
        val TAG: String = ChannelDetailsActivity::class.java.simpleName
    }

    @AutoBundleField
    lateinit var channelId: String

    //-1 do not scroll to Course
    @AutoBundleField(required = false)
    var scrollToCoursePosition = -1

    override fun createViewModel(): ChannelViewModel {
        return ChannelViewModel(channelId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.channel
            .observe(this) {
                setupView(it)
            }
    }

    private fun setupView(channel: Channel) {
        title = channel.title

        val color = channel.colorOrDefault
        collapsingToolbar.setContentScrimColor(color)
        collapsingToolbar.setBackgroundColor(color)
        collapsingToolbar.setStatusBarScrimColor(color)

        if (channel.stageStream?.hlsUrl != null ||
            channel.stageStream?.hdUrl != null ||
            channel.stageStream?.sdUrl != null
        ) {
            imageView.visibility = View.GONE
            lockCollapsingToolbar(channel.title)
        } else if (channel.imageUrl != null) {
            imageView.visibility = View.VISIBLE
            GlideApp.with(this).load(channel.imageUrl).into(imageView)
        } else {
            imageView.visibility = View.GONE
            lockCollapsingToolbar(channel.title)
        }

        val tag = "content"
        val fragmentManager = supportFragmentManager
        if (fragmentManager.findFragmentByTag(tag) == null) {
            val fragment = ChannelDetailsFragmentAutoBundle.builder(channelId)
                .scrollToCoursePosition(scrollToCoursePosition).build()
            val transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.content, fragment, tag)
            transaction.commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.helpdesk, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home    -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
            R.id.action_share    -> {
                shareCourseLink(channelId)
                return true
            }
            R.id.action_helpdesk -> {
                val dialog = CreateTicketDialogAutoBundle.builder().build()
                dialog.show(supportFragmentManager, CreateTicketDialog.TAG)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
