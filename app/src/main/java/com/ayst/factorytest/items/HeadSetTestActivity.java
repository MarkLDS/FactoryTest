package com.ayst.factorytest.items;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.ayst.factorytest.R;
import com.ayst.factorytest.base.ChildTestActivity;
import com.blankj.utilcode.util.VolumeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;

public class HeadSetTestActivity extends ChildTestActivity implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private static final String TAG = "HeadSetTestActivity";

    private MediaPlayer mMediaPlayer;

    @BindView(R.id.btnPlaySound)
    Button btnPlaySound;

    @BindView(R.id.btnRecordPlay)
    Button btnRecordPlay;

    private static final int SAMPLE_RATE_DEFAULT = 16000; // 16KHz

    private boolean isRecording = false;
    private int mRecordBufferSize;
    private AudioRecord mAudioRecord;
    private AudioTrack mAudioTrack;
    private String mRecordFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initRecord();
    }

    private void initView() {

        btnPlaySound = findViewById(R.id.btnPlaySound);
        btnPlaySound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isWiredHeadsetOn()) {
                    Toast.makeText(HeadSetTestActivity.this, getString(R.string.please_insert_headset), Toast.LENGTH_SHORT).show();
                    return;
                }
                playLeftAndRightAudioChannel();
            }
        });
        btnRecordPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isWiredHeadsetOn()) {
                    Toast.makeText(HeadSetTestActivity.this, getString(R.string.please_insert_headset), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isRecording) {
                    btnRecordPlay.setText(R.string.mic_test_record);
                    play();
                } else {
                    btnRecordPlay.setText(R.string.play_record);
                    record();
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void initRecord() {
        mRecordFilePath = ContextCompat.getExternalFilesDirs(this,
                Environment.DIRECTORY_MUSIC)[0].getAbsolutePath() + File.separator + "HeadSetRecord.pcm";
        mRecordBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_DEFAULT,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_DEFAULT,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, mRecordBufferSize);
        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE_DEFAULT,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE_DEFAULT,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize, AudioTrack.MODE_STREAM);
    }

    private boolean isWiredHeadsetOn() {
        AudioManager audioManager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        if (audioManager == null) {
            return true;
        }
        return !audioManager.isWiredHeadsetOn();
    }

    public void playLeftAndRightAudioChannel() {
        stop();
        AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.factory_test_left_right_sound);
        VolumeUtils.setVolume(AudioManager.STREAM_MUSIC,
                VolumeUtils.getMaxVolume(AudioManager.STREAM_MUSIC),
                AudioManager.FLAG_SHOW_UI);
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public void onPrepared(MediaPlayer mp) {
        mMediaPlayer.start();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mMediaPlayer.start();
    }

    @Override
    public int getContentLayout() {
        return R.layout.content_head_set_test;
    }

    @Override
    public int getFullscreenLayout() {
        return 0;
    }

    @Override
    protected void onStop() {
        stop();
        super.onStop();
    }

    private void record() {
        isRecording = true;
        mAudioTrack.stop();
        mAudioRecord.startRecording();
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(mRecordFilePath);
                if (file.exists()) {
                    file.delete();
                }

                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(mRecordFilePath);
                    byte[] buffer = new byte[mRecordBufferSize];
                    while (mAudioRecord.read(buffer, 0, mRecordBufferSize) > 0) {
                        Log.d(TAG, "AudioRecord read: " + buffer.length);
                        fos.write(buffer);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private void play() {
        isRecording = false;
        mAudioRecord.stop();
        VolumeUtils.setVolume(AudioManager.STREAM_MUSIC,
                VolumeUtils.getMaxVolume(AudioManager.STREAM_MUSIC),
                AudioManager.FLAG_SHOW_UI);
        mAudioTrack.play();

        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(mRecordFilePath);
                if (!file.exists()) {
                    Log.e(TAG, "play, " + mRecordFilePath + " not exist");
                    return;
                }

                long fileSize = file.length();
                if (fileSize > Integer.MAX_VALUE) {
                    Log.w(TAG, "play, file too big");
                    fileSize = Integer.MAX_VALUE;
                }

                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(mRecordFilePath);

                    int size;
                    byte[] buffer = new byte[mRecordBufferSize];
                    while ((size = fis.read(buffer, 0, mRecordBufferSize)) >= 0) {
                        Log.d(TAG, "AudioTrack write: " + size + "/" + fileSize);
                        mAudioTrack.write(buffer.clone(), 0, size);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        mAudioTrack.stop();
        mAudioTrack.release();

        mAudioRecord.stop();
        mAudioRecord.release();

        File file = new File(mRecordFilePath);
        if (file.exists()) {
            file.delete();
        }
        super.onDestroy();
    }

}