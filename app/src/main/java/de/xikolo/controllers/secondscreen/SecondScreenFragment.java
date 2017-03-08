package de.xikolo.controllers.secondscreen;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import de.xikolo.GlobalApplication;
import de.xikolo.R;
import de.xikolo.controllers.helper.ImageHelper;
import de.xikolo.managers.ItemManager;
import de.xikolo.managers.Result;
import de.xikolo.managers.SecondScreenManager;
import de.xikolo.models.Course;
import de.xikolo.models.Item;
import de.xikolo.models.Module;
import de.xikolo.models.Subtitle;
import de.xikolo.models.VideoItemDetail;
import de.xikolo.models.WebSocketMessage;
import de.xikolo.storages.preferences.ApplicationPreferences;
import de.xikolo.storages.preferences.StorageType;
import de.xikolo.utils.Config;
import de.xikolo.utils.LanalyticsUtil;
import de.xikolo.utils.TimeUtil;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SecondScreenFragment extends Fragment {

    public static final String TAG = SecondScreenFragment.class.getSimpleName();

    private TextView textVideoTitle;

    private View cardVideo;
    private View cardNoVideo;

    private ImageView imageVideoPoster;

    private TextView textVideoTime;

    private LinearLayout layoutVideoActions;

    private static final String KEY_COURSE = "course";
    private static final String KEY_MODULE = "module";
    private static final String KEY_ITEM = "item";
    private static final String KEY_SUBTITLES = "subtitles";

    private Course course;
    private Module module;
    private Item<VideoItemDetail> item;
    private List<Subtitle> subtitleList;

    private ApplicationPreferences appPreferences;

    public SecondScreenFragment() {
        // Required empty public constructor
    }

    public static SecondScreenFragment newInstance() {
        return new SecondScreenFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_COURSE)) {
                course = savedInstanceState.getParcelable(KEY_COURSE);
            }
            if (savedInstanceState.containsKey(KEY_MODULE)) {
                module = savedInstanceState.getParcelable(KEY_MODULE);
            }
            if (savedInstanceState.containsKey(KEY_ITEM)) {
                item = savedInstanceState.getParcelable(KEY_ITEM);
            }
            if (savedInstanceState.containsKey(KEY_SUBTITLES)) {
                subtitleList = savedInstanceState.getParcelableArrayList(KEY_SUBTITLES);
            }
        }

        appPreferences = (ApplicationPreferences) GlobalApplication.getStorage(StorageType.APP);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_second_screen, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cardVideo = view.findViewById(R.id.card_video);
        textVideoTitle = (TextView) view.findViewById(R.id.text_video_title);
        textVideoTime = (TextView) view.findViewById(R.id.text_video_time);
        imageVideoPoster = (ImageView) view.findViewById(R.id.image_video_poster);
        layoutVideoActions = (LinearLayout) view.findViewById(R.id.layout_video_actions);

        cardNoVideo = view.findViewById(R.id.card_no_video);
        cardNoVideo.setVisibility(View.VISIBLE);
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onSecondScreenNewVideoEvent(SecondScreenManager.SecondScreenNewVideoEvent event) {
        if (item == null || !item.equals(event.getItem())) {
            subtitleList = null;
        }

        course = event.getCourse();
        module = event.getModule();
        item = event.getItem();

        if (course != null && module != null && item != null) {

            if (cardVideo != null) {
                if (cardVideo.getVisibility() == View.VISIBLE) {
                    // for animation
                    cardVideo.setVisibility(View.GONE);
                }
                cardNoVideo.setVisibility(View.GONE);
                cardVideo.setVisibility(View.VISIBLE);
                textVideoTitle.setText(item.title);

                textVideoTime.setText(TimeUtil.getTimeString(item.detail.minutes, item.detail.seconds));

                ImageHelper.load(item.detail.stream.poster, imageVideoPoster);

                initSeconScreenActions(event.getWebSocketMessage());

                // clear notification, user is already here
                NotificationManager notificationManager = (NotificationManager) GlobalApplication.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(SecondScreenManager.NOTIFICATION_ID);
            }

            if (appPreferences != null) {
                appPreferences.setUsedSecondScreen(true);
            }
        }
    }

    private void initSeconScreenActions(WebSocketMessage message) {
        if (layoutVideoActions != null) {
            layoutVideoActions.removeAllViews();

            final View viewSlides = addSlidesAction();
            final View viewTranscript = addTranscriptAction();
            final View viewQuiz = addQuizAction();
            final View viewPinboard = addPinboardAction();

            ItemManager itemManager = new ItemManager(GlobalApplication.getInstance().getJobManager());

            // pdf
            if (!"".equals(item.detail.slides_url)) {
                viewSlides.setVisibility(View.VISIBLE);
            }

            // transcript
            if (subtitleList == null) {
                Result<List<Subtitle>> result = new Result<List<Subtitle>>() {
                    @Override
                    protected void onSuccess(List<Subtitle> result, DataSource dataSource) {
                        subtitleList = result;
                        if (subtitleList != null && subtitleList.size() > 0) {
                            viewTranscript.setVisibility(View.VISIBLE);
                        }
                    }
                };

                itemManager.getVideoSubtitles(result, message.payload().get("course_id"), message.payload().get("section_id"), item.id);
            } else {
                if (subtitleList.size() > 0) {
                    viewTranscript.setVisibility(View.VISIBLE);
                }
            }

            // quiz
            if (module != null && module.items != null) {
                final int itemIndex = module.items.indexOf(item);

                final Item nextItem;
                if (itemIndex + 1 < module.items.size()) {
                    nextItem = module.items.get(itemIndex + 1);
                } else {
                    nextItem = null;
                }

                if (nextItem != null && Item.EXERCISE_TYPE_SELFTEST.equals(nextItem.exercise_type)) {
                    viewQuiz.setVisibility(View.VISIBLE);
                    viewQuiz.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getActivity(), QuizActivity.class);
                            intent.putExtra(QuizActivity.ARG_URL, Config.URI + "go/items/" + nextItem.id);
                            intent.putExtra(QuizActivity.ARG_TITLE, item.title + " - " + getString(R.string.second_screen_action_title_quiz));
                            intent.putExtra(QuizActivity.ARG_IN_APP_LINKS, true);
                            intent.putExtra(QuizActivity.ARG_EXTERNAL_LINKS, false);
                            intent.putExtra(QuizActivity.ARG_ITEM, (Parcelable) item);
                            intent.putExtra(QuizActivity.ARG_COURSE, (Parcelable) course);
                            intent.putExtra(QuizActivity.ARG_MODULE, (Parcelable) module);
                            startActivity(intent);

                            if (item != null && module != null && course != null) {
                                LanalyticsUtil.trackVisitedSecondScreenQuiz(item.id, course.id, module.id);
                            }
                        }
                    });
                }
            }

            // pinboard
            viewPinboard.setVisibility(View.VISIBLE);
        }
    }

    private View addSlidesAction() {
        View view = inflateSeconScreenAction(
                R.string.second_screen_action_title_pdf_viewer,
                R.string.second_screen_action_description_pdf_viewer,
                R.string.icon_download_pdf);
        view.setVisibility(View.GONE);
        layoutVideoActions.addView(view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SlideViewerActivity.class);
                intent.putExtra(SlideViewerActivity.ARG_COURSE, (Parcelable) course);
                intent.putExtra(SlideViewerActivity.ARG_MODULE, (Parcelable) module);
                intent.putExtra(SlideViewerActivity.ARG_ITEM, (Parcelable) item);
                startActivity(intent);

                if (item != null && module != null && course != null) {
                    LanalyticsUtil.trackVisitedSecondScreenSlides(item.id, course.id, module.id);
                }
            }
        });

        return view;
    }

    private View addTranscriptAction() {
        View view = inflateSeconScreenAction(
                R.string.second_screen_action_title_transcript,
                R.string.second_screen_action_description_transcript,
                R.string.icon_text);
        view.setVisibility(View.GONE);
        layoutVideoActions.addView(view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), TranscriptViewerActivity.class);
                intent.putExtra(TranscriptViewerActivity.ARG_ITEM, (Parcelable) item);
                intent.putExtra(TranscriptViewerActivity.ARG_COURSE, (Parcelable) course);
                intent.putExtra(TranscriptViewerActivity.ARG_MODULE, (Parcelable) module);
                intent.putParcelableArrayListExtra(TranscriptViewerActivity.ARG_SUBTITLES, (ArrayList<? extends Parcelable>) subtitleList);
                startActivity(intent);

                if (item != null && module != null && course != null) {
                    LanalyticsUtil.trackVisitedSecondScreenTranscript(item.id, course.id, module.id);
                }
            }
        });

        return view;
    }

    private View addQuizAction() {
        View view = inflateSeconScreenAction(
                R.string.second_screen_action_title_quiz,
                R.string.second_screen_action_description_quiz,
                R.string.icon_selftest);
        view.setVisibility(View.GONE);
        layoutVideoActions.addView(view);

        return view;
    }

    private View addPinboardAction() {
        View view = inflateSeconScreenAction(
                R.string.second_screen_action_title_pinboard,
                R.string.second_screen_action_description_pinboard,
                R.string.icon_pinboard);
        view.setVisibility(View.GONE);
        layoutVideoActions.addView(view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PinboardActivity.class);
                intent.putExtra(QuizActivity.ARG_URL, Config.URI + "go/items/" + item.id + "/pinboard");
                intent.putExtra(QuizActivity.ARG_TITLE, item.title + " - " + getString(R.string.tab_discussions));
                intent.putExtra(QuizActivity.ARG_IN_APP_LINKS, true);
                intent.putExtra(QuizActivity.ARG_EXTERNAL_LINKS, false);
                intent.putExtra(QuizActivity.ARG_ITEM, (Parcelable) item);
                intent.putExtra(QuizActivity.ARG_COURSE, (Parcelable) course);
                intent.putExtra(QuizActivity.ARG_MODULE, (Parcelable) module);
                startActivity(intent);

                if (item != null && module != null && course != null) {
                    LanalyticsUtil.trackVisitedSecondScreenPinboard(item.id, course.id, module.id);
                }
            }
        });

        return view;
    }

    private View inflateSeconScreenAction(@StringRes int title, @StringRes int description, @StringRes int icon) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View layout = inflater.inflate(R.layout.item_second_screen, null);

        TextView textTitle = (TextView) layout.findViewById(R.id.text_action_title);
        textTitle.setText(getContext().getString(title));

        TextView textDescription = (TextView) layout.findViewById(R.id.text_action_description);
        textDescription.setText(getContext().getString(description));

        TextView textIcon = (TextView) layout.findViewById(R.id.text_icon_action);
        textIcon.setText(getContext().getString(icon));

        return layout;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSecondScreenUpdateVideoEvent(SecondScreenManager.SecondScreenUpdateVideoEvent event) {
        item = event.getItem();

        if (event.getWebSocketMessage().payload().containsKey("current_time")) {
            textVideoTime.setText(
                    TimeUtil.getTimeString(TimeUtil.secondsToMillis(event.getWebSocketMessage().payload().get("current_time"))) +
                    " / " +
                    TimeUtil.getTimeString(item.detail.minutes, item.detail.seconds)
            );
        }

        if (cardVideo != null && event.getWebSocketMessage().action().equals("video_close")) {
            cardVideo.setVisibility(View.GONE);
            cardNoVideo.setVisibility(View.VISIBLE);

            course = null;
            module = null;
            item = null;
            subtitleList = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (course != null) {
//            outState.putParcelable(KEY_COURSE, course);
        }
        if (module != null) {
            outState.putParcelable(KEY_MODULE, module);
        }
        if (item != null) {
            outState.putParcelable(KEY_ITEM, item);
        }
        if (subtitleList != null) {
            outState.putParcelableArrayList(KEY_SUBTITLES, (ArrayList<Subtitle>) subtitleList);
        }
        super.onSaveInstanceState(outState);
    }

}
