package com.estiuk.studyxp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** A tree that grows with the level: seed -> sprout -> pine -> ancient forest tree. */
final class TreeView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rf = new RectF();
    private final Pal pal;
    private final int stage;

    TreeView(Context c, Pal pal, int stage) {
        super(c);
        this.pal = pal;
        this.stage = Math.max(0, Math.min(6, stage));
    }

    static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    private void leaf(Canvas c, float x, float y, float len, float angle, int color) {
        c.save();
        c.translate(x, y);
        c.rotate(angle);
        p.setColor(color);
        rf.set(0, -len * 0.24f, len, len * 0.24f);
        c.drawOval(rf, p);
        c.restore();
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float cx = w / 2f, ground = h * 0.92f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(mix(pal.surface2, pal.accentDeep, 0.35f));
        rf.set(cx - w * 0.40f, ground - h * 0.045f, cx + w * 0.40f, ground + h * 0.045f);
        c.drawOval(rf, p);

        int light = mix(pal.accent, 0xFFFFFFFF, 0.25f);
        if (stage <= 1) {
            float ht = h * (stage == 0 ? 0.30f : 0.52f);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(w * 0.035f);
            p.setColor(pal.accentDeep);
            path.reset();
            path.moveTo(cx, ground);
            path.quadTo(cx - w * 0.03f, ground - ht * 0.5f, cx, ground - ht);
            c.drawPath(path, p);
            p.setStyle(Paint.Style.FILL);
            float len = w * (stage == 0 ? 0.20f : 0.24f);
            leaf(c, cx, ground - ht, len, -38, pal.accent);
            leaf(c, cx, ground - ht, len, -142, light);
            if (stage == 1) {
                leaf(c, cx, ground - ht * 0.55f, len * 0.9f, -25, light);
                leaf(c, cx, ground - ht * 0.55f, len * 0.9f, -155, pal.accent);
                leaf(c, cx, ground - ht, len * 0.8f, -90, pal.accent);
            }
            return;
        }

        float total = h * 0.86f * (0.46f + 0.54f * (stage - 2) / 4f);
        float trunkH = total * 0.17f;
        float trunkW = w * 0.075f;
        p.setColor(pal.dark ? 0xFF8A5A33 : 0xFF7A4E2B);
        rf.set(cx - trunkW / 2f, ground - trunkH, cx + trunkW / 2f, ground);
        c.drawRoundRect(rf, 3f, 3f, p);

        int n = stage;
        float canopyH = total - trunkH;
        float layerH = canopyH / (1f + (n - 1) * 0.55f);
        float maxHalf = Math.min(w * 0.46f, total * 0.34f);
        int deep = mix(pal.accentDeep, 0xFF000000, pal.dark ? 0.25f : 0.05f);
        for (int i = 0; i < n; i++) {
            float bottom = ground - trunkH * 0.85f - i * layerH * 0.55f;
            float top = bottom - layerH;
            float half = maxHalf * (1f - 0.62f * i / Math.max(1, n));
            p.setColor(mix(deep, light, n == 1 ? 0f : i / (float) (n - 1)));
            path.reset();
            path.moveTo(cx, top);
            path.lineTo(cx + half, bottom);
            path.lineTo(cx - half, bottom);
            path.close();
            c.drawPath(path, p);
        }
        if (stage >= 5) {
            p.setColor(pal.gold);
            float topY = ground - trunkH * 0.85f - (n - 1) * layerH * 0.55f - layerH;
            c.drawCircle(cx, topY - w * 0.015f, w * 0.045f, p);
            c.drawCircle(cx - maxHalf * 0.35f, ground - trunkH - layerH * 0.35f, w * 0.022f, p);
            c.drawCircle(cx + maxHalf * 0.30f, ground - trunkH - layerH * 0.9f, w * 0.022f, p);
        }
    }
}
