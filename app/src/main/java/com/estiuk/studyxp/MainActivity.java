package com.estiuk.studyxp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    static final int M = Ui.M, W = Ui.W;
    static final int TAB_HOME = 0, TAB_FOCUS = 1, TAB_BOOST = 2, TAB_STATS = 3, TAB_REWARDS = 4;
    static final String[] TAB_LABELS = {"Home", "Focus", "Boost", "Stats", "Rewards"};
    static final int[] GOALS = {25, 45, 60, 0};

    interface StrCb { void on(String s); }

    Store st;
    Pal pal;
    Ui ui;
    FrameLayout frame, content;
    LinearLayout navRow;
    FrameLayout[] navPills = new FrameLayout[5];
    NavIconView[] navIcons = new NavIconView[5];
    TextView[] navLabels = new TextView[5];
    ScrollView sv;
    int tab = TAB_HOME, topInset = 0, bottomInset = 0;
    final Handler h = new Handler(Looper.getMainLooper());

    RingView focusRing;
    TextView xpLive;
    EditText subjectInput;
    String subjectText = "";
    int goalSel = 45;
    boolean goalNotified = false;

    final boolean[] bonusSel = new boolean[7];
    View[] bRows = new View[7];
    TextView[] bChecks = new TextView[7];
    TextView bonusBtn;
    int penaltySel = 10;
    int statsMode = 0;

    final Runnable ticker = new Runnable() {
        @Override public void run() {
            updateFocusUi();
            h.postDelayed(this, 500);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        st = new Store(this);
        st.rollDay();
        st.sync();
        st.pollLevelUp();
        goalSel = st.p.getInt("lastGoal", 45);
        buildShell();
        showTab(TAB_HOME, true);
    }

    @Override protected void onResume() {
        super.onResume();
        st.rollDay();
        st.sync();
        int d = st.syncUsage();
        rebuild();
        if (d > 0) snack("Auto usage penalty: −" + d + " XP");
    }

    @Override protected void onPause() {
        stopTick();
        super.onPause();
    }

    // ------------------------------------------------------------ shell
    void buildShell() {
        pal = Pal.get(st.dark());
        ui = new Ui(this, pal);
        getWindow().setBackgroundDrawable(new ColorDrawable(pal.bg));
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        } else if (Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        applyBarIcons();

        frame = new FrameLayout(this);
        frame.setBackgroundColor(pal.bg);
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        content = new FrameLayout(this);
        main.addView(content, new LinearLayout.LayoutParams(M, 0, 1f));
        main.addView(buildNav(), new LinearLayout.LayoutParams(M, W));
        frame.addView(main, new FrameLayout.LayoutParams(M, M));
        setContentView(frame);

        frame.setOnApplyWindowInsetsListener((v, in) -> {
            int t = in.getSystemWindowInsetTop(), bt = in.getSystemWindowInsetBottom();
            if (t != topInset || bt != bottomInset) {
                topInset = t;
                bottomInset = bt;
                navRow.setPadding(0, ui.dp(8), 0, ui.dp(8) + bottomInset);
                rebuild();
            }
            return in;
        });
        frame.requestApplyInsets();
    }

    void applyBarIcons() {
        boolean light = !pal.dark;
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                c.setSystemBarsAppearance(light ? mask : 0, mask);
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            int f = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (light) {
                f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 26) f |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(f);
        }
    }

    View buildNav() {
        LinearLayout wrap = ui.vbox();
        wrap.setBackgroundColor(pal.nav);
        View line = new View(this);
        line.setBackgroundColor(pal.stroke);
        wrap.addView(line, new LinearLayout.LayoutParams(M, Math.max(1, ui.dp(1))));
        navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setPadding(0, ui.dp(8), 0, ui.dp(8) + bottomInset);
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            LinearLayout item = ui.vbox();
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            FrameLayout pill = new FrameLayout(this);
            NavIconView icon = new NavIconView(this, i);
            pill.addView(icon, new FrameLayout.LayoutParams(ui.dp(26), ui.dp(26), Gravity.CENTER));
            item.addView(pill, new LinearLayout.LayoutParams(ui.dp(58), ui.dp(32)));
            TextView lab = ui.tvM(TAB_LABELS[i], 11, pal.dim);
            lab.setGravity(Gravity.CENTER);
            item.addView(lab, ui.lpm(W, W, 0, 3, 0, 0));
            item.setOnClickListener(v -> { hideKeyboard(); showTab(idx, true); });
            navPills[i] = pill; navIcons[i] = icon; navLabels[i] = lab;
            navRow.addView(item, new LinearLayout.LayoutParams(0, W, 1f));
        }
        wrap.addView(navRow, new LinearLayout.LayoutParams(M, W));
        return wrap;
    }

    void updateNav() {
        for (int i = 0; i < 5; i++) {
            boolean on = i == tab;
            navPills[i].setBackground(on ? ui.shape(Ui.alpha(pal.accent, 45), 16) : null);
            navIcons[i].setColor(on ? pal.accent : pal.dim);
            navLabels[i].setTextColor(on ? pal.accent : pal.dim);
        }
    }

    void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(frame.getWindowToken(), 0);
        } catch (Exception ignored) {
        }
    }

    void showTab(int t, boolean animate) {
        stopTick();
        tab = t;
        focusRing = null;
        xpLive = null;
        subjectInput = null;
        content.removeAllViews();
        ScrollView s = new ScrollView(this);
        s.setVerticalScrollBarEnabled(false);
        s.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = ui.vbox();
        switch (t) {
            case TAB_HOME: buildHome(col); break;
            case TAB_FOCUS: buildFocus(col); break;
            case TAB_BOOST: buildBoost(col); break;
            case TAB_STATS: buildStats(col); break;
            default: buildRewards(col); break;
        }
        s.addView(col, new FrameLayout.LayoutParams(M, W));
        content.addView(s, new FrameLayout.LayoutParams(M, M));
        sv = s;
        updateNav();
        if (animate) {
            s.setAlpha(0f);
            s.animate().alpha(1f).setDuration(220).start();
        }
    }

    void rebuild() {
        if (content == null) return;
        final int y = sv != null ? sv.getScrollY() : 0;
        showTab(tab, false);
        final ScrollView cur = sv;
        cur.post(() -> cur.scrollTo(0, y));
    }

    void afterChange() {
        rebuild();
        int up = st.pollLevelUp();
        if (up >= 0) levelUpDialog(up);
        else if (st.pollStudyDay()) snack("🔥 Study day complete — streak " + st.streak());
    }

    // ------------------------------------------------------------ helpers
    String num(int n) { return String.format(Locale.US, "%,d", n); }

    String dur(int mins) {
        return mins < 60 ? mins + "m" : (mins / 60) + "h " + (mins % 60) + "m";
    }

    String clock(long s) {
        long hh = s / 3600, mm = (s % 3600) / 60, ss = s % 60;
        return hh > 0 ? String.format(Locale.US, "%d:%02d:%02d", hh, mm, ss) : String.format(Locale.US, "%02d:%02d", mm, ss);
    }

    String greeting() {
        int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hr < 12 ? "Good morning" : hr < 17 ? "Good afternoon" : "Good evening";
    }

    View header(String title, String sub) {
        LinearLayout l = ui.vbox();
        l.addView(ui.tvM(title, 30, pal.text));
        l.addView(ui.tv(sub, 14, pal.dim), ui.lpm(W, W, 0, 6, 0, 0));
        l.setPadding(0, 0, 0, ui.dp(18));
        return l;
    }

    TextView section(String s) {
        TextView t = ui.caps(s, pal.dim);
        t.setLayoutParams(ui.lpm(W, W, 4, 22, 0, 10));
        return t;
    }

    LinearLayout bar(float frac, int fill, float heightDp) {
        LinearLayout b = new LinearLayout(this);
        b.setBackground(ui.shape(pal.track, 8));
        View f = new View(this);
        f.setBackground(ui.shape(fill, 8));
        b.addView(f, new LinearLayout.LayoutParams(0, M, Math.max(0f, frac)));
        View r = new View(this);
        b.addView(r, new LinearLayout.LayoutParams(0, M, Math.max(0f, 1f - frac)));
        b.setLayoutParams(ui.lp(M, ui.dp(heightDp)));
        return b;
    }

    /** Sets layout weight 1 on a view already added to a horizontal LinearLayout. */
    static void weight(View v) { ((LinearLayout.LayoutParams) v.getLayoutParams()).weight = 1f; }

    View statRow(int dot, String label, String value) {
        LinearLayout r = ui.hbox();
        View d = new View(this);
        d.setBackground(ui.oval(dot));
        r.addView(d, new LinearLayout.LayoutParams(ui.dp(9), ui.dp(9)));
        TextView l = ui.tv(label, 14, pal.dim);
        r.addView(l, ui.lpm(0, W, 10, 0, 0, 0));
        weight(l);
        r.addView(ui.tvM(value, 15, pal.text));
        r.setPadding(0, ui.dp(7), 0, ui.dp(7));
        return r;
    }

    void snack(String msg) {
        if (frame == null) return;
        final TextView t = ui.tvM(msg, 14, pal.bg);
        t.setPadding(ui.dp(18), ui.dp(12), ui.dp(18), ui.dp(12));
        t.setBackground(ui.shape(pal.text, 24));
        t.setMaxWidth(ui.dp(330));
        t.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(W, W, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = ui.dp(92) + bottomInset;
        frame.addView(t, lp);
        t.setAlpha(0f);
        t.setTranslationY(ui.dp(10));
        t.animate().alpha(1f).translationY(0).setDuration(220).start();
        h.postDelayed(() -> t.animate().alpha(0f).setDuration(260).withEndAction(() -> {
            if (t.getParent() != null) ((ViewGroup) t.getParent()).removeView(t);
        }).start(), 2400);
    }

    // ------------------------------------------------------------ HOME
    void buildHome(LinearLayout col) {
        col.setPadding(0, 0, 0, ui.dp(28));
        int xp = st.totalXp(), lv = st.level();

        FrameLayout hero = new FrameLayout(this);
        ForestView fv = new ForestView(this);
        fv.setScene(pal.dark, pal.bg);
        hero.addView(fv, new FrameLayout.LayoutParams(M, M));
        LinearLayout gl = ui.vbox();
        gl.setPadding(ui.dp(22), topInset + ui.dp(18), ui.dp(70), 0);
        gl.addView(ui.tv(greeting(), 14, Ui.alpha(pal.text, 210)));
        gl.addView(ui.tvM(st.name(), 26, pal.text), ui.lpm(W, W, 0, 4, 0, 0));
        hero.addView(gl, new FrameLayout.LayoutParams(M, W));
        TextView gear = ui.tv("⚙", 20, pal.text);
        gear.setGravity(Gravity.CENTER);
        gear.setBackground(ui.ripple(ui.oval(Ui.alpha(pal.surface, 150)), Ui.alpha(pal.text, 50), 22));
        gear.setOnClickListener(v -> settingsDialog());
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(ui.dp(44), ui.dp(44), Gravity.TOP | Gravity.END);
        gp.setMargins(0, topInset + ui.dp(12), ui.dp(16), 0);
        hero.addView(gear, gp);
        col.addView(hero, new LinearLayout.LayoutParams(M, ui.dp(250) + topInset));

        LinearLayout lc = ui.card();
        col.addView(lc, ui.lpm(M, W, 16, -84, 16, 0));
        LinearLayout top = ui.hbox();
        LinearLayout left = ui.vbox();
        left.addView(ui.caps("Level " + (lv + 1), pal.accent));
        left.addView(ui.tvM(Store.NAMES[lv], 28, pal.text), ui.lpm(W, W, 0, 4, 0, 0));
        left.addView(ui.tv(Store.STAGES[lv] + " stage", 13, pal.dim), ui.lpm(W, W, 0, 4, 0, 0));
        left.addView(ui.tvM("⭐ " + num(xp) + " XP", 20, pal.gold), ui.lpm(W, W, 0, 14, 0, 0));
        top.addView(left, new LinearLayout.LayoutParams(0, W, 1f));
        top.addView(new TreeView(this, pal, lv), new LinearLayout.LayoutParams(ui.dp(120), ui.dp(130)));
        lc.addView(top);
        lc.addView(bar(st.levelProgress(), pal.accent, 10), ui.lpm(M, ui.dp(10), 0, 14, 0, 0));
        LinearLayout labs = ui.hbox();
        labs.addView(ui.tv(num(Store.LEVELS[lv]), 12, pal.dim));
        labs.addView(ui.hFill());
        if (!st.maxLevel()) labs.addView(ui.tv(num(Store.LEVELS[lv + 1]), 12, pal.dim));
        lc.addView(labs, ui.lpm(M, W, 0, 6, 0, 0));
        String togo = st.maxLevel() ? "Max level reached — you are a Master 🏆"
                : num(Store.LEVELS[lv + 1] - xp) + " XP to Level " + (lv + 2) + " · " + Store.NAMES[lv + 1];
        lc.addView(ui.tv(togo, 13, pal.dim), ui.lpm(W, W, 0, 8, 0, 0));

        LinearLayout tc = ui.card();
        col.addView(tc, ui.lpm(M, W, 16, 14, 16, 0));
        LinearLayout th = ui.hbox();
        th.addView(ui.tvM("Today", 17, pal.text));
        th.addView(ui.hFill());
        th.addView(ui.tv(new SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(new Date()), 13, pal.dim));
        tc.addView(th);
        LinearLayout tr = ui.hbox();
        int net = st.net();
        boolean done = net >= Store.STUDY_DAY_XP;
        RingView ring = new RingView(this);
        ring.setColors(pal.track, done ? pal.gold : pal.accent, pal.text, pal.dim, pal.track);
        ring.setTexts(String.valueOf(net), "OF " + Store.STUDY_DAY_XP + " XP");
        ring.setMainScale(0.26f);
        ring.setProgress(Math.max(0, net) / (float) Store.STUDY_DAY_XP, true);
        tr.addView(ring, new LinearLayout.LayoutParams(ui.dp(132), ui.dp(132)));
        LinearLayout stats = ui.vbox();
        stats.addView(statRow(pal.accent, "Focus", st.focus() + " min"));
        stats.addView(statRow(pal.gold, "Bonus", "+" + st.bonus() + " XP"));
        stats.addView(statRow(pal.danger, "Penalty", "−" + st.penalty() + " XP"));
        tr.addView(stats, ui.lpm(0, W, 18, 0, 0, 0));
        weight(stats);
        tc.addView(tr, ui.lpm(M, W, 0, 12, 0, 0));
        TextView status = ui.tvM(done ? "🔥 Study day complete" : "Earn " + (Store.STUDY_DAY_XP - Math.max(0, net)) + " more XP to count today as a study day",
                13, done ? pal.gold : pal.dim);
        status.setGravity(Gravity.CENTER);
        status.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
        status.setBackground(ui.shape(done ? Ui.alpha(pal.gold, 36) : pal.surface2, 12));
        tc.addView(status, ui.lpm(M, W, 0, 14, 0, 0));

        LinearLayout sc = ui.card();
        col.addView(sc, ui.lpm(M, W, 16, 14, 16, 0));
        LinearLayout sh = ui.hbox();
        sh.addView(ui.tv("🔥", 30, pal.text));
        LinearLayout sCol = ui.vbox();
        sCol.addView(ui.tvM(st.streak() + " day streak", 18, pal.text));
        sCol.addView(ui.tv("Best: " + st.bestStreak() + " days", 13, pal.dim), ui.lpm(W, W, 0, 4, 0, 0));
        sh.addView(sCol, ui.lpm(W, W, 14, 0, 0, 0));
        sc.addView(sh);
        LinearLayout week = ui.hbox();
        int[] wn = st.weekNet();
        String[] wl = st.weekLabels();
        for (int i = 0; i < 7; i++) {
            LinearLayout dcol = ui.vbox();
            dcol.setGravity(Gravity.CENTER_HORIZONTAL);
            boolean ok = wn[i] >= Store.STUDY_DAY_XP;
            TextView dot = ui.tvM(ok ? "✓" : "", 14, pal.onAccent);
            dot.setGravity(Gravity.CENTER);
            GradientDrawable g = ui.oval(ok ? pal.accent : pal.surface2);
            if (!ok) g.setStroke(Math.max(1, ui.dp(i == 6 ? 1.5f : 1f)), i == 6 ? pal.accent : pal.stroke);
            dot.setBackground(g);
            dcol.addView(dot, new LinearLayout.LayoutParams(ui.dp(32), ui.dp(32)));
            dcol.addView(ui.tv(wl[i], 12, i == 6 ? pal.accent : pal.dim), ui.lpm(W, W, 0, 6, 0, 0));
            week.addView(dcol, new LinearLayout.LayoutParams(0, W, 1f));
        }
        sc.addView(week, ui.lpm(M, W, 0, 16, 0, 0));

        LinearLayout rc = ui.card();
        col.addView(rc, ui.lpm(M, W, 16, 14, 16, 0));
        LinearLayout rr = ui.hbox();
        rr.addView(ui.tv(st.maxLevel() ? "🏆" : "🎁", 30, pal.text));
        LinearLayout rCol = ui.vbox();
        if (st.maxLevel()) {
            rCol.addView(ui.tvM("All rewards unlocked", 17, pal.text));
            rCol.addView(ui.tv("You reached the Ancient Forest.", 13, pal.dim), ui.lpm(W, W, 0, 4, 0, 0));
        } else {
            rCol.addView(ui.caps("Next reward", pal.dim));
            rCol.addView(ui.tvM(st.reward(lv + 1), 18, pal.gold), ui.lpm(W, W, 0, 4, 0, 0));
            rCol.addView(ui.tv(num(Store.LEVELS[lv + 1] - xp) + " XP to go", 13, pal.dim), ui.lpm(W, W, 0, 4, 0, 0));
        }
        rr.addView(rCol, ui.lpm(W, W, 14, 0, 0, 0));
        rc.addView(rr);

        col.addView(ui.button("▶   Start a focus session", true, v -> showTab(TAB_FOCUS, true)), ui.lpm(M, W, 16, 18, 16, 0));
    }

    // ------------------------------------------------------------ FOCUS
    void saveSubject() {
        if (subjectInput != null) subjectText = subjectInput.getText().toString();
    }

    void buildFocus(LinearLayout col) {
        col.setPadding(ui.dp(20), topInset + ui.dp(24), ui.dp(20), ui.dp(28));
        col.addView(header("Focus", "Deep work, one minute = one XP"));
        boolean open = st.sessionOpen(), run = st.sessionRunning();

        if (!open) {
            subjectInput = ui.input("What are you studying?", subjectText, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            col.addView(subjectInput, ui.lp(M, W));
            LinearLayout chips = ui.hbox();
            for (int i = 0; i < GOALS.length; i++) {
                final int g = GOALS[i];
                chips.addView(ui.chip(g == 0 ? "Free" : g + " min", goalSel == g, v -> {
                    saveSubject();
                    goalSel = g;
                    st.p.edit().putInt("lastGoal", g).apply();
                    rebuild();
                }), ui.lpm(W, W, 0, 0, 10, 0));
            }
            col.addView(chips, ui.lpm(M, W, 0, 14, 0, 0));
        } else {
            String subj = st.sessionSubject();
            TextView t = ui.tvM(subj.isEmpty() ? "Deep focus" : subj, 18, pal.text);
            t.setGravity(Gravity.CENTER);
            col.addView(t, ui.lp(M, W));
            TextView g = ui.tv(st.sessionGoal() > 0 ? "Goal: " + st.sessionGoal() + " min" : "No time limit", 13, pal.dim);
            g.setGravity(Gravity.CENTER);
            col.addView(g, ui.lpm(M, W, 0, 6, 0, 0));
        }

        focusRing = new RingView(this);
        focusRing.setColors(pal.track, pal.accent, pal.text, pal.dim, Ui.alpha(pal.dim, 90));
        focusRing.setTicks(true);
        focusRing.setMainScale(0.19f);
        LinearLayout.LayoutParams rp = ui.lpm(ui.dp(290), ui.dp(290), 0, 22, 0, 8);
        rp.gravity = Gravity.CENTER_HORIZONTAL;
        col.addView(focusRing, rp);

        xpLive = ui.tvM("", 14, pal.accent);
        xpLive.setGravity(Gravity.CENTER);
        col.addView(xpLive, ui.lpm(M, W, 0, 4, 0, 18));

        if (!open) {
            col.addView(ui.button("▶   Start focus", true, v -> {
                saveSubject();
                String subj = subjectText;
                subjectText = "";
                hideKeyboard();
                st.sessionStart(goalSel, subj);
                goalNotified = false;
                rebuild();
            }), ui.lp(M, W));
        } else {
            LinearLayout row = ui.hbox();
            row.addView(ui.button(run ? "❚❚  Pause" : "▶  Resume", false, v -> {
                if (st.sessionRunning()) st.sessionPause(); else st.sessionResume();
                rebuild();
            }), ui.lpm(0, W, 0, 0, 6, 0));
            row.addView(ui.button("✓  Finish", true, v -> {
                int mins = st.sessionFinish();
                if (mins < 1) snack("Less than 1 minute — no XP added");
                else snack("+" + mins + " Focus XP 🌿");
                afterChange();
            }), ui.lpm(0, W, 6, 0, 0, 0));
            weight(row.getChildAt(0));
            weight(row.getChildAt(1));
            col.addView(row, ui.lp(M, W));
            TextView disc = ui.tvM("Discard session", 14, pal.dim);
            disc.setGravity(Gravity.CENTER);
            disc.setPadding(0, ui.dp(16), 0, ui.dp(6));
            disc.setOnClickListener(v -> confirm("Discard this session?", "No XP will be added for it.", "Discard", true, () -> {
                st.sessionDiscard();
                rebuild();
            }));
            col.addView(disc, ui.lp(M, W));
        }

        col.addView(section("Today"));
        LinearLayout tc = ui.card();
        LinearLayout tr = ui.hbox();
        tr.addView(ui.tvM(dur(st.focus()), 26, pal.text));
        tr.addView(ui.tv("focused today", 14, pal.dim), ui.lpm(W, W, 10, 6, 0, 0));
        tc.addView(tr);
        List<String[]> rs = st.recentSessions(3);
        for (String[] s : rs) tc.addView(sessionRow(s), ui.lpm(M, W, 0, 12, 0, 0));
        col.addView(tc, ui.lp(M, W));

        if (st.sessionRunning()) startTick(); else updateFocusUi();
    }

    View sessionRow(String[] s) {
        LinearLayout r = ui.hbox();
        LinearLayout l = ui.vbox();
        l.addView(ui.tvM(s[3].isEmpty() ? "Focus session" : s[3], 15, pal.text));
        String when = s[0].equals(st.date()) ? "Today" : s[0].equals(st.dayOffset(-1)) ? "Yesterday" : s[0];
        l.addView(ui.tv(when + " · " + s[1], 12, pal.dim), ui.lpm(W, W, 0, 3, 0, 0));
        r.addView(l, new LinearLayout.LayoutParams(0, W, 1f));
        TextView xp = ui.tvM("+" + s[2] + " XP", 13, pal.accent);
        xp.setPadding(ui.dp(10), ui.dp(5), ui.dp(10), ui.dp(5));
        xp.setBackground(ui.shape(Ui.alpha(pal.accent, 36), 12));
        r.addView(xp);
        return r;
    }

    void startTick() {
        h.removeCallbacks(ticker);
        h.post(ticker);
    }

    void stopTick() { h.removeCallbacks(ticker); }

    void updateFocusUi() {
        if (focusRing == null || tab != TAB_FOCUS) return;
        long sec = st.sessionSeconds();
        boolean open = st.sessionOpen();
        int goal = open ? st.sessionGoal() : goalSel;
        focusRing.setTexts(open ? clock(sec) : (goal > 0 ? clock(goal * 60L) : "00:00"),
                open ? (st.sessionRunning() ? "FOCUSING" : "PAUSED") : "READY");
        float prog = !open ? 0f : (goal > 0 ? sec / (goal * 60f) : (sec % 60) / 60f);
        focusRing.setProgress(prog, false);
        if (xpLive != null) xpLive.setText(open ? "+" + (sec / 60) + " XP earned so far" : "1 focused minute = 1 XP");
        if (open && goal > 0 && sec >= goal * 60L && !goalNotified) {
            goalNotified = true;
            snack("🎯 Goal reached — finish to bank your XP");
        }
    }

    // ------------------------------------------------------------ BOOST
    void styleBonus(int i) {
        boolean on = bonusSel[i];
        bRows[i].setBackground(ui.ripple(on ? ui.shape(Ui.alpha(pal.accent, 30), 18, pal.accent, 1.5f) : ui.shape(pal.surface, 18, pal.stroke, 1),
                Ui.alpha(pal.text, 40), 18));
        bChecks[i].setText(on ? "✓" : "");
        GradientDrawable g = ui.oval(on ? pal.accent : Color.TRANSPARENT);
        g.setStroke(Math.max(1, ui.dp(1.5f)), on ? pal.accent : pal.dim);
        bChecks[i].setBackground(g);
    }

    int bonusTotal() {
        int x = 0;
        for (int i = 0; i < 7; i++) if (bonusSel[i]) x += Store.BONUS_VALS[i];
        return x;
    }

    void updateBonusBtn() {
        int x = bonusTotal();
        bonusBtn.setText(x == 0 ? "Select bonuses to add" : "Add +" + x + " XP");
        bonusBtn.setAlpha(x == 0 ? 0.5f : 1f);
    }

    void buildBoost(LinearLayout col) {
        col.setPadding(ui.dp(20), topInset + ui.dp(24), ui.dp(20), ui.dp(28));
        col.addView(header("Boost", "Log learning wins, keep distractions honest"));
        col.addView(section("Learning bonuses"));
        for (int i = 0; i < 7; i++) {
            final int idx = i;
            LinearLayout row = ui.hbox();
            row.setPadding(ui.dp(14), ui.dp(14), ui.dp(14), ui.dp(14));
            row.setClickable(true);
            TextView chk = ui.tvM("", 14, pal.onAccent);
            chk.setGravity(Gravity.CENTER);
            row.addView(chk, new LinearLayout.LayoutParams(ui.dp(26), ui.dp(26)));
            TextView title = ui.tv(Store.BONUS_TITLES[i], 15, pal.text);
            row.addView(title, ui.lpm(0, W, 14, 0, 10, 0));
            weight(title);
            TextView pill = ui.tvM("+" + Store.BONUS_VALS[i], 13, pal.gold);
            pill.setPadding(ui.dp(10), ui.dp(5), ui.dp(10), ui.dp(5));
            pill.setBackground(ui.shape(Ui.alpha(pal.gold, 36), 12));
            row.addView(pill);
            bRows[i] = row;
            bChecks[i] = chk;
            row.setOnClickListener(v -> { bonusSel[idx] = !bonusSel[idx]; styleBonus(idx); updateBonusBtn(); });
            col.addView(row, ui.lpm(M, W, 0, 0, 0, 10));
            styleBonus(i);
        }
        bonusBtn = ui.button("", true, v -> {
            int x = bonusTotal();
            if (x == 0) { snack("Select at least one bonus"); return; }
            st.addBonus(x);
            for (int i = 0; i < 7; i++) bonusSel[i] = false;
            snack("+" + x + " Bonus XP ⚡");
            afterChange();
        });
        updateBonusBtn();
        col.addView(bonusBtn, ui.lpm(M, W, 0, 6, 0, 0));

        col.addView(section("Scrolling penalty"));
        LinearLayout pc = ui.card();
        pc.addView(ui.tvM("Distracted? Be honest.", 16, pal.text));
        pc.addView(ui.tv("Every minute of mindless scrolling costs 1 XP.", 13, pal.dim), ui.lpm(W, W, 0, 5, 0, 0));
        LinearLayout chips = ui.hbox();
        int[] opts = {5, 10, 15, 30};
        for (int i = 0; i < opts.length; i++) {
            final int o = opts[i];
            chips.addView(ui.chip("" + o, penaltySel == o, v -> { penaltySel = o; rebuild(); }), ui.lpm(W, W, 0, 0, 8, 0));
        }
        chips.addView(ui.chip("Custom", false, v -> promptText("Custom penalty", "Minutes", "", InputType.TYPE_CLASS_NUMBER, s -> {
            try {
                int m = Integer.parseInt(s.trim());
                if (m > 0 && m <= 1440) { st.addPenalty(m); snack("−" + m + " XP"); afterChange(); }
            } catch (Exception ignored) {
            }
        })));
        pc.addView(chips, ui.lpm(M, W, 0, 14, 0, 0));
        pc.addView(ui.buttonColor("Apply −" + penaltySel + " XP", Ui.alpha(pal.danger, 45), pal.danger, false, v -> {
            st.addPenalty(penaltySel);
            snack("−" + penaltySel + " XP penalty");
            afterChange();
        }), ui.lpm(M, W, 0, 14, 0, 0));
        col.addView(pc, ui.lp(M, W));

        col.addView(section("Auto usage tracking"));
        LinearLayout uc = ui.card();
        boolean ok = st.hasUsageAccess();
        LinearLayout uh = ui.hbox();
        uh.addView(ui.tvM("Instagram · Facebook · TikTok", 15, pal.text));
        uh.addView(ui.hFill());
        TextView badge = ui.tvM(ok ? "ACTIVE" : "OFF", 11, ok ? pal.accent : pal.danger);
        badge.setPadding(ui.dp(10), ui.dp(4), ui.dp(10), ui.dp(4));
        badge.setBackground(ui.shape(Ui.alpha(ok ? pal.accent : pal.danger, 40), 10));
        uh.addView(badge);
        uc.addView(uh);
        uc.addView(ui.tv("Android can't tell scrolling from other use, so screen time in these apps becomes an estimated penalty.", 13, pal.dim),
                ui.lpm(M, W, 0, 8, 0, 0));
        if (ok) {
            int[] u = st.usageToday();
            LinearLayout boxes = ui.hbox();
            for (int i = 0; i < 3; i++) {
                LinearLayout b = ui.vbox();
                b.setGravity(Gravity.CENTER_HORIZONTAL);
                b.setPadding(0, ui.dp(12), 0, ui.dp(12));
                b.setBackground(ui.shape(pal.surface2, 14));
                b.addView(ui.tvM(u[i] + "m", 18, pal.text));
                b.addView(ui.tv(Store.TRACKED_NAMES[i], 12, pal.dim), ui.lpm(W, W, 0, 4, 0, 0));
                boxes.addView(b, ui.lpm(0, W, i == 0 ? 0 : 4, 0, i == 2 ? 0 : 4, 0));
                weight(b);
            }
            uc.addView(boxes, ui.lpm(M, W, 0, 14, 0, 0));
            uc.addView(ui.tv("Auto penalty applied today: −" + st.autoPenaltyToday() + " XP", 13, pal.dim), ui.lpm(W, W, 0, 12, 0, 0));
            uc.addView(ui.button("Sync now", false, v -> {
                int d = st.syncUsage();
                snack(d > 0 ? "Auto usage penalty: −" + d + " XP" : "Already up to date");
                if (d > 0) afterChange(); else rebuild();
            }), ui.lpm(M, W, 0, 14, 0, 0));
        } else {
            uc.addView(ui.button("Grant usage access", true, v -> {
                try { startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); } catch (Exception ignored) { }
                snack("Enable Study XP, then come back");
            }), ui.lpm(M, W, 0, 14, 0, 0));
        }
        col.addView(uc, ui.lp(M, W));
    }

    // ------------------------------------------------------------ STATS
    View tile(String label, String value, int color) {
        LinearLayout t = ui.card();
        t.setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(16));
        t.addView(ui.tv(label, 12, pal.dim));
        t.addView(ui.tvM(value, 22, color), ui.lpm(W, W, 0, 6, 0, 0));
        return t;
    }

    void tileRow(LinearLayout col, String l1, String v1, int c1, String l2, String v2, int c2) {
        LinearLayout r = ui.hbox();
        r.addView(tile(l1, v1, c1), ui.lpm(0, W, 0, 0, 6, 0));
        r.addView(tile(l2, v2, c2), ui.lpm(0, W, 6, 0, 0, 0));
        weight(r.getChildAt(0));
        weight(r.getChildAt(1));
        col.addView(r, ui.lpm(M, W, 0, 0, 0, 12));
    }

    void buildStats(LinearLayout col) {
        col.setPadding(ui.dp(20), topInset + ui.dp(24), ui.dp(20), ui.dp(28));
        col.addView(header("Statistics", "Your growth over time"));
        tileRow(col, "Total XP", num(st.totalXp()), pal.gold, "Total focus", dur(st.totalFocusMin()), pal.accent);
        tileRow(col, "Sessions", String.valueOf(st.totalSessions()), pal.text, "Best day", st.bestDayNet() + " XP", pal.gold);
        tileRow(col, "Current streak", st.streak() + " days", pal.accent, "Best streak", st.bestStreak() + " days", pal.text);

        col.addView(section("Last 7 days"));
        LinearLayout cc = ui.card();
        LinearLayout chips = ui.hbox();
        chips.addView(ui.chip("Net XP", statsMode == 0, v -> { statsMode = 0; rebuild(); }), ui.lpm(W, W, 0, 0, 8, 0));
        chips.addView(ui.chip("Focus min", statsMode == 1, v -> { statsMode = 1; rebuild(); }));
        cc.addView(chips);
        BarChartView chart = new BarChartView(this);
        chart.setColors(pal.accent, pal.track, pal.dim, pal.text, pal.gold);
        int[] data = statsMode == 0 ? st.weekNet() : st.weekFocus();
        chart.setData(data, st.weekLabels(), statsMode == 0 ? Store.STUDY_DAY_XP : -1);
        cc.addView(chart, ui.lpm(M, ui.dp(190), 0, 14, 0, 0));
        int sum = 0;
        for (int v : data) sum += Math.max(0, v);
        cc.addView(ui.tv("7-day total: " + sum + (statsMode == 0 ? " XP" : " min") + "  ·  avg " + (sum / 7) + " per day"
                + (statsMode == 0 ? "  ·  dashed line = study-day goal" : ""), 12, pal.dim), ui.lpm(W, W, 0, 10, 0, 0));
        col.addView(cc, ui.lp(M, W));

        col.addView(section("Recent sessions"));
        LinearLayout rc = ui.card();
        List<String[]> rs = st.recentSessions(8);
        if (rs.isEmpty()) {
            rc.addView(ui.tv("No sessions yet. Start your first focus session 🌱", 14, pal.dim));
        } else {
            for (int i = 0; i < rs.size(); i++) rc.addView(sessionRow(rs.get(i)), ui.lpm(M, W, 0, i == 0 ? 0 : 14, 0, 0));
        }
        col.addView(rc, ui.lp(M, W));
    }

    // ------------------------------------------------------------ REWARDS
    void buildRewards(LinearLayout col) {
        col.setPadding(ui.dp(20), topInset + ui.dp(24), ui.dp(20), ui.dp(28));
        col.addView(header("Rewards", "Grow your tree. Tap a reward to rename it."));
        final int lv = st.level(), xp = st.totalXp();
        for (int i = 0; i < Store.LEVELS.length; i++) {
            final int idx = i;
            boolean unlocked = i <= lv, current = i == lv;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            LinearLayout rail = ui.vbox();
            rail.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView node = ui.tvM(unlocked && !current ? "✓" : String.valueOf(i + 1), 14, unlocked ? pal.onAccent : pal.dim);
            node.setGravity(Gravity.CENTER);
            GradientDrawable g = ui.oval(unlocked ? pal.accent : pal.surface2);
            g.setStroke(Math.max(1, ui.dp(current ? 3 : 1)), current ? Ui.alpha(pal.accent, 120) : pal.stroke);
            node.setBackground(g);
            rail.addView(node, new LinearLayout.LayoutParams(ui.dp(38), ui.dp(38)));
            if (i < Store.LEVELS.length - 1) {
                View line = new View(this);
                line.setBackgroundColor(i < lv ? pal.accent : pal.stroke);
                rail.addView(line, new LinearLayout.LayoutParams(ui.dp(2), 0, 1f));
            }
            row.addView(rail, new LinearLayout.LayoutParams(ui.dp(40), M));

            LinearLayout card = ui.vbox();
            card.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
            card.setBackground(ui.ripple(current ? ui.shape(Ui.alpha(pal.accent, 24), 18, pal.accent, 1.5f) : ui.shape(pal.surface, 18, pal.stroke, 1),
                    Ui.alpha(pal.text, 40), 18));
            card.addView(ui.caps("Level " + (i + 1), unlocked ? pal.accent : pal.dim));
            card.addView(ui.tvM(Store.NAMES[i], 18, unlocked ? pal.text : pal.dim), ui.lpm(W, W, 0, 4, 0, 0));
            card.addView(ui.tv(Store.STAGES[i] + " · " + num(Store.LEVELS[i]) + " XP", 13, pal.dim), ui.lpm(W, W, 0, 4, 0, 0));
            if (i > 0) {
                LinearLayout rw = ui.hbox();
                rw.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
                rw.setBackground(ui.shape(unlocked ? Ui.alpha(pal.gold, 30) : pal.surface2, 12));
                rw.addView(ui.tv(unlocked ? "🎁" : "🔒", 16, pal.text));
                TextView rn = ui.tvM(st.reward(i), 14, unlocked ? pal.gold : pal.dim);
                rw.addView(rn, ui.lpm(0, W, 10, 0, 0, 0));
                weight(rn);
                rw.addView(ui.tv(unlocked ? "Unlocked" : num(Store.LEVELS[i] - xp) + " XP to go", 12, unlocked ? pal.accent : pal.dim));
                card.addView(rw, ui.lpm(M, W, 0, 12, 0, 0));
                card.setOnClickListener(v -> promptText("Rename reward", "Level " + (idx + 1) + " reward", st.reward(idx),
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, s -> { st.setReward(idx, s); rebuild(); }));
            }
            row.addView(card, ui.lpm(0, W, 12, 0, 0, 12));
            weight(card);
            col.addView(row, ui.lp(M, W));
        }
    }

    // ------------------------------------------------------------ dialogs
    void promptText(String title, String hint, String initial, int type, final StrCb cb) {
        LinearLayout d = ui.dialogCard();
        d.addView(ui.tvM(title, 20, pal.text));
        final EditText e = ui.input(hint, initial, type);
        d.addView(e, ui.lpm(M, W, 0, 16, 0, 0));
        final Dialog dlg = ui.dialog(d);
        LinearLayout btns = ui.hbox();
        btns.addView(ui.button("Cancel", false, v -> dlg.dismiss()), ui.lpm(0, W, 0, 0, 6, 0));
        btns.addView(ui.button("Save", true, v -> { cb.on(e.getText().toString()); dlg.dismiss(); }), ui.lpm(0, W, 6, 0, 0, 0));
        weight(btns.getChildAt(0));
        weight(btns.getChildAt(1));
        d.addView(btns, ui.lpm(M, W, 0, 18, 0, 0));
        dlg.show();
    }

    void confirm(String title, String msg, String okLabel, boolean danger, final Runnable ok) {
        LinearLayout d = ui.dialogCard();
        d.addView(ui.tvM(title, 20, pal.text));
        d.addView(ui.tv(msg, 14, pal.dim), ui.lpm(W, W, 0, 8, 0, 0));
        final Dialog dlg = ui.dialog(d);
        LinearLayout btns = ui.hbox();
        btns.addView(ui.button("Cancel", false, v -> dlg.dismiss()), ui.lpm(0, W, 0, 0, 6, 0));
        View okBtn = danger ? ui.buttonColor(okLabel, pal.danger, Color.WHITE, false, v -> { dlg.dismiss(); ok.run(); })
                : ui.button(okLabel, true, v -> { dlg.dismiss(); ok.run(); });
        btns.addView(okBtn, ui.lpm(0, W, 6, 0, 0, 0));
        weight(btns.getChildAt(0));
        weight(btns.getChildAt(1));
        d.addView(btns, ui.lpm(M, W, 0, 18, 0, 0));
        dlg.show();
    }

    void levelUpDialog(int lv) {
        LinearLayout d = ui.dialogCard();
        d.setGravity(Gravity.CENTER_HORIZONTAL);
        d.addView(ui.caps("Level up", pal.accent));
        LinearLayout.LayoutParams tp = ui.lpm(ui.dp(170), ui.dp(180), 0, 10, 0, 0);
        tp.gravity = Gravity.CENTER_HORIZONTAL;
        d.addView(new TreeView(this, pal, lv), tp);
        TextView t = ui.tvM("Level " + (lv + 1) + " · " + Store.NAMES[lv], 24, pal.text);
        t.setGravity(Gravity.CENTER);
        d.addView(t, ui.lpm(M, W, 0, 8, 0, 0));
        TextView s = ui.tv("Your tree grew into a " + Store.STAGES[lv] + ".", 14, pal.dim);
        s.setGravity(Gravity.CENTER);
        d.addView(s, ui.lpm(M, W, 0, 6, 0, 0));
        TextView r = ui.tvM("🎁  " + st.reward(lv) + " unlocked", 15, pal.gold);
        r.setGravity(Gravity.CENTER);
        r.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
        r.setBackground(ui.shape(Ui.alpha(pal.gold, 36), 14));
        d.addView(r, ui.lpm(M, W, 0, 16, 0, 0));
        final Dialog dlg = ui.dialog(d);
        d.addView(ui.button("Awesome", true, v -> dlg.dismiss()), ui.lpm(M, W, 0, 16, 0, 0));
        dlg.show();
    }

    void settingsDialog() {
        LinearLayout d = ui.dialogCard();
        d.addView(ui.tvM("Settings", 22, pal.text));
        d.addView(ui.caps("Your name", pal.dim), ui.lpm(W, W, 2, 18, 0, 8));
        final EditText name = ui.input("Name", st.name(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        d.addView(name, ui.lp(M, W));
        d.addView(ui.caps("Theme", pal.dim), ui.lpm(W, W, 2, 18, 0, 8));
        final Dialog dlg = ui.dialog(d);
        LinearLayout themes = ui.hbox();
        themes.addView(ui.chip("🌲 Night forest", st.dark(), v -> {
            st.setName(name.getText().toString()); st.setDark(true); dlg.dismiss(); buildShell(); showTab(tab, false);
        }), ui.lpm(W, W, 0, 0, 8, 0));
        themes.addView(ui.chip("☀ Morning mist", !st.dark(), v -> {
            st.setName(name.getText().toString()); st.setDark(false); dlg.dismiss(); buildShell(); showTab(tab, false);
        }));
        d.addView(themes, ui.lp(M, W));
        d.addView(ui.button("Save", true, v -> {
            st.setName(name.getText().toString());
            dlg.dismiss();
            rebuild();
        }), ui.lpm(M, W, 0, 20, 0, 0));
        TextView reset = ui.tvM("Reset all progress", 14, pal.danger);
        reset.setGravity(Gravity.CENTER);
        reset.setPadding(0, ui.dp(16), 0, ui.dp(4));
        reset.setOnClickListener(v -> {
            dlg.dismiss();
            confirm("Reset everything?", "This deletes your XP, streak, history and rewards. It cannot be undone.", "Reset", true, () -> {
                st.resetAll();
                for (int i = 0; i < 7; i++) bonusSel[i] = false;
                buildShell();
                showTab(TAB_HOME, false);
                snack("Fresh start 🌱");
            });
        });
        d.addView(reset, ui.lp(M, W));
        dlg.show();
    }
}
