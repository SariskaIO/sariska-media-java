package org.sariska.myapplication;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.oney.WebRTCModule.WebRTCView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.sariska.sdk.Conference;
import io.sariska.sdk.Connection;
import io.sariska.sdk.JitsiLocalTrack;
import io.sariska.sdk.JitsiRemoteTrack;
import io.sariska.sdk.SariskaMediaTransport;


public class MainActivity extends AppCompatActivity {

    private Conference conference;

    private Connection connection;

    private int tap = 0;

    private RelativeLayout mLocalContainer;

    private List<JitsiLocalTrack> localTracks;

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

    @BindView(R.id.rvOtherMembers)
    RecyclerView rvOtherMembers;
    ArrayList<JitsiRemoteTrack> userList;
    RemoteAdapter sariskaRemoteAdapter;
    AlertDialog alert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        alert = getBuilder().create();

        ButterKnife.bind(this);

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        SariskaMediaTransport.initializeSdk(getApplication()); // initialize sdk
        mLocalContainer = findViewById(R.id.local_video_view_container);

        imageViewEndCall = findViewById(R.id.endcall);
        imageViewMuteAudio = findViewById(R.id.muteCall);
        imageViewMuteVideo = findViewById(R.id.muteVideo);


        this.setupLocalStream();

        Thread tokenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String token = GetToken.generateToken("abcdefgh");
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
                    if (track.getStreamURL().equals(localTracks.get(1).getStreamURL())) {
                        //So as to not add local track in remote container
                        return;
                    }

                    runOnUiThread(() -> {
                        if (track.getType().equals("video")) {
                            System.out.println("Adding to userList");
                            userList.add(0,track);
                            System.out.println("USer list length is: "+ userList.size());
                            sariskaRemoteAdapter.notifyDataSetChanged();
                        }
                    });
                });

                conference.addEventListener("TRACK_REMOVED", p -> {
                    JitsiRemoteTrack track = (JitsiRemoteTrack) p;
                    runOnUiThread(() -> {
                        sariskaRemoteAdapter.notifyDataSetChanged();
                    });
                });
                conference.join();
                System.out.println("We are past createConference");
            }
        });

        tokenThread.start();
        userList = new ArrayList<>();
        sariskaRemoteAdapter = new RemoteAdapter();
        rvOtherMembers.setAdapter(sariskaRemoteAdapter);
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

        //Add Listener to mute audio
        imageViewMuteAudio.setOnClickListener(new View.OnClickListener() {
            int tapAudio = 0;
            @Override
            public void onClick(View v) {
                JitsiLocalTrack localTrack = localTracks.get(0);
                if (tapAudio%2 == 0) {
                    localTrack.mute();
                    System.out.println("The track is muted? "+ localTrack.isMuted());
                    imageViewMuteAudio.setImageResource(R.drawable.ic_baseline_mic_off_24);
                } else {
                    localTrack.unmute();
                    System.out.println("The track is muted? "+ localTrack.isMuted());
                    imageViewMuteAudio.setImageResource(R.drawable.ic_baseline_mic_24);
                }
                tapAudio++;
            }
        });

        // Add Listener to mute video
        imageViewMuteVideo.setOnClickListener(new View.OnClickListener() {
            int tapVideo = 0;
            @Override
            public void onClick(View v) {
                JitsiLocalTrack localTrack = localTracks.get(1);
                if(tapVideo%2 == 0){
                    localTrack.mute();
                    System.out.println("The track is muted? "+ localTrack.isMuted());
                    imageViewMuteVideo.setImageResource(R.drawable.ic_baseline_videocam_off_24);
                }else{
                    localTrack.unmute();
                    System.out.println("The track is muted? "+ localTrack.isMuted());
                    imageViewMuteVideo.setImageResource(R.drawable.ic_baseline_videocam_24);
                }
                tapVideo++;
            }
        });

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

    public class RemoteAdapter extends RecyclerView.Adapter<RemoteAdapter.ItemViewHolder> {

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_item_remote_views, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            WebRTCView view = userList.get(position).render();
            view.setMirror(true);
            view.setObjectFit("cover");
            holder.remote_video_view_container.addView(view);
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        public class ItemViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.remote_video_view_container)
            RelativeLayout remote_video_view_container;

            public ItemViewHolder(@NonNull View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }
    }
}
