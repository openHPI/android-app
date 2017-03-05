package de.xikolo.controllers;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import de.xikolo.R;
import de.xikolo.controllers.activities.BaseActivity;
import de.xikolo.controllers.dialogs.UnenrollDialog;
import de.xikolo.controllers.helper.EnrollmentController;
import de.xikolo.models.Course;
import de.xikolo.utils.Config;

public class CourseDetailsActivity extends BaseActivity implements UnenrollDialog.UnenrollDialogListener {

    public static final String TAG = CourseDetailsActivity.class.getSimpleName();

    public static final String ARG_COURSE = "arg_course";

    private Course course;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blank);
        setupActionBar();

        Bundle b = getIntent().getExtras();
        this.course = b.getParcelable(ARG_COURSE);

        if (course != null) {
            setTitle(course.title);
        }

        String tag = "content";

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag(tag) == null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.content, WebViewFragment.newInstance(Config.URI + Config.COURSES + course.slug, false, false), tag);
            transaction.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (course != null && course.isEnrolled()) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.unenroll, menu);
        }
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_unenroll:
                UnenrollDialog dialog = new UnenrollDialog();
                dialog.setUnenrollDialogListener(this);
                dialog.show(getSupportFragmentManager(), UnenrollDialog.TAG);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        EnrollmentController.unenroll(this, course);
    }

}
