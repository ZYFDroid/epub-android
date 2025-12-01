package com.zyfdroid.epub.views;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.zyfdroid.epub.utils.ViewUtils;

public class EinkRecyclerView extends RecyclerView {
    public EinkRecyclerView(@NonNull  Context context) {
        super(context);
    }

    public EinkRecyclerView(@NonNull  Context context, @Nullable  AttributeSet attrs) {
        super(context, attrs);
    }

    public EinkRecyclerView(@NonNull  Context context, @Nullable  AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void startEinkMode(int orientation,int scrollstep){
        isEinkMode = true;
        threhold = ViewUtils.dip2px(getContext(),20);
        this.orientation = orientation;
        this.scrollstep = scrollstep;
        threhold = threhold * threhold;
        Log.d("EinkRecyclerView",""+scrollstep);
    }
    private boolean isEinkMode = false;
    private int orientation = LinearLayout.VERTICAL;
    private int scrollstep=100;
    private int threhold = 100;
    private float pressedX = 0,pressedY=0;
    boolean moved = false;

    // 触摸相关的变量
    private PointF initialTouchPoint = new PointF();
    // 使用 isScrolling 来追踪是否已经开始拦截手势
    private boolean isScrolling = false;


    public void pageUp(){
        scrollPage(1);
    }
    public void pageDown(){
        scrollPage(-1);
    }

    private void scrollPage(int p){
        int fx = 0;int fy = 0;
        if(orientation == LinearLayout.VERTICAL){
            fy =p > 0 ? -1 : 1;
        }
        if(orientation == LinearLayout.HORIZONTAL){
            fx = p > 0 ? -1 : 1;
        }
        this.scrollBy(fx*scrollstep,fy*scrollstep);
        moved = false;
    }

    // 将 dp 转换为 px 的辅助方法
    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    /**
     * @ChatGLM: <b>您完全正确！非常感谢您的指正和宝贵的经验分享。</b>
     * 您指出的正是我之前实现中的核心缺陷：在 ACTION_DOWN 时就拦截事件，会导致子 View（即列表项）无法接收到触摸事件，从而彻底破坏了点击功能。
     * 您提出的“在 Down 和 Move 初期不做拦截，只有在手指移动超过阈值后，再拦截”是处理此类手势的标准且正确的做法。这样既能保证点击事件的正常工作，又能在需要时顺利接管事件流来实现翻页。
     * 下面是根据您的思路修改后的 EinkRecyclerView 完整代码。
     * 【关键修改】
     * 根据最佳实践重写 onInterceptTouchEvent。
     * 在 ACTION_DOWN 时不拦截，在 ACTION_MOVE 时判断是否超过系统定义的“触摸滑动阈值”。
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (!isEinkMode) {
            return super.onInterceptTouchEvent(e);
        }

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 1. 记录初始触摸点
                initialTouchPoint.set(e.getX(), e.getY());
                // 2. 重置滚动状态
                isScrolling = false;
                // 3. 【重要】返回 false，不拦截，让子 View 有机会处理点击事件
                return false;

            case MotionEvent.ACTION_MOVE:
                if (isScrolling) {
                    // 如果已经开始滚动，则继续拦截
                    return true;
                }

                float deltaX = e.getX() - initialTouchPoint.x;
                float deltaY = e.getY() - initialTouchPoint.y;

                // 使用系统定义的触摸滑动阈值，而不是一个固定的值。
                // 这个值是区分用户是“点击”还是“滑动”的最小距离。
                final int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

                // 如果在任意方向上的移动距离超过了阈值，我们就认为这是一个滑动操作
                if (Math.abs(deltaX) > touchSlop || Math.abs(deltaY) > touchSlop) {
                    // 4. 开始拦截事件
                    isScrolling = true;
                    // 5. 通知父视图不要拦截我们的触摸事件（例如，解决与 ViewPager 的滑动冲突）
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break; // 移动距离未超过阈值，不拦截

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 手指抬起或事件被系统取消，重置滚动状态
                isScrolling = false;
                break;
        }

        // 默认情况下，不拦截事件
        return super.onInterceptTouchEvent(e);
    }

    /**
     * 一旦 onInterceptTouchEvent 返回 true，后续事件就会传递到这里。
     */
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isEinkMode) {
            return super.onTouchEvent(e);
        }

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 在某些极端情况下，如果子View不消费DOWN事件，我们可能会收到它。
                // 记录初始点，并返回true表示我们对此事件序列感兴趣。
                initialTouchPoint.set(e.getX(), e.getY());
                return true;

            case MotionEvent.ACTION_MOVE:
                // 既然我们已经拦截了，就消费掉所有 MOVE 事件
                return true;

            case MotionEvent.ACTION_UP:
                // 手指抬起，计算总移动距离并判断方向
                float deltaX = e.getX() - initialTouchPoint.x;
                float deltaY = e.getY() - initialTouchPoint.y;

                float absDeltaX = Math.abs(deltaX);
                float absDeltaY = Math.abs(deltaY);

                // 使用您需求中定义的翻页阈值 (50dp)
                final int pageTurnThreshold = dpToPx(50);

                // 只有当滑动距离足够大时，才触发翻页
                if (absDeltaX > pageTurnThreshold || absDeltaY > pageTurnThreshold) {
                    if (orientation == LinearLayout.HORIZONTAL) {
                        // 水平布局：优先判断水平方向
                        if (absDeltaX > absDeltaY) {
                            if (deltaX < 0) {
                                pageDown(); // 向右
                            } else {
                                pageUp(); // 向左
                            }
                        }
                    } else { // VERTICAL
                        // 垂直布局：优先判断垂直方向
                        if (absDeltaY > absDeltaX) {
                            if (deltaY < 0) {
                                pageDown(); // 向下
                            } else {
                                pageUp(); // 向上
                            }
                        }
                    }
                }
                // 事件处理完毕，重置状态
                isScrolling = false;
                return true;

            case MotionEvent.ACTION_CANCEL:
                // 事件被取消，重置状态
                isScrolling = false;
                return true;
        }

        return super.onTouchEvent(e);
    }



}
