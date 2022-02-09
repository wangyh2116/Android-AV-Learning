package com.wyh.audiorecordtest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static String TAG="MainActivity";
    private AudioRecorder mAudioRecorder=null;
    private AudioTraker mAudioTraker=null;
    private String[] permissions = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    //被拒绝的权限列表
    private List<String> mPermissionList = new ArrayList<>();
    private static final int MY_PERMISSIONS_REQUEST = 1001;
    private void checkPermissions() {
        // Marshmallow开始才用申请运行时权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                if (ContextCompat.checkSelfPermission(this, permissions[i]) !=
                        PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permissions[i]);
                }
            }
            if (!mPermissionList.isEmpty()) {
                String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);
                ActivityCompat.requestPermissions(this, permissions, MY_PERMISSIONS_REQUEST);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, permissions[i] + " 权限被用户禁止！");
                }
            }
            // 运行时权限的申请不是本demo的重点，所以不再做更多的处理，请同意权限申请。
        }
    }
    @BindView(R.id.start_record)
    public Button mStartRecordButton;
    @OnClick(R.id.start_record) public void onRecordStart(){
        if(mAudioRecorder==null){
            mAudioRecorder=new AudioRecorder(this);
            mAudioRecorder.setmRecorderListener(new AudioRecorder.RecorderListener() {
                @Override
                public void onStart() {
                    runOnUiThread(()->{
                        mStartRecordButton.setText("Stop Record");
                    });
                }

                @Override
                public void onConvert() {
                    runOnUiThread(()->{
                        mStartRecordButton.setText("Converting");
                        mStartRecordButton.setClickable(false);
                    });
                }

                @Override
                public void onEnd() {
                    runOnUiThread(()->{
                        mStartRecordButton.setText("Start Record");
                        mStartRecordButton.setClickable(true);
                    });
                }
            });
            mAudioRecorder.startRecord();


        }else{
            mAudioRecorder.setRecord(false);
            mAudioRecorder=null;
            mStartRecordButton.setText("Start Record");
        }
    }
    @BindView(R.id.start_music)
    public Button mStartMusicButton;
    @OnClick(R.id.start_music) public void onStartMusicStart(){
        if(mAudioTraker==null){
            mAudioTraker=new AudioTraker();
        }
        if(mStartMusicButton.getText()!="Stop Music"){
            mAudioTraker.playInModeStream(this);
            mStartMusicButton.setText("Stop Music");
        }else{
            mAudioTraker.stopPlay();
            mStartMusicButton.setText("Start Music");
        }
    }
    @OnClick(R.id.start_ding) public void onStartDingStart(){
        if(mAudioTraker==null){
            mAudioTraker=new AudioTraker();
        }
        mAudioTraker.playInModeStatic(this);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
        ButterKnife.bind(this);
    }


}