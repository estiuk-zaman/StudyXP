package com.estiuk.studyxp;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** All data + XP rules. Keeps the original preference keys so old data still works. */
final class Store {
    static final int[] LEVELS = {0, 500, 1500, 3000, 5000, 8000, 12000};
    static final String[] NAMES = {"Beginner", "Learner", "Builder", "Problem Solver", "Engineer", "Advanced", "Master"};
    static final String[] STAGES = {"Seed", "Sprout", "Sapling", "Young Pine", "Evergreen", "Grove", "Ancient Forest"};
    static final String[] DEF_REWARDS = {"—", "Small Reward", "Medium Reward", "Big Reward", "Major Reward", "Elite Reward", "Hair Dryer"};
    static final String[] BONUS_TITLES = {
            "Concept understood independently", "Problem solved independently", "Code written independently",
            "Explained in own words", "Mistake found & fixed", "Practice / Revision", "Wall / hard problem attempt"};
    static final int[] BONUS_VALS = {10, 15, 15, 10, 15, 10, 15};
    static final int STUDY_DAY_XP = 20;
    static final String[] TRACKED_NAMES = {"Instagram", "Facebook", "TikTok"};
    static final String[][] TRACKED = {
            {"com.instagram.android", "com.instagram.lite"},
            {"com.facebook.katana", "com.facebook.lite"},
            {"com.zhiliaoapp.musically", "com.ss.android.ugc.trill"}};

    final Context ctx;
    final SharedPreferences p;

    Store(Context c) {
        ctx = c.getApplicationContext();
        p = ctx.getSharedPreferences("studyxp", 0);
    }

    // ---------- dates ----------
    static String fmt(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    String date() { return fmt(Calendar.getInstance()); }

    String dayOffset(int n) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, n);
        return fmt(c);
    }

    void rollDay() {
        String today = date();
        String saved = p.getString("day", "");
        if (!today.equals(saved)) {
            SharedPreferences.Editor e = p.edit();
            if (!saved.isEmpty()) {
                e.putString("h_" + saved, p.getInt("todayFocus", 0) + "," + p.getInt("todayBonus", 0) + "," + p.getInt("todayPenalty", 0));
            }
            e.putString("day", today).putInt("todayFocus", 0).putInt("todayBonus", 0).putInt("todayPenalty", 0)
                    .putInt("todayNet", 0).putInt("loggedNet", 0).putLong("usageBase", 0).apply();
        }
    }

    // ---------- XP ----------
    int focus() { return p.getInt("todayFocus", 0); }
    int bonus() { return p.getInt("todayBonus", 0); }
    int penalty() { return p.getInt("todayPenalty", 0); }
    int net() { return focus() + bonus() - penalty(); }
    int totalXp() { return p.getInt("totalXp", 0); }

    void sync() {
        int net = net();
        int prev = p.getInt("loggedNet", 0);
        if (net != prev) {
            p.edit().putInt("totalXp", Math.max(0, totalXp() + net - prev)).putInt("loggedNet", net).apply();
        }
        updateStreak();
    }

    void addFocus(int mins, String subject) {
        p.edit().putInt("todayFocus", focus() + mins)
                .putInt("totFocus", p.getInt("totFocus", 0) + mins)
                .putInt("totSessions", p.getInt("totSessions", 0) + 1).apply();
        logSession(mins, subject);
        sync();
    }

    void addBonus(int x) { p.edit().putInt("todayBonus", bonus() + x).apply(); sync(); }
    void addPenalty(int m) { p.edit().putInt("todayPenalty", penalty() + m).apply(); sync(); }

    // ---------- level ----------
    int level() {
        int xp = totalXp();
        int lv = 0;
        for (int i = 0; i < LEVELS.length; i++) if (xp >= LEVELS[i]) lv = i;
        return lv;
    }

    boolean maxLevel() { return level() >= LEVELS.length - 1; }

    float levelProgress() {
        int lv = level();
        if (lv >= LEVELS.length - 1) return 1f;
        int base = LEVELS[lv], next = LEVELS[lv + 1];
        return Math.max(0f, Math.min(1f, (totalXp() - base) / (float) (next - base)));
    }

    int pollLevelUp() {
        int lv = level();
        int seen = p.getInt("seenLevel", -1);
        p.edit().putInt("seenLevel", lv).apply();
        return (seen >= 0 && lv > seen) ? lv : -1;
    }

    String reward(int idx) {
        if (idx <= 0) return "—";
        return p.getString("reward_" + idx, DEF_REWARDS[idx]);
    }

    void setReward(int idx, String s) {
        if (s == null || s.trim().isEmpty()) p.edit().remove("reward_" + idx).apply();
        else p.edit().putString("reward_" + idx, clean(s)).apply();
    }

    // ---------- streak ----------
    void updateStreak() {
        String d = date();
        String last = p.getString("lastStudyDay", "");
        if (net() >= STUDY_DAY_XP && !d.equals(last)) {
            int s = p.getInt("streak", 0);
            s = last.equals(dayOffset(-1)) ? s + 1 : 1;
            p.edit().putString("lastStudyDay", d).putInt("streak", s)
                    .putInt("bestStreak", Math.max(s, p.getInt("bestStreak", 0))).apply();
        }
    }

    int streak() {
        String last = p.getString("lastStudyDay", "");
        if (last.equals(date()) || last.equals(dayOffset(-1))) return p.getInt("streak", 0);
        return 0;
    }

    int bestStreak() { return Math.max(p.getInt("bestStreak", 0), streak()); }
    boolean studyDayDone() { return date().equals(p.getString("lastStudyDay", "")); }

    boolean pollStudyDay() {
        if (studyDayDone() && !date().equals(p.getString("celebrated", ""))) {
            p.edit().putString("celebrated", date()).apply();
            return true;
        }
        return false;
    }

    // ---------- history ----------
    int[] dayStats(String day) {
        if (day.equals(date())) return new int[]{focus(), bonus(), penalty()};
        String s = p.getString("h_" + day, null);
        if (s == null) return new int[]{0, 0, 0};
        try {
            String[] a = s.split(",");
            return new int[]{Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2])};
        } catch (Exception e) {
            return new int[]{0, 0, 0};
        }
    }

    int[] weekNet() {
        int[] r = new int[7];
        for (int i = 0; i < 7; i++) {
            int[] d = dayStats(dayOffset(i - 6));
            r[i] = d[0] + d[1] - d[2];
        }
        return r;
    }

    int[] weekFocus() {
        int[] r = new int[7];
        for (int i = 0; i < 7; i++) r[i] = dayStats(dayOffset(i - 6))[0];
        return r;
    }

    String[] weekLabels() {
        String[] names = {"S", "M", "T", "W", "T", "F", "S"};
        String[] r = new String[7];
        for (int i = 0; i < 7; i++) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE, i - 6);
            r[i] = names[c.get(Calendar.DAY_OF_WEEK) - 1];
        }
        return r;
    }

    int bestDayNet() {
        int best = Math.max(0, net());
        for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
            if (e.getKey().startsWith("h_") && e.getValue() instanceof String) {
                try {
                    String[] a = ((String) e.getValue()).split(",");
                    best = Math.max(best, Integer.parseInt(a[0]) + Integer.parseInt(a[1]) - Integer.parseInt(a[2]));
                } catch (Exception ignored) {
                }
            }
        }
        return best;
    }

    int totalFocusMin() { return Math.max(p.getInt("totFocus", 0), 0); }
    int totalSessions() { return p.getInt("totSessions", 0); }

    // ---------- sessions log ----------
    static String clean(String s) {
        return s == null ? "" : s.replace('|', ' ').replace('\n', ' ').trim();
    }

    private void logSession(int mins, String subject) {
        String t = new SimpleDateFormat("yyyy-MM-dd|HH:mm", Locale.US).format(new Date());
        String line = t + "|" + mins + "|" + clean(subject);
        String old = p.getString("sessions", "");
        String all = old.isEmpty() ? line : old + "\n" + line;
        String[] ls = all.split("\n");
        if (ls.length > 150) {
            StringBuilder sb = new StringBuilder();
            for (int i = ls.length - 150; i < ls.length; i++) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(ls[i]);
            }
            all = sb.toString();
        }
        p.edit().putString("sessions", all).apply();
    }

    /** Newest first. Each entry: {day, time, minutes, subject}. */
    List<String[]> recentSessions(int max) {
        List<String[]> out = new ArrayList<>();
        String all = p.getString("sessions", "");
        if (all.isEmpty()) return out;
        String[] ls = all.split("\n");
        for (int i = ls.length - 1; i >= 0 && out.size() < max; i--) {
            String[] a = ls[i].split("\\|", 4);
            if (a.length >= 3) out.add(new String[]{a[0], a[1], a[2], a.length > 3 ? a[3] : ""});
        }
        return out;
    }

    // ---------- live focus session (survives app being closed) ----------
    boolean sessionOpen() { return p.getBoolean("sOpen", false); }
    boolean sessionRunning() { return p.getBoolean("sRun", false); }
    int sessionGoal() { return p.getInt("sGoal", 0); }
    String sessionSubject() { return p.getString("sSubject", ""); }

    long sessionSeconds() {
        long acc = p.getLong("sAcc", 0);
        if (sessionRunning()) acc += (System.currentTimeMillis() - p.getLong("sStart", 0)) / 1000;
        return Math.max(0, acc);
    }

    void sessionStart(int goalMin, String subject) {
        p.edit().putBoolean("sOpen", true).putBoolean("sRun", true).putLong("sStart", System.currentTimeMillis())
                .putLong("sAcc", 0).putInt("sGoal", goalMin).putString("sSubject", clean(subject)).apply();
    }

    void sessionPause() {
        if (!sessionRunning()) return;
        long s = sessionSeconds();
        p.edit().putBoolean("sRun", false).putLong("sAcc", s).apply();
    }

    void sessionResume() {
        if (!sessionOpen() || sessionRunning()) return;
        p.edit().putBoolean("sRun", true).putLong("sStart", System.currentTimeMillis()).apply();
    }

    void sessionDiscard() {
        p.edit().putBoolean("sOpen", false).putBoolean("sRun", false).putLong("sAcc", 0).apply();
    }

    /** Returns banked minutes (0 if the session was shorter than a minute). */
    int sessionFinish() {
        int mins = (int) (sessionSeconds() / 60);
        String subj = sessionSubject();
        sessionDiscard();
        if (mins >= 1) addFocus(mins, subj);
        return mins;
    }

    // ---------- usage access (auto penalty) ----------
    boolean hasUsageAccess() {
        try {
            AppOpsManager a = (AppOpsManager) ctx.getSystemService(Context.APP_OPS_SERVICE);
            int m = a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.getPackageName());
            return m == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, UsageStats> aggregate() {
        try {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            UsageStatsManager u = (UsageStatsManager) ctx.getSystemService(Context.USAGE_STATS_SERVICE);
            return u.queryAndAggregateUsageStats(c.getTimeInMillis(), System.currentTimeMillis());
        } catch (Exception e) {
            return null;
        }
    }

    /** Minutes today for {Instagram, Facebook, TikTok}. */
    int[] usageToday() {
        int[] r = new int[3];
        if (!hasUsageAccess()) return r;
        Map<String, UsageStats> m = aggregate();
        if (m == null) return r;
        for (int i = 0; i < 3; i++) {
            long t = 0;
            for (String pkg : TRACKED[i]) {
                UsageStats s = m.get(pkg);
                if (s != null) t += s.getTotalTimeInForeground();
            }
            r[i] = (int) (t / 60000);
        }
        return r;
    }

    /** Applies new usage minutes as penalty. Returns the delta applied. */
    int syncUsage() {
        if (!hasUsageAccess()) return 0;
        int[] u = usageToday();
        long total = u[0] + u[1] + u[2];
        long base = p.getLong("usageBase", 0);
        if (total > base) {
            int delta = (int) (total - base);
            p.edit().putLong("usageBase", total).putInt("todayPenalty", penalty() + delta).apply();
            sync();
            return delta;
        }
        return 0;
    }

    int autoPenaltyToday() { return (int) p.getLong("usageBase", 0); }

    // ---------- settings ----------
    boolean dark() { return p.getBoolean("dark", true); }
    void setDark(boolean b) { p.edit().putBoolean("dark", b).apply(); }
    String name() { return p.getString("name", "Learner"); }
    void setName(String s) { p.edit().putString("name", s.trim().isEmpty() ? "Learner" : clean(s)).apply(); }

    void resetAll() {
        p.edit().clear().apply();
        rollDay();
        sync();
        pollLevelUp();
    }
}
