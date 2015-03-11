package com.aaron.dragview.demo;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.aaron.dragview.demo.util.ImageLoaderUtil;
import com.aaron.dragview.demo.view.DragLinearView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.LinkedList;

/**
 * Created by linjinfa 331710168@qq.com on 2015/3/11.
 */
public class MainActivity extends Activity {

    private DragLinearView dragLinearView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dragLinearView = (DragLinearView) findViewById(R.id.dragLinerView);

//        dragLinearView.setDisableDrag(false);
//        dragLinearView.setShowAddImg(true);
//        dragLinearView.setShowDelBtn(true);
        //设置最大行数
        dragLinearView.setMaxRows(2);
        //设置一行的个数
        dragLinearView.setMaxRowsItemCount(4);

        dragLinearView.setOnAddClickListener(new DragLinearView.OnAddClickListener() {
            @Override
            public void onAddClick() {
                dragLinearView.addDelayItemView(BitmapFactory.decodeResource(getResources(),R.drawable.test),null);
            }
        });
        dragLinearView.setOnItemViewListener(new DragLinearView.OnItemViewListener() {
            @Override
            public void onAddItem(ImageView imageView, Object tag) {
                Toast.makeText(MainActivity.this,"添加成功的回调",Toast.LENGTH_SHORT).show();
                if(tag!=null && !TextUtils.isEmpty(tag.toString())){
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    DisplayImageOptions displayImageOptions = new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisk(true).build();
                    ImageLoaderUtil.displayImage(MainActivity.this,tag.toString(),imageView,displayImageOptions);
                }
            }

            @Override
            public void onItemClick(View itemView, Object tag) {
                Toast.makeText(MainActivity.this,"点击",Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 一次性添加多张图片
     * @param view
     */
    public void addMutilImgNoAnimClick(View view){
        dragLinearView.removeAllItemView();
        LinkedList<DragLinearView.ImageTagElement> imageTagElementList = new LinkedList<DragLinearView.ImageTagElement>();
        for(int i=0;i<8;i++){
            imageTagElementList.add(new DragLinearView.ImageTagElement(BitmapFactory.decodeResource(getResources(),R.drawable.test),null));
        }
        dragLinearView.addMutilItemView(imageTagElementList,false);
    }

    /**
     * 一次性添加多张图片
     * @param view
     */
    public void addMutilImgClick(View view){
        dragLinearView.removeAllItemView();
        LinkedList<DragLinearView.ImageTagElement> imageTagElementList = new LinkedList<DragLinearView.ImageTagElement>();
        for(int i=0;i<8;i++){
            imageTagElementList.add(new DragLinearView.ImageTagElement(BitmapFactory.decodeResource(getResources(),R.drawable.test),null));
        }
        dragLinearView.addMutilItemView(imageTagElementList);
    }

    /**
     * 异步加载图片
     * @param view
     */
    public void addAsyncMutilImgClick(View view){
        dragLinearView.removeAllItemView();
        LinkedList<DragLinearView.ImageTagElement> imageTagElementList = new LinkedList<DragLinearView.ImageTagElement>();
        String url = "http://www.iyi8.com/uploadfile/2014/1102/20141102084643851.jpg";
        for(int i=0;i<8;i++){
            imageTagElementList.add(new DragLinearView.ImageTagElement(null,url));
        }
        dragLinearView.addMutilItemView(imageTagElementList);
    }

}
