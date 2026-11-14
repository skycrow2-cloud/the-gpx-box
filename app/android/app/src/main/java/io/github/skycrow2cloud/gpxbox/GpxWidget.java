package io.github.skycrow2cloud.gpxbox;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

import com.getcapacitor.Bridge;

/**
 * Widget « Enregistrement GPS » pensé pour l'écran externe du Motorola razr (format carré, fond sombre),
 * utilisable aussi sur l'écran d'accueil. Affiche le temps en mouvement (chronomètre géré par le système,
 * aucune mise à jour nécessaire chaque seconde) et la distance ; boutons Démarrer / Pause-Reprendre / Terminer.
 * L'état est envoyé par la page (GpxSuite.widgetState) et mémorisé dans les préférences.
 */
public class GpxWidget extends AppWidgetProvider {
    static final String PREFS = "gpxbox.widget";
    static final String EXTRA = "gpxbox.widget";
    static final String ACTION_PAUSE = "io.github.skycrow2cloud.gpxbox.WIDGET_PAUSE";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        render(c);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        super.onReceive(c, intent);
        if (!ACTION_PAUSE.equals(intent.getAction())) return;
        Bridge b = GpxSuitePlugin.liveBridge();
        if (b != null && b.getWebView() != null) {
            b.getWebView().post(() -> b.getWebView()
                .evaluateJavascript("window.__gpxWidget && window.__gpxWidget('pause')", null));
        } else {
            try { c.startActivity(appIntent(c, "pause").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception ignored) { }
        }
    }

    static Intent appIntent(Context c, String action) {
        Intent i = new Intent(c, MainActivity.class);
        i.setAction("io.github.skycrow2cloud.gpxbox.WIDGET." + action);   // action distincte : une PendingIntent par bouton
        i.putExtra(EXTRA, action);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return i;
    }

    static PendingIntent openApp(Context c, String action, int code) {
        return PendingIntent.getActivity(c, code, appIntent(c, action),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void save(Context c, String state, long movingMs, String km) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("state", state)
            .putLong("moving", movingMs)
            .putLong("at", System.currentTimeMillis())
            .putString("km", km)
            .apply();
        render(c);
    }

    static void render(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, GpxWidget.class));
        if (ids == null || ids.length == 0) return;

        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String state = p.getString("state", "idle");
        long moving = p.getLong("moving", 0);
        long at = p.getLong("at", System.currentTimeMillis());
        String km = p.getString("km", "0,00");
        boolean rec = "rec".equals(state), paused = "paused".equals(state), idle = !rec && !paused;

        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_rec);
        long elapsed = moving + (rec ? Math.max(0, System.currentTimeMillis() - at) : 0);
        v.setChronometer(R.id.wg_chrono, SystemClock.elapsedRealtime() - (idle ? 0 : elapsed), null, rec);
        v.setTextViewText(R.id.wg_km, (idle ? "0,00" : km) + " km");
        v.setTextViewText(R.id.wg_status, rec ? "● Enregistrement" : paused ? "❚❚ En pause" : "Prêt");
        v.setTextColor(R.id.wg_status, rec ? 0xFFFF6B5E : paused ? 0xFFE8A33D : 0xB3FFFFFF);

        v.setViewVisibility(R.id.wg_start, idle ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.wg_pause, idle ? View.GONE : View.VISIBLE);
        v.setViewVisibility(R.id.wg_stop, idle ? View.GONE : View.VISIBLE);
        v.setImageViewResource(R.id.wg_pause, paused ? R.drawable.ic_wg_play : R.drawable.ic_wg_pause);
        v.setContentDescription(R.id.wg_pause, paused ? "Reprendre" : "Pause");

        v.setOnClickPendingIntent(R.id.wg_root, openApp(c, "open", 1));
        v.setOnClickPendingIntent(R.id.wg_start, openApp(c, "start", 2));
        v.setOnClickPendingIntent(R.id.wg_stop, openApp(c, "stop", 3));
        v.setOnClickPendingIntent(R.id.wg_map, openApp(c, "open", 4));
        Intent pause = new Intent(c, GpxWidget.class).setAction(ACTION_PAUSE);
        v.setOnClickPendingIntent(R.id.wg_pause, PendingIntent.getBroadcast(c, 5, pause,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        m.updateAppWidget(ids, v);
    }
}
