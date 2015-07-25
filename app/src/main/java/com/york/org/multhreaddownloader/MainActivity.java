package com.york.org.multhreaddownloader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.york.org.multhreaddownloader.network.DownloadProgressListener;
import com.york.org.multhreaddownloader.network.FileDownloader;
import com.york.org.multhreaddownloader.util.StreamTool;
import com.york.org.multhreaddownloader.util.UploadLogService;
import com.york.org.multhreaddownloader.util.NetStateUtil;

import java.io.File;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends Activity {

    private EditText downloadpathText;
    private TextView resultView;
    private ProgressBar progressBar;
    private Toast mToast = null;
    private ExecutorService executorServiceUpload = null;
    File uploadFile;

    /**
     * 当Handler被创建会关联到创建它的当前线程的消息队列，该类用于往消息队列发送消息
     * 消息队列中的消息由当前线程内部进行处理
     */
    private Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    progressBar.setProgress(msg.getData().getInt("size"));
                    float num = (float)progressBar.getProgress()/(float)progressBar.getMax();
                    int result = (int)(num*100);
                    resultView.setText(result+ "%");

                    if(progressBar.getProgress()==progressBar.getMax()){
                        Toast.makeText(MainActivity.this, R.string.success, Toast.LENGTH_LONG).show();
                    }
                    break;
                case -1:
                    Toast.makeText(MainActivity.this, R.string.error, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        downloadpathText = (EditText) this.findViewById(R.id.path);
        progressBar = (ProgressBar) this.findViewById(R.id.downloadbar);
        resultView = (TextView) this.findViewById(R.id.resultView);
        Button button = (Button) this.findViewById(R.id.button);
        Button upload = (Button) this.findViewById(R.id.btn_upload);

        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                String path = downloadpathText.getText().toString();
                System.out.println(Environment.getExternalStorageState()+"------"+Environment.MEDIA_MOUNTED);

                if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                    if(NetStateUtil.isNetworkAvailable(MainActivity.this)){
                        download(path, Environment.getExternalStorageDirectory());
                    }else{
                        if(mToast == null){
                            mToast=Toast.makeText(MainActivity.this, R.string.networkerror, Toast.LENGTH_SHORT);
                        }
                        mToast.show();
                    }

                }else{
                    Toast.makeText(MainActivity.this, R.string.sdcarderror, Toast.LENGTH_LONG).show();
                }
            }
        });
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String filename = "weixin622android580.apk";
                if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                    uploadFile = new File(Environment.getExternalStorageDirectory().toString(), filename);
                    if(uploadFile.exists()){
                        uploadFile(uploadFile);
                    }else{
                        System.out.println("file not exit");
                        Toast.makeText(MainActivity.this,"文件不存在",Toast.LENGTH_LONG).show();
                    }
                }else{
                    System.out.println("SDcard error");
                }
            }
        });
    }

    /**
     * 主线程(UI线程)
     * 对于显示控件的界面更新只是由UI线程负责，如果是在非UI线程更新控件的属性值，更新后的显示界面不会反映到屏幕上
     * @param path
     * @param savedir
     */
    private void download(final String path, final File savedir) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileDownloader loader = new FileDownloader(MainActivity.this, path, savedir, 3);
                progressBar.setMax(loader.getFileSize());//设置进度条的最大刻度为文件的长度

                try {
                    loader.download(new DownloadProgressListener() {
                        @Override
                        public void onDownloadSize(int size) {//实时获知文件已经下载的数据长度
                            Message msg = new Message();
                            msg.what = 1;
                            msg.getData().putInt("size", size);
                            handler.sendMessage(msg);//发送消息
                        }
                    });
                } catch (Exception e) {
                    handler.obtainMessage(-1).sendToTarget();
                }
            }
        }).start();
    }
    /**
     * 文件上传到指定IP,需要服务端配合
     * @param uploadFile
     */
    private void uploadFile(final File uploadFile) {
        if(executorServiceUpload!=null){
            executorServiceUpload.shutdownNow();
            executorServiceUpload = null;
        }
        executorServiceUpload = Executors.newFixedThreadPool(1);

        executorServiceUpload.execute(new Runnable() {

            public void run() {
                try {
                    UploadLogService logService = new UploadLogService(MainActivity.this);
                    String souceid = logService.getBindId(uploadFile);
                    String head = "Content-Length=" + uploadFile.length() + ";filename=" + uploadFile.getName() + ";sourceid=" +
                            (souceid == null ? "" : souceid) + "\r\n";
                    Socket socket = new Socket("172.16.19.25", 7878);
                    OutputStream outStream = socket.getOutputStream();
                    outStream.write(head.getBytes());

                    PushbackInputStream inStream = new PushbackInputStream(socket.getInputStream());
                    String response = StreamTool.readLine(inStream);
                    String[] items = response.split(";");
                    String responseid = items[0].substring(items[0].indexOf("=") + 1);
                    String position = items[1].substring(items[1].indexOf("=") + 1);
                    if (souceid == null) {//代表原来没有上传过此文件，往数据库添加一条绑定记录
                        logService.save(responseid, uploadFile);
                    }
                    RandomAccessFile fileOutStream = new RandomAccessFile(uploadFile, "r");
                    fileOutStream.seek(Integer.valueOf(position));
                    byte[] buffer = new byte[1024];
                    int len = -1;
                    int length = Integer.valueOf(position);
                    while ((len = fileOutStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, len);
                        length += len;
                        Message msg = new Message();
                        msg.getData().putInt("size", length);

                    }
                    fileOutStream.close();
                    outStream.close();
                    inStream.close();
                    socket.close();
                    if (length == uploadFile.length()) logService.delete(uploadFile);
//                    if (uploadFile != null) {
//                        if (uploadFile.exists()) {
//                            uploadFile.delete();
//                        }
//                    }
                    Toast.makeText(MainActivity.this,"上传完成",Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }
}

