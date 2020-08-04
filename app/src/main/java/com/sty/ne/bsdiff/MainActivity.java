package com.sty.ne.bsdiff;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.sty.ne.bsdiff.utils.PermissionUtils;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private String[] needPermissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final String APK_PATH = Environment.getExternalStorageDirectory() + "/sty/bsdiff/";
    private TextView tvVersion;
    private Button btnUpdate;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!PermissionUtils.checkPermissions(this, needPermissions)) {
            PermissionUtils.requestPermissions(this, needPermissions);
        }

        initView();
    }

    private void initView() {
        tvVersion = findViewById(R.id.tv_version);
        btnUpdate = findViewById(R.id.btn_update);

        tvVersion.setText("当前版本：" + BuildConfig.VERSION_NAME);
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnUpdateClicked();
            }
        });
    }

    private void onBtnUpdateClicked() {

        new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... voids) {
                //下载更新补丁包（省略）
                String patchPath = new File(APK_PATH, "patch.diff").getAbsolutePath();
                //合成：旧版本apk文件（当前运行的apk）+ 从服务器下载的补丁包文件 = 新版本的apk安装包文件
                String oldApkPath = getApplicationInfo().sourceDir;
                File newApk = new File(APK_PATH, "new.apk");
                Log.e("sty", newApk.getAbsolutePath());
                if(!newApk.exists()) {
                    try {
                        newApk.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                doPatchNative(oldApkPath, newApk.getAbsolutePath(), patchPath);
                return newApk;
            }

            @Override
            protected void onPostExecute(File newApk) {
                //安装
                if(!newApk.exists()) {
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if(Build.VERSION.SDK_INT >= 24) { //Android7.0以上
                    //参数2：清单文件中provider节点里面的authorities  参数3：共享的文件，即apk包的file类
                    Uri apkUri = FileProvider.getUriForFile(MainActivity.this,
                            getApplicationInfo().packageName + ".provider", newApk);
                    //对目标应用临时授权该URI所代表的文件
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                }else {
                    intent.setDataAndType(Uri.fromFile(newApk), "application/vnd.android.package-archive");
                }
                startActivity(intent);
            }
        }.execute();
    }

    private native void doPatchNative(String oldApkPath, String newApkPath, String patchPath);

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PermissionUtils.REQUEST_PERMISSIONS_CODE) {
            if (!PermissionUtils.verifyPermissions(grantResults)) {
                PermissionUtils.showMissingPermissionDialog(this);
            } else {

            }
        }
    }

}
