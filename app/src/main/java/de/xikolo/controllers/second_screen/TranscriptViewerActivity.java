package de.xikolo.controllers.second_screen;

import de.xikolo.controllers.base.BaseActivity;

public class TranscriptViewerActivity extends BaseActivity {

//    public static final String TAG = TranscriptViewerActivity.class.getSimpleName();
//
//    public static final String ARG_COURSE = "arg_course";
//    public static final String ARG_MODULE = "arg_module";
//    public static final String ARG_ITEM = "arg_item";
//
//    public static final String ARG_SUBTITLES = "arg_subtitles";
//
//    private Course course;
//    private Section module;
//    private Item item;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_transcript);
//        setupActionBar();
//
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        Bundle b = getIntent().getExtras();
//        course = b.getParcelable(ARG_COURSE);
//        module = b.getParcelable(ARG_MODULE);
//        item = b.getParcelable(ARG_ITEM);
//        List<SubtitleTrack> subtitleList = b.getParcelableArrayList(ARG_SUBTITLES);
//
//        setMessageTitle(item.title + " - " + getString(R.string.second_screen_transcript));
//
//        String tag = "content";
//
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        if (fragmentManager.findFragmentByTag(tag) == null) {
//            FragmentTransaction transaction = fragmentManager.beginTransaction();
////            transaction.replace(R.id.content, TranscriptViewerFragment.newInstance(item, subtitleList), tag);
//            transaction.commit();
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        if (course != null && module != null && item != null) {
//            LanalyticsUtil.trackSecondScreenTranscriptStart(item.id, course.id, module.id);
//        }
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//
//        if (course != null && module != null && item != null) {
//            LanalyticsUtil.trackSecondScreenTranscriptStop(item.id, course.id, module.id);
//        }
//    }

}
