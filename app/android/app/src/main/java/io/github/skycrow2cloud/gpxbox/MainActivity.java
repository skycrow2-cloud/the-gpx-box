package io.github.skycrow2cloud.gpxbox;

import android.content.Intent;
import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(GpxSuitePlugin.class);           // passage direct vers / depuis GPX 3D
        super.onCreate(savedInstanceState);
        String p = GpxSuitePlugin.read(this, getIntent());
        if (p != null) GpxSuitePlugin.pending = p;      // lue par la page une fois la carte prête
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String p = GpxSuitePlugin.read(this, intent);
        if (p != null && getBridge() != null) {
            GpxSuitePlugin.pending = p;
            getBridge().getWebView().post(() -> getBridge().getWebView()
                .evaluateJavascript("window.__gpxSuiteReceive && window.__gpxSuiteReceive()", null));
        }
    }
}
