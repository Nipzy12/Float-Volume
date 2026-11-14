package com.lumavun.floatvolume;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class OverlayService extends Service {
    public static volatile boolean running = false;
    private static final int NOTIFICATION_ID = 81;
    private static final String CHANNEL_ID = "float_volume";
    private static final String ACTION_STOP = "com.lumavun.floatvolume.STOP";

    private WindowManager wm;
    private AudioManager audio;
    private View bubble;
    private View panel;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;
    private SeekBar volumeSlider;
    private TextView levelText;
    private int lastNonZero = 5;
    private int bubbleSize;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        createNotificationChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        Notification notif = makeNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notif);
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        running = true;
        if (bubble == null) createBubble();
        return START_NOT_STICKY;
    }

    private void createBubble() {
        android.content.SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        bubbleSize = dp(prefs.getInt("size", 56));
        float alpha = prefs.getInt("opacity", 90) / 100f;

        TextView button = new TextView(this);
        button.setText("＋\n−");
        button.setTextColor(Color.WHITE);
        button.setTextSize(20);
        button.setTypeface(null, 1);
        button.setGravity(Gravity.CENTER);
        button.setLineSpacing(-dp(8), 1);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(102,96,244), Color.rgb(60,65,182)});
        bg.setCornerRadius(dp(50));
        button.setBackground(bg);
        button.setAlpha(alpha);
        button.setElevation(dp(10));
        bubble = button;

        bubbleParams = overlayParams(bubbleSize, bubbleSize);
        bubbleParams.x = prefs.getInt("x", dp(18));
        bubbleParams.y = prefs.getInt("y", dp(230));
        wm.addView(button, bubbleParams);

        button.setOnTouchListener(new View.OnTouchListener() {
            float originX, originY;
            int originalWindowX, originalWindowY;
            boolean dragging;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch(e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        originX = e.getRawX();
                        originY = e.getRawY();
                        originalWindowX = bubbleParams.x;
                        originalWindowY = bubbleParams.y;
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX()-originX, dy = e.getRawY()-originY;
                        if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) dragging = true;
                        if (dragging) {
                            if (panel != null) hidePanel();
                            int screenW = getResources().getDisplayMetrics().widthPixels;
                            int screenH = getResources().getDisplayMetrics().heightPixels;
                            bubbleParams.x = clamp(originalWindowX + (int)dx, 0, Math.max(0,screenW-bubbleSize));
                            bubbleParams.y = clamp(originalWindowY + (int)dy, 0, Math.max(0,screenH-bubbleSize));
                            wm.updateViewLayout(bubble, bubbleParams);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!dragging) {
                            if (panel == null) showPanel(); else hidePanel();
                        } else {
                            prefs.edit().putInt("x", bubbleParams.x).putInt("y", bubbleParams.y).apply();
                        }
                        return true;
                    default: return true;
                }
            }
        });
    }

    private void showPanel() {
        if (panel != null) return;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(247,247,255));
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), Color.rgb(219,219,240));
        card.setBackground(bg);
        card.setElevation(dp(14));

        TextView heading = text("Kontrol Volume", 16, true);
        card.addView(heading);
        levelText = text("", 13, false);
        withMargin(card, levelText, 6, 11);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button minus = control("−");
        Button mute = control("Mute");
        Button plus = control("+");
        row.addView(minus, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams middle = new LinearLayout.LayoutParams(0, dp(48), 1.5f);
        middle.leftMargin = dp(8);
        middle.rightMargin = dp(8);
        row.addView(mute, middle);
        row.addView(plus, new LinearLayout.LayoutParams(0, dp(48), 1));
        card.addView(row);

        int max = Math.max(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volumeSlider = new SeekBar(this);
        volumeSlider.setMax(max);
        withMargin(card, volumeSlider, 13, 7);
        volumeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) changeVolume(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        Button close = new Button(this);
        close.setText("Tutup panel");
        close.setAllCaps(false);
        close.setTextColor(Color.rgb(81,75,211));
        close.setTextSize(12);
        close.setBackgroundColor(Color.TRANSPARENT);
        withMargin(card, close, 3, 0);
        close.setOnClickListener(v -> hidePanel());

        minus.setOnClickListener(v -> changeVolume(getVolume()-1));
        plus.setOnClickListener(v -> changeVolume(getVolume()+1));
        mute.setOnClickListener(v -> {
            int current = getVolume();
            if (current > 0) {
                lastNonZero = current;
                changeVolume(0);
            } else {
                changeVolume(Math.max(1,lastNonZero));
            }
        });

        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        int width = Math.min(dp(265), Math.max(dp(200),screenW-dp(24)));
        panelParams = overlayParams(width, WindowManager.LayoutParams.WRAP_CONTENT);
        boolean toLeft = bubbleParams.x + bubbleSize + width + dp(10) > screenW;
        panelParams.x = toLeft ? bubbleParams.x - width - dp(10) : bubbleParams.x + bubbleSize + dp(10);
        panelParams.x = clamp(panelParams.x, 0, Math.max(0, screenW - width));
        panelParams.y = clamp(bubbleParams.y, 0, Math.max(0, screenH - dp(260)));
        wm.addView(card, panelParams);
        panel = card;
        refreshVolume();
    }

    private void hidePanel() {
        if (panel != null) {
            try { wm.removeView(panel); } catch (Exception ignored) {}
            panel = null;
            volumeSlider = null;
            levelText = null;
        }
    }

    private void changeVolume(int desired) {
        int max = Math.max(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        int volume = clamp(desired, 0, max);
        try { audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0); }
        catch (SecurityException ignored) {}
        refreshVolume();
    }

    private int getVolume() {
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void refreshVolume() {
        int max = Math.max(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        int current = getVolume();
        if (volumeSlider != null && volumeSlider.getProgress() != current) volumeSlider.setProgress(current);
        if (levelText != null) levelText.setText("Volume media: " + (current*100/max) + "%");
    }

    private TextView text(String label, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(sp);
        t.setTextColor(bold ? Color.rgb(28,28,55) : Color.rgb(103,103,128));
        if (bold) t.setTypeface(null, 1);
        return t;
    }

    private Button control(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(label.length()>2 ? 13 : 19);
        b.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(81,75,211));
        bg.setCornerRadius(dp(13));
        b.setBackground(bg);
        return b;
    }

    private void withMargin(LinearLayout parent, View v, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.topMargin = dp(top);
        p.bottomMargin = dp(bottom);
        parent.addView(v,p);
    }

    private WindowManager.LayoutParams overlayParams(int width, int height) {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        return p;
    }

    private Notification makeNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 8, open,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent stop = new Intent(this, OverlayService.class).setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 9, stop,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentTitle("Float Volume aktif")
            .setContentText("Ketuk bubble untuk mengatur volume")
            .setContentIntent(openPi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Matikan", stopPi)
            .setOngoing(true)
            .build();
    }

    private void createNotificationChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
            "Kontrol Volume", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Notifikasi untuk tombol volume mengambang");
        manager.createNotificationChannel(channel);
    }

    @Override public void onDestroy() {
        hidePanel();
        if (bubble != null) {
            try { wm.removeView(bubble); } catch (Exception ignored) {}
            bubble = null;
        }
        running = false;
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    private int dp(float n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    private int clamp(int n, int lo, int hi) { return Math.max(lo, Math.min(n,hi)); }
}
