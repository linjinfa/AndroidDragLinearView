package com.aaron.dragview.demo.util;

import android.content.Context;
import android.widget.ImageView;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

/**
 * Created by wanghuan on 15/1/8.
 */
public class ImageLoaderUtil {

    /**
     * ImageLoader display
     * @param context
     * @param url
     * @param imageAware
     * @param displayImageOptions
     */
    public static void displayImage(Context context , String url , ImageView imageAware, DisplayImageOptions displayImageOptions){
        if(ImageLoader.getInstance().isInited()){
            ImageLoader.getInstance().displayImage(url , imageAware, displayImageOptions);
        }else{
            init(context);
            ImageLoader.getInstance().displayImage(url, imageAware, displayImageOptions);
        }
    }

    /**
     *
     * @param context
     */
    public static void init(Context context){
        ImageLoaderConfiguration configuration = new ImageLoaderConfiguration.Builder(context)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                .diskCacheSize(50 * 1024 * 1024) // 50M
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .build();
        ImageLoader.getInstance().init(configuration);
    }

}
