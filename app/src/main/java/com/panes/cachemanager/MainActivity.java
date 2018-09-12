package com.panes.cachemanager;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.panes.cachemanager.core.LruMM;

import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "LruMM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LruMM<String, String> lruMM = new LruMM<>(6);
        for (int i = 0; i < 10; i++) {
            lruMM.put("k" + i, "v" + i);
        }
        String v5 = lruMM.get("k5");
        String v6 = lruMM.get("k6");
        Log.d(TAG, "v5 = " + v5 + "; " + "v6 = " + v6);
        lruMM.put("k10", "v10");
        lruMM.put("k11", "v11");
//        lruMM.put("k8", "v8");
//        lruMM.put("k9", "v9");
//        lruMM.put("k1", "v1");
        Map<String, String> major = lruMM.snapshotMajor();
        Log.d(TAG, "major: ");
        for (Map.Entry<String, String> entry : major.entrySet()) {
            Log.d(TAG, entry.getKey() + ":" + entry.getValue());
        }
        Map<String, String> minor = lruMM.snapshotMinor();
        Log.d(TAG, "minor: ");
        for (Map.Entry<String, String> entry : minor.entrySet()) {
            Log.d(TAG, entry.getKey() + ":" + entry.getValue());
        }

    }
}
