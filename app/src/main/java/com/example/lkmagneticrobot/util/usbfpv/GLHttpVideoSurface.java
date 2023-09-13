package com.example.lkmagneticrobot.util.usbfpv;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;

import com.skydroid.fpvlibrary.widget.GLTextureView;

import java.util.Arrays;

public class GLHttpVideoSurface extends GLTextureView {
    private Context c;
    private GLHttpVideoRenderer renderer;
    private double mRequestedAspect;

    public GLHttpVideoSurface(Context context) {
        this(context, (AttributeSet)null);
        this.c = context;
    }

    public GLHttpVideoSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRequestedAspect = 1;
        this.c = context;
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.setRenderMode(0);
    }

    public void setAspectRatio(double aspectRatio, Handler handler) {
        if (aspectRatio < 0.0D) {
            throw new IllegalArgumentException();
        } else {
//            if (this.mRequestedAspect != aspectRatio) {
//                this.mRequestedAspect = aspectRatio;
//                handler.post(new Runnable() {
//                    public void run() {
//                        requestLayout();
//                    }
//                });
//            }
            handler.post(new Runnable() {
                public void run() {
                    requestLayout();
                }
            });
        }
    }

    public void resetView(Handler handler) {
        this.mRequestedAspect = 1;
        this.renderer.setPosition(0.0F, 0.0F);
        byte[] black = new byte[18];
        Arrays.fill(black, 12, 17, (byte)-128);
        this.renderer.update(12, 12);
        this.renderer.update(black);
        handler.post(new Runnable() {
            public void run() {
                requestRender();
            }
        });

    }

    public void setVideoSize(int width, int height, Handler handler) {
        this.setAspectRatio((double)((float)width * 1.0F / (float)height), handler);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mRequestedAspect > 0.0D) {
            int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
            int initialHeight = MeasureSpec.getSize(heightMeasureSpec);
            int horizPadding = this.getPaddingLeft() + this.getPaddingRight();
            int vertPadding = this.getPaddingTop() + this.getPaddingBottom();
            initialWidth -= horizPadding;
            initialHeight -= vertPadding;
            double viewAspectRatio = (double)initialWidth / (double)initialHeight;
            double aspectDiff = this.mRequestedAspect / viewAspectRatio - 1.0D;
            if (Math.abs(aspectDiff) > 0.01D) {
                initialWidth += horizPadding;
                initialHeight += vertPadding;
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY);
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public final GLHttpVideoRenderer getRenderer() {
        return this.renderer;
    }

    public void init() {
        this.setEGLContextClientVersion(2);
        this.renderer = new GLHttpVideoRenderer(this);
        this.setRenderer(this.renderer);
        this.setFocusable(true);
    }


    public void renderI420(byte[] i420, int width, int height) {
        this.getRenderer().update(width, height);
        this.getRenderer().update(i420);
    }
}
