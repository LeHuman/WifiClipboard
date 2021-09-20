package com.lehuman.wificlipboard;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicBoolean;

public class SingleTapWidget extends AppWidgetProvider {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({READY, ERROR, WAITING, STANDBY, HIDDEN})
    @interface StatusColor {
    }

    private static final int READY = 0;
    private static final int ERROR = 1;
    private static final int WAITING = 2;
    private static final int STANDBY = 3;
    private static final int HIDDEN = 4;
    private static final int[] colors = {0, 0, 0, 0, 0};

    public static final String TCP_RECEIVE = "com.lehuman.wificlipboard.TCP_RECEIVE";
    public static final String TCP_DONE = "com.lehuman.wificlipboard.TCP_DONE";
    private static final StatusCoordinator statusCoordinator = new StatusCoordinator();

    public static int TCP_PORT = Settings.TCP_DEFAULT_PORT;
    public static int TCP_TIMEOUT = Settings.TCP_DEFAULT_TIMEOUT;
    public static boolean TCP_ENABLE_TOAST = Settings.TCP_DEFAULT_TOAST;

    private static TCPServer server;
    private static RemoteViews mWidgetView;
    private static int status = -1;
    private static int count = 0;

    public static void loadSettings(Context context) {
        TCP_PORT = Settings.getPORT(context);
        TCP_TIMEOUT = Settings.getTIMEOUT(context);
        TCP_ENABLE_TOAST = Settings.getTOAST(context);
    }

    public static void updateSettings(Context context) {
        updateWidgets(context);
        if (server != null) {
            server.stop();
        }
        newServer(context);
    }

    private static class StatusCoordinator extends Thread {

        private static class Status {
            final Context context;
            @StatusColor
            final int color;
            final int delay;
            Status nextStatus;

            public Status(Context context, @StatusColor int color, int delay) {
                this.context = context;
                this.color = color;
                this.delay = delay;
            }

            public Status(Context context, @StatusColor int color, int delay, Status nextStatus) {
                this(context, color, delay);
                this.nextStatus = nextStatus;
            }
        }

        private static final AtomicBoolean running = new AtomicBoolean();
        private static final Object lock = new Object();
        private Status nextStatus;
        private boolean run = true;

        @Override
        public void run() {
            synchronized (lock) {
                while (run) {
                    int lastColor = -1;
                    Context context;
                    try {
                        if (nextStatus == null)
                            lock.wait();
                        running.set(true);
                        do {
                            context = nextStatus.context;
                            if (nextStatus.delay != 0)
                                lock.wait(nextStatus.delay);
                            lastColor = nextStatus.color;
                            mWidgetView.setInt(R.id.ConnectionStatus, "setColorFilter", colors[lastColor]);
                            updateWidgets(context);
                        } while ((nextStatus = nextStatus.nextStatus) != null);
                    } catch (InterruptedException ignored) {
                        context = null;
                    } finally {
                        running.set(false);
                    }
                    if (context != null && (lastColor == ERROR || lastColor == READY))
                        setColor(context, STANDBY, 700);
                }
            }
        }

        @Override
        public synchronized void start() {
            run = true;
            nextStatus = null;
            running.set(false);
            super.start();
        }

        public void kill() {
            run = false;
            interrupt();
        }

        public void update() {
            if (!running.get())
                synchronized (lock) {
                    lock.notify();
                }
        }

        public void cancel() {
            if (running.get()) {
                interrupt();
            }
        }

        public void setColor(Context context, @StatusColor int color) {
            setColor(context, color, 0);
        }

        public void setColor(Context context, @StatusColor int color, int delay) {
            cancel();
            nextStatus = new Status(context, color, delay);
            update();
        }

        public void blink(Context context, @StatusColor int onColor, @StatusColor int offColor, int delay, int count) {
            cancel();
            count *= 2;
            nextStatus = new Status(context, onColor, delay);
            while (count > 0) {
                nextStatus = new Status(context, count % 2 == 1 ? onColor : offColor, delay, nextStatus);
                count--;
            }
            update();
        }
    }

    private static void updateWidgets(Context context) {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, SingleTapWidget.class);
        int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
        Intent update = new Intent(context, SingleTapWidget.class);
        update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        context.sendBroadcast(update);
    }

    private static void printStatus(Context context) {
        switch (status) {
            case 1:
                showToast(context, "Invalid response");
                statusCoordinator.setColor(context, ERROR);
                break;
            case -1:
                showToast(context, "Timeout / Disconnected");
                statusCoordinator.setColor(context, ERROR);
                statusCoordinator.blink(context, ERROR, HIDDEN, 50, 4);
                break;
            default:
                showToast(context, "Response copied to clipboard");
                statusCoordinator.setColor(context, READY);
                statusCoordinator.blink(context, READY, STANDBY, 100, 2);
                break;
        }
        status = -1;
    }

    private static void showToast(Context context, String string) {
        if (TCP_ENABLE_TOAST)
            Toast.makeText(context, string, Toast.LENGTH_SHORT).show();
    }

    private static void newServer(Context context) {
        server = new TCPServer(TCP_PORT);
        server.setListener(message -> {
            if (message == null) {
                status = -1;
            } else if (message.length() == 0) {
                status = 1;
            } else {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Wi-fi Copy", message);
                clipboard.setPrimaryClip(clip);
                status = 0;
            }
            server.stop();
            Intent intent = new Intent(context, SingleTapWidget.class);
            intent.setAction(TCP_DONE);
            context.sendBroadcast(intent);
        });
    }

    public static void receive(Context context) {
        if (server == null) {
            newServer(context);
        }
        if (server.running()) {
            count++;
            if (count > 1) {
                server.stop();
                statusCoordinator.setColor(context, ERROR);
                showToast(context, "Canceling");
                return;
            }
            showToast(context, "Already Running");
            statusCoordinator.blink(context, WAITING, ERROR, 100, 2);
            return;
        }
        count = 0;
        statusCoordinator.setColor(context, WAITING);
        server.start();
        server.setTimeout(context, TCP_TIMEOUT);
        server.receive();
        showToast(context, "Receiving");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        switch (action) {
            case TCP_RECEIVE:
                receive(context);
                break;
            case TCP_DONE:
                printStatus(context);
                break;
        }
    }

    private static void newWidgetView(Context context) {
        Intent intent = new Intent(context, SingleTapWidget.class);
        intent.setAction(TCP_RECEIVE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        mWidgetView = new RemoteViews(context.getPackageName(), R.layout.single_tap_widget_layout);
        mWidgetView.setOnClickPendingIntent(R.id.mainIcon, pendingIntent);
        mWidgetView.setImageViewResource(R.id.mainIcon, R.drawable.round_content_copy_24);
        mWidgetView.setImageViewResource(R.id.ConnectionStatus, R.drawable.ic_round_wifi_24_mod);

        colors[READY] = context.getColor(R.color.green);
        colors[ERROR] = context.getColor(R.color.red);
        colors[WAITING] = context.getColor(R.color.yellow);
        colors[STANDBY] = context.getColor(R.color.blue);
        colors[HIDDEN] = Color.TRANSPARENT;

        loadSettings(context);
        if (server == null) {
            newServer(context);
        }
    }

    private static RemoteViews getWidgetView(Context context) {
        if (mWidgetView == null) {
            newWidgetView(context);
        }
        return mWidgetView;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;
        if (N < 1)
            return;
        if (!statusCoordinator.isAlive())
            statusCoordinator.start();

        RemoteViews views = getWidgetView(context);
        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        loadSettings(context);
        updateSettings(context);
    }

    @Override
    public void onEnabled(Context context) { // TODO: Fix slow start on reboot
        if (!statusCoordinator.isAlive())
            statusCoordinator.start();
        newWidgetView(context);
        updateWidgets(context);
        statusCoordinator.setColor(context, ERROR);
    }

    @Override
    public void onDisabled(Context context) {
        statusCoordinator.kill();
    }

}