package cordovaPluginYanap;


abstract public class YanapPlayer implements IYanapPlayer {
    public Yanap.STATE state = Yanap.STATE.NONE;
    protected Yanap yanap;
    protected String uid = null;

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
    // - Garbage collector kindness --
    // -------------------------------

    @Override
    protected void finalize() throws Throwable
    {
        try {
            release();
        } catch (Throwable t) {
            throw t;
        } finally {
            super.finalize();
        }
    }
}
