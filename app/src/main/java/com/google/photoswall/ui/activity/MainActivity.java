package com.google.photoswall.ui.activity;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewTreeObserver;
import android.widget.GridView;

import com.google.photoswall.R;
import com.google.photoswall.imageloader.ImageLoader;
import com.google.photoswall.provider.Images;
import com.google.photoswall.ui.adapter.PhotoWallAdapter;

/**
 * ============================================================
 * Copyright：Google有限公司版权所有 (c) 2016
 * Author：   陈冠杰
 * Email：    815712739@qq.com
 * GitHub：   https://github.com/JackChen1999
 *
 * Project_Name：PhotosWall
 * Package_Name：com.google.photoswall.imageloader
 * Version：1.0
 * time：2016/8/6 16:01
 * des ：照片墙
 * gitVersion：
 * updateAuthor：
 * updateDate：
 * updateDes：
 * ============================================================
 **/
public class MainActivity extends AppCompatActivity {

	private GridView mPhotoWall;
	private PhotoWallAdapter mAdapter;

	private int mImageThumbSize;
	private int mImageThumbSpacing;

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mImageThumbSize = getResources().getDimensionPixelSize(
				R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(
				R.dimen.image_thumbnail_spacing);
		mPhotoWall = (GridView) findViewById(R.id.photo_wall);
		mAdapter = new PhotoWallAdapter(this, 0, Images.imageThumbUrls);
		mPhotoWall.setAdapter(mAdapter);
		mPhotoWall.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					
					@Override
					public void onGlobalLayout() {
						final int numColumns = (int) Math.floor(mPhotoWall
								.getWidth() / (mImageThumbSize + mImageThumbSpacing));
						if (numColumns > 0) {
							int columnWidth = (mPhotoWall.getWidth() / numColumns)
									- mImageThumbSpacing;
							mAdapter.setItemHeight(columnWidth);
							mPhotoWall.getViewTreeObserver().removeGlobalOnLayoutListener(this);
						}
					}
				});
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		ImageLoader.instance.fluchCache();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ImageLoader.instance.cancelAllTasks();
	}

}