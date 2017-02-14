package com.google.photoswall.imageloader;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.LruCache;
import android.widget.ImageView;

import com.google.photoswall.R;
import com.google.photoswall.cache.disk.DiskLruCacheHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
 * des ：图片加载，三级缓存
 * gitVersion：
 * updateAuthor：
 * updateDate：
 * updateDes：
 * ============================================================
 **/
public enum  ImageLoader {
    instance;

    private LruCache<String, BitmapDrawable> mMemoryCache;
    private Set<BitmapWorkerTask> taskCollection;
    private DiskLruCacheHelper helper;
    private Context mContext;
    private Bitmap mLoadingBitmap;

    public ImageLoader init(Context context){
        mContext = context;

        mLoadingBitmap = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.empty_photo);

        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, BitmapDrawable>(cacheSize) {
            @Override
            protected int sizeOf(String key, BitmapDrawable value) {
                return value.getBitmap().getByteCount();
            }
        };

        try {
            helper = new DiskLruCacheHelper(context);
        } catch (IOException e) {
            e.printStackTrace();
        }

        taskCollection = new HashSet<BitmapWorkerTask>();

        return instance;
    }

    /**
     * 加载Bitmap对象。此方法会在LruCache中检查所有屏幕中可见的ImageView的Bitmap对象，
     * 如果发现任何一个ImageView的Bitmap对象不在缓存中，就会开启异步线程去下载图片。
     */
    public void loadBitmaps(ImageView imageView, String imageUrl) {
        try {
            BitmapDrawable drawable = getBitmapFromMemoryCache(imageUrl);
            if (drawable == null) {
                if (cancelPotentialWork(imageUrl,imageView)){
                    BitmapWorkerTask task = new BitmapWorkerTask(imageView);
                    taskCollection.add(task);
                    AsyncDrawable asyncDrawable = new AsyncDrawable(mContext
                            .getResources(), mLoadingBitmap, task);
                    imageView.setImageDrawable(asyncDrawable);
                    task.execute(imageUrl);
                }
            } else {
                if (imageView != null) {
                    imageView.setImageDrawable(drawable);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 取消所有正在下载或等待下载的任务。
     */
    public void cancelAllTasks() {
        if (taskCollection != null) {
            for (BitmapWorkerTask task : taskCollection) {
                task.cancel(false);
            }
        }
    }

    public void fluchCache(){
        try {
            helper.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addBitmapToMemoryCache(String key, BitmapDrawable bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public BitmapDrawable getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }

    class BitmapWorkerTask extends AsyncTask<String, Void, BitmapDrawable> {

        private String imageUrl;
        private WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected BitmapDrawable doInBackground(String... params) {
            imageUrl = params[0];
            BitmapDrawable drawable = (BitmapDrawable) helper.getAsDrawable(imageUrl);
            if (drawable == null){
                drawable = new BitmapDrawable(mContext.getResources(),downloadBitmap(imageUrl));
                helper.put(imageUrl,drawable);
                addBitmapToMemoryCache(imageUrl,drawable);
            }
            return drawable;
        }

        @Override
        protected void onPostExecute(BitmapDrawable drawable) {
            super.onPostExecute(drawable);
            ImageView iv = getAttachedImageView();
            if (iv != null && drawable != null) {
                iv.setImageDrawable(drawable);
            }
            taskCollection.remove(this);
        }

        /**
         * 获取当前BitmapWorkerTask所关联的ImageView。
         */
        private ImageView getAttachedImageView() {
            ImageView imageView = imageViewReference.get();
            BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
            if (this == bitmapWorkerTask) {
                return imageView;
            }
            return null;
        }

        private Bitmap downloadBitmap(String imageUrl) {
            Bitmap bitmap = null;
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10000L,TimeUnit.MILLISECONDS)
                    .readTimeout(10000L,TimeUnit.MILLISECONDS)
                    .build();
            Request request = new Request.Builder().url(imageUrl).build();
            Call call = client.newCall(request);
            try {
                Response response = call.execute();
                bitmap = BitmapFactory.decodeStream(response.body().byteStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

    }

    /**
     * 自定义的一个Drawable，让这个Drawable持有BitmapWorkerTask的弱引用。
     */
    class AsyncDrawable extends BitmapDrawable {

        private WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(
                    bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }

    }

    /**
     * 获取传入的ImageView它所对应的BitmapWorkerTask。
     */
    private BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    /**
     * 取消掉后台的潜在任务，当认为当前ImageView存在着一个另外图片请求任务时
     * ，则把它取消掉并返回true，否则返回false。
     */
    public boolean cancelPotentialWork(String url, ImageView imageView) {
        BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            String imageUrl = bitmapWorkerTask.imageUrl;
            if (imageUrl == null || !imageUrl.equals(url)) {
                bitmapWorkerTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }
}
