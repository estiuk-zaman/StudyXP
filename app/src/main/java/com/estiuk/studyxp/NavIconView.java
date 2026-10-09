package com.estiuk.studyxp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Line icons for the bottom bar: 0 home, 1 timer, 2 bolt, 3 chart, 4 gift. */
final class NavIconView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final int type;
    private int color = 0xFF888888;

    NavIconView(Context c, int type) {
        super(c);
        this.type = type;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    void setColor(int c) { color = c; invalidate(); }

    @Override protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight()) / 24f;
        if (s <= 0) return;
        c.save();
        c.translate((getWidth() - 24 * s) / 2f, (getHeight() - 24 * s) / 2f);
        c.scale(s, s);
        p.setColor(color);
        p.setStrokeWidth(2f);
        switch (type) {
            case 0:
                path.reset(); path.moveTo(3, 11); path.lineTo(12, 3); path.lineTo(21, 11); c.drawPath(path, p);
                path.reset(); path.moveTo(5.5f, 9.5f); path.lineTo(5.5f, 20); path.lineTo(18.5f, 20); path.lineTo(18.5f, 9.5f); c.drawPath(path, p);
                path.reset(); path.moveTo(10, 20); path.lineTo(10, 14.5f); path.lineTo(14, 14.5f); path.lineTo(14, 20); c.drawPath(path, p);
                break;
            case 1:
                c.drawCircle(12, 13.5f, 8f, p);
                c.drawLine(12, 13.5f, 12, 9, p);
                c.drawLine(9.5f, 2.5f, 14.5f, 2.5f, p);
                c.drawLine(12, 2.5f, 12, 5.5f, p);
                break;
            case 2:
                path.reset(); path.moveTo(13.5f, 2); path.lineTo(5, 13.5f); path.lineTo(11, 13.5f); path.lineTo(10, 22);
                path.lineTo(19, 10); path.lineTo(13, 10); path.close(); c.drawPath(path, p);
                break;
            case 3:
                p.setStrokeWidth(3.2f);
                c.drawLine(5, 20, 5, 12, p); c.drawLine(12, 20, 12, 5, p); c.drawLine(19, 20, 19, 9, p);
                break;
            default:
                c.drawRoundRect(4, 10, 20, 21, 2, 2, p);
                c.drawRoundRect(3, 6.5f, 21, 10, 1.5f, 1.5f, p);
                c.drawLine(12, 6.5f, 12, 21, p);
                path.reset(); path.moveTo(12, 6.5f); path.cubicTo(11, 2.5f, 7, 2.2f, 7.4f, 4.6f); path.cubicTo(7.8f, 6.2f, 10.5f, 6.5f, 12, 6.5f); c.drawPath(path, p);
                path.reset(); path.moveTo(12, 6.5f); path.cubicTo(13, 2.5f, 17, 2.2f, 16.6f, 4.6f); path.cubicTo(16.2f, 6.2f, 13.5f, 6.5f, 12, 6.5f); c.drawPath(path, p);
                break;
        }
        c.restore();
    }
}
