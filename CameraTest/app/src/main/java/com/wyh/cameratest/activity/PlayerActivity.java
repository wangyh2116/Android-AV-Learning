package com.wyh.cameratest.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;

import com.wyh.cameratest.AutoFitSurfaceView;
import com.wyh.cameratest.AutoFitTextureView;
import com.wyh.cameratest.CameraHelper;
import com.wyh.cameratest.R;
import com.wyh.cameratest.SimplePlayer;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PlayerActivity extends AppCompatActivity {
    private File mFile;
    @BindView(R.id.player_texture) public AutoFitSurfaceView playerSurfaceView;
    private Surface mSurface;
    private SimplePlayer mSimplePlayer;
    public static void openPlayerActivity(Context context, File file){
        Intent intent=new Intent(context,PlayerActivity.class);
        intent.putExtra("file",file);
        context.startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        Intent intent=getIntent();
        mFile=(File)intent.getSerializableExtra("file");
        ButterKnife.bind(this);
        playerSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback(){

            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                try {
                    mSimplePlayer=new SimplePlayer(playerSurfaceView,mFile.toString());
                    mSimplePlayer.startPlay();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                System.out.println("====================================================");
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mSimplePlayer!=null){
            mSimplePlayer.endPlay();
        }

    }
}