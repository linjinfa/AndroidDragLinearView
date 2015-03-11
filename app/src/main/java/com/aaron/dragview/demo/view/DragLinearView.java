package com.aaron.dragview.demo.view;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.aaron.dragview.demo.R;
import com.aaron.dragview.demo.util.FloatRoute;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;


/**
 * 添加子view 可左右拖动 长按删除
 * Created by linjinfa 331710168@qq.com on 2014/12/17.
 */
public class DragLinearView extends LinearLayout {

    /**
     * 子View之间的间隔
     */
    private final int ITEM_SPACE = 10;
    /**
     * 每一行最大的子View个数
     */
    private int maxRowsItemCount = 5;
    /**
     * 最大行数
     */
    private int maxRows = 1;
    /**
     * 子View宽度
     */
    private int itemWidth;
    /**
     * 子View高度
     */
    private int itemHeight;
    /**
     * 是否正在执行动画
     */
    private boolean isAniming = false;
    /**
     * 当前被长按的View
     */
    private View currLongView;
    /**
     * 拖拽的 BitmapDrawable
     */
    private BitmapDrawable dragBitmapDrawable;
    /**
     *
     */
    private Rect currRect = new Rect();
    /**
     * 记录touch坐标
     */
    private FloatRoute touchFloatRoute = new FloatRoute();
    /**
     * 是否拦截Touch
     */
    private boolean isInterceptTouch = false;
    /**
     * 是否显示删除按钮
     */
    private boolean isShowDelBtn = true;
    /**
     * 是否禁用拖动
     */
    private boolean isDisableDrag = false;
    /**
     * 是否显示加号view
     */
    private boolean isShowAddImg = true;
    /**
     * 加好 ImageView
     */
    private ImageView addImgView;
    /**
     * 晃动Anima
     */
    private AnimatorSet wobbleAnimatorSet;
    /**
     * 增加子View 事件
     */
    private OnAddClickListener onAddClickListener;
    /**
     *
     */
    private OnItemViewListener onItemViewListener;
    private MoveChildRunnable moveChildRunnable = new MoveChildRunnable();
    /**
     * 要一次性添加的多个ImageTagElement队列
     */
    private Queue<ImageTagElement> bitmapQueue = new LinkedBlockingDeque<ImageTagElement>();
    private LinkedList<ImageTagElement> imageTagElementList;
    /**
     * 是否初始化
     */
    private boolean isInitAddItemView = false;

    public DragLinearView(Context context) {
        super(context);
        init();
    }

    public DragLinearView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DragLinearView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//		for (int index = 0; index < getChildCount(); index++) {
//			final View child = getChildAt(index);
//			child.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
//		}
        if (itemWidth == 0 && getMeasuredWidth() != 0) {
            itemWidth = getMeasuredWidth() / maxRowsItemCount;
            itemHeight = itemWidth;
        }
        int currRowsCount = getCurrRowsCount();
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(currRowsCount * itemHeight + (currRowsCount - 1) * ITEM_SPACE, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (itemWidth != 0) {
            layoutChildView();
            if (isInitAddItemView) {
                isInitAddItemView = false;
                addMutilItemView();
            }
        }
    }

    /**
     * 初始化
     */
    private void init() {
        removeAllViews();
        if(isShowAddImg){
            addAddImageView();
        }
    }

    /**
     * 删除所有子View
     */
    public void removeAllItemView() {
        bitmapQueue.clear();
        if(imageTagElementList!=null){
            imageTagElementList.clear();
        }
        stopWobbleAnim();
        clearAnimation();
        init();
        requestLayout();
    }

    /**
     * 添加加号ImageView
     */
    private void addAddImageView() {
        Bitmap addBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.wdspk_icon_2);
        if (itemHeight == 0) {
            itemHeight = addBitmap.getHeight() + ITEM_SPACE;
            setMinimumHeight(itemHeight);
        }
        addImgView = createImageView(addBitmap);
        addImgView.setBackgroundColor(Color.WHITE);
        addImgView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAniming)
                    return;
                if (onAddClickListener != null) {
                    onAddClickListener.onAddClick();
                }
            }
        });
        setAnimation(null);
        addView(addImgView);
    }

    /**
     * 删除加号ImageView
     */
    private boolean delAddImageView(int addCount) {
        if (getChildCount() + addCount - 1 == maxRows * maxRowsItemCount && isAddChildView(getChildAt(getChildCount() - 1))) {    //超过指定的个数 删除最后一个加号
            removeViewAt(getChildCount() - 1);
            return true;
        }
        return false;
    }

    /**
     * layout子View
     */
    private void layoutChildView() {
        int lineTotalRight = 0;
        int left = 0;
        int right = 0;
        int top = 0;
        int bottom = 0;
        int rowCount = 0; // 行数
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View childView = getChildAt(i);
            if (lineTotalRight >= getMeasuredWidth()) {
                lineTotalRight = 0;
                rowCount++;
            }
            int rowItemNum = (i - rowCount * maxRowsItemCount);
            left = rowItemNum * itemWidth;
//			left = rowItemNum * (childView.getMeasuredWidth()+ITEM_SPACE);
            right = left + itemWidth;
//			right = left + childView.getMeasuredWidth();
            top = rowCount * itemHeight;
            if (rowCount != 0) {
                top += ITEM_SPACE;
            }
            bottom = top + itemHeight;
            childView.layout(left, top, right, bottom);
            lineTotalRight = right;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            touchFloatRoute.setDown(ev.getX(), ev.getY());
        }
        return isInterceptTouch;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (dragBitmapDrawable == null || getChildCount() < 1)
            return super.dispatchTouchEvent(event);
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                touchFloatRoute.setDown(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                touchFloatRoute.setCurrent(event.getX(), event.getY());
                actionMove();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isInterceptTouch = false;
                dragBitmapDrawable = null;
                currLongView.setVisibility(View.VISIBLE);
                touchFloatRoute.reset();
                stopWobbleAnim();
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (dragBitmapDrawable != null) {
            dragBitmapDrawable.setBounds(currRect);
            dragBitmapDrawable.draw(canvas);
        }
    }

    /**
     * ACTION_MOVE 处理
     */
    private void actionMove() {
        int deltaX = (int) touchFloatRoute.getDeltaX();
        int deltaY = (int) touchFloatRoute.getDeltaY();
        currRect.left += deltaX;
        currRect.right += deltaX;
        currRect.top += deltaY;
        currRect.bottom += deltaY;
        if (currRect.left < 0) {
            currRect.left = 0;
            currRect.right = itemWidth;
        }
        if (currRect.top < 0) {
            currRect.top = 0;
            currRect.bottom = itemHeight;
        }
        if (currRect.right > getMeasuredWidth()) {
            currRect.right = getMeasuredWidth();
            currRect.left = getMeasuredWidth() - itemWidth;
        }
        if (currRect.bottom > getMeasuredHeight()) {
            currRect.bottom = getMeasuredHeight();
            currRect.top = getMeasuredHeight() - itemHeight;
        }
        moveChildView();
        invalidate();
    }

    /**
     * 移动所有相关子view
     */
    private void moveChildView() {
        if (isAniming)
            return;
        final int count = getItemCount();
        for (int i = 0; i < count; i++) {
            final View childView = getChildAt(i);
            if (childView == currLongView)
                continue;
            int centerX = currRect.left + itemWidth / 2;
            int centerY = currRect.top + itemHeight / 2;
            int diffOffset = itemWidth / 8;
            if (centerX > childView.getLeft() + diffOffset && centerX < childView.getRight() - diffOffset && centerY > childView.getTop() + diffOffset && centerY < childView.getBottom() - diffOffset) {
                isAniming = true;
                removeCallbacks(moveChildRunnable);
                moveChildRunnable.setTargetIndex(i);
                postDelayed(moveChildRunnable, 50);
//            	startMoveChildAnim(i);
                break;
            }
        }
    }

    /**
     * 开始移动所有相关子view动画
     *
     * @param targetIndex 目标位置
     */
    private void startMoveChildAnim(final int targetIndex) {
        int position = indexOfChild(currLongView);
        if (position == -1)
            return;
        isAniming = true;
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setDuration(200);
        if (position < targetIndex) {
            for (int i = position + 1; i <= targetIndex; i++) {
                View preChildView = getChildAt(i - 1);
                View nextChildView = getChildAt(i);
                animationSet.addAnimation(new MoveAnim(nextChildView, nextChildView.getLeft(), preChildView.getLeft(), nextChildView.getRight(), preChildView.getRight(), nextChildView.getTop(), preChildView.getTop(), nextChildView.getBottom(), preChildView.getBottom()));
            }
        } else {
            for (int i = targetIndex; i < position; i++) {
                View preChildView = getChildAt(i);
                View nextChildView = getChildAt(i + 1);
                animationSet.addAnimation(new MoveAnim(preChildView, preChildView.getLeft(), nextChildView.getLeft(), preChildView.getRight(), nextChildView.getRight(), preChildView.getTop(), nextChildView.getTop(), preChildView.getBottom(), nextChildView.getBottom()));
            }
        }
        animationSet.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isAniming = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                removeView(currLongView);
                addView(currLongView, targetIndex);
                isAniming = false;
            }
        });
        startAnimation(animationSet);
    }

    /**
     * 执行抖动动画
     */
    private void startWobbleAnim() {
        stopWobbleAnim();
        wobbleAnimatorSet = new AnimatorSet();
        int length = getItemCount();
        ObjectAnimator[] objectAnimators = new ObjectAnimator[length];
        for (int i = 0; i < length; i++) {
            View childView = getChildAt(i);
            ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(childView, "rotation", 3, -3);
            objectAnimator.setRepeatMode(ValueAnimator.REVERSE);
            objectAnimator.setRepeatCount(ValueAnimator.INFINITE);
            objectAnimators[i] = objectAnimator;
        }
        wobbleAnimatorSet.playTogether(objectAnimators);
        wobbleAnimatorSet.setDuration(180);
        wobbleAnimatorSet.start();
    }

    /**
     * 停止抖动
     */
    private void stopWobbleAnim() {
        if (wobbleAnimatorSet != null) {
            wobbleAnimatorSet.end();
            wobbleAnimatorSet = null;
            for (int i = 0; i < getChildCount(); i++) {
                View childView = getChildAt(i);
                if (!isAddChildView(childView)) {
                    childView.setRotation(0);
                }
            }
        }
    }

    /**
     * 创建一个可拖动的Bitmap
     */
    private void createDragBitmap() {
        int w = currLongView.getWidth();
        int h = currLongView.getHeight();
        int top = currLongView.getTop();
        int left = currLongView.getLeft();
        currRect = new Rect(left, top, left + w, top + h);

        Bitmap bitmap = getBitmapFromView(currLongView);
        dragBitmapDrawable = new BitmapDrawable(getResources(), bitmap);
        currLongView.setVisibility(View.INVISIBLE);
        invalidate();
    }

    /**
     * 将View转成Bitmap
     *
     * @param view
     * @return
     */
    private Bitmap getBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    /**
     * 动画添加多个子View
     */
    public void addMutilItemView(LinkedList<ImageTagElement> imageTagElementList) {
        addMutilItemView(imageTagElementList, true);
    }

    /**
     * 动画添加多个子View
     */
    public void addMutilItemView(LinkedList<ImageTagElement> imageTagElementList, boolean isAnim) {
        if (imageTagElementList == null) {
            return;
        }
        if ((getItemCount() + imageTagElementList.size()) > getMaxItemCount()) {
//            throw new RuntimeException("最多只能添加 "+getMaxItemCount()+" 个子View,请注意 setMaxRows 和  setMaxRowsItemCount");
            return;
        }
        if (isAnim) {
            bitmapQueue.addAll(imageTagElementList);
            addDelayItemViewByQueue();
        } else {
            bitmapQueue.clear();
            this.imageTagElementList = imageTagElementList;
            if (itemWidth != 0) {
                addMutilItemView();
            } else {
                isInitAddItemView = true;
            }
        }
    }

    /**
     * 无动画添加多个子View
     */
    private void addMutilItemView() {
        if (imageTagElementList != null) {
            delAddImageView(imageTagElementList.size());
            for (int i = 0; i < imageTagElementList.size(); i++) {
                ImageTagElement imageTagElement = imageTagElementList.get(i);
                addItemViewAnim(imageTagElement.bitmap, imageTagElement.tag);
            }
            imageTagElementList = null;
        }
    }

    /**
     * 根据队列中的数据添加子View
     */
    private void addDelayItemViewByQueue() {
        ImageTagElement imageTagElement = bitmapQueue.poll();
        if (imageTagElement != null) {
            addDelayItemView(imageTagElement.bitmap, imageTagElement.tag);
        }
    }

    /**
     * 动画添加子View
     */
    private void addItemViewAnim(Bitmap bitmap, Object tag) {
        isAniming = true;
        View view = createItemView(bitmap, tag);
        int index = isAddChildViewLast() ? (getChildCount() - 1) : -1;
        addView(view, index);
        view.startAnimation(createFadeInAnim());
        addDelayItemViewByQueue();
    }

    /**
     * 添加子View
     *
     * @param bitmap
     * @param tag
     */
    public void addItemView(Bitmap bitmap, Object tag) {
        startAddAnim(bitmap, tag);
    }

    /**
     * 获取添加的子View
     *
     * @return
     */
    public LinkedList<View> getItemViewList() {
        LinkedList<View> viewList = new LinkedList<View>();
        for (int i = 0; i < getChildCount(); i++) {
            View childView = getChildAt(i);
            if (!isAddChildView(childView)) {
                viewList.add(getChildAt(i));
            }
        }
        return viewList;
    }

    /**
     * 获取子View的bitmap
     *
     * @return
     */
    public List<Bitmap> getItemBitmapList() {
        List<Bitmap> bitmapList = new ArrayList<Bitmap>();
        for (int i = 0; i < getChildCount() - 1; i++) {
            View childView = getChildAt(i);
            if (childView instanceof ViewGroup && !isAddChildView(childView)) {
                ViewGroup viewGroup = (ViewGroup) childView;
                for (int innerChildIndex = 0; innerChildIndex < viewGroup.getChildCount(); innerChildIndex++) {
                    View innerChildView = viewGroup.getChildAt(innerChildIndex);
                    if (innerChildView instanceof ImageView) {
                        ImageView imgView = (ImageView) innerChildView;
                        BitmapDrawable bitmapDrawable = (BitmapDrawable) imgView.getDrawable();
                        if (bitmapDrawable != null && bitmapDrawable.getBitmap() != null) {
                            bitmapList.add(bitmapDrawable.getBitmap());
                        }
                    }
                }
            }
        }
        return bitmapList;
    }

    /**
     * 获取子View个数
     *
     * @return
     */
    public int getItemCount() {
        return isAddChildView(getChildAt(getChildCount() - 1)) ? getChildCount() - 1 : getChildCount();
    }

    /**
     * 获取最大的子View个数
     *
     * @return
     */
    public int getMaxItemCount() {
        return maxRows * maxRowsItemCount;
    }

    /**
     * 延迟加载子View
     *
     * @param bitmap
     * @param tag
     */
    public void addDelayItemView(final Bitmap bitmap, final Object tag) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                startAddAnim(bitmap, tag);
            }
        }, 150);
    }

    /**
     * 延迟加载子View
     *
     * @param bitmap
     */
    public void addDelayItemView(final Bitmap bitmap) {
        addDelayItemView(bitmap, null);
    }

    /**
     * 执行添加动画
     */
    private void startAddAnim(final Bitmap bitmap, final Object tag) {
        isAniming = true;
        if (delAddImageView(1)) {
            addItemViewAnim(bitmap, tag);
        } else {
            AnimationSet animationSet = new AnimationSet(true);
            animationSet.setDuration(140);
            int toLeft, toRight, toTop, toBottom;
            int startPosition = getChildCount() - 1;
            for (int i = startPosition; i < getChildCount(); i++) {
                View childView = getChildAt(i);
                toLeft = childView.getLeft() + itemWidth;
                toRight = childView.getRight() + itemWidth;
                int currRows = (i + 1) / maxRowsItemCount;
                toTop = currRows * itemHeight + currRows * ITEM_SPACE;
                toBottom = toTop + itemHeight;
                if (toLeft >= getMeasuredWidth()) {
                    toLeft = 0;
                }
                if (toRight > getMeasuredWidth()) {
                    toRight = itemWidth;
                }
                animationSet.addAnimation(new MoveAnim(childView, toLeft, toRight, toTop, toBottom));
            }
            animationSet.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    addItemViewAnim(bitmap, tag);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            startAnimation(animationSet);
        }
    }

    /**
     * 执行删除动画
     *
     * @param delChildView 要删除的子View
     */
    private void startDelAnim(final View delChildView) {
        if (isAniming) {
            return;
        }
        int delPosition = indexOfChild(delChildView);
        if (delPosition == -1)
            return;
        isAniming = true;
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setDuration(200);
        int toLeft, toRight, toTop, toBottom;
        for (int i = delPosition + 1; i < getChildCount(); i++) {
            View childView = getChildAt(i);
            toLeft = childView.getLeft() - itemWidth;
            toRight = childView.getRight() - itemWidth;
            int currRows = (i - 1) / maxRowsItemCount;
            toTop = currRows * itemHeight + currRows * ITEM_SPACE;
            toBottom = toTop + itemHeight;
            if (toLeft < 0) {
                toLeft = getMeasuredWidth() - itemWidth;
            }
            if (toRight <= 0) {
                toRight = getMeasuredWidth();
            }
            animationSet.addAnimation(new MoveAnim(childView, toLeft, toRight, toTop, toBottom));
        }
        animationSet.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                removeView(delChildView);
                if (!isAddChildView(getChildAt(getChildCount() - 1))) {    //最后一个不是加号 直接添加
                    addAddImageView();
                }
                isAniming = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        startAnimation(animationSet);
    }

    /**
     * 隐藏加号View
     */
    private void hiddenAddImg() {
        if (!isShowAddImg && addImgView != null && addImgView.getVisibility() == View.VISIBLE) {
            addImgView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * 创建ImageView
     *
     * @param bitmap
     * @return
     */
    private ImageView createImageView(Bitmap bitmap) {
        ImageView imageView = new ImageView(getContext());
        imageView.setImageBitmap(bitmap);
        return imageView;
    }

    /**
     * 创建要添加的子View
     *
     * @return
     */
    private View createItemView(Bitmap bitmap, final Object tag) {
        hiddenAddImg();
        final RelativeLayout relativeLayout = new RelativeLayout(getContext());
        relativeLayout.setTag(tag);
        if (!isDisableDrag) {
            relativeLayout.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    isInterceptTouch = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    currLongView = v;
                    createDragBitmap();
                    startWobbleAnim();
                    return true;
                }
            });
        }
        if (onItemViewListener != null) {
            relativeLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemViewListener.onItemClick(relativeLayout, tag);
                }
            });
        }
        ImageView itemImgView = createImageView(bitmap);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(itemWidth - ITEM_SPACE, itemHeight);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        itemImgView.setLayoutParams(layoutParams);
        relativeLayout.addView(itemImgView);
        if (onItemViewListener != null) {
            onItemViewListener.onAddItem(itemImgView, tag);
        }

        ImageView deleteImgView = createImageView(BitmapFactory.decodeResource(getResources(), R.drawable.wdspk_icon_1));
        if (!isShowDelBtn) {
            deleteImgView.setVisibility(View.INVISIBLE);
        }
        deleteImgView.setScaleType(ImageView.ScaleType.CENTER);
        deleteImgView.setPadding(25, 0, 0, 25);
        layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        relativeLayout.addView(deleteImgView, layoutParams);
        deleteImgView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startDelAnim(relativeLayout);
            }
        });
        relativeLayout.setLayoutParams(new LayoutParams(itemWidth - ITEM_SPACE, itemHeight));
        return relativeLayout;
    }

    /**
     * 创建Alpha出现动画
     *
     * @return
     */
    private Animation createFadeInAnim() {
        AlphaAnimation fadeInAnim = new AlphaAnimation(0, 1);
        fadeInAnim.setInterpolator(new DecelerateInterpolator());
        fadeInAnim.setDuration(140);
        fadeInAnim.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isAniming = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isAniming = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        return fadeInAnim;
    }

    /**
     * 创建Alpha消失动画
     *
     * @return
     */
    private Animation createFadeOutAnim() {
        AlphaAnimation fadeOutAnim = new AlphaAnimation(1, 0);
        fadeOutAnim.setInterpolator(new AccelerateInterpolator());
        fadeOutAnim.setDuration(140);
        fadeOutAnim.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isAniming = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isAniming = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        return fadeOutAnim;
    }

    /**
     * 是否是加号ImageView
     *
     * @param childView
     * @return
     */
    private boolean isAddChildView(View childView) {
        if (childView instanceof ImageView) {
            return true;
        }
        return false;
    }

    /**
     * 最后一个ChildView是否是加号ImageView
     *
     * @return
     */
    private boolean isAddChildViewLast() {
        if (getChildCount() != 0) {
            return isAddChildView(getChildAt(getChildCount() - 1));
        }
        return false;
    }

    /**
     * 设置增加子View事件
     *
     * @param onAddClickListener
     */
    public void setOnAddClickListener(OnAddClickListener onAddClickListener) {
        this.onAddClickListener = onAddClickListener;
    }

    /**
     * 设置子View事件
     *
     * @param onItemViewListener
     */
    public void setOnItemViewListener(OnItemViewListener onItemViewListener) {
        this.onItemViewListener = onItemViewListener;
    }

    /**
     * 获取当前的行数  注意：从1开始
     *
     * @return
     */
    public int getCurrRowsCount() {
        return (int) Math.ceil(getChildCount() * 1f / maxRowsItemCount);
    }

    /**
     * 获取每一行子View最大个数
     *
     * @return
     */
    public int getMaxRowsItemCount() {
        return maxRowsItemCount;
    }

    /**
     * 设置每一行子View最大个数
     *
     * @param maxRowsItemCount
     */
    public void setMaxRowsItemCount(int maxRowsItemCount) {
        this.maxRowsItemCount = maxRowsItemCount;
    }

    /**
     * 获取最大行数
     *
     * @return
     */
    public int getMaxRows() {
        return maxRows;
    }

    /**
     * 设置最大行数
     *
     * @param maxRows
     */
    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    /**
     * 是否显示删除按钮
     *
     * @return
     */
    public boolean isShowDelBtn() {
        return isShowDelBtn;
    }

    /**
     * 设置是否显示删除按钮
     *
     * @param isShowDelBtn
     */
    public void setShowDelBtn(boolean isShowDelBtn) {
        this.isShowDelBtn = isShowDelBtn;
    }

    /**
     * 是否禁用Drag
     *
     * @return
     */
    public boolean isDisableDrag() {
        return isDisableDrag;
    }

    /**
     * 设置是否禁用Drag
     *
     * @param isDisableDrag
     */
    public void setDisableDrag(boolean isDisableDrag) {
        this.isDisableDrag = isDisableDrag;
    }

    /**
     * 是否显示加号View
     *
     * @return
     */
    public boolean isShowAddImg() {
        return isShowAddImg;
    }

    /**
     * 设置是否显示加号View
     *
     * @param isShowAddImg
     */
    public void setShowAddImg(boolean isShowAddImg) {
        this.isShowAddImg = isShowAddImg;
        hiddenAddImg();
    }

    /**
     * 移动动画
     */
    class MoveAnim extends Animation {

        private View targetView;
        private int fromLeft;
        private int toLeft;
        private int fromRight;
        private int toRight;
        private int fromTop;
        private int toTop;
        private int fromBottom;
        private int toBottom;

        public MoveAnim(View targetView, int fromLeft, int toLeft,
                        int fromRight, int toRight, int fromTop, int toTop,
                        int fromBottom, int toBottom) {
            super();
            this.targetView = targetView;
            this.fromLeft = fromLeft;
            this.toLeft = toLeft;
            this.fromRight = fromRight;
            this.toRight = toRight;
            this.fromTop = fromTop;
            this.toTop = toTop;
            this.fromBottom = fromBottom;
            this.toBottom = toBottom;
        }

        public MoveAnim(View targetView, int toLeft, int toRight, int toTop,
                        int toBottom) {
            super();
            this.targetView = targetView;
            this.fromLeft = targetView.getLeft();
            this.toLeft = toLeft;
            this.fromRight = targetView.getRight();
            this.toRight = toRight;
            this.fromTop = targetView.getTop();
            this.toTop = toTop;
            this.fromBottom = targetView.getBottom();
            this.toBottom = toBottom;
        }

        @Override
        protected void applyTransformation(float interpolatedTime,
                                           Transformation t) {
            int left = (int) (fromLeft - (fromLeft - toLeft) * interpolatedTime);
            int right = (int) (fromRight - (fromRight - toRight) * interpolatedTime);
            int top = (int) (fromTop - (fromTop - toTop) * interpolatedTime);
            int bottom = (int) (fromBottom - (fromBottom - toBottom) * interpolatedTime);
            targetView.setLeft(left);
            targetView.setRight(right);
            targetView.setTop(top);
            targetView.setBottom(bottom);
        }
    }

    /**
     * 延迟执行动画Runnable
     * Created by linjinfa 331710168@qq.com
     */
    class MoveChildRunnable implements Runnable {

        private int targetIndex;

        @Override
        public void run() {
            startMoveChildAnim(targetIndex);
        }

        public void setTargetIndex(int targetIndex) {
            this.targetIndex = targetIndex;
        }
    }

    /**
     * 添加子View Click事件
     */
    public interface OnAddClickListener {
        void onAddClick();
    }

    /**
     * 子View事件
     */
    public interface OnItemViewListener {
        void onAddItem(ImageView imageView, Object tag);

        void onItemClick(View itemView, Object tag);
    }

    /**
     * 一次性添加多个子View时的对象类
     *
     * @author Administrator
     */
    public static class ImageTagElement {

        Bitmap bitmap;
        Object tag;

        public ImageTagElement(Bitmap bitmap, Object tag) {
            super();
            this.bitmap = bitmap;
            this.tag = tag;
        }

        @Override
        public String toString() {
            return "ImageTagElement{" + "tag=" + tag + "}";
        }
    }
}
