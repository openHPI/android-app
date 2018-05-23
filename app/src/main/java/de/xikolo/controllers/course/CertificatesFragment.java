package de.xikolo.controllers.course;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yatatsu.autobundle.AutoBundleField;

import butterknife.BindView;
import de.xikolo.App;
import de.xikolo.R;
import de.xikolo.controllers.base.LoadingStatePresenterFragment;
import de.xikolo.models.Course;
import de.xikolo.presenters.base.PresenterFactory;
import de.xikolo.presenters.course.CertificatesPresenter;
import de.xikolo.presenters.course.CertificatesPresenterFactory;
import de.xikolo.presenters.course.CertificatesView;
import de.xikolo.utils.IntentUtil;

public class CertificatesFragment extends LoadingStatePresenterFragment<CertificatesPresenter, CertificatesView> implements CertificatesView {

    public final static String TAG = CertificatesFragment.class.getSimpleName();

    @AutoBundleField String courseId;

    @BindView(R.id.content_view) NestedScrollView scrollView;
    @BindView(R.id.container) LinearLayout container;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public int getLayoutResource() {
        return R.layout.content_certificates;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void showCertificates(Course course) {
        boolean isEnrolled = course.isEnrolled();

        container.removeAllViews();

        if (course.certificates.qualifiedCertificateAvailable)
            setupItem(getString(R.string.course_qualified_certificate_desc),
                    isEnrolled,
                    course.certificates.qualifiedCertificateUrl != null,
                    v -> IntentUtil.openDoc(App.getInstance(), course.certificates.qualifiedCertificateUrl)
            );

        if (course.certificates.recordOfAchievementAvailable)
            setupItem(getString(R.string.course_record_of_achievement_desc, course.certificates.recordOfAchievementThreshold),
                    isEnrolled,
                    course.certificates.recordOfAchievementUrl != null,
                    v -> IntentUtil.openDoc(App.getInstance(), course.certificates.recordOfAchievementUrl)
            );

        if (course.certificates.confirmationOfParticipationAvailable)
            setupItem(getString(R.string.course_confirmation_of_participation_desc, course.certificates.confirmationOfParticipationThreshold),
                    isEnrolled,
                    course.certificates.confirmationOfParticipationUrl != null,
                    v -> IntentUtil.openDoc(App.getInstance(), course.certificates.confirmationOfParticipationUrl)
            );
    }

    private void setupItem(String text, boolean enrolled, boolean documentAvailable, View.OnClickListener downloadClickListener) {
        View v = getLayoutInflater().inflate(R.layout.item_certificate, null);
        ((TextView) v.findViewById(R.id.textContent)).setText(Html.fromHtml(text));
        Button downloadButton = v.findViewById(R.id.button_certificate_download);
        if (documentAvailable) {
            downloadButton.setText(R.string.course_certificate_view);
            downloadButton.setOnClickListener(downloadClickListener);
        } else {
            if (enrolled) {
                downloadButton.setEnabled(false);
                downloadButton.setText(R.string.course_certificate_not_available);
            } else {
                downloadButton.setVisibility(View.GONE);
            }
        }

        container.addView(v);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_refresh:
                onRefresh();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    @Override
    protected PresenterFactory<CertificatesPresenter> getPresenterFactory() {
        return new CertificatesPresenterFactory(courseId);
    }

}
