package cordovaPluginYanap;

interface IYanapPlayer {
    public void play();
    public void stop();
    public void setVolume(float volume1, float volume2);
    public void release();
}