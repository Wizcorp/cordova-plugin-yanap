package cordovaPluginYanap;

// -------------------------------
// ----------- Imports -----------
// -------------------------------

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.AudioAttributes;

import java.util.HashMap;

// -------------------------------
// ------ Class description ------
// -------------------------------

public class SoundPlayer extends YanapPlayer {

    // -------------------------------
    // ----------- Statics -----------
    // -------------------------------

    // Log TAG definition
    private static final String TAG = SoundPlayer.class.getSimpleName();

    // Constant(s)
    final private static int MAX_CHANNELS = 20;

    private static SoundPool soundPool = null;
    private static HashMap<Integer, SoundPlayer> soundIdToSoundPlayer;

    // -------------------------------
    // ------- Local variables -------
    // -------------------------------

    private int soundId = -1;
    private int priority = 0;
    private int streamId = 0;

    private boolean playPending = false;
    private boolean releasePending = false;

    // -------------------------------
    // --------- Constructor ---------
    // -------------------------------

    public SoundPlayer(Yanap yanap, AssetFileDescriptor afd, String uid, int priority, float volume) {
        super(yanap, uid, volume);
        if (soundPool == null) createSoundPool(MAX_CHANNELS); // only one SoundPool for all SoundPlayers
        stateUpdate(Yanap.STATE.LOADING);
        this.priority = priority;

        this.soundId = soundPool.load(afd, priority);
        soundIdToSoundPlayer.put(this.soundId, this);
        soundPool.setVolume(this.soundId, volume1, volume2);
    }

    // -------------------------------
    // -- Unique SoundPool creation --
    // -------------------------------

    protected static void createSoundPool(int maxStreams) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            createNewSoundPool(maxStreams);
        } else {
            createOldSoundPool(maxStreams);
        }
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int soundId, int status) {
                SoundPlayer soundPlayer = soundIdToSoundPlayer.get(soundId);
                if (soundPlayer == null) { return; }
                if (status != 0) {
                    soundPlayer.stateUpdate(Yanap.STATE.ERROR, "unable to load file (status " + status + ")");
                }
                if (soundPlayer.state == Yanap.STATE.ERROR) { return; }
                soundPlayer.stateUpdate(Yanap.STATE.LOADED);

                if (soundPlayer.playPending && !soundPlayer.releasePending) {
                    soundPlayer.play();
                }
                if (soundPlayer.releasePending) {
                    soundPlayer.release();
                }
            }
        });
        soundIdToSoundPlayer = new HashMap<Integer, SoundPlayer>(); // TODO: use SparseArray?
    }

    // modern style declaration
    @android.annotation.TargetApi(android.os.Build.VERSION_CODES.LOLLIPOP)
    protected static void createNewSoundPool(int maxStreams){
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME) // TODO: or USAGE_MEDIA...
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION) // TODO: or CONTENT_TYPE_MUSIC, CONTENT_TYPE_SPEECH...
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(maxStreams) // TODO: really required?
                .setAudioAttributes(attributes)
                .build();
    }

    // old style declaration (for compatibility)
    @SuppressWarnings("deprecation")
    protected static void createOldSoundPool(int maxStreams){
        soundPool = new SoundPool(maxStreams, AudioManager.STREAM_MUSIC, 0);
    }

    // -------------------------------
    // ------- Interface: PLAY -------
    // -------------------------------

    @Override
    public void play() {
        if (state == Yanap.STATE.LOADING) {
            playPending = true;
        } else if (state == Yanap.STATE.LOADED) {
            playPending = false;
            this.streamId = soundPool.play(this.soundId, this.volume1, this.volume2, this.priority, 0, 1f);
            if (this.streamId == 0) {
                stateUpdate(Yanap.STATE.ERROR, "unable to play file");
                return;
            }
        }
    }

    // -------------------------------
    // ------- Interface: STOP -------
    // -------------------------------

    @Override
    public void stop() {
        playPending = false;
        if (streamId == 0) return;
        soundPool.stop(streamId);
    }

    // -------------------------------
    // ---- Interface: SET VOLUME ----
    // -------------------------------

    @Override
    public void setVolume(float volume1, float volume2) {
        super.setVolume(volume1, volume2);
        if (streamId == 0) return;
        soundPool.setVolume(this.soundId, volume1, volume2);
    }

    // -------------------------------
    // ------ Interface: RELEASE -----
    // -------------------------------

    @Override
    public void release() {
        if (state == Yanap.STATE.RELEASED) return;

        playPending = false;
        releasePending = false;

        soundPool.stop(this.soundId);
        soundPool.unload(this.soundId);
        if (soundIdToSoundPlayer.containsKey(this.soundId)) {
            soundIdToSoundPlayer.remove(this.soundId);
        }

        soundId = -1;
        priority = 0;
        streamId = 0;

        stateUpdate(Yanap.STATE.RELEASED);
        yanap = null;
        uid = null;
    }
}