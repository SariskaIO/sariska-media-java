package org.sariska.myapplication;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.os.Bundle;
import com.oney.WebRTCModule.WebRTCView;
import android.widget.RelativeLayout;
import org.sariska.sdk.Connection;
import org.sariska.sdk.Conference;
import org.sariska.sdk.JitsiRemoteTrack;
import org.sariska.sdk.SariskaMediaTransport;
import org.sariska.sdk.JitsiLocalTrack;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Conference conference;

    private Connection connection;

    private RelativeLayout mRemoteContainer;

    private RelativeLayout mLocalContainer;

    private List<JitsiLocalTrack> localTracks;

    private WebRTCView remoteView;

    String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
    };
    int PERMISSION_ALL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
        SariskaMediaTransport.init(getApplication()); // initialize sdk
        mLocalContainer = findViewById(R.id.local_video_view_container);
        mRemoteContainer = findViewById(R.id.remote_video_view_container);

        this.setupLocalStream();

        String token = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjI0ZmQ2ZjkyZDZkMDE3NDkyZTNlOThlMzM0ZWJhZmM3NmRkMzUwYmI5M2EwNzI5ZDM4IiwidHlwIjoiSldUIn0.eyJjb250ZXh0Ijp7InVzZXIiOnsiaWQiOiJ6dG5reWw3biIsIm5hbWUiOiJqaiJ9LCJncm91cCI6Imc3cWtua205YWJ0cDFuYWd2eXk1ZnUifSwic3ViIjoiMiIsInJvb20iOiJwNHN1anR5YWsiLCJpYXQiOjE2MTg0NDMxNTksIm5iZiI6MTYxODQ0MzE1OSwiaXNzIjoic2FyaXNrYSIsImF1ZCI6Im1lZGlhX21lc3NhZ2luZ19zYXJpc2thIiwiZXhwIjoxNjE4NTI5NTU5fQ.dhGrKmmRE7E1Mr_Hp3Nu9VSoR7mMEmYz5nI-Fp8ZR97wOXMdvvgjUA_xr2ghYTMP6DGm81MztqsJFW5BSZ18D5ejtx11MyjdTDcsVBiVXNmUO7C6KCHHPDEjRirC1mNc5d9V7Unta-Fo6K6oLyWnsPyfctXdrURk8ChrnXPyHvX_TiZalotdkmChooTbCQlL8SNRk-j8-HVWToYxukv7aB7AvTSvh_e9xshAlsZMzO9dfoenkMH4XILfxCfcWcj-gdfZnflwSu5kpBW6CmY-I9Fe6M4DUGamNplgyua3xe1olwXMe-Ofo48-Yu0vkHbE4ssuoMEsgm0vCt7GXTRd2w";

        connection = SariskaMediaTransport.JitsiConnection(token);

        connection.addEventListener("CONNECTION_ESTABLISHED", this::createConference);

        connection.addEventListener("CONNECTION_FAILED", () -> {
        });

        connection.addEventListener("CONNECTION_DISCONNECTED", () -> {
        });
        connection.connect();
    }

    public void setupLocalStream() {
        Bundle options = new Bundle();
        options.putBoolean("audio", true);
        options.putBoolean("video", true);
        options.putInt("resolution", 240);  // 180, 240, 360, 720, 1080
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
                        view.setObjectFit("cover");
                        mLocalContainer.addView(view);
                    }
                }
            });
        });
    }


    public void createConference() {

        conference = connection.initJitsiConference();
        
        conference.addEventListener("CONFERENCE_JOINED", () -> {
            for (JitsiLocalTrack track : localTracks) {
                conference.addTrack(track);
            }
        });

        conference.addEventListener("DOMINANT_SPEAKER_CHANGED", p -> {
            String id = (String) p;

        });

        conference.addEventListener("CONFERENCE_LEFT", () -> {

        });

        conference.addEventListener("TRACK_ADDED", p -> {
            JitsiRemoteTrack track = (JitsiRemoteTrack) p;
            runOnUiThread(() -> {
                if (track.getType().equals("video")) {
                    WebRTCView view = track.render();
                    view.setObjectFit("cover");
                    remoteView = view;
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

}