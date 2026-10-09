package com.estiuk.studyxp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

import java.util.Random;

/** Layered pine-forest scene with moon (dark) or sun (light). */
final class ForestView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private boolean dark = true;
    private int bg = 0xFF0B1A13;

    ForestView(Context c) { super(c); }

    void setScene(boolean dark, int bg) { this.dark = dark; this.bg = bg; invalidate(); }

    private float hillY(float x, float w, float base, float amp, float phase) {
        return base - amp * (float) Math.sin(x / w * 6.2831f * 1.3f + phase);
    }

    private void layer(Canvas c, float w, float h, float base, float amp, float treeH, int color, int count, long seed, float phase) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        path.reset();
        path.moveTo(0, h);
        for (float x = 0; x <= w + 8; x += 8) path.lineTo(x, hillY(x, w, base, amp, phase));
        path.lineTo(w, h);
        path.close();
        c.drawPath(path, p);
        Random rnd = new Random(seed);
        for (int i = 0; i < count; i++) {
            float x = w * (i + 0.5f) / count + (rnd.nextFloat() - 0.5f) * w / count * 0.8f;
            float ph = treeH * (0.7f + rnd.nextFloat() * 0.5f);
            pine(c, x, hillY(x, w, base, amp, phase) + 3f, ph, color);
        }
    }

    private void pine(Canvas c, float x, float baseY, float ph, int color) {
        p.setColor(color);
        float tw = ph * 0.40f;
        c.drawRect(x - ph * 0.03f, baseY - ph * 0.14f, x + ph * 0.03f, baseY, p);
        for (int i = 0; i < 3; i++) {
            float bottom = baseY - ph * 0.12f - i * ph * 0.24f;
            float top = bottom - ph * 0.42f;
            float half = tw * (1f - i * 0.28f);
            path.reset();
            path.moveTo(x, top);
            path.lineTo(x + half, bottom);
            path.lineTo(x - half, bottom);
            path.close();
            c.drawPath(path, p);
        }
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        int top = dark ? 0xFF04100A : 0xFFB9DDC2;
        int bot = dark ? 0xFF15432D : 0xFFEAF5E4;
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 0, 0, h, top, bot, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);

        float cx = w * 0.80f, cy = h * 0.30f, r = Math.min(w, h) * 0.075f;
        int cel = dark ? 0xFFF6EBC3 : 0xFFFFE9A6;
        p.setShader(new RadialGradient(cx, cy, r * 3.4f, (cel & 0x00FFFFFF) | 0x50000000, cel & 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r * 3.4f, p);
        p.setShader(null);
        p.setColor(cel);
        c.drawCircle(cx, cy, r, p);

        if (dark) {
            Random rnd = new Random(5);
            for (int i = 0; i < 26; i++) {
                float sx = rnd.nextFloat() * w, sy = rnd.nextFloat() * h * 0.5f;
                p.setColor(0xFFFFFFFF);
                p.setAlpha(60 + rnd.nextInt(150));
                c.drawCircle(sx, sy, 1f + rnd.nextFloat() * 1.6f, p);
            }
            p.setAlpha(255);
        }

        int[] cols = dark ? new int[]{0xFF1A5239, 0xFF113A29, bg} : new int[]{0xFFB1D8BA, 0xFF84C195, bg};
        layer(c, w, h, h * 0.60f, h * 0.035f, h * 0.17f, cols[0], 12, 11, 0.4f);
        layer(c, w, h, h * 0.74f, h * 0.040f, h * 0.24f, cols[1], 8, 22, 1.7f);
        layer(c, w, h, h * 0.92f, h * 0.030f, h * 0.30f, cols[2], 6, 33, 2.9f);
    }
}
