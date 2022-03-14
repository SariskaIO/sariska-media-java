package org.sariska.myapplication;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.oney.WebRTCModule.WebRTCView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import io.sariska.sdk.Connection;
import io.sariska.sdk.Conference;
import io.sariska.sdk.JitsiRemoteTrack;
import io.sariska.sdk.SariskaMediaTransport;
import io.sariska.sdk.JitsiLocalTrack;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Conference conference;

    private Connection connection;

    private int tap = 0;

    private RelativeLayout mRemoteContainer;

    private RelativeLayout mLocalContainer;

    private List<JitsiLocalTrack> localTracks;

    private WebRTCView remoteView;

    private WebRTCView localView;

    private ImageView imageViewEndCall;

    private ImageView imageViewSwitchCamera;

    private ImageView imageViewMuteAudio;

    private ImageView imageViewMuteVideo;


    String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
    };
    int PERMISSION_ALL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AlertDialog alert = getBuilder().create();
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        SariskaMediaTransport.initializeSdk(getApplication()); // initialize sdk
        mLocalContainer = findViewById(R.id.local_video_view_container);
        mRemoteContainer = findViewById(R.id.remote_video_view_container);
        imageViewEndCall = findViewById(R.id.endcall);
        imageViewMuteAudio = findViewById(R.id.unmuteCall);
        imageViewMuteVideo = findViewById(R.id.muteVideo);


        this.setupLocalStream();

        Thread tokenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String token = GetToken.generateToken("abcdefgh", "dipak");
                    connection = SariskaMediaTransport.JitsiConnection(token, "dipak", false);
                    connection.addEventListener("CONNECTION_ESTABLISHED", this::createConference);
                    connection.addEventListener("CONNECTION_FAILED", () -> {
                    });
                    connection.addEventListener("CONNECTION_DISCONNECTED", () -> {
                    });
                    connection.connect();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }

            public void createConference() {
                System.out.println("We are in createConference");
                conference = connection.initJitsiConference();

                conference.addEventListener("CONFERENCE_JOINED", () -> {
                    for (JitsiLocalTrack track : localTracks) {
                            conference.addTrack(track);
                    }
                });

                conference.addEventListener("DOMINANT_SPEAKER_CHANGED", p -> {
                    String id = (String) p;
                    conference.selectParticipant(id);
                });
                conference.addEventListener("CONFERENCE_LEFT", () -> {
                });

                conference.addEventListener("TRACK_ADDED", p -> {
                    JitsiRemoteTrack track = (JitsiRemoteTrack) p;
                    runOnUiThread(() -> {
                        if (track.getType().equals("video")) {
                            WebRTCView view = track.render();
                            view.setMirror(true);
                            view.setObjectFit("cover");
                            mRemoteContainer.addView(view);
                        }
                    });
                });

                conference.addEventListener("TRACK_REMOVED", p -> {
                    JitsiRemoteTrack track = (JitsiRemoteTrack) p;
                    runOnUiThread(() -> {
                        mRemoteContainer.removeView(remoteView);
                    });
                });
                conference.join();


                System.out.println("We are past createConference");
            }
        });

        tokenThread.start();

        // Add button and container click listeners
        addRequiredListeners(alert);
    }

    private void addRequiredListeners(AlertDialog alert) {
        //Add listener to end call
        imageViewEndCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert.show();
            }
        });

        //Add listener to change container focus
        mRemoteContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeContainerFocus();
            }
        });

        //Add listener to switch Camera
//        imageViewSwitchCamera.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                for (JitsiLocalTrack track : localTracks) {
//                    track.switchCamera();
//                }
//            }
//        });

        //Add Listener to mute audio
        imageViewMuteAudio.setOnClickListener(new View.OnClickListener() {
            int tap = 0;
            @Override
            public void onClick(View v) {
                if(tap%2 == 0){
                    localTracks.get(0).mute();
                    imageViewMuteAudio = findViewById(R.id.muteCall);
                    tap++;
                }else{
                    localTracks.get(0).unmute();
                    imageViewMuteAudio = findViewById(R.id.unmuteCall);
                    tap++;
                }
            }
        });

        // Add Listener to mute video
        imageViewMuteVideo.setOnClickListener(new View.OnClickListener() {
            int tapVideo = 0;
            @Override
            public void onClick(View v) {
                if(tapVideo%2 == 0){
                    localTracks.get(1).mute();
                    tapVideo++;
                }else{
                    localTracks.get(1).unmute();
                    tapVideo++;
                }
            }
        });

    }

    private void changeContainerFocus() {
        if(localView == null){
            mLocalContainer.addView(remoteView);
            remoteView=null;
        }
        if(remoteView == null){
            return;
        }
        if(tap %2  == 0){
            mRemoteContainer.removeView(remoteView);
            mLocalContainer.removeView(localView);
            mLocalContainer.addView(remoteView);
            mRemoteContainer.addView(localView);
            tap++;
        }else{
            mLocalContainer.removeView(remoteView);
            mRemoteContainer.removeView(localView);
            mLocalContainer.addView(localView);
            mRemoteContainer.addView(remoteView);
            tap++;
        }
    }

    public void setupLocalStream() {
        Bundle options = new Bundle();
        options.putBoolean("audio", true);
        options.putBoolean("video", true);
        options.putInt("resolution", 360);  // 180, 240, 360, 720, 1080
//      options.putString("facingMode", "user");   user or environment
//      options.putBoolean("desktop", true);  for screen sharing
//      options.putString("micDeviceId", "mic_device_id");
//      options.putString("cameraDeviceId", "camera_device_id");
        SariskaMediaTransport.createLocalTracks(options, tracks -> {
            runOnUiThread(() -> {
                localTracks = tracks;
                for (JitsiLocalTrack track : tracks) {
                    if (track.getType().equals("video")) {
                        WebRTCView view = track.render();
                        view.setMirror(true);
                        localView = view;
                        view.setObjectFit("cover");
                        mLocalContainer.addView(view);
                    }
                }
            });
        });
    }

    public boolean hasPermissions(MainActivity context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        conference.leave();
        connection.disconnect();
        finish();
        System.gc();
        System.exit(0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        conference.leave();
        connection.disconnect();
        finish();
    }

    public Builder getBuilder(){
        Builder builder = new Builder(MainActivity.this);
        builder.setMessage("Are you sure you want to leave?");
        builder.setCancelable(true);
        builder.setPositiveButton(
                "Leave",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(conference != null){
                            conference.leave();
                        }
                        connection.disconnect();
                        finish();
                    }
                }).setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        return builder;
    }
}
