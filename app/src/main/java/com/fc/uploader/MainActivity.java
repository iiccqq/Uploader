package com.fc.uploader;


import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
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


    protected static final int SET_SERVER_IP = 6;
    /***
     * 这里的这个URL是我服务器的javaEE环境URL
     */
    private static String requestURL = "";//http://10.0.2.2:8080/upload";
    private Button  uploadButton;
    private TextView uploadImageResult;
    private ProgressBar progressBar;

    private String picPath = null;
    private String dirPaths = "/storage/emulated/0/DCIM/Camera;/storage/emulated/0/tencent/MicroMsg/WeiXin";

    private EditText serverIpText;
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
        uploadImageResult = (TextView) findViewById(R.id.uploadImageResult);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        serverIpText =  (EditText) findViewById(R.id.serverIp);
    }

    @Override
    public void onClick(View v) {
     //   picPath = "/storage/emulated/0/DCIM/Camera/IMG_20171117_020954.jpg";
        switch (v.getId()) {
            case R.id.uploadImage:
                    handler.sendEmptyMessage(TO_UPLOAD_FILE);
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

    private void toUploadFile() {
        String serverIp = serverIpText.getText().toString();
        requestURL = String.format("http://%s:8080/upload",serverIp);
                uploadImageResult.setText("开始上传到" + requestURL);
        String []  dirPathsArray = dirPaths.split(";");
       for(String dirPath : dirPathsArray) {
            File dir = new File(dirPath);
           if(dir.exists() && dir.isDirectory())
            for (File f : dir.listFiles()) {
                if (f.isDirectory())
                    continue;
                String fileKey = "uploadfile";
                UploadUtil uploadUtil = UploadUtil.getInstance();
                ;
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
                    progressBar.setMax(msg.arg1);
                    break;
                case UPLOAD_IN_PROCESS:
                    progressBar.setProgress(msg.arg1);
                    break;
                case UPLOAD_FILE_DONE:
                    String result = "响应码：" + msg.arg1 + "\n响应信息：" + msg.obj + "\n耗时：" + UploadUtil.getRequestTime() + "秒\n";

                    uploadImageResult.append(result);
                    break;
                case SET_SERVER_IP:

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