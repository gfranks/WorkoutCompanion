package com.github.gfranks.workoutcompanion.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.util.List;

public class WCGymHours implements Parcelable, Type {

    public static final Parcelable.Creator<WCGymHours> CREATOR = new Parcelable.Creator<WCGymHours>() {
        public WCGymHours createFromParcel(Parcel in) {
            return new WCGymHours(in);
        }

        public WCGymHours[] newArray(int size) {
            return new WCGymHours[size];
        }
    };

    @SerializedName("open_now")
    private boolean open_now;
    @SerializedName("weekday_text")
    private List<String> weekday_text;

    public WCGymHours() {
    }

    public WCGymHours(Parcel in) {
        readFromParcel(in);
    }

    public WCGymHours(Builder builder) {
        open_now = builder.open_now;
        weekday_text = builder.weekday_text;
    }

    public boolean isOpen_now() {
        return open_now;
    }

    public void setOpen_now(boolean open_now) {
        this.open_now = open_now;
    }

    public List<String> getWeekday_text() {
        return weekday_text;
    }

    public void setWeekday_text(List<String> weekday_text) {
        this.weekday_text = weekday_text;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeValue(open_now ? 1 : 0);
        out.writeList(weekday_text);
    }

    private void readFromParcel(Parcel in) {
        open_now = ((int) in.readValue(Integer.class.getClassLoader())) == 1;
        weekday_text = in.readArrayList(String.class.getClassLoader());
    }

    public static class Builder {

        private boolean open_now;
        private List<String> weekday_text;

        public Builder() {
        }

        public Builder setOpen_now(boolean open_now) {
            this.open_now = open_now;

            return this;
        }

        public Builder setWeekday_text(List<String> weekday_text) {
            this.weekday_text = weekday_text;

            return this;
        }

        public WCGymHours build() {
            return new WCGymHours(this);
        }
    }
}
