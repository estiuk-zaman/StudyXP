package com.estiuk.studyxp;

import android.graphics.Color;

/** Colour palette: "Night Forest" (dark) and "Morning Mist" (light). */
final class Pal {
    final boolean dark;
    final int bg, surface, surface2, stroke, text, dim, accent, accentDeep, onAccent, gold, danger, track, nav;

    private Pal(boolean dark, String bg, String surface, String surface2, String stroke, String text, String dim,
                String accent, String accentDeep, String onAccent, String gold, String danger, String track, String nav) {
        this.dark = dark;
        this.bg = Color.parseColor(bg);
        this.surface = Color.parseColor(surface);
        this.surface2 = Color.parseColor(surface2);
        this.stroke = Color.parseColor(stroke);
        this.text = Color.parseColor(text);
        this.dim = Color.parseColor(dim);
        this.accent = Color.parseColor(accent);
        this.accentDeep = Color.parseColor(accentDeep);
        this.onAccent = Color.parseColor(onAccent);
        this.gold = Color.parseColor(gold);
        this.danger = Color.parseColor(danger);
        this.track = Color.parseColor(track);
        this.nav = Color.parseColor(nav);
    }

    static Pal get(boolean dark) {
        if (dark) {
            return new Pal(true, "#0B1A13", "#12281D", "#183326", "#234734", "#E7F3EA", "#8CA999",
                    "#5BD38A", "#2E9E62", "#05210F", "#F2C14E", "#F07167", "#1F3D2D", "#0E2118");
        }
        return new Pal(false, "#EDF4EA", "#FFFFFF", "#E2EDE0", "#D0E2CE", "#12261A", "#5B7A67",
                "#2E8B57", "#1F6B41", "#FFFFFF", "#C98A00", "#C94A3F", "#D6E6D3", "#FFFFFF");
    }
}
