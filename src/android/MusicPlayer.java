package cordovaPluginYanap;

// -------------------------------
// ----------- Imports -----------
// -------------------------------

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;

import java.io.IOException;

// -------------------------------
// ------ Class description ------
// -------------------------------

public class MusicPlayer extends YanapPlayer {

    // Log TAG definition
    public static final String TAG = MusicPlayer.class.getSimpleName();

    private MediaPlayer mediaPlayer = null;

    private boolean playPending = false;
    private boolean releasePending = false;

    // -------------------------------
    // --------- Constructor ---------
    // -------------------------------

    public MusicPlayer(Yanap yanap, AssetFileDescriptor afd, String uid, float volume) {
        super(yanap, uid, volume);
        stateUpdate(Yanap.STATE.LOADING);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                if (state == Yanap.STATE.ERROR) { return; }
                mediaPlayer.setOnCompletionListener(onCompletionListener);
                stateUpdate(Yanap.STATE.LOADED);
                if (playPending && !releasePending) {
                    play();
                }
                if (releasePending) {
                    release();
                }
            }
        });
        try {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.setVolume(volume1, volume2);
            mediaPlayer.prepare();
        } catch (IOException e) {
            stateUpdate(Yanap.STATE.ERROR, "unable to load file");
        }
    }

    // -------------------------------
    // ----- Current track ended -----
    // -------------------------------

    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            stateUpdate(Yanap.STATE.STOPPED);
        }
    };

    // -------------------------------
    // ------- Interface: PLAY -------
    // -------------------------------

    @Override
    public void play() {
        if (state == Yanap.STATE.LOADING) {
            playPending = true;
        } else if (state == Yanap.STATE.LOADED || state == Yanap.STATE.STOPPED) {
            mediaPlayer.start();
            playPending = false;
            stateUpdate(Yanap.STATE.PLAYING);
        }
    }

    // -------------------------------
    // ------- Interface: STOP -------
    // -------------------------------

    @Override
    public void stop() {
        playPending = false;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            stateUpdate(Yanap.STATE.STOPPED);
        }
    }

    // -------------------------------
    // ---- Interface: SET VOLUME ----
    // -------------------------------

    @Override
    public void setVolume(float volume1, float volume2) {
        super.setVolume(volume1, volume2);
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume1, volume2);
        }
    }

    // -------------------------------
    // ------ Interface: RELEASE -----
    // -------------------------------

    @Override
    public void release() {
        if (state == Yanap.STATE.RELEASED) return;

        if (state == Yanap.STATE.LOADING) {
            releasePending = true;
            return;
        }

        playPending = false;
        releasePending = false;

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        stateUpdate(Yanap.STATE.RELEASED);
        yanap = null;
        uid = null;
    }
}