package cordovaPluginYanap;

// -------------------------------
// ----------- Imports -----------
// -------------------------------

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import android.util.Log;
import android.content.res.AssetFileDescriptor;
import android.os.ParcelFileDescriptor;

// -------------------------------
// ------ Class description ------
// -------------------------------

public class Yanap extends CordovaPlugin {

    // Log TAG definition
    public static final String TAG = Yanap.class.getSimpleName();

    // States used by the players
    public enum STATE {
        NONE,     // LoopPlayer, MusicPlayer, SoundPlayer
        ERROR,    // LoopPlayer, MusicPlayer, SoundPlayer
        LOADING,  // LoopPlayer, MusicPlayer, SoundPlayer
        LOADED,   // LoopPlayer, MusicPlayer, SoundPlayer
        PLAYING,  // LoopPlayer, MusicPlayer
        LOOPING,  // LoopPlayer
        STOPPED,  // LoopPlayer, MusicPlayer
        RELEASED  // LoopPlayer, MusicPlayer, SoundPlayer
    };

    // We retain this callback to be able to emit messages to javascript at anytime
    private CallbackContext messageChannel;

    // Collections of players
    private HashMap<String, YanapPlayer> yanapPlayers;

    // -------------------------------
    // --------- Constructor ---------
    // -------------------------------

    public Yanap() {
        yanapPlayers = new HashMap<String, YanapPlayer>();
    }

    // -------------------------------
    // --- Javascript entry point ----
    // -------------------------------

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("createAudioInstance")) {
            createAudioInstance(args);
            callbackContext.success();
        } else if (action.equals("play")) {
            play(args);
            callbackContext.success();
        } else if (action.equals("stop")) {
            stop(args);
            callbackContext.success();
        } else if (action.equals("release")) {
            release(args);
            callbackContext.success();
        } else if (action.equals("setVolume")) {
            setVolume(args);
            callbackContext.success();
        } else if (action.equals("messageChannel")) {
            messageChannel = callbackContext;
            // in this case we want to keep the callbackContext
        } else {
            return false;
        }
        return true;
    }

    // -------------------------------
    // ---- Audio object creation ----
    // -------------------------------

    private void createAudioInstance(JSONArray args) throws JSONException {
        String uid = args.getString(0);
        String audioType = args.getString(1);
        String filePath = args.getString(2);

        if (yanapPlayers.containsKey(uid)) {
            statusUpdate(uid, STATE.ERROR, "uid " + uid + " already exists");
            return;
        }

        final File cacheFile = new File(cordova.getActivity().getApplicationContext().getCacheDir(), filePath);
        AssetFileDescriptor afd;
        try {
            afd = new AssetFileDescriptor(ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY), 0, -1);
        } catch (java.io.FileNotFoundException e) {
            statusUpdate(uid, STATE.ERROR, "unable to open file `" + filePath + "` at `" + cacheFile.getAbsolutePath() + "`");
            return;
        }
        YanapPlayer yanapPlayer = null;

        if (audioType.equals("loop")) {
            yanapPlayer = new LoopPlayer(this, afd, uid, 1.0f);
        } else if (audioType.equals("music")) {
            yanapPlayer = new MusicPlayer(this, afd, uid, 1.0f);
        } else if (audioType.equals("sound")) {
            yanapPlayer = new SoundPlayer(this, afd, uid, 1, 1.0f);
        } else {
            statusUpdate(uid, STATE.ERROR, "unknown audioType `" + audioType + "`");
            return;
        }

        yanapPlayers.put(uid, yanapPlayer);
    }

    // -------------------------------
    // ------- Interface: PLAY -------
    // -------------------------------

    private void play(JSONArray args) throws JSONException {
        String uid = args.getString(0);
        if (yanapPlayers.containsKey(uid)) {
            yanapPlayers.get(uid).play();
        } else {
            statusUpdate(uid, STATE.ERROR, "(play) audioInstance `" + uid + "` not found");
        }
    }

    // -------------------------------
    // ------- Interface: STOP -------
    // -------------------------------

    private void stop(JSONArray args) throws JSONException {
        String uid = args.getString(0);
        if (yanapPlayers.containsKey(uid)) {
            yanapPlayers.get(uid).stop();
        } else {
            statusUpdate(uid, STATE.ERROR, "(stop) audioInstance `" + uid + "` not found");
        }
    }

    // -------------------------------
    // ------ Interface: RELEASE -----
    // -------------------------------

    private void release(JSONArray args) throws JSONException {
        String uid = args.getString(0);
        if (yanapPlayers.containsKey(uid)) {
            yanapPlayers.get(uid).release();
        } else {
            statusUpdate(uid, STATE.ERROR, "(release) audioInstance `" + uid + "` not found");
        }
    }

    // -------------------------------
    // ---- Interface: SET VOLUME ----
    // -------------------------------

    private void setVolume(JSONArray args) throws JSONException {
        String uid = args.getString(0);
        float volume1 = (float) args.getDouble(1);
        float volume2 = (float) args.getDouble(2);
        if (yanapPlayers.containsKey(uid)) {
            yanapPlayers.get(uid).setVolume(volume1, volume2);
        } else {
            statusUpdate(uid, STATE.ERROR, "(setVolume) audioInstance `" + uid + "` not found");
        }
    }

    // -------------------------------
    // ----- Native to JS events -----
    // -------------------------------

    // To transmit any message to javascript
    public void transmitToJs(JSONObject message) {
        if (messageChannel == null) { return; }
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, message);
        pluginResult.setKeepCallback(true); // we want to retain this callback forever
        messageChannel.sendPluginResult(pluginResult);
    }

    // Transmit an audio object status update to JS
    public void statusUpdate(String uid, Yanap.STATE state, String additionalInfo) {
        JSONObject message = new JSONObject();
        try {
            message.put("msgType", "statusUpdate");
            message.put("audioUid", uid);
            message.put("newStatus", state.toString());
            if (!additionalInfo.equals("")) {
                message.put("additionalInfo", additionalInfo);
            }
            transmitToJs(message);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create status details", e);
            return;
        }
    }
}