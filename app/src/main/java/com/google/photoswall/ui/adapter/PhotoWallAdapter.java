package com.google.photoswall.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.google.photoswall.R;
import com.google.photoswall.imageloader.ImageLoader;

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
 * des ：adapter
 * gitVersion：
 * updateAuthor：
 * updateDate：
 * updateDes：
 * ============================================================
 **/

public class PhotoWallAdapter extends ArrayAdapter<String> {

	private int mItemHeight = 0;
	private Context mContext;

	public PhotoWallAdapter(Context context, int textViewResourceId, String[] objects) {
		super(context, textViewResourceId, objects);
		mContext = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final String url = getItem(position);
		View view;
		if (convertView == null) {
			view = LayoutInflater.from(getContext()).inflate(R.layout.photo_layout, null);
		} else {
			view = convertView;
		}
		final ImageView imageView = (ImageView) view.findViewById(R.id.photo);
		if (imageView.getLayoutParams().height != mItemHeight) {
			imageView.getLayoutParams().height = mItemHeight;
		}
		ImageLoader.instance.init(mContext).loadBitmaps(imageView,url);
		return view;
	}

	/**
	 * 设置item子项的高度。
	 */
	public void setItemHeight(int height) {
		if (height == mItemHeight) {
			return;
		}
		mItemHeight = height;
		notifyDataSetChanged();
	}

}