package com.MyMusicPlayer.RecyclerViewFastScroll;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.annotation.ColorInt;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.MyMusicPlayer.R;


public class FastScroller
{

    ////////////////
    // Attributes //
    ////////////////

    private static final int DEFAULT_AUTO_HIDE_DELAY = 1500;    // The delay to hide the scroll bar

    private FastScrollRecyclerView mRecyclerView;               // The RecyclerView
    private FastScrollPopup mPopup;                             // The pop-up that display info on the current position. Ex: the first letter on the top of the view

    private int mThumbHeight;                                   // The height of the thumb
    private int mWidth;                                         // The width of the thumb

    private Paint mThumb;                                       // The graphic of the scrollbar
    private Paint mTrack;                                       // The tracker of the scrollbar

    private Rect mTmpRect = new Rect();
    private Rect mInvalidateRect = new Rect();
    private Rect mInvalidateTmpRect = new Rect();

    private int mTouchInset;                                    // The inset is the buffer around which a point will still register as a click on the scrollbar


    private int mTouchOffset;                                   // This is the offset from the top of the scrollbar when the user first starts touching. To prevent jumping, this offset is applied as the user scrolls.

    private Point mThumbPosition = new Point(-1, -1);           // The current position of the thumb
    private Point mOffset = new Point(0, 0);

    private boolean mIsDragging;                                 // Boolean to knows if the user is actually scrolling

    private Animator mAutoHideAnimator;
    private boolean mAnimatingShow;
    private int mAutoHideDelay = DEFAULT_AUTO_HIDE_DELAY;
    private boolean mAutoHideEnabled = true;
    private final Runnable mHideRunnable;


    //////////////////
    // Constructors //
    //////////////////

    FastScroller(Context context, FastScrollRecyclerView recyclerView, AttributeSet attrs)
    {

        Resources resources = context.getResources();

        // Setters
        mRecyclerView = recyclerView;
        mPopup = new FastScrollPopup(resources, recyclerView);

        mThumbHeight = Utils.toPixels(resources, 48);
        mWidth = Utils.toPixels(resources, 8);

        mTouchInset = Utils.toPixels(resources, -24);

        mThumb = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTrack = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Get all attributes from attrs.xml
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FastScrollRecyclerView, 0, 0);

        try
        {
            mAutoHideEnabled = typedArray.getBoolean(R.styleable.FastScrollRecyclerView_fastScrollAutoHide, true);
            mAutoHideDelay = typedArray.getInteger(R.styleable.FastScrollRecyclerView_fastScrollAutoHideDelay, DEFAULT_AUTO_HIDE_DELAY);

            // Set colors, size
            int trackColor = typedArray.getColor(R.styleable.FastScrollRecyclerView_fastScrollTrackColor, 0x1f000000);
            int thumbColor = typedArray.getColor(R.styleable.FastScrollRecyclerView_fastScrollThumbColor, 0xff000000);
            int popupBgColor = typedArray.getColor(R.styleable.FastScrollRecyclerView_fastScrollPopupBgColor, 0xff000000);
            int popupTextColor = typedArray.getColor(R.styleable.FastScrollRecyclerView_fastScrollPopupTextColor, 0xffffffff);
            int popupTextSize = typedArray.getDimensionPixelSize(R.styleable.FastScrollRecyclerView_fastScrollPopupTextSize, Utils.toScreenPixels(resources, 56));
            int popupBackgroundSize = typedArray.getDimensionPixelSize(R.styleable.FastScrollRecyclerView_fastScrollPopupBackgroundSize, Utils.toPixels(resources, 88));

            mTrack.setColor(trackColor);
            mThumb.setColor(thumbColor);
            mPopup.setBgColor(popupBgColor);
            mPopup.setTextColor(popupTextColor);
            mPopup.setTextSize(popupTextSize);
            mPopup.setBackgroundSize(popupBackgroundSize);
        }
        finally
        {
            typedArray.recycle();
        }

        mHideRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                if (!mIsDragging)
                {
                    if (mAutoHideAnimator != null)
                    {
                        mAutoHideAnimator.cancel();
                    }
                    mAutoHideAnimator = ObjectAnimator.ofInt(FastScroller.this, "offsetX", (Utils.isRtl(mRecyclerView.getResources()) ? -1 : 1) * mWidth);
                    mAutoHideAnimator.setInterpolator(new FastOutLinearInInterpolator());
                    mAutoHideAnimator.setDuration(200);
                    mAutoHideAnimator.start();
                }
            }
        };

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy)
            {
                super.onScrolled(recyclerView, dx, dy);
                show();
            }
        });

        if (mAutoHideEnabled)
        {
            postAutoHideDelayed();
        }
    }


    /////////////
    // Methods //
    /////////////

    // Handles the touch event and determines whether to show the fast scroller (or updates it if it is already showing).
    void handleTouchEvent(MotionEvent ev, int downX, int downY, int lastY, OnFastScrollStateChangeListener stateChangeListener)
    {

        ViewConfiguration config = ViewConfiguration.get(mRecyclerView.getContext());

        int action = ev.getAction();
        int y = (int) ev.getY();
        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                if (isNearPoint(downX, downY))
                {
                    mTouchOffset = downY - mThumbPosition.y;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // Check if we should start scrolling
                if (!mIsDragging && isNearPoint(downX, downY) && Math.abs(y - downY) > config.getScaledTouchSlop())
                {
                    mRecyclerView.getParent().requestDisallowInterceptTouchEvent(true);
                    mIsDragging = true;
                    mTouchOffset += (lastY - downY);
                    mPopup.animateVisibility(true);
                    if (stateChangeListener != null)
                    {
                        stateChangeListener.onFastScrollStart();
                    }
                }
                if (mIsDragging)
                {
                    // Update the fast scroller section name at this touch position
                    int top = 0;
                    int bottom = mRecyclerView.getHeight() - mThumbHeight;
                    float boundedY = (float) Math.max(top, Math.min(bottom, y - mTouchOffset));
                    String sectionName = mRecyclerView.scrollToPositionAtProgress((boundedY - top) / (bottom - top));
                    mPopup.setSectionName(sectionName);
                    mPopup.animateVisibility(!sectionName.isEmpty());
                    mRecyclerView.invalidate(mPopup.updateFastScrollerBounds(mRecyclerView, mThumbPosition.y));
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchOffset = 0;
                if (mIsDragging)
                {
                    mIsDragging = false;
                    mPopup.animateVisibility(false);
                    if (stateChangeListener != null)
                    {
                        stateChangeListener.onFastScrollStop();
                    }
                }
                break;

        }
    }

    // Draw the scrollbar
    void draw(Canvas canvas)
    {

        if (mThumbPosition.x < 0 || mThumbPosition.y < 0)
        {
            return;
        }

        //Background
        canvas.drawRect(mThumbPosition.x + mOffset.x, mThumbHeight / 2 + mOffset.y, mThumbPosition.x + mOffset.x + mWidth, mRecyclerView.getHeight() + mOffset.y - mThumbHeight / 2, mTrack);

        //Handle
        canvas.drawRect(mThumbPosition.x + mOffset.x, mThumbPosition.y + mOffset.y, mThumbPosition.x + mOffset.x + mWidth, mThumbPosition.y + mOffset.y + mThumbHeight, mThumb);

        //Popup
        mPopup.draw(canvas);
    }

    // Returns whether the specified points are near the scroll bar bounds.
    private boolean isNearPoint(int x, int y)
    {
        mTmpRect.set(mThumbPosition.x, mThumbPosition.y, mThumbPosition.x + mWidth, mThumbPosition.y + mThumbHeight);
        mTmpRect.inset(mTouchInset, mTouchInset);
        return mTmpRect.contains(x, y);
    }

    // Show the scrollbar
    private void show()
    {
        if (!mAnimatingShow)
        {
            if (mAutoHideAnimator != null)
            {
                mAutoHideAnimator.cancel();
            }
            mAutoHideAnimator = ObjectAnimator.ofInt(this, "offsetX", 0);
            mAutoHideAnimator.setInterpolator(new LinearOutSlowInInterpolator());
            mAutoHideAnimator.setDuration(150);
            mAutoHideAnimator.addListener(new AnimatorListenerAdapter()
            {
                @Override
                public void onAnimationCancel(Animator animation)
                {
                    super.onAnimationCancel(animation);
                    mAnimatingShow = false;
                }

                @Override
                public void onAnimationEnd(Animator animation)
                {
                    super.onAnimationEnd(animation);
                    mAnimatingShow = false;
                }
            });
            mAnimatingShow = true;
            mAutoHideAnimator.start();
        }
        if (mAutoHideEnabled)
        {
            postAutoHideDelayed();
        }
        else
        {
            cancelAutoHide();
        }
    }

    // Tell that the auto hide has been delayed
    private void postAutoHideDelayed()
    {
        if (mRecyclerView != null)
        {
            cancelAutoHide();
            mRecyclerView.postDelayed(mHideRunnable, mAutoHideDelay);
        }
    }

    // Cancel scrollbar auto hide
    private void cancelAutoHide()
    {
        if (mRecyclerView != null)
        {
            mRecyclerView.removeCallbacks(mHideRunnable);
        }
    }


    /////////////
    // Setters //
    /////////////

    void setThumbPosition(int x, int y)
    {
        if (mThumbPosition.x == x && mThumbPosition.y == y)
        {
            return;
        }
        // do not create new objects here, this is called quite often
        mInvalidateRect.set(mThumbPosition.x + mOffset.x, mOffset.y, mThumbPosition.x + mOffset.x + mWidth, mRecyclerView.getHeight() + mOffset.y);
        mThumbPosition.set(x, y);
        mInvalidateTmpRect.set(mThumbPosition.x + mOffset.x, mOffset.y, mThumbPosition.x + mOffset.x + mWidth, mRecyclerView.getHeight() + mOffset.y);
        mInvalidateRect.union(mInvalidateTmpRect);
        mRecyclerView.invalidate(mInvalidateRect);
    }

    void setThumbColor(@ColorInt int color)
    {
        mThumb.setColor(color);
        mRecyclerView.invalidate(mInvalidateRect);
    }

    void setTrackColor(@ColorInt int color)
    {
        mTrack.setColor(color);
        mRecyclerView.invalidate(mInvalidateRect);
    }

    void setPopupBgColor(@ColorInt int color)
    {
        mPopup.setBgColor(color);
    }

    void setPopupTextColor(@ColorInt int color)
    {
        mPopup.setTextColor(color);
    }

    void setPopupTypeface(Typeface typeface)
    {
        mPopup.setTypeface(typeface);
    }

    void setPopupTextSize(int size)
    {
        mPopup.setTextSize(size);
    }

    void setAutoHideDelay(int hideDelay)
    {
        mAutoHideDelay = hideDelay;
        if (mAutoHideEnabled)
        {
            postAutoHideDelayed();
        }
    }

    void setAutoHideEnabled(boolean autoHideEnabled)
    {
        mAutoHideEnabled = autoHideEnabled;
        if (autoHideEnabled)
        {
            postAutoHideDelayed();
        }
        else
        {
            cancelAutoHide();
        }
    }


    /////////////
    // Getters //
    /////////////

    boolean isDragging()
    {
        return mIsDragging;
    }

    int getThumbHeight()
    {
        return mThumbHeight;
    }

    int getWidth()
    {
        return mWidth;
    }

    public FastScrollPopup getPopup()
    {
        return mPopup;
    }
}