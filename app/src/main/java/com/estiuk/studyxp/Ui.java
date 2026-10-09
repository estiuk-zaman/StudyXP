package com.estiuk.studyxp;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small view-building helpers bound to a palette. */
final class Ui {
    static final int M = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int W = ViewGroup.LayoutParams.WRAP_CONTENT;

    final Context c;
    final Pal pal;
    final float d;

    Ui(Context c, Pal pal) {
        this.c = c;
        this.pal = pal;
        this.d = c.getResources().getDisplayMetrics().density;
    }

    static int alpha(int color, int a) { return (color & 0x00FFFFFF) | (a << 24); }
    int dp(float v) { return Math.round(v * d); }

    GradientDrawable shape(int color, float rDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(rDp));
        return g;
    }

    GradientDrawable shape(int color, float rDp, int stroke, float wDp) {
        GradientDrawable g = shape(color, rDp);
        g.setStroke(Math.max(1, dp(wDp)), stroke);
        return g;
    }

    GradientDrawable oval(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    Drawable ripple(Drawable content, int rippleColor, float rDp) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, shape(Color.WHITE, rDp));
    }

    TextView tv(String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    TextView tvM(String s, float sp, int color) {
        TextView t = tv(s, sp, color);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    TextView caps(String s, int color) {
        TextView t = tvM(s.toUpperCase(), 12, color);
        t.setLetterSpacing(0.12f);
        return t;
    }

    LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }

    LinearLayout.LayoutParams lpm(int w, int h, float l, float t, float r, float b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    LinearLayout vbox() {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    LinearLayout hbox() {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    LinearLayout card() {
        LinearLayout l = vbox();
        l.setPadding(dp(18), dp(18), dp(18), dp(18));
        l.setBackground(shape(pal.surface, 22, pal.stroke, 1));
        return l;
    }

    View hFill() {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    TextView button(String label, boolean primary, View.OnClickListener l) {
        return buttonColor(label, primary ? pal.accent : pal.surface2, primary ? pal.onAccent : pal.text, !primary, l);
    }

    TextView buttonColor(String label, int bg, int fg, boolean stroked, View.OnClickListener l) {
        TextView t = tvM(label, 16, fg);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(18), dp(16), dp(18), dp(16));
        Drawable b = stroked ? shape(bg, 16, pal.stroke, 1) : shape(bg, 16);
        t.setBackground(ripple(b, alpha(fg, 50), 16));
        t.setClickable(true);
        t.setOnClickListener(l);
        return t;
    }

    TextView chip(String label, boolean selected, View.OnClickListener l) {
        TextView t = tvM(label, 14, selected ? pal.accent : pal.dim);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(16), dp(10), dp(16), dp(10));
        Drawable b = selected ? shape(alpha(pal.accent, 40), 20, pal.accent, 1) : shape(pal.surface2, 20, pal.stroke, 1);
        t.setBackground(ripple(b, alpha(pal.text, 40), 20));
        t.setClickable(true);
        t.setOnClickListener(l);
        return t;
    }

    EditText input(String hint, String text, int inputType) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setText(text);
        e.setTextColor(pal.text);
        e.setHintTextColor(pal.dim);
        e.setTextSize(16);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setPadding(dp(16), dp(14), dp(16), dp(14));
        e.setBackground(shape(pal.surface2, 14, pal.stroke, 1));
        return e;
    }

    Dialog dialog(View content) {
        Dialog dlg = new Dialog(c);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlg.setContentView(content);
        Window w = dlg.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout((int) (c.getResources().getDisplayMetrics().widthPixels * 0.90f), WindowManager.LayoutParams.WRAP_CONTENT);
            w.setDimAmount(0.65f);
        }
        return dlg;
    }

    LinearLayout dialogCard() {
        LinearLayout l = vbox();
        l.setPadding(dp(22), dp(22), dp(22), dp(18));
        l.setBackground(shape(pal.surface, 26, pal.stroke, 1));
        return l;
    }
}
