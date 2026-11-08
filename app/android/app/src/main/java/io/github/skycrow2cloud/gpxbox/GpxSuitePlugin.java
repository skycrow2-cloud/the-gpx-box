package io.github.skycrow2cloud.gpxbox;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Passage direct The-GPX-Box ⇄ GPX 3D entre les deux applications Android.
 * JS : Capacitor.registerPlugin('GpxSuite').openOther({ payload }) → { ok } ; takeHandoff() → { payload }.
 * La trace (JSON {gpx, name, from, t}) est écrite dans le cache, partagée par le FileProvider de l'application
 * et transmise par une intention explicite. ok = false si GPX 3D est absente (ou trop ancienne) : la page bascule sur le site.
 */
@CapacitorPlugin(name = "GpxSuite")
public class GpxSuitePlugin extends Plugin {
    static final String ACTION = "io.github.skycrow2cloud.gpxsuite.HANDOFF";
    static final String OTHER_PKG = "io.github.skycrow2cloud.gpx3d";
    static volatile String pending;   // trace reçue de GPX 3D, en attente d'être lue par la page

    @PluginMethod
    public void openOther(PluginCall call) {
        JSObject r = new JSObject();
        r.put("ok", send(getActivity(), call.getString("payload", "")));
        call.resolve(r);
    }

    @PluginMethod
    public void takeHandoff(PluginCall call) {
        JSObject r = new JSObject();
        String p = pending;
        pending = null;
        r.put("payload", p == null ? "" : p);
        call.resolve(r);
    }

    static boolean send(Activity a, String payload) {
        Intent launch = a.getPackageManager().getLaunchIntentForPackage(OTHER_PKG);
        if (launch == null) return false;
        try {
            if (payload == null || payload.isEmpty()) {
                a.startActivity(launch);
                return true;
            }
            File f = new File(a.getCacheDir(), "handoff.json");
            try (FileOutputStream out = new FileOutputStream(f)) {
                out.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            Uri u = FileProvider.getUriForFile(a, a.getPackageName() + ".fileprovider", f);
            Intent i = new Intent(ACTION);
            i.setPackage(OTHER_PKG);
            i.setClipData(ClipData.newRawUri("gpx", u));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            a.startActivity(i);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /** Contenu reçu d'une intention HANDOFF, ou null. */
    static String read(Context c, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction()) || intent.getClipData() == null
                || intent.getClipData().getItemCount() == 0) return null;
        Uri u = intent.getClipData().getItemAt(0).getUri();
        if (u == null) return null;
        try (InputStream in = c.getContentResolver().openInputStream(u)) {
            if (in == null) return null;
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return new String(bo.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
