package com.estiuk.studyxp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Circular progress ring with centre text and optional clock ticks. */
final class RingView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tMain = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tSub = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float progress = 0f;
    private String main = "", sub = "";
    private boolean ticks = false;
    private float mainScale = 0.20f;
    private int cTrack, cArc, cMain, cSub, cTickOff;
    private ValueAnimator anim;

    RingView(Context c) {
        super(c);
        track.setStyle(Paint.Style.STROKE);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.ROUND);
        tick.setStyle(Paint.Style.STROKE);
        tick.setStrokeCap(Paint.Cap.ROUND);
        tMain.setTextAlign(Paint.Align.CENTER);
        tMain.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tSub.setTextAlign(Paint.Align.CENTER);
        tSub.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tSub.setLetterSpacing(0.14f);
    }

    void setColors(int track, int arc, int main, int sub, int tickOff) {
        cTrack = track; cArc = arc; cMain = main; cSub = sub; cTickOff = tickOff;
        invalidate();
    }

    void setTicks(boolean b) { ticks = b; invalidate(); }
    void setMainScale(float f) { mainScale = f; invalidate(); }
    void setTexts(String m, String s) { main = m; sub = s; invalidate(); }

    void setProgress(float target, boolean animate) {
        target = Math.max(0f, Math.min(1f, target));
        if (anim != null) anim.cancel();
        if (!animate) { progress = target; invalidate(); return; }
        anim = ValueAnimator.ofFloat(progress, target);
        anim.setDuration(800);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> { progress = (Float) a.getAnimatedValue(); invalidate(); });
        anim.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (anim != null) anim.cancel();
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float size = Math.min(w, h);
        if (size <= 0) return;
        float sw = size * 0.065f;
        float pad = sw / 2f + (ticks ? size * 0.075f : size * 0.01f);
        float cx = w / 2f, cy = h / 2f;
        float r = size / 2f - pad;
        rect.set(cx - r, cy - r, cx + r, cy + r);

        track.setColor(cTrack); track.setStrokeWidth(sw);
        arc.setColor(cArc); arc.setStrokeWidth(sw);
        c.drawArc(rect, 0, 360, false, track);
        if (progress > 0.001f) c.drawArc(rect, -90, 360f * Math.min(1f, progress), false, arc);

        if (ticks) {
            float outer = size / 2f - size * 0.006f;
            float inner = outer - size * 0.034f;
            tick.setStrokeWidth(Math.max(2f, size * 0.008f));
            for (int i = 0; i < 60; i++) {
                double a = Math.toRadians(i * 6 - 90);
                tick.setColor((i / 60f) < progress ? cArc : cTickOff);
                float len = (i % 5 == 0) ? 0f : size * 0.012f;
                c.drawLine(cx + (float) Math.cos(a) * (inner + len), cy + (float) Math.sin(a) * (inner + len),
                        cx + (float) Math.cos(a) * outer, cy + (float) Math.sin(a) * outer, tick);
            }
        }

        tMain.setColor(cMain);
        tMain.setTextSize(size * mainScale);
        float maxW = r * 1.55f;
        float mw = tMain.measureText(main);
        if (mw > maxW && mw > 0) tMain.setTextSize(tMain.getTextSize() * maxW / mw);
        Paint.FontMetrics fm = tMain.getFontMetrics();
        float y = cy - (fm.ascent + fm.descent) / 2f - (sub.isEmpty() ? 0f : size * 0.035f);
        c.drawText(main, cx, y, tMain);
        if (!sub.isEmpty()) {
            tSub.setColor(cSub);
            tSub.setTextSize(size * 0.052f);
            c.drawText(sub, cx, cy + size * 0.15f, tSub);
        }
    }
}
