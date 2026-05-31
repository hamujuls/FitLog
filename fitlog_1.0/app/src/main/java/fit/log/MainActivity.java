package fit.log;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.JsResult;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.app.AlertDialog;
import android.view.Window;
import android.view.View;
import android.view.WindowInsets;
import android.os.Build;
import android.graphics.Color;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;
    private static final int CREATE_BACKUP_REQUEST_CODE = 1002;
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private String pendingBackupData;
    private String pendingBackupFilename;
    private boolean germanLocale;

    private String text(String german, String english) {
        return germanLocale ? german : english;
    }

    private boolean isGermanSystemLanguage() {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = getResources().getConfiguration().getLocales().size() > 0
                    ? getResources().getConfiguration().getLocales().get(0)
                    : Locale.getDefault();
        } else {
            locale = getResources().getConfiguration().locale;
        }
        return locale != null && "de".equalsIgnoreCase(locale.getLanguage());
    }

    public class BackupBridge {
        @JavascriptInterface
        public void saveBackup(String data, String filename) {
            runOnUiThread(() -> {
                String safeName = sanitizeFilename(filename == null || filename.trim().isEmpty()
                        ? text("trainingsplan_tracker_backup.json", "training_plan_tracker_backup.json")
                        : filename.trim());
                pendingBackupData = String.valueOf(data);
                pendingBackupFilename = safeName;
                try {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    intent.putExtra(Intent.EXTRA_TITLE, safeName);
                    startActivityForResult(intent, CREATE_BACKUP_REQUEST_CODE);
                } catch (Exception e) {
                    pendingBackupData = null;
                    pendingBackupFilename = null;
                    Toast.makeText(MainActivity.this, text("Speicherort-Auswahl konnte nicht geöffnet werden.", "Save location picker could not be opened."), Toast.LENGTH_LONG).show();
                }
            });
        }

        private String sanitizeFilename(String name) {
            String cleaned = name.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (!cleaned.toLowerCase().endsWith(".json")) cleaned += ".json";
            return cleaned;
        }
    }

    public class FeedbackBridge {
        @JavascriptInterface
        public void vibrate(int milliseconds) {
            runOnUiThread(() -> {
                try {
                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator == null) return;
                    int ms = Math.max(1, Math.min(milliseconds, 5000));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(ms);
                    }
                } catch (Exception ignored) { }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        germanLocale = isGermanSystemLanguage();
        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor("#000000"));
        window.setNavigationBarColor(Color.parseColor("#070a10"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Edge-to-edge: let the web layer handle insets via CSS env(safe-area-inset-*).
            window.setDecorFitsSystemWindows(false);
        }

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        webView.addJavascriptInterface(new BackupBridge(), "AndroidBackup");
        webView.addJavascriptInterface(new FeedbackBridge(), "AndroidFeedback");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton(text("Ja", "Yes"), (dialog, which) -> result.confirm())
                        .setNegativeButton(text("Nein", "No"), (dialog, which) -> result.cancel())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                } catch (Exception e) {
                    MainActivity.this.filePathCallback = null;
                    Toast.makeText(MainActivity.this, text("Dateiauswahl konnte nicht geöffnet werden.", "File picker could not be opened."), Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }
        });
        // Render fully edge-to-edge; the page insets itself via CSS env(safe-area-inset-*),
        // and a black overlay (body::before) covers the status-bar zone with a soft fade.
        webView.setFitsSystemWindows(false);
        webView.setBackgroundColor(Color.parseColor("#070a10"));
        webView.loadUrl(germanLocale ? "file:///android_asset/index.html" : "file:///android_asset/index_en.html");
        setContentView(webView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CREATE_BACKUP_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingBackupData != null) {
                try (OutputStream outputStream = getContentResolver().openOutputStream(data.getData())) {
                    if (outputStream == null) throw new Exception(text("Datei konnte nicht geöffnet werden.", "File could not be opened."));
                    outputStream.write(pendingBackupData.getBytes(StandardCharsets.UTF_8));
                    Toast.makeText(this, text("Backup gespeichert: ", "Backup saved: ") + (pendingBackupFilename == null ? text("Datei", "file") : pendingBackupFilename), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, text("Export fehlgeschlagen: ", "Export failed: ") + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            pendingBackupData = null;
            pendingBackupFilename = null;
            return;
        }
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK) {
                results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
