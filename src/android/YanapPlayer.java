package cordovaPluginYanap;

abstract public class YanapPlayer implements IYanapPlayer {
    public Yanap.STATE state = Yanap.STATE.NONE;
    protected Yanap yanap;
    protected String uid = null;
    protected float volume1, volume2; // TODO: rename to volumeR/volumeL

    public YanapPlayer(Yanap yanap, String uid, float volume) {
        this.yanap = yanap;
        this.uid = uid;
        this.volume1 = volume;
        this.volume2 = volume;
    }

    // -------------------------------
    // --- State update + JS event ---
    // -------------------------------

    // Update state and transmit it to JS
    public void stateUpdate(Yanap.STATE status, String additionalInfo) {
        if (state == status && state == Yanap.STATE.ERROR) { return; }
        state = status;
        yanap.statusUpdate(uid, status, additionalInfo);
        if (state == Yanap.STATE.ERROR) {
            release();
        }
    }

    // Update state and transmit it to JS (overload)
    protected void stateUpdate(Yanap.STATE status) {
        stateUpdate(status, "");
    }

    // -------------------------------
    // ---------- Interface ----------
    // -------------------------------

    @Override
    public void setVolume(float volume1, float volume2) {
        this.volume1 = volume1;
        this.volume2 = volume2;
    }

    // -------------------------------
    // - Garbage collector kindness --
    // -------------------------------

    @Override
    protected void finalize() throws Throwable {
        try {
            release();
        } catch (Throwable t) {
            throw t;
        } finally {
            super.finalize();
        }
    }
}
