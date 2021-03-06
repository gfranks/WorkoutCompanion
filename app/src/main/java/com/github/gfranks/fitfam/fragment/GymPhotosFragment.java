package com.github.gfranks.fitfam.fragment;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.github.gfranks.fitfam.data.model.FFGym;
import com.github.gfranks.fitfam.R;
import com.github.gfranks.fitfam.activity.FullScreenGymPhotosActivity;
import com.github.gfranks.fitfam.fragment.base.BaseFragment;
import com.github.gfranks.fitfam.util.GymUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.List;

import javax.inject.Inject;

import butterknife.InjectView;

public class GymPhotosFragment extends BaseFragment {

    public static final String TAG = "gym_photos_fragment";

    @Inject
    Picasso mPicasso;

    @InjectView(R.id.pager)
    ViewPager mViewPager;

    private FFGym mGym;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mGym = getArguments().getParcelable(FFGym.EXTRA);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gym_photos, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewPager.setOffscreenPageLimit(3);
        setGym(mGym);
    }

    public void setGym(FFGym gym) {
        mGym = gym;

        if (isDetached() || getActivity() == null || mGym == null) {
            return;
        }

        mViewPager.setAdapter(new PhotoPagerAdapter(GymUtils.getScaledGymPhotos(getContext(), mGym.getPhotos())));
    }

    private class PhotoPagerAdapter extends PagerAdapter {

        private List<String> mPhotoUrls;

        PhotoPagerAdapter(List<String> photoUrls) {
            mPhotoUrls = photoUrls;
        }

        @Override
        public int getCount() {
            if (mPhotoUrls != null && mPhotoUrls.size() > 0) {
                return mPhotoUrls.size();
            }
            return 1;
        }

        public String getItem(int position) {
            return mPhotoUrls.get(position);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final AppCompatImageView imageView = new AppCompatImageView(container.getContext());
            imageView.setFitsSystemWindows(true);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            Drawable defaultImage = ContextCompat.getDrawable(getContext(), R.drawable.ic_gym);
            if (mPhotoUrls != null && mPhotoUrls.size() > 0) {
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                mPicasso.load(getItem(position))
                        .placeholder(defaultImage)
                        .error(defaultImage)
                        .into(imageView, new Callback() {
                            @Override
                            public void onSuccess() {
                            }

                            @Override
                            public void onError() {
                                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            }
                        });
            } else {
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                imageView.setImageResource(R.drawable.ic_gym);
            }

            container.addView(imageView);

            if (mPhotoUrls != null && mPhotoUrls.size() > 0) {
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), FullScreenGymPhotosActivity.class);
                        intent.putExtra(FFGym.EXTRA, mGym);
                        intent.putExtra(FullScreenGymPhotosActivity.EXTRA_INDEX, mViewPager.getCurrentItem());
                        startActivity(intent);
                    }
                });
            }

            return imageView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((ImageView) object);
        }
    }
}
