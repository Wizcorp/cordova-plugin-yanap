package cordovaPluginYanap;

// -------------------------------
// ----------- Imports -----------
// -------------------------------

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;

import org.chromium.base.Log;

import java.io.IOException;

// -------------------------------
// ------ Class description ------
// -------------------------------

public class LoopPlayer extends YanapPlayer {

    // Log TAG definition
    public static final String TAG = LoopPlayer.class.getSimpleName();

    // SDK >= 16 is required to use the method setNextMediaPlayer
    private static boolean gaplessLoopSupported = android.os.Build.VERSION.SDK_INT >= 16;

    private MediaPlayer mCurrentPlayer = null;
    private MediaPlayer mNextPlayer = null;
    private AssetFileDescriptor afd;

    private boolean playPending = false;
    private boolean releasePending = false;
    private boolean secondLoopLoading = false;

    // -------------------------------
    // --------- Constructor ---------
    // -------------------------------

    public LoopPlayer(Yanap yanap, AssetFileDescriptor afd, String uid, float volume) {
        super(yanap, uid, volume);
        stateUpdate(Yanap.STATE.LOADING);
        this.afd = afd;

        mCurrentPlayer = new MediaPlayer();
        mCurrentPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                if (state == Yanap.STATE.ERROR) { return; }
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
            mCurrentPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mCurrentPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mCurrentPlayer.setVolume(volume1, volume2);
            mCurrentPlayer.prepare();
            if (gaplessLoopSupported) {
                createNextMediaPlayer();
            } else {
                mCurrentPlayer.setLooping(true);
            }
        } catch (IOException e) {
            stateUpdate(Yanap.STATE.ERROR, "unable to load file");
        }
    }

    // -------------------------------
    // ------ Next loop creation -----
    // -------------------------------

    private void createNextMediaPlayer() {
        if (android.os.Build.VERSION.SDK_INT < 16) { // required by compiler but already filtered with gaplessLoopSupported
            return;
        }
        mNextPlayer = new MediaPlayer();
        mNextPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                secondLoopLoading = false;
                if (releasePending) {
                    release();
                }
            }
        });
        try {
            mNextPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mNextPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mNextPlayer.setVolume(volume1, volume2);
            secondLoopLoading = true;
            mNextPlayer.prepare();
        } catch (IOException e) {
            secondLoopLoading = false;
            if (releasePending) {
                release();
                return;
            }
            if (mCurrentPlayer.isPlaying()) mCurrentPlayer.stop();
            mCurrentPlayer.reset();
            mCurrentPlayer.release();
            mNextPlayer.reset();
            mNextPlayer.release();
            stateUpdate(Yanap.STATE.ERROR, "unable to prepare next loop");
            return;
        }
        mCurrentPlayer.setNextMediaPlayer(mNextPlayer);
        mCurrentPlayer.setOnCompletionListener(onCompletionListener);
    }

    // -------------------------------
    // ----- Current track ended -----
    // -------------------------------

    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mCurrentPlayer = mNextPlayer;
            stateUpdate(Yanap.STATE.LOOPING);
            createNextMediaPlayer();
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
            mCurrentPlayer.start();
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
        if (mCurrentPlayer.isPlaying()) {
            mCurrentPlayer.pause();
            stateUpdate(Yanap.STATE.STOPPED);
        }
    }

    // -------------------------------
    // ---- Interface: SET VOLUME ----
    // -------------------------------

    @Override
    public void setVolume(float volume1, float volume2) {
        super.setVolume(volume1, volume2);
        if (mCurrentPlayer != null) {
            mCurrentPlayer.setVolume(volume1, volume2);
        }
        if (mNextPlayer != null) {
            mNextPlayer.setVolume(volume1, volume2);
        }
    }

    // -------------------------------
    // ------ Interface: RELEASE -----
    // -------------------------------

    @Override
    public void release() {
        if (state == Yanap.STATE.RELEASED) return;

        if (state == Yanap.STATE.LOADING || secondLoopLoading) {
            releasePending = true;
            return;
        }

        playPending = false;
        releasePending = false;
        secondLoopLoading = false;

        if (mCurrentPlayer != null) {
            if (mCurrentPlayer.isPlaying()) mCurrentPlayer.stop();
            mCurrentPlayer.reset();
            mCurrentPlayer.release();
            mCurrentPlayer = null;
        }

        if (mNextPlayer != null) {
            if (mNextPlayer.isPlaying()) mNextPlayer.stop();
            mNextPlayer.reset();
            mNextPlayer.release();
            mNextPlayer = null;
        }

        // if (afd != null) afd.close(); // TODO: is this required?
        afd = null;
        stateUpdate(Yanap.STATE.RELEASED);
        yanap = null;
        uid = null;
    }
}