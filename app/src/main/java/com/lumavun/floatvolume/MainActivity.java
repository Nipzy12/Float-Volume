package com.lumavun.floatvolume;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int PURPLE = Color.rgb(81, 75, 211);
    private TextView status;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(247,247,252));
        getWindow().setNavigationBarColor(Color.rgb(247,247,252));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ScrollView page = new ScrollView(this);
        page.setFillViewport(true);
        page.setBackgroundColor(Color.rgb(247,247,252));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(34), dp(24), dp(32));
        page.addView(root);

        TextView title = text("Float Volume", 29, Color.rgb(27,27,47), true);
        root.addView(title);
        TextView intro = text("Kontrol volume tanpa tombol fisik. Ketuk bubble untuk membuka panel, tahan lalu geser untuk memindahkannya.", 15, Color.rgb(99,99,120), false);
        addWithMargin(root, intro, 10, 20);

        status = text("", 14, PURPLE, true);
        addWithMargin(root, status, 8, 18);

        Button permission = button("1. Izinkan tampil di atas aplikasi lain", true);
        addWithMargin(root, permission, 8, 9);
        permission.setOnClickListener(v -> requestOverlayPermission());

        Button enable = button("2. Aktifkan tombol mengambang", true);
        addWithMargin(root, enable, 8, 9);
        enable.setOnClickListener(v -> enableOverlay());

        Button disable = button("Matikan tombol mengambang", false);
        addWithMargin(root, disable, 8, 24);
        disable.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            status.setText("Tombol dimatikan.");
        });

        TextView customize = text("Kustomisasi", 21, Color.rgb(32,32,54), true);
        addWithMargin(root, customize, 12, 16);

        final android.content.SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        final TextView sizeText = text("Ukuran bubble: " + prefs.getInt("size", 56) + " dp", 14, Color.rgb(72,72,94), false);
        addWithMargin(root, sizeText, 7, 8);
        SeekBar size = new SeekBar(this);
        size.setMax(44);
        size.setProgress(prefs.getInt("size", 56) - 44);
        root.addView(size);
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean byUser) {
                int value = 44 + p;
                sizeText.setText("Ukuran bubble: " + value + " dp");
                if (byUser) prefs.edit().putInt("size", value).apply();
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });

        final TextView opacityText = text("Transparansi: " + prefs.getInt("opacity", 90) + "%", 14, Color.rgb(72,72,94), false);
        addWithMargin(root, opacityText, 15, 8);
        SeekBar opacity = new SeekBar(this);
        opacity.setMax(60);
        opacity.setProgress(prefs.getInt("opacity", 90) - 40);
        root.addView(opacity);
        opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean byUser) {
                int value = 40 + p;
                opacityText.setText("Transparansi: " + value + "%");
                if (byUser) prefs.edit().putInt("opacity", value).apply();
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });

        TextView tip = text("Perubahan ukuran dan transparansi diterapkan saat tombol diaktifkan kembali. Tidak diperlukan izin aksesibilitas atau koneksi internet.", 13, Color.rgb(112,112,130), false);
        addWithMargin(root, tip, 20, 0);
        setContentView(page);
        refreshStatus();
    }

    @Override protected void onResume() {
        super.onResume();
        if (status != null) refreshStatus();
    }

    private void refreshStatus() {
        boolean granted = Settings.canDrawOverlays(this);
        status.setText(granted
            ? (OverlayService.running ? "● Aktif: overlay berjalan" : "✓ Izin overlay tersedia")
            : "⚠ Izin overlay belum diberikan");
    }

    private void requestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            refreshStatus();
            return;
        }
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
        }
    }

    private void enableOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
            && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 201);
        }
        Intent intent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
        else startService(intent);
        status.setText("● Memulai kontrol volume...");
    }

    private Button button(String label, boolean filled) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(filled ? Color.WHITE : PURPLE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(filled ? PURPLE : Color.WHITE);
        bg.setCornerRadius(dp(16));
        if (!filled) bg.setStroke(dp(1), Color.rgb(210,210,230));
        b.setBackground(bg);
        b.setPadding(dp(12), dp(13), dp(12), dp(13));
        return b;
    }

    private TextView text(String txt, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(txt);
        t.setTextColor(color);
        t.setTextSize(sp);
        if (bold) t.setTypeface(null, 1);
        t.setGravity(Gravity.START);
        return t;
    }

    private void addWithMargin(LinearLayout root, View view, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        p.bottomMargin = dp(bottom);
        root.addView(view, p);
    }

    private int dp(float d) { return (int)(getResources().getDisplayMetrics().density*d+0.5f); }
}
