package com.fc.uploader;


import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity implements OnClickListener, UploadUtil.OnUploadProcessListener {
    private static final String TAG = "uploadImage";

    protected static final int TO_UPLOAD_FILE = 1;
    /**
     * 上传文件响应
     */
    protected static final int UPLOAD_FILE_DONE = 2;  //
    /**
     * 选择文件
     */
    public static final int TO_SELECT_PHOTO = 3;
    /**
     * 上传初始化
     */
    private static final int UPLOAD_INIT_PROCESS = 4;
    /**
     * 上传中
     */
    private static final int UPLOAD_IN_PROCESS = 5;


    protected static final int COPY_DOWNLOAD_URL = 6;
    /***
     * 这里的这个URL是我服务器的javaEE环境URL
     */
    private static String requestURL = "";//http://10.0.2.2:8080/upload";
    private Button  uploadButton;
    private Button copyDownloadUrlButton;
    private TextView uploadImageResult;
    private ProgressBar progressBar;

    private String picPath = null;
    private String dirPaths = "/storage/emulated/0/DCIM/Camera;/storage/emulated/0/tencent/MicroMsg/WeiXin";

    private EditText serverIpText;

    private int uploadSuccessCount = 0;
    private int uploadCallbackCount = 0;
    private int uploadTotalTime = 0;
    private int fileCount = 0;

    public static final String PREFS_NAME = "MyPrefsFile";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    /**
     * 初始化数据
     */
    private void initView() {
        uploadButton = (Button) this.findViewById(R.id.uploadImage);
        uploadButton.setOnClickListener(this);
        uploadButton.setFocusable(true);
        uploadButton.setFocusableInTouchMode(true);
        uploadButton.requestFocus();
        uploadButton.requestFocusFromTouch();
        copyDownloadUrlButton = (Button) this.findViewById(R.id.copyDownloadUrl);
        copyDownloadUrlButton.setOnClickListener(this);
        uploadImageResult = (TextView) findViewById(R.id.uploadImageResult);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        serverIpText =  (EditText) findViewById(R.id.serverIp);
        String serverIp = getServerIp();
        serverIpText.setText(serverIp);

    }

    public String getServerIp() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String serverIp = settings.getString("serverIp", "192.168.1.101");
        return serverIp;
    }

    private void setServerIp() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("serverIp", serverIpText.getText().toString());
        editor.commit();
    }

    @Override
    public void onClick(View v) {
     //   picPath = "/storage/emulated/0/DCIM/Camera/IMG_20171117_020954.jpg";
        switch (v.getId()) {
            case R.id.uploadImage:
                    handler.sendEmptyMessage(TO_UPLOAD_FILE);
                break;
            case R.id.copyDownloadUrl:
                handler.sendEmptyMessage(COPY_DOWNLOAD_URL);
                break;
            default:
                break;
        }
    }



    /**
     * 上传服务器响应回调
     */
    @Override
    public void onUploadDone(int responseCode, String message) {

        Message msg = Message.obtain();
        msg.what = UPLOAD_FILE_DONE;
        msg.arg1 = responseCode;
        msg.obj = message;
        handler.sendMessage(msg);
    }
    public boolean isGranted(String permission) {
        return !isMarshmallow() || isGranted_(permission);
    }
    private boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
    private boolean isGranted_(String permission) {
        int checkSelfPermission = checkSelfPermission(permission);
        return checkSelfPermission == PackageManager.PERMISSION_GRANTED;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1||requestCode == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doUpload();
            } else {
               // Toast.makeText(MainActivity.this, "您没有授权该权限，请在设置中打开授权", Toast.LENGTH_SHORT).show();
                uploadImageResult.setText("您没有授权该权限，请在设置中打开授权");
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    private void toUploadFile() {

        if(!isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)){
            if(shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)){
                uploadImageResult.setText("上传照片和视频需要读你本地存储权限");
            }
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return;
        }
        doUpload();

    }
    private void doUpload(){
       //int a =  progressBar.re
        UploadUtil uploadUtil = UploadUtil.getInstance();
        if(uploadButton.getText().toString().equals("上传")) {
            uploadButton.setText("停止");
        }
        else{
            uploadButton.setText("上传");
            uploadUtil.setStop();
            progressBar.setProgress(0);
            return;
        }
        setServerIp();
        String serverIp = serverIpText.getText().toString();
        requestURL = String.format("http://%s/upload",serverIp);
        uploadImageResult.setText("开始上传到" + requestURL  + "\n");
        //计算文件个数
        fileCount = 0;
        String []  dirPathsArray = dirPaths.split(";");
        for(String dirPath : dirPathsArray) {
            File dir = new File(dirPath);
            if (dir.exists() && dir.isDirectory())
                for (File f : dir.listFiles()) {
                    if (f.isDirectory())
                        continue;
                    fileCount ++;
                }
        }
        uploadSuccessCount = 0;
        uploadCallbackCount = 0;
        progressBar.setProgress(0);
        progressBar.setMax(fileCount);

        for(String dirPath : dirPathsArray) {
            File dir = new File(dirPath);
            if(dir.exists() && dir.isDirectory())
                for (File f : dir.listFiles()) {
                    if (f.isDirectory())
                        continue;
                    String fileKey = "uploadfile";                         ;
                    uploadUtil.setOnUploadProcessListener(this);  //设置监听器监听上传状态
                    Map<String, String> params = new HashMap<String, String>();
                    String desDir = f.getParent().substring(f.getParent().lastIndexOf("/")+1);
                    params.put("filePath", desDir);
                    params.put("uploadfile", "uploadfile");
                    uploadUtil.uploadFile(f.getAbsolutePath(), fileKey, requestURL, params);
                }
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TO_UPLOAD_FILE:
                    toUploadFile();
                    break;
                case UPLOAD_INIT_PROCESS:
                  //  progressBar.setMax(msg.arg1);
                    break;
                case UPLOAD_IN_PROCESS:
                 //   progressBar.setProgress(msg.arg1);
                    break;
                case UPLOAD_FILE_DONE:

                    uploadCallbackCount ++;
                  //  String result = "响应码：" + msg.arg1 + ",响应信息：" + msg.obj + ",耗时：" + UploadUtil.getRequestTime() + "秒";
                    String result =  msg.obj + ",耗时：" + UploadUtil.getRequestTime() + "秒\n";
                    if(msg.arg1 == UploadUtil.UPLOAD_SUCCESS_CODE) {
                        progressBar.incrementProgressBy(1);
                        uploadSuccessCount++;
                    }
                    else{
                        uploadImageResult.setText("1.确保电脑端upload.exe是否启动，没有启动双击即可。\nupload.exe下载地址为：https://iiccqq.github.io/Uploader/upload.exe\n2.检查网络或防火墙80端口开发");
                        uploadButton.setText("上传");
                    }
                    uploadTotalTime += UploadUtil.getRequestTime();
                    uploadImageResult.append(result);
                    if(uploadCallbackCount == fileCount) {
                        uploadImageResult.append("上传文件成功个数" + uploadSuccessCount + ",总耗时：" + uploadTotalTime + "秒\n");
                        uploadButton.setText("上传");
                    }
                    break;
                case COPY_DOWNLOAD_URL:
                    ClipboardManager clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("simple text", "https://iiccqq.github.io/Uploader/upload.exe");
                    clipboard.setPrimaryClip(clip);
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }

    };

    @Override
    public void onUploadProcess(int uploadSize) {
        Message msg = Message.obtain();
        msg.what = UPLOAD_IN_PROCESS;
        msg.arg1 = uploadSize;
        handler.sendMessage(msg);
    }

    @Override
    public void initUpload(int fileSize) {
        Message msg = Message.obtain();
        msg.what = UPLOAD_INIT_PROCESS;
        msg.arg1 = fileSize;
        handler.sendMessage(msg);
    }

}