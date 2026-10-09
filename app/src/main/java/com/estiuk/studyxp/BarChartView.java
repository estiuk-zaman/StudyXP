package com.estiuk.studyxp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** 7-day bar chart with optional dashed goal line. */
final class BarChartView extends View {
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private int[] vals = new int[7];
    private String[] labels = {"", "", "", "", "", "", ""};
    private int goal = -1;
    private int cAccent, cTrack, cDim, cText, cGold;
    private final float d;

    BarChartView(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        txt.setTextAlign(Paint.Align.CENTER);
        txt.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(1.5f * d);
    }

    void setColors(int accent, int track, int dim, int text, int gold) {
        cAccent = accent; cTrack = track; cDim = dim; cText = text; cGold = gold;
        invalidate();
    }

    void setData(int[] v, String[] l, int goalLine) {
        vals = v; labels = l; goal = goalLine;
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        int n = vals.length;
        if (w <= 0 || n == 0) return;
        float topPad = 26 * d, botPad = 26 * d;
        float chartH = h - topPad - botPad;
        float slot = w / n, bw = slot * 0.46f;
        int max = 1;
        for (int v : vals) max = Math.max(max, v);
        if (goal > 0) max = Math.max(max, goal);
        float scale = chartH / (max * 1.08f);

        if (goal > 0) {
            float gy = h - botPad - goal * scale;
            line.setColor(cGold);
            line.setPathEffect(new DashPathEffect(new float[]{8 * d, 6 * d}, 0));
            c.drawLine(0, gy, w, gy, line);
            line.setPathEffect(null);
        }
        for (int i = 0; i < n; i++) {
            int v = Math.max(0, vals[i]);
            float bh = Math.max(3 * d, v * scale);
            float left = slot * i + (slot - bw) / 2f;
            float top = h - botPad - bh;
            boolean today = i == n - 1;
            bar.setColor(v == 0 ? cTrack : cAccent);
            bar.setAlpha(v == 0 ? 255 : (today ? 255 : 150));
            rf.set(left, top, left + bw, h - botPad);
            c.drawRoundRect(rf, bw * 0.35f, bw * 0.35f, bar);
            txt.setTextSize(11 * d);
            txt.setColor(cText);
            if (vals[i] != 0) c.drawText(String.valueOf(vals[i]), left + bw / 2f, top - 6 * d, txt);
            txt.setColor(today ? cAccent : cDim);
            c.drawText(labels[i], left + bw / 2f, h - 7 * d, txt);
        }
    }
}
