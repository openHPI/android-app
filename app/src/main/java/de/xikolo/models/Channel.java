package de.xikolo.models;

import android.graphics.Color;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.squareup.moshi.Json;

import de.xikolo.App;
import de.xikolo.R;
import de.xikolo.config.Config;
import de.xikolo.models.base.RealmAdapter;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import moe.banana.jsonapi2.JsonApi;
import moe.banana.jsonapi2.Resource;

public class Channel extends RealmObject {

    public static final String TAG = Channel.class.getSimpleName();

    @PrimaryKey
    public String id;

    public String title;

    public String color;

    public int position;

    public String description;

    public String imageUrl;

    public VideoStream stageStream;

    public int getColorOrDefault() {
        if (color != null)
            try {
                return Color.parseColor(color);
            } catch (IllegalArgumentException e) {
                if (Config.DEBUG) Log.d(TAG, "Channel color '" + color + "' could not be parsed");
            }

        return ContextCompat.getColor(App.getInstance(), R.color.apptheme_primary);
    }

    @JsonApi(type = "channels")
    public static class JsonModel extends Resource implements RealmAdapter<Channel> {

        public String title;

        public String color;

        public int position;

        public String description;

        @Json(name = "mobile_image_url")
        public String mobileImageUrl;

        @Json(name = "stage_stream")
        public VideoStream stageStream;

        @Override
        public Channel convertToRealmObject() {
            Channel model = new Channel();
            model.id = getId();
            model.title = title;
            model.color = color;
            model.position = position;
            model.description = description;
            model.imageUrl = mobileImageUrl;
            model.stageStream = stageStream;

            return model;
        }

    }

}
