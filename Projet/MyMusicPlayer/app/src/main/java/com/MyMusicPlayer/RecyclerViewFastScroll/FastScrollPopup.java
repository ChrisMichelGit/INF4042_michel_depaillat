package com.MyMusicPlayer.RecyclerViewFastScroll;

import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;

import com.MyMusicPlayer.Utilities.Utils;


public class FastScrollPopup
{

    ////////////////
    // Attributes //
    ////////////////

    private FastScrollRecyclerView mRecyclerView;

    private Resources mRes;

    private int mBackgroundSize;
    private int mCornerRadius;

    private Path mBackgroundPath = new Path();
    private RectF mBackgroundRect = new RectF();
    private Paint mBackgroundPaint;

    private Rect mInvalidateRect = new Rect();
    private Rect mTmpRect = new Rect();

    // The absolute bounds of the fast scroller bg
    private Rect mBgBounds = new Rect();

    private String mSectionName;

    private Paint mTextPaint;
    private Rect mTextBounds = new Rect();

    private float mAlpha = 1;

    private ObjectAnimator mAlphaAnimator;
    private boolean mVisible;


    //////////////////
    // Constructors //
    //////////////////

    FastScrollPopup(Resources resources, FastScrollRecyclerView recyclerView)
    {

        mRes = resources;

        mRecyclerView = recyclerView;

        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setAlpha(0);

        setTextSize(Utils.toScreenPixels(mRes, 56));
        setBackgroundSize(Utils.toPixels(mRes, 88));
    }


    /////////////
    // Methods //
    /////////////

    // Animates the visibility of the fast scroller popup.
    void animateVisibility(boolean visible)
    {
        if (mVisible != visible)
        {
            mVisible = visible;
            if (mAlphaAnimator != null)
            {
                mAlphaAnimator.cancel();
            }
            mAlphaAnimator = ObjectAnimator.ofFloat(this, "alpha", visible ? 1f : 0f);
            mAlphaAnimator.setDuration(visible ? 200 : 150);
            mAlphaAnimator.start();
        }
    }

    // Draw the fast scroller popup
    void draw(Canvas canvas)
    {
        if (isVisible())
        {

            int restoreCount = canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.translate(mBgBounds.left, mBgBounds.top);
            mTmpRect.set(mBgBounds);
            mTmpRect.offsetTo(0, 0);

            mBackgroundPath.reset();
            mBackgroundRect.set(mTmpRect);

            float[] radii;

            if (Utils.isRtl(mRes))
            {
                radii = new float[]{mCornerRadius, mCornerRadius, mCornerRadius, mCornerRadius, mCornerRadius, mCornerRadius, 0, 0};
            }
            else
            {

                radii = new float[]{mCornerRadius, mCornerRadius, mCornerRadius, mCornerRadius, 0, 0, mCornerRadius, mCornerRadius};
            }

            mBackgroundPath.addRoundRect(mBackgroundRect, radii, Path.Direction.CW);

            mBackgroundPaint.setAlpha((int) (mAlpha * 255));
            mTextPaint.setAlpha((int) (mAlpha * 255));
            canvas.drawPath(mBackgroundPath, mBackgroundPaint);
            canvas.drawText(mSectionName, (mBgBounds.width() - mTextBounds.width()) / 2,
                    mBgBounds.height() - (mBgBounds.height() - mTextBounds.height()) / 2,
                    mTextPaint);
            canvas.restoreToCount(restoreCount);
        }
    }

    /**
     * Updates the bounds for the fast scroller.
     *
     * @return the invalidation rect for this update.
     */
    Rect updateFastScrollerBounds(FastScrollRecyclerView recyclerView, int thumbOffsetY)
    {
        mInvalidateRect.set(mBgBounds);

        if (isVisible())
        {
            // Calculate the dimensions and position of the fast scroller popup
            int edgePadding = recyclerView.getScrollBarWidth();
            int bgPadding = (mBackgroundSize - mTextBounds.height()) / 2;
            int bgHeight = mBackgroundSize;
            int bgWidth = Math.max(mBackgroundSize, mTextBounds.width() + (2 * bgPadding));
            if (Utils.isRtl(mRes))
            {
                mBgBounds.left = (2 * recyclerView.getScrollBarWidth());
                mBgBounds.right = mBgBounds.left + bgWidth;
            }
            else
            {
                mBgBounds.right = recyclerView.getWidth() - (2 * recyclerView.getScrollBarWidth());
                mBgBounds.left = mBgBounds.right - bgWidth;
            }
            mBgBounds.top = thumbOffsetY - bgHeight + recyclerView.getScrollBarThumbHeight() / 2;
            mBgBounds.top = Math.max(edgePadding, Math.min(mBgBounds.top, recyclerView.getHeight() - edgePadding - bgHeight));
            mBgBounds.bottom = mBgBounds.top + bgHeight;
        }
        else
        {
            mBgBounds.setEmpty();
        }

        // Combine the old and new fast scroller bounds to create the full invalidate rect
        mInvalidateRect.union(mBgBounds);
        return mInvalidateRect;
    }

    /////////////
    // Setters //
    /////////////


    // Set the popup alpha for animations
    public void setAlpha(float alpha)
    {
        mAlpha = alpha;
        mRecyclerView.invalidate(mBgBounds);
    }

    void setBgColor(int color)
    {
        mBackgroundPaint.setColor(color);
        mRecyclerView.invalidate(mBgBounds);
    }

    void setTextColor(int color)
    {
        mTextPaint.setColor(color);
        mRecyclerView.invalidate(mBgBounds);
    }

    void setTextSize(int size)
    {
        mTextPaint.setTextSize(size);
        mRecyclerView.invalidate(mBgBounds);
    }

    void setBackgroundSize(int size)
    {
        mBackgroundSize = size;
        mCornerRadius = mBackgroundSize / 2;
        mRecyclerView.invalidate(mBgBounds);
    }

    void setTypeface(Typeface typeface)
    {
        mTextPaint.setTypeface(typeface);
        mRecyclerView.invalidate(mBgBounds);
    }

    void setSectionName(String sectionName)
    {
        if (!sectionName.equals(mSectionName))
        {
            mSectionName = sectionName;
            mTextPaint.getTextBounds(sectionName, 0, sectionName.length(), mTextBounds);
            // Update the width to use measureText since that is more accurate
            mTextBounds.right = (int) (mTextBounds.left + mTextPaint.measureText(sectionName));
        }
    }


    /////////////
    // Getters //
    /////////////

    private boolean isVisible()
    {
        return (mAlpha > 0f) && (!TextUtils.isEmpty(mSectionName));
    }
}