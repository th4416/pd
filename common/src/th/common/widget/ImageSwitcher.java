
package th.common.widget;

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

import th.common.R;

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
 * All update requests applies in the model - <code>ImageStatus</code> for <code>onDraw()</code>
 * reading.<br/>
 * <br/>
 */
public class ImageSwitcher extends View {

    private class ImageStatus {

        private Bitmap bitmap;
        private Rect rect; // where the whole bitmap is drew into, while onDraw() starts at (0,0)
        private int alpha;

        public ImageStatus() {
            rect = new Rect();
            clear();
        }

        public void applyAlpha(float alpha) {
            if (alpha > 1f) {
                alpha = 1f;
            } else if (alpha < 0f) {
                alpha = 0f;
            }
            this.alpha = (int) (alpha * 0xFF);
        }

        public void applyOffset(int x, int y) {
            rect.offsetTo(x, y);
        }

        /**
         * the pivot is (left,top)
         */
        public void applyScale(float scale) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            rect.right = rect.left + (int) (scale * w);
            rect.bottom = rect.top + (int) (scale * h);
        }

        /**
         * reset each attribute to its default value
         */
        public void clear() {
            this.bitmap = null;
            this.rect.setEmpty();
            this.alpha = 0xFF;
        }

        /**
         * Make the image centralized and suitably scaled down. Will definitely overwrite relative
         * attribute.<br/>
         */
        private void fitRect(int width, int height) {
            int rectWidth = rect.width();
            int rectHeight = rect.height();
            if (rectWidth > width
                    || rectHeight > height) {
                float scale = Math.min(1f * width / rectWidth, 1f * height / rectHeight);
                rectWidth *= scale;
                rectHeight *= scale;
                rect.right = rect.left + rectWidth;
                rect.bottom = rect.top + rectHeight;
            }
            int x = (width - rect.width()) / 2;
            int y = (height - rect.height()) / 2;
            applyOffset(x, y);
        }

        public void initialize(Bitmap bitmap, int containerWidth, int containerHeight) {
            if (bitmap == null) {
                clear();
                return;
            }
            reset(bitmap);
            fitRect(containerWidth, containerHeight);
        }

        public boolean isValid() {
            return bitmap != null;
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
        }
    }

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

        if (!imageStatus.isValid()) {
            return;
        }

        boolean isEnter = (flags & FLAG_ENTER) != 0;

        if ((flags & FLAG_ALPHA) != 0) {
            final float ALPHA_START = 0.4f;
            final float ALPHA_FINAL = 1.0f;
            imageStatus.applyAlpha(isEnter
                    ? (ALPHA_FINAL - ALPHA_START) * animatedFraction + ALPHA_START
                    : (ALPHA_START - ALPHA_FINAL) * animatedFraction + ALPHA_FINAL);
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
                        offsetX = (int) (totalX * (1f - animatedFraction));
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
                        offsetX = (int) (totalX * animatedFraction);
                    }
                    break;
                }
                case FLAG_TRANS_TO_TOP:
                    // not be here yet, so no test
                    offsetY = (int) (imageStatus.bitmap.getHeight() * animatedFraction);
                    break;
                case FLAG_TRANS_TO_BOTTOM:
                    // not be here yet, so no test
                    offsetY = -(int) (imageStatus.bitmap.getHeight() * animatedFraction);
                    break;
            }
            imageStatus.applyOffset(offsetX, offsetY);
        }

        if ((flags & FLAG_SCALE) != 0) {
            final float SCALE_START = 0.4f;
            final float SCALE_FINAL = 1.0f;
            imageStatus.applyScale(isEnter
                    ? (SCALE_FINAL - SCALE_START) * animatedFraction + SCALE_START
                    : (SCALE_START - SCALE_FINAL) * animatedFraction + SCALE_FINAL);

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

    private float mScale = 1f;

    private Paint mPaint;

    public ImageSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
        mImage = new ImageStatus();
        mComingImage = new ImageStatus();
        mPaint = new Paint();
    }

    public void doneSwitching() {
        if (isSwitching()) {
            mAnimatorSet.end();
        }
    }

    public void setScale(float scale) {
        if (scale >= 0.98f && scale <= 1.02f) {
            scale = 1f;
        }
        mScale = scale;
        updateImageStatus(mImage, FLAG_ENTER | FLAG_SCALE, mScale, getWidth(), getHeight());
        invalidate();
    }

    public void doScale(float scale) {
        setScale(scale * mScale);
    }

    public void doScroll(Bitmap bitmap, Bitmap comingBitmap,
            boolean asNext, float animatedFraction) {
        if (isSwitching()) {
            return;
        }

        int hostWidth = getWidth();
        int hostHeight = getHeight();
        mImage.initialize(bitmap, hostWidth, hostHeight);
        mComingImage.initialize(comingBitmap, hostWidth, hostHeight);
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

    private void doSwitch(Bitmap bitmap, Bitmap comingBitmap,
            boolean asNext, float startAnimatedFraction,
            final float endAnimatedFraction) {

        if (isSwitching()) {
            return;
        }

        final int hostWidth = getWidth();
        final int hostHeight = getHeight();
        mImage.initialize(bitmap, hostWidth, hostHeight);
        mComingImage.initialize(comingBitmap, hostWidth, hostHeight);
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
                mImage.initialize(mComingImage.bitmap, hostWidth, hostHeight);
                mComingImage.clear();
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
        mImage.initialize(bitmap, hostWidth, hostHeight);
        mComingImage.initialize(comingBitmap, hostWidth, hostHeight);
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

    public boolean isScaled() {
        return mScale > 1.02f || mScale < 0.98f;
    }

    public boolean isSwitching() {
        return mAnimatorSet != null && mAnimatorSet.isRunning();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(getResources().getColor(R.color.dev_gray9));
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
            paint.setAlpha(image.alpha);
            canvas.drawBitmap(image.bitmap, null, image.rect, paint);
        }
    }
}
