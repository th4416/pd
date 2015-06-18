package th.media;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import th.common.widget.SquareInterpolator;
import th.common.widget.SimpleAnimatorListener;

/**
 * **Used by <code>ImageSwitcher</code> for paint**<br/>
 * For an image, there're several rects:<br/>
 * <code>imageOrigRect</code> - the bitmap dimension<br/>
 * <code>hostRect</code> - the container dimension<br/>
 * <code>imageFitRect</code> - the best fit rect results from the former two and thus calculated
 * <code>fitScale</code><br/>
 *
 * @author tanghao
 */
class ImageStatus {

    private static int[] findCentralizedOffset(int imageWidth, int imageHeight,
            int containerWidth, int containerHeight) {
        return new int[] {
                (containerWidth - imageWidth) / 2,
                (containerHeight - imageHeight) / 2
        };
    }

    /**
     * Principle: the container contains the scaled image
     */
    private static float findFitScale(int originalWidth, int originalHeight,
            int containerWidth, int containerHeight) {
        if (originalWidth <= 0 || originalHeight <= 0
                || containerWidth <= 0 || containerHeight <= 0) {
            return 0f;
        }

        if (containerWidth < originalWidth
                || containerHeight < originalHeight) {
            return Math.min(
                    1f * containerWidth / originalWidth,
                    1f * containerHeight / originalHeight);
        }
        return 1f;
    }

    Bitmap bitmap;

    // where the whole bitmap is drew into, while onDraw() starts at (0,0)
    Rect rect;

    // an instantaneous value that may change during animation, finalize to 0xFF
    private int alpha;

    private float fitScale;

    public ImageStatus() {
        clear();
    }

    /**
     * accept [0f, 1f]
     */
    public void applyAlpha(float alpha) {
        if (alpha > 1f) {
            alpha = 1f;
        } else if (alpha < 0f) {
            alpha = 0f;
        }
        this.alpha = (int) (alpha * 0xFF);
    }

    /**
     * accept [0, 0xFF]
     */
    public void applyAlpha(int alpha) {
        if (alpha > 0xFF) {
            alpha = 0xFF;
        }
        if (alpha < 0) {
            alpha = 0;
        }
        this.alpha = alpha;
    }

    public void applyOffset(int x, int y) {
        rect.offsetTo(x, y);
    }

    public void applyOffset(int[] coord) {
        applyOffset(coord[0], coord[1]);
    }

    /**
     * the pivot is (left,top)
     */
    public void applyScale(float scale) {
        if (scale <= 0f) {
            return;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        rect.right = rect.left + (int) (scale * w);
        rect.bottom = rect.top + (int) (scale * h);
    }

    public void centralize(int hostWidth, int hostHeight) {
        applyOffset(findCentralizedOffset(rect.width(), rect.height(), hostWidth, hostHeight));
    }

    /**
     * reset each attribute to its default value
     */
    public void clear() {
        this.bitmap = null;
        if (this.rect == null) {
            this.rect = new Rect();
        } else {
            this.rect.setEmpty();
        }
        this.alpha = 0xFF;
        this.fitScale = 1f;
    }

    public boolean isValid() {
        return bitmap != null;
    }

    /**
     * "find" implies it requires calculation
     */
    public float findOpacity() {
        return alpha / 255f;
    }

    public float getFitScale() {
        return this.fitScale;
    }

    public int getPaintOpacity() {
        return this.alpha;
    }

    /**
     * restore the image attributes and move the image to the origin
     */
    private void reset(Bitmap bitmap) {
        if (bitmap == null) {
            clear();
            return;
        }
        this.bitmap = bitmap;
        this.rect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        this.alpha = 0xFF;
        this.fitScale = 1f;
    }

    public void resetAndFit(Bitmap bitmap, int hostWidth, int hostHeight) {
        reset(bitmap);

        if (bitmap == null
                || hostWidth <= 0 || hostHeight <= 0) {
            return;
        }

        this.fitScale = findFitScale(
                bitmap.getWidth(), bitmap.getHeight(),
                hostWidth, hostHeight);
        applyScale(this.fitScale);
        applyOffset(findCentralizedOffset(
                this.rect.width(), this.rect.height(),
                hostWidth, hostHeight));
    }

    /**
     * just the setter for fit scale, no couple with any operation
     */
    public void updateFitScale(int hostWidth, int hostHeight) {
        this.fitScale = findFitScale(
                this.bitmap.getWidth(), this.bitmap.getHeight(),
                hostWidth, hostHeight);
    }
}

/**
 * Pay attention to the terms.<br/>
 * It always switches from "this image" to the "coming image", i.e. the neighborhood in time. While,
 * the "coming image" may be the "next image" or the "prev image", i.e. the neighborhood in
 * location.<br/>
 * <br/>
 * There's a mapping for time to location during switching, y = f(x), where x stands for the elapsed
 * time(fraction), y stands for the swept length(animatedFraction) and f is seldom linear. So there
 * would be a distinguishable difference between them.<br/>
 * <br/>
 * All update requests are applied to the model - <code>ImageStatus</code> for <code>onDraw()</code>
 * reading.<br/>
 * <br/>
 */
public class ImageSwitcher extends View {

    private static final int FLAG_ENTER = 0x1;
    private static final int FLAG_SCALE = 0x10;
    private static final int FLAG_ALPHA = 0x20;
    private static final int FLAG_TRANS = 0x40;
    private static final int FLAG_TRANS_TO_BOTTOM = 0x200;
    private static final int FLAG_TRANS_TO_LEFT = 0x400;
    private static final int FLAG_TRANS_TO_RIGHT = 0x600;
    private static final int FLAG_TRANS_TO_TOP = 0x800;

    private static SquareInterpolator interpolator = new SquareInterpolator();

    private static ValueAnimator getAnimator(float interpolated) {
        final int DURATION = 500;

        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(DURATION);
        a.setInterpolator(interpolator);

        final int playedTime = (int) (SquareInterpolator
                .getInversed(interpolated) * DURATION);
        a.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                if (animator instanceof ValueAnimator) {
                    ValueAnimator a = (ValueAnimator) animator;
                    a.setCurrentPlayTime(playedTime);
                }
            }
        });

        return a;
    }

    private static int getFlagsForAnim(boolean isEnter, boolean asNext) {
        if (isEnter) {
            return asNext
                    ? FLAG_ENTER | FLAG_SCALE | FLAG_ALPHA
                    : FLAG_ENTER | FLAG_TRANS | FLAG_TRANS_TO_RIGHT;
        } else {
            return asNext
                    ? FLAG_TRANS | FLAG_TRANS_TO_LEFT
                    : FLAG_SCALE | FLAG_ALPHA;
        }
    }

    private static void updateImageStatus(ImageStatus imageStatus, int flags,
            float animatedFraction, int hostWidth, int hostHeight) {
        if (!imageStatus.isValid()
                || hostWidth == 0 || hostHeight == 0) {
            return;
        }

        imageStatus.updateFitScale(hostWidth, hostHeight);

        boolean isEnter = (flags & FLAG_ENTER) != 0;

        if ((flags & FLAG_ALPHA) != 0) {
            float alphaStart = 0.4f;
            float alphaFinal = 1.0f;
            imageStatus.applyAlpha(isEnter
                    ? (alphaFinal - alphaStart) * animatedFraction + alphaStart
                    : (alphaStart - alphaFinal) * animatedFraction + alphaFinal);
        } else {
            imageStatus.applyAlpha(1.0f);
        }

        if ((flags & FLAG_TRANS) != 0) {
            int offsetX = 0;
            int offsetY = 0;
            switch (flags & 0xF00) {
                case FLAG_TRANS_TO_LEFT: {
                    int totalX = (imageStatus.rect.width() + hostWidth) / 2;
                    offsetY = imageStatus.rect.top;
                    if (isEnter) {
                        // not be here yet, so no test
                    } else {
                        int endX = -imageStatus.rect.width();
                        offsetX = (int) (totalX * (1f - animatedFraction)) + endX;
                    }
                    break;
                }
                case FLAG_TRANS_TO_RIGHT: {
                    int totalX = (imageStatus.rect.width() + hostWidth) / 2;
                    offsetY = imageStatus.rect.top;
                    if (isEnter) {
                        int startX = -imageStatus.rect.width();
                        offsetX = (int) (totalX * animatedFraction) + startX;
                    } else {
                        // not be here yet, so no test
                    }
                    break;
                }
                case FLAG_TRANS_TO_TOP:
                    // not be here yet, so no test
                    break;
                case FLAG_TRANS_TO_BOTTOM:
                    // not be here yet, so no test
                    break;
            }
            imageStatus.applyOffset(offsetX, offsetY);
        }

        if ((flags & FLAG_SCALE) != 0) {
            float scaleStart = 0.4f * imageStatus.getFitScale();
            float scaleFinal = 1.0f * imageStatus.getFitScale();
            imageStatus.applyScale(isEnter
                    ? (scaleFinal - scaleStart) * animatedFraction + scaleStart
                    : (scaleStart - scaleFinal) * animatedFraction + scaleFinal);

            int w = imageStatus.rect.width();
            int h = imageStatus.rect.height();
            imageStatus.applyOffset((hostWidth - w) / 2, (hostHeight - h) / 2);
        }
    }

    private AnimatorSet mAnimatorSet = null;

    private ImageStatus mImage;
    private ImageStatus mComingImage;

    // mainly for onDraw() to keep consistent animation
    private boolean mComingAsNext = true;

    // as a trigger to tell which mode we are in
    private float mScale = 1f;

    private Paint mPaint;

    // to fix the time sequence of data load and view load
    private Runnable mFirstLoadRunnable;

    public ImageSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
        mImage = new ImageStatus();
        mComingImage = new ImageStatus();
        mPaint = new Paint();
    }

    private void applyScale(float scale) {
        if (!mImage.isValid()) {
            return;
        }

        float maxScale = 64f;
        float minScale = Math.min(1f * getWidth() / mImage.bitmap.getWidth(),
                1f * getHeight() / mImage.bitmap.getHeight());
        minScale = Math.min(0.1f, minScale);

        if (scale > maxScale) {
            scale = maxScale;
        } else if (scale < minScale) {
            scale = minScale;
        }

        if (scale >= 0.98f * mImage.getFitScale() && scale <= 1.02f * mImage.getFitScale()) {
            scale = mImage.getFitScale();
        }

        mImage.applyScale(scale);
        mScale = scale;
    }

    public void doneSwitching() {
        if (isSwitching()) {
            mAnimatorSet.end();
        }
    }

    public void doOffset(int dx, int dy) {
        if (!mImage.isValid()) {
            return;
        }
        int x = mImage.rect.left + dx;
        int y = mImage.rect.top + dy;
        mImage.applyOffset(x, y);
        invalidate();
    }

    public void doScale(float scale) {
        // focus on the center: the sepcial case avoids accumulated error
        applyScale(scale * mScale);
        mImage.centralize(getWidth(), getHeight());
        invalidate();
    }

    /**
     * (anchorX, anchorY) is stable on the screen
     */
    public void doScale(float scale, int anchorX, int anchorY) {
        // focus on the anchor
        anchorX -= mImage.rect.left; // relative to (left, top)
        anchorY -= mImage.rect.top;
        float fractionX = 1f * anchorX / mImage.rect.width();
        float fractionY = 1f * anchorY / mImage.rect.height();
        applyScale(scale * mScale);
        float offsetX = fractionX * mImage.rect.width() - anchorX;
        float offsetY = fractionY * mImage.rect.height() - anchorY;
        mImage.applyOffset(mImage.rect.left - (int) offsetX,
                mImage.rect.top - (int) offsetY);
        invalidate();
    }

    public void doScroll(Bitmap bitmap, Bitmap comingBitmap,
            boolean asNext, float animatedFraction) {
        if (isSwitching()) {
            return;
        }

        int hostWidth = getWidth();
        int hostHeight = getHeight();
        mImage.resetAndFit(bitmap, hostWidth, hostHeight);
        mComingImage.resetAndFit(comingBitmap, hostWidth, hostHeight);
        mComingAsNext = asNext;

        updateImageStatus(mComingImage,
                getFlagsForAnim(true, mComingAsNext),
                animatedFraction,
                hostWidth, hostHeight);
        updateImageStatus(mImage,
                getFlagsForAnim(false, mComingAsNext),
                animatedFraction,
                hostWidth, hostHeight);

        invalidate();
    }

    public void doSwitch(Bitmap bitmap, Bitmap comingBitmap,
            boolean asNext,
            float startAnimatedFraction) {
        doSwitch(bitmap, comingBitmap, asNext,
                startAnimatedFraction, 7749f);
    }

    private void doSwitch(Bitmap bitmap, Bitmap comingBitmap, boolean asNext,
            float startAnimatedFraction, final float endAnimatedFraction) {

        if (isSwitching()) {
            return;
        }

        final int hostWidth = getWidth();
        final int hostHeight = getHeight();
        mImage.resetAndFit(bitmap, hostWidth, hostHeight);
        mComingImage.resetAndFit(comingBitmap, hostWidth, hostHeight);
        mComingAsNext = asNext;

        ValueAnimator animator = getAnimator(startAnimatedFraction);
        animator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedFraction = valueAnimator.getAnimatedFraction();
                if (animatedFraction >= endAnimatedFraction) {
                    mAnimatorSet.cancel();
                }
                updateImageStatus(mComingImage,
                        getFlagsForAnim(true, mComingAsNext),
                        animatedFraction,
                        hostWidth, hostHeight);
                updateImageStatus(mImage,
                        getFlagsForAnim(false, mComingAsNext),
                        animatedFraction,
                        hostWidth, hostHeight);
                invalidate();
            }
        });

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animator) {
                mImage.resetAndFit(mComingImage.bitmap, hostWidth, hostHeight);
                mComingImage.clear();
                mScale = mImage.getFitScale();
                invalidate();
            }
        });

        mAnimatorSet.playTogether(animator);
        mAnimatorSet.start();
    }

    public void doSwitchAndFallback(Bitmap bitmap, Bitmap comingBitmap,
            boolean asNext, final float turnPointAnimatedFraction) {
        if (isSwitching()) {
            return;
        }

        final int hostWidth = getWidth();
        final int hostHeight = getHeight();
        mImage.resetAndFit(bitmap, hostWidth, hostHeight);
        mComingImage.resetAndFit(comingBitmap, hostWidth, hostHeight);
        mComingAsNext = asNext;

        ValueAnimator animator = getAnimator(0f);
        animator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedFraction = valueAnimator.getAnimatedFraction();
                if (animatedFraction >= turnPointAnimatedFraction) {
                    valueAnimator.cancel();
                }
                updateImageStatus(mComingImage,
                        getFlagsForAnim(true, mComingAsNext),
                        animatedFraction,
                        hostWidth, hostHeight);
                updateImageStatus(mImage,
                        getFlagsForAnim(false, mComingAsNext),
                        animatedFraction,
                        hostWidth, hostHeight);
                invalidate();
            }
        });

        ValueAnimator fallbackAnimator = getAnimator(1 - turnPointAnimatedFraction);
        fallbackAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedFraction = valueAnimator.getAnimatedFraction();
                updateImageStatus(mImage,
                        getFlagsForAnim(true, !mComingAsNext),
                        animatedFraction,
                        hostWidth, hostHeight);
                updateImageStatus(mComingImage,
                        getFlagsForAnim(false, !mComingAsNext),
                        animatedFraction,
                        hostWidth, hostHeight);
                invalidate();
            }
        });

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playSequentially(animator, fallbackAnimator);
        mAnimatorSet.start();
    }

    public void firstLoad(final Bitmap bitmap) {
        if (getWidth() == 0 || getHeight() == 0) {
            mFirstLoadRunnable = new Runnable() {
                @Override
                public void run() {
                    firstLoad(bitmap);
                }
            };
        } else {
            mImage.resetAndFit(bitmap, getWidth(), getHeight());
            mScale = mImage.getFitScale();
            invalidate();
        }
    }

    public Rect getImageRect() {
        return new Rect(mImage.rect);
    }

    public boolean isScaled() {
        return mScale != mImage.getFitScale();
    }

    public boolean isSwitching() {
        return mAnimatorSet != null && mAnimatorSet.isRunning();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mFirstLoadRunnable != null) {
            mFirstLoadRunnable.run();
            mFirstLoadRunnable = null;
            return;
        }
        if (mComingAsNext) {
            onDrawImage(canvas, mPaint, mComingImage);
            onDrawImage(canvas, mPaint, mImage);
        } else {
            onDrawImage(canvas, mPaint, mImage);
            onDrawImage(canvas, mPaint, mComingImage);
        }
        super.onDraw(canvas);
    }

    private void onDrawImage(Canvas canvas, Paint paint, ImageStatus image) {
        if (image.isValid()) {
            paint.setAlpha(image.getPaintOpacity());
            canvas.drawBitmap(image.bitmap, null, image.rect, paint);
        }
    }

    public void resetImage() {
        mImage.resetAndFit(mImage.bitmap, getWidth(), getHeight());
        mScale = mImage.getFitScale();
        invalidate();
    }
}
