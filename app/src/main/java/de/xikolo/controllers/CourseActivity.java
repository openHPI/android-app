package de.xikolo.controllers;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import de.xikolo.BuildConfig;
import de.xikolo.R;
import de.xikolo.controllers.course.CourseLearningsFragment;
import de.xikolo.controllers.course.ProgressFragment;
import de.xikolo.controllers.dialogs.ProgressDialog;
import de.xikolo.controllers.dialogs.UnenrollDialog;
import de.xikolo.controllers.helper.CacheController;
import de.xikolo.controllers.helper.EnrollmentController;
import de.xikolo.events.NetworkStateEvent;
import de.xikolo.managers.CourseManager;
import de.xikolo.managers.Result;
import de.xikolo.models.Course;
import de.xikolo.utils.BuildFlavor;
import de.xikolo.utils.Config;
import de.xikolo.utils.DeepLinkingUtil;
import de.xikolo.utils.LanalyticsUtil;
import de.xikolo.utils.ToastUtil;

public class CourseActivity extends BaseActivity implements UnenrollDialog.UnenrollDialogListener {

    public static final String TAG = CourseActivity.class.getSimpleName();

    public static final String ARG_COURSE = "arg_course";

    private Course course;

    private ViewPager viewPager;
    private CoursePagerAdapter adapter;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blank_tabs);
        setupActionBar();

        // Initialize the ViewPager and set an adapter
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        viewPager = (ViewPager) findViewById(R.id.viewpager);

        String action = getIntent().getAction();

        if (action != null && action.equals(Intent.ACTION_VIEW)) {
            handleDeepLinkIntent(getIntent());
        } else {
            Bundle b = getIntent().getExtras();
            if (b == null || !b.containsKey(ARG_COURSE)) {
                CacheController cacheController = new CacheController();
                cacheController.readCachedExtras();
                if (cacheController.getCourse() != null) {
                    course = cacheController.getCourse();
                }
                if (course != null) {
                    Bundle restartBundle = new Bundle();
//                    restartBundle.putParcelable(ARG_COURSE, course);
                    Intent restartIntent = new Intent(CourseActivity.this, CourseActivity.class);
                    restartIntent.putExtras(restartBundle);
                    finish();
                    startActivity(restartIntent);
                }
            } else {
                this.course = b.getParcelable(ARG_COURSE);
                setupView(0);
            }
        }
    }

    private void setupView(int firstItem) {
        setTitle(course.title);

        adapter = new CoursePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(2);

        // Bind the tabs to the ViewPager
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setOnTabSelectedListener(adapter);

        viewPager.setCurrentItem(firstItem);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null) {
            String action = intent.getAction();

            if (action != null && action.equals(Intent.ACTION_VIEW)) {
                handleDeepLinkIntent(intent);
            }
        }
    }

    private void handleDeepLinkIntent(Intent intent) {
        final Uri data = intent.getData();
        final String courseIntent = DeepLinkingUtil.getCourseIdentifierFromResumeUri(data);

        final ProgressDialog progressDialog = ProgressDialog.getInstance();

        Result<List<Course>> result = new Result<List<Course>>() {

            @Override
            protected void onSuccess(List<Course> result, DataSource dataSource) {
                super.onSuccess(result, dataSource);

                if (dataSource == DataSource.NETWORK) {
                    for (Course fetchedCourse : result) {
                        if (fetchedCourse.slug.equals(courseIntent)) {
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }

                            course = fetchedCourse;
                            if (!course.accessible || !course.is_enrolled) {
                                setTitle(course.title);

                                if (course.accessible) {
                                    ToastUtil.show(R.string.notification_course_locked);
                                } else if (!course.is_enrolled) {
                                    ToastUtil.show(R.string.notification_not_enrolled);
                                }

                                Intent intent = new Intent(CourseActivity.this, CourseDetailsActivity.class);
                                Bundle b = new Bundle();
//                                b.putParcelable(CourseDetailsActivity.ARG_COURSE, course);
                                intent.putExtras(b);
                                startActivity(intent);
                                finish();
                            } else {
                                DeepLinkingUtil.CourseTab courseTab = DeepLinkingUtil.getTab(data.getPath());

                                int firstFragment = 0;
                                if (courseTab != null) {
                                    switch (courseTab) {
                                        case RESUME:
                                            firstFragment = 0;
                                            break;
                                        case PINBOARD:
                                            firstFragment = 1;
                                            break;
                                        case PROGRESS:
                                            firstFragment = 2;
                                            break;
                                        case LEARNING_ROOMS:
                                            firstFragment = 3;
                                            break;
                                        case ANNOUNCEMENTS:
                                            firstFragment = 4;
                                            break;
                                        case DETAILS:
                                            firstFragment = 5;
                                            break;
                                    }
                                }

                                setupView(firstFragment);
                            }
                            break;
                        }
                    }
                }

            }

            @Override
            protected void onWarning(WarnCode warnCode) {
                super.onWarning(warnCode);
            }

            @Override
            protected void onError(ErrorCode errorCode) {
                super.onError(errorCode);
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }

                ToastUtil.show(R.string.error);

                finish();
            }
        };

        CourseManager courseManager = new CourseManager(jobManager);
        courseManager.requestCourses();
        progressDialog.show(getSupportFragmentManager(), ProgressDialog.TAG);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.unenroll, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onNetworkEvent(NetworkStateEvent event) {
        super.onNetworkEvent(event);

        if (tabLayout != null) {
            if (event.isOnline()) {
                tabLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.apptheme_toolbar));
            } else {
                tabLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.offline_mode_toolbar));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        adapter.getItem(viewPager.getCurrentItem()).onActivityResult(requestCode, resultCode, data);
    }

    public class CoursePagerAdapter extends FragmentPagerAdapter implements TabLayout.OnTabSelectedListener {

        private final List<String> TITLES;

        {
            TITLES = new ArrayList<>();
            TITLES.add(getString(R.string.tab_learnings));
            TITLES.add(getString(R.string.tab_discussions));
            TITLES.add(getString(R.string.tab_progress));
            TITLES.add(getString(R.string.tab_rooms));
            TITLES.add(getString(R.string.tab_details));
            TITLES.add(getString(R.string.tab_announcements));

            if (BuildConfig.X_FLAVOR == BuildFlavor.OPEN_HPI) {
                TITLES.add(getString(R.string.tab_quiz_recap));
            }
        }

        private FragmentManager mFragmentManager;

        public CoursePagerAdapter(FragmentManager fm) {
            super(fm);
            mFragmentManager = fm;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES.get(position);
        }

        @Override
        public int getCount() {
            return TITLES.size();
        }

        @Override
        public Fragment getItem(int position) {
            // Check if this Fragment already exists.
            // Fragment Name is saved by FragmentPagerAdapter implementation.
            String name = makeFragmentName(R.id.viewpager, position);
            Fragment fragment = mFragmentManager.findFragmentByTag(name);
            if (fragment == null) {
                switch (position) {
                    case 0:
                        fragment = CourseLearningsFragment.newInstance(course);
                        break;
                    case 1:
                        fragment = WebViewFragment.newInstance(Config.URI + Config.COURSES + course.slug + "/" + Config.DISCUSSIONS, true, false);
                        break;
                    case 2:
                        fragment = ProgressFragment.newInstance(course);
                        break;
                    case 3:
                        fragment = WebViewFragment.newInstance(Config.URI + Config.COURSES + course.slug + "/" + Config.ROOMS, true, false);
                        break;
                    case 4:
                        fragment = WebViewFragment.newInstance(Config.URI + Config.COURSES + course.slug, false, false);
                        break;
                    case 5:
                        fragment = WebViewFragment.newInstance(Config.URI + Config.COURSES + course.slug + "/" + Config.ANNOUNCEMENTS, false, false);
                        break;
                    case 6:
                        fragment = WebViewFragment.newInstance(Config.URI + Config.QUIZ_RECAP + course.id, true, false);
                        break;
                }
            }
            return fragment;
        }

        private String makeFragmentName(int viewId, int index) {
            return "android:switcher:" + viewId + ":" + index;
        }

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            viewPager.setCurrentItem(tabLayout.getSelectedTabPosition(), true);
            switch (tabLayout.getSelectedTabPosition()) {
                case 1:
                    LanalyticsUtil.trackVisitedPinboard(course.id);
                    break;
                case 2:
                    LanalyticsUtil.trackVisitedProgress(course.id);
                    break;
                case 3:
                    LanalyticsUtil.trackVisitedLearningRooms(course.id);
                    break;
                case 5:
                    LanalyticsUtil.trackVisitedAnnouncements(course.id);
                    break;
                case 6:
                    LanalyticsUtil.trackVisitedRecap(course.id);
                    break;
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    }

}
