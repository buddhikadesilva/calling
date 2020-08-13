package io.agora.openduo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;

import io.agora.openduo.Constants;
import io.agora.openduo.R;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.ss.Constant;
import io.agora.rtc.ss.ScreenSharingClient;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;
import io.agora.rtm.RemoteInvitation;

public class VideoActivity extends BaseCallActivity {
    private static final String TAG = VideoActivity.class.getSimpleName();

    private FrameLayout mLocalPreviewLayout;
    private FrameLayout mRemotePreviewLayout;
    private AppCompatImageView mMuteBtn;
    private String mChannel;
    private int mPeerUid;


    private RtcEngine mRtcEngine;
    private FrameLayout mFlCam;
  //  private FrameLayout mFlSS;
    private boolean mSS = false;
    private VideoEncoderConfiguration mVEC;
    private ScreenSharingClient mSSClient;

    private final ScreenSharingClient.IStateListener mListener = new ScreenSharingClient.IStateListener() {
        @Override
        public void onError(int error) {
            Log.e(TAG, "Screen share service error happened: " + error);
        }

        @Override
        public void onTokenWillExpire() {
            Log.d(TAG, "Screen share service token will expire");
            mSSClient.renewToken(null); // Replace the token with your valid token
        }
    };

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onUserOffline(int uid, int reason) {
            Log.d(TAG, "onUserOffline: " + uid + " reason: " + reason);
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.d(TAG, "onJoinChannelSuccess: " + channel + " " + elapsed);
        }

        @Override
        public void onUserJoined(final int uid, int elapsed) {
            Log.d(TAG, "onUserJoined: " + (uid&0xFFFFFFL));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(uid == Constants.SCREEN_SHARE_UID) {
                        setupRemoteView(uid);
                    }
                }
            });
        }
    };

    /////

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        initUI();

        mSSClient = ScreenSharingClient.getInstance();
        mSSClient.setListener(mListener);

        initVideo();
    }

    private void initUI() {
        mLocalPreviewLayout = findViewById(R.id.local_preview_layout);
        mRemotePreviewLayout = findViewById(R.id.remote_preview_layout);

        mMuteBtn = findViewById(R.id.btn_mute);
        mMuteBtn.setActivated(true);
    }

    @Override
    protected void onGlobalLayoutCompleted() {
        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) mLocalPreviewLayout.getLayoutParams();
        params.topMargin += statusBarHeight;
        mLocalPreviewLayout.setLayoutParams(params);

        RelativeLayout buttonLayout = findViewById(R.id.button_layout);
        params = (RelativeLayout.LayoutParams) buttonLayout.getLayoutParams();
        params.bottomMargin = displayMetrics.heightPixels / 8;
        params.leftMargin = displayMetrics.widthPixels / 6;
        params.rightMargin = displayMetrics.widthPixels / 6;
        buttonLayout.setLayoutParams(params);
    }
/////////////////////////////////////////////////////////////////////

    private void initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getApplicationContext(), getString(R.string.private_app_id), mRtcEventHandler);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));

            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void setupVideoProfile() {
        mRtcEngine.setChannelProfile(io.agora.rtc.Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        mRtcEngine.enableVideo();
        mVEC = new VideoEncoderConfiguration(VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT);
        mRtcEngine.setVideoEncoderConfiguration(mVEC);
        mRtcEngine.setClientRole(io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER);
    }

    private void setupLocalVideo() {
        SurfaceView camV = RtcEngine.CreateRendererView(getApplicationContext());
        camV.setZOrderOnTop(true);
        camV.setZOrderMediaOverlay(true);
        mFlCam.addView(camV, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mRtcEngine.setupLocalVideo(new VideoCanvas(camV, VideoCanvas.RENDER_MODE_FIT, Constants.CAMERA_UID));
        mRtcEngine.enableLocalVideo(false);
    }

    private void initAgoraEngineAndJoinChannel() {
   //     initializeAgoraEngine();
   //     setupVideoProfile();
    //    setupLocalVideo();
     //   joinChannel();
    }

    private void setupRemoteView(int uid) { //importent
        SurfaceView ssV = RtcEngine.CreateRendererView(getApplicationContext());
        ssV.setZOrderOnTop(true);
        ssV.setZOrderMediaOverlay(true);
        mRemotePreviewLayout.addView(ssV, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mRtcEngine.setupRemoteVideo(new VideoCanvas(ssV, VideoCanvas.RENDER_MODE_FIT, uid));
    }

////////////////////////////////////////////////////////////////
    private void initVideo() {
        Intent intent  = getIntent();
        mChannel = intent.getStringExtra(Constants.KEY_CALLING_CHANNEL);
        try {
            mPeerUid = Integer.valueOf(intent.getStringExtra(Constants.KEY_CALLING_PEER));
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.message_wrong_number,
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        rtcEngine().setClientRole(io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER);
        setVideoConfiguration();
        setupLocalPreview();
        joinRtcChannel(mChannel, "", Integer.parseInt(config().getUserId()));
    }

    private void setupLocalPreview() {
        SurfaceView surfaceView = setupVideo(Integer.parseInt(config().getUserId()), true);
        surfaceView.setZOrderOnTop(true);
        mLocalPreviewLayout.addView(surfaceView);
    }

    @Override
    public void onUserJoined(final int uid, int elapsed) {
        if (uid != mPeerUid) return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRemotePreviewLayout.getChildCount() == 0) {
                    SurfaceView surfaceView = setupVideo(uid, false);
                    mRemotePreviewLayout.addView(surfaceView);
                }
            }
        });
    }

    @Override
    public void onUserOffline(int uid, int reason) {
        if (uid != mPeerUid) return;
        finish();
    }

    public void onButtonClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_endcall:
                finish();
                break;
            case R.id.btn_mute:
                rtcEngine().muteLocalAudioStream(mMuteBtn.isActivated());
                mMuteBtn.setActivated(!mMuteBtn.isActivated());
                break;
            case R.id.btn_switch_camera:
                rtcEngine().switchCamera();
                break;
        }
    }

    @Override
    public void finish() {
        super.finish();
        leaveChannel();
    }

    @Override
    public void onRemoteInvitationReceived(RemoteInvitation remoteInvitation) {
        // Do not respond to any other calls
        Log.i(TAG, "Ignore remote invitation from " +
                remoteInvitation.getCallerId() + " while in calling");
    }
//////////////////////////////

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//
//        leaveChannel();
//        RtcEngine.destroy();
//        mRtcEngine = null;
//        if (mSS) {
//            mSSClient.stop(getApplicationContext());
//        }
//    }



    public void onScreenSharingClicked(View view) {
        Button button = (Button) view;
        boolean selected = button.isSelected();
        button.setSelected(!selected);

        if (button.isSelected()) {
            mSSClient.start(getApplicationContext(), getResources().getString(R.string.private_app_id), null,
                    mChannel, Constants.SCREEN_SHARE_UID, mVEC); //SCREEN_SHARE_UID
            button.setText("SS Stop");
            mSS = true;
        } else {
            mSSClient.stop(getApplicationContext());
            button.setText("SS start");
            mSS = false;
        }
    }
}
