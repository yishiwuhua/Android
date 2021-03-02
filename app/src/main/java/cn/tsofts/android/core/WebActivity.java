package cn.tsofts.android.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.tencent.smtt.export.external.interfaces.JsResult;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.CookieSyncManager;
import com.tencent.smtt.sdk.DownloadListener;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.io.File;
import java.net.URL;

import cn.tsofts.android.core.utils.PictureUtil;
import cn.tsofts.android.core.utils.WebViewJavaScriptFunction;
import cn.tsofts.android.core.utils.X5WebView;

public class WebActivity extends Activity {
    private int FILE_CHOOSER_RESULT_CODE = 1, CAMERA_RESULT_CODE = 2;

    private SharedPreferences sp;

    private ViewGroup mViewParent;
    protected X5WebView mWebView;
    private TextView webTitle;
    private ProgressBar loadingProgressBar = null;
    private ValueCallback<Uri> uploadFile;
    private ValueCallback<Uri[]> uploadFiles;
    private File cameraFile;
    private URL mIntentUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);//状态栏透明
        //设置状态栏颜色
        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        sp = getPreferences(WebActivity.MODE_PRIVATE);
        //接受传入的地址
        Intent intent = getIntent();
        if (intent != null) {
            try {
                mIntentUrl = new URL(intent.getStringExtra("URL"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {//使用硬件加速
            getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mViewParent = findViewById(R.id.webView);
        webTitle = findViewById(R.id.webTitle);
        init();
    }

    private void init() {
        mWebView = new X5WebView(this, null);
        mViewParent.addView(mWebView);
        //初始化进度条
        loadingProgressBar = findViewById(R.id.progressBar);
        loadingProgressBar.setMax(100);
        loadingProgressBar.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.color_progressbar));

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //获取cookies
                CookieManager cm = CookieManager.getInstance();
                String cookies = cm.getCookie(url);
                sp.edit().putString("JSESSIONID", cookies).apply();
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //若存在前进后退按钮，可以在此控制其状态
                super.onPageFinished(view, url);
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView arg0, String arg1, String arg2, JsResult arg3) {
                return super.onJsConfirm(arg0, arg1, arg2, arg3);
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onReceivedTitle(WebView webView, String titles) {
                String title = mWebView.getTitle();
                webTitle.setText(" " + title);//设置显示标题
                super.onReceivedTitle(webView, titles);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {//设置进度条
                    loadingProgressBar.setProgress(0);//显示默认背景色
                } else {
                    loadingProgressBar.setProgress(newProgress);//设置进度值
                }
            }

            // file upload For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                WebActivity.this.uploadFile = uploadFile;
                openFileChooseProcess();
            }

            // file upload For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                WebActivity.this.uploadFile = uploadFile;
                openFileChooseProcess();
            }

            // file upload For Android  > 4.1.1
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                WebActivity.this.uploadFile = uploadFile;
                openFileChooseProcess();
            }

            // file upload For Android  >= 5.0
            public boolean onShowFileChooser(com.tencent.smtt.sdk.WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                WebActivity.this.uploadFiles = filePathCallback;
                openFileChooseProcess();
                return true;
            }
        });

        mWebView.addJavascriptInterface(new WebViewJavaScriptFunction() {
            @Override
            public void onJsFunctionCalled(String tag) {
            }

            @JavascriptInterface
            public void onTest() {
                Toast.makeText(WebActivity.this, "called Test Function", Toast.LENGTH_SHORT).show();
            }
        }, "Android");

        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                String JSESSIONID = sp.getString("JSESSIONID", "");
                //调用浏览器下载
//                Uri uri = Uri.parse(url);
//                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//                startActivity(intent);

                //使用系统的下载服务下载
                //创建request对象
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.addRequestHeader("Cookie", JSESSIONID);
                request.addRequestHeader("Cookie", "JSESSIONID=" + JSESSIONID);
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                // 设置通知的显示类型，下载进行时和完成后显示通知
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                //设置什么网络情况下可以下载
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
                // 允许媒体扫描，根据下载的文件类型被加入相册、音乐等媒体库
                request.allowScanningByMediaScanner();
                // 设置通知栏的标题，如果不设置，默认使用文件名
                request.setTitle(fileName);
                //设置通知栏的message
                request.setDescription(fileName + " 正在下载...");
                //设置漫游状态下是否可以下载
                request.setAllowedOverRoaming(false);
                //设置文件存放目录
                request.setDestinationInExternalFilesDir(WebActivity.this, Environment.DIRECTORY_DOWNLOADS, fileName);
                //获取系统服务
                DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                //进行下载
                if (downloadManager != null) {
                    downloadManager.enqueue(request);
                }
            }
        });

        WebSettings webSetting = mWebView.getSettings();
        webSetting.setAllowFileAccess(true);
        webSetting.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        webSetting.setSupportZoom(true);
        webSetting.setBuiltInZoomControls(true);
        webSetting.setUseWideViewPort(true);
        webSetting.setSupportMultipleWindows(false);
        // webSetting.setLoadWithOverviewMode(true);
        webSetting.setAppCacheEnabled(true);
        // webSetting.setDatabaseEnabled(true);
        webSetting.setDomStorageEnabled(true);
        webSetting.setJavaScriptEnabled(true);
        webSetting.setGeolocationEnabled(true);
        webSetting.setAppCacheMaxSize(Long.MAX_VALUE);
        webSetting.setAppCachePath(this.getDir("appcache", 0).getPath());
        webSetting.setDatabasePath(this.getDir("databases", 0).getPath());
        webSetting.setGeolocationDatabasePath(this.getDir("geolocation", 0)
                .getPath());
        // webSetting.setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY);
        webSetting.setPluginState(WebSettings.PluginState.ON_DEMAND);
        // webSetting.setRenderPriority(WebSettings.RenderPriority.HIGH);
        // webSetting.setPreFectch(true);
        mWebView.loadUrl(mIntentUrl.toString());
        CookieSyncManager.createInstance(this);
        CookieSyncManager.getInstance().sync();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mWebView != null && mWebView.canGoBack()) {
                mWebView.goBack();//返回按钮触发网页后退操作
                return true;
            } else
                return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (mWebView != null) {
            mWebView.destroy();
        }
        super.onDestroy();
    }

    private void openFileChooseProcess() {
        String[] selectPicTypeStr = {"相机", "文件/图片"};
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
                .setItems(selectPicTypeStr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // 相机拍摄
                                openCamera();
                                break;
                            case 1:// 手机相册
                                openFileChooser();
                                break;
                            default:
                                break;
                        }
                    }
                });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (uploadFiles != null) {
                    Uri[] uris = new Uri[1];
                    uris[0] = Uri.parse("");
                    uploadFiles.onReceiveValue(uris);
                    uploadFiles = null;
                }
            }
        });
        alertDialog.show();
    }

    /**
     * 打开文件选择器
     */
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "选择文件"), FILE_CHOOSER_RESULT_CODE);
    }

    /**
     * 打开相机
     */
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 必须确保文件夹路径存在，否则拍照后无法完成回调
        cameraFile = new File(this.getExternalFilesDir(Environment.DIRECTORY_DCIM + File.separator + "Tsofts"), System.currentTimeMillis() + ".jpg");
        if (!cameraFile.exists()) {
            File vDirPath = cameraFile.getParentFile();
            assert vDirPath != null;
            vDirPath.mkdirs();
        } else {
            if (cameraFile.exists()) {
                cameraFile.delete();
            }
        }
        //cameraUri = Uri.fromFile(vFile);
        Uri cameraUri = FileProvider.getUriForFile(this, "cn.tsofts.standard.provider", cameraFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, CAMERA_RESULT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //选择文件上传-响应
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (resultCode == WebActivity.RESULT_OK) {
                if (uploadFiles == null) {
                    return;
                }
                Uri[] uris = null;
                if (data != null) {
                    String dataString = data.getDataString();
                    ClipData clipData = data.getClipData();
                    if (clipData != null) {
                        uris = new Uri[clipData.getItemCount()];
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            uris[i] = item.getUri();
                        }
                    }
                    if (dataString != null)
                        uris = new Uri[]{Uri.parse(dataString)};
                }
                uploadFiles.onReceiveValue(uris);
                uploadFiles = null;
            } else if (resultCode == RESULT_CANCELED) {
                if (null != uploadFiles) {
                    uploadFiles.onReceiveValue(null);
                    uploadFiles = null;
                }
            }
        } else if (requestCode == CAMERA_RESULT_CODE) {
            if (uploadFiles == null) {
                return;
            }
            String path = cameraFile.getAbsolutePath();
            //获取图片旋转角度
            int rotate = PictureUtil.getPictureRotate(path);
            //压缩图片
            Bitmap bitmap = PictureUtil.getCompressPicure(path, 4);
            //旋转图片
            bitmap = PictureUtil.rotatePicture(rotate, bitmap);
            PictureUtil.saveBitmap(bitmap, cameraFile);
            Uri[] uris = new Uri[1];
            uris[0] = FileProvider.getUriForFile(this, "cn.tsofts.standard.provider", cameraFile);
            uploadFiles.onReceiveValue(uris);
            uploadFiles = null;
        }
    }
}
