package com.strongstep.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

  private static final String CHANNEL_ID = "strongstep_run";
  private static final int    NOTIF_ID   = 1001;
  private static final int    PERM_REQ   = 2001;

  private NotificationManager notifManager;

  public class NotifBridge {

    @JavascriptInterface
    public boolean hasPermission() {
      if (Build.VERSION.SDK_INT < 33) return true;
      return ContextCompat.checkSelfPermission(
        MainActivity.this, Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED;
    }

    @JavascriptInterface
    public void requestPermission() {
      if (Build.VERSION.SDK_INT >= 33) {
        ActivityCompat.requestPermissions(
          MainActivity.this,
          new String[]{ Manifest.permission.POST_NOTIFICATIONS },
          PERM_REQ
        );
      }
    }

    @JavascriptInterface
    public void showNotification(String title, String body) {
      if (!hasPermission()) return;
      NotificationCompat.Builder builder = new NotificationCompat.Builder(
        MainActivity.this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_media_play)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setSilent(true)
        .setOnlyAlertOnce(true);
      notifManager.notify(NOTIF_ID, builder.build());
    }

    @JavascriptInterface
    public void cancelNotification() {
      notifManager.cancel(NOTIF_ID);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 26) {
      NotificationChannel channel = new NotificationChannel(
        CHANNEL_ID, "StrongStep - Carrera",
        NotificationManager.IMPORTANCE_LOW
      );
      channel.setDescription("Metricas en tiempo real mientras corres");
      channel.setSound(null, null);
      notifManager.createNotificationChannel(channel);
    }

    WebView webView = getBridge().getWebView();
    WebSettings settings = webView.getSettings();
    settings.setMediaPlaybackRequiresUserGesture(false);
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);

    webView.addJavascriptInterface(new NotifBridge(), "AndroidNotif");

    if (Build.VERSION.SDK_INT >= 33) {
      if (ContextCompat.checkSelfPermission(this,
          Manifest.permission.POST_NOTIFICATIONS)
          != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this,
          new String[]{ Manifest.permission.POST_NOTIFICATIONS }, PERM_REQ);
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
      String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERM_REQ) {
      boolean granted = grantResults.length > 0
        && grantResults[0] == PackageManager.PERMISSION_GRANTED;
      String js = "if(window._onNotifPermission) window._onNotifPermission(" + granted + ")";
      getBridge().getWebView().post(() ->
        getBridge().getWebView().evaluateJavascript(js, null)
      );
    }
  }
}
