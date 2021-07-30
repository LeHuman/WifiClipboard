package com.lehuman.wificlipboard;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

public class SingleTapWidget extends AppWidgetProvider {

    public static final String TCP_RECEIVE = "com.lehuman.wificlipboard.TCP_RECEIVE";
    public static final String TCP_DONE = "com.lehuman.wificlipboard.TCP_DONE";
    public static final String TCP_STATUS = "com.lehuman.wificlipboard.TCP_STATUS";

    public static int TCP_PORT = Settings.TCP_DEFAULT_PORT;
    public static int TCP_TIMEOUT = Settings.TCP_DEFAULT_TIMEOUT;
    public static boolean TCP_ENABLE_TOAST = Settings.TCP_DEFAULT_TOAST;

    public static void reloadSettings(Context context) {
        TCP_PORT = Settings.getPORT(context);
        TCP_TIMEOUT = Settings.getTIMEOUT(context);
        TCP_ENABLE_TOAST = Settings.getTOAST(context);
        updateWidgets(context);
        if (server != null) {
            server.stop();
        }
        newServer(context);
    }

    private static Handler delayedIntent;
    private static TCPServer server;
    private static RemoteViews mWidgetView;
    private static int status = -1;
    private static int count = 0;

    public enum IconState {
        READY,
        WAITING,
        ERROR,
        STANDBY,
        HIDDEN
    }

    private static void clearStandbyQueue() {
        delayedIntent.removeCallbacksAndMessages(1);
    }

    private static void queueStandby(Context context) {
        clearStandbyQueue();
        delayedIntent.postDelayed(() -> {
            Intent intent = new Intent(context, SingleTapWidget.class);
            intent.putExtra("STATE", IconState.STANDBY);
            intent.setAction(TCP_STATUS);
            context.sendBroadcast(intent);
        }, 1, 1500);
    }

    private static void blink(Context context, IconState active, IconState blinked) { // TODO: not this
        delayedIntent.removeCallbacksAndMessages(null);
        setIconState(context, blinked);
        delayedIntent.postDelayed(() -> {
            Intent intent = new Intent(context, SingleTapWidget.class);
            intent.putExtra("STATE", active);
            intent.setAction(TCP_STATUS);
            context.sendBroadcast(intent);
        },  100);
        delayedIntent.postDelayed(() -> {
            Intent intent = new Intent(context, SingleTapWidget.class);
            intent.putExtra("STATE", blinked);
            intent.setAction(TCP_STATUS);
            context.sendBroadcast(intent);
        },  200);
        delayedIntent.postDelayed(() -> {
            Intent intent = new Intent(context, SingleTapWidget.class);
            intent.putExtra("STATE", active);
            intent.setAction(TCP_STATUS);
            context.sendBroadcast(intent);
        },  300);
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

    private static void setIconState(Context context, IconState state) {
        if (mWidgetView == null)
            getWidgetView(context);

        switch (state) {
            case READY:
                mWidgetView.setViewVisibility(R.id.ConnectionReady, View.VISIBLE);
                mWidgetView.setViewVisibility(R.id.ConnectionStandby, View.INVISIBLE);
                mWidgetView.setViewVisibility(R.id.ConnectionWaiting, View.INVISIBLE);
                mWidgetView.setViewVisibility(R.id.ConnectionError, View.INVISIBLE);
                queueStandby(context);
                break;
            case WAITING:
                mWidgetView.setViewVisibility(R.id.ConnectionWaiting, View.VISIBLE);
                mWidgetView.setViewVisibility(R.id.ConnectionStandby, View.INVISIBLE);
                mWidgetView.setViewVisibility(R.id.ConnectionReady, View.INVISIBLE);
                mWidgetView.setViewVisibility(R.id.ConnectionError, View.INVISIBLE);
                clearStandbyQueue();
                break;
            case ERROR:
                mWidgetView.setViewVisibility(R.id.ConnectionError, View.VISIBLE);
                mWidgetView.setViewVisibility(R.id.ConnectionStandby, View.INVISIBLE);
                mWidgetView.setViewVisibility(R.id.ConnectionReady, View.INVISIBLE);
                mWidgetView.setViewVisibility(R.id.ConnectionWaiting, View.INVISIBLE);
                queueStandby(context);
                break;
            case HIDDEN:
                mWidgetView.setViewVisibility(R.id.ConnectionError, View.INVISIBLE);
                mWidgetView.setViewVisibility(R.id.ConnectionStandby, View.INVISIBLE);
                mWidgetView.setViewVisibility(R.id.ConnectionReady, View.INVISIBLE);
                mWidgetView.setViewVisibility(R.id.ConnectionWaiting, View.INVISIBLE);
                break;
            case STANDBY:
            default:
                mWidgetView.setViewVisibility(R.id.ConnectionStandby, View.VISIBLE);
                mWidgetView.setViewVisibility(R.id.ConnectionReady, View.INVISIBLE);
                mWidgetView.setViewVisibility(R.id.ConnectionWaiting, View.INVISIBLE);
                mWidgetView.setViewVisibility(R.id.ConnectionError, View.INVISIBLE);
                break;
        }
        updateWidgets(context);
    }

    private static void printStatus(Context context) {
        switch (status) {
            case 1:
                showToast(context, "Invalid response");
                setIconState(context, IconState.ERROR);
                break;
            case -1:
                showToast(context, "Timeout / Disconnected");
                setIconState(context, IconState.ERROR);
                blink(context, IconState.ERROR, IconState.HIDDEN);
                break;
            default:
                showToast(context, "Response copied to clipboard");
                setIconState(context, IconState.READY);
                blink(context, IconState.READY, IconState.STANDBY);
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
        delayedIntent = new Handler(context.getMainLooper());
        server.setListener(message -> {
            if (message == null) {
                status = -1;
            } else if (message.length() == 0) {
                status = 1;
            } else {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("WifiCopy", message);
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
                delayedIntent.removeCallbacksAndMessages(null);
                setIconState(context, IconState.ERROR);
                showToast(context, "Canceling");
                return;
            }
            showToast(context, "Already Running");
            blink(context, IconState.WAITING, IconState.ERROR);
            return;
        }
        count = 0;
        setIconState(context, IconState.WAITING);
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
            case TCP_STATUS:
                setIconState(context, (IconState) intent.getSerializableExtra("STATE"));
                break;
        }
    }

    private static RemoteViews getWidgetView(Context context) {
        if (mWidgetView == null) {
            reloadSettings(context);
            Intent intent = new Intent(context, SingleTapWidget.class);
            intent.setAction(TCP_RECEIVE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            mWidgetView = new RemoteViews(context.getPackageName(), R.layout.single_tap_widget_layout);
            mWidgetView.setOnClickPendingIntent(R.id.mainIcon, pendingIntent);
            mWidgetView.setImageViewResource(R.id.mainIcon, R.drawable.round_content_copy_24);
            mWidgetView.setImageViewResource(R.id.ConnectionStandby, R.drawable.ic_round_wifi_24_mod); // TODO: find way to change tint instead
            mWidgetView.setImageViewResource(R.id.ConnectionReady, R.drawable.ic_round_wifi_24_mod);
            mWidgetView.setImageViewResource(R.id.ConnectionWaiting, R.drawable.ic_round_wifi_24_mod);
            mWidgetView.setImageViewResource(R.id.ConnectionError, R.drawable.ic_round_wifi_24_mod);
        }
        return mWidgetView;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (appWidgetIds.length < 1)
            return;
        appWidgetManager.updateAppWidget(appWidgetIds, getWidgetView(context));
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        reloadSettings(context);
    }
}