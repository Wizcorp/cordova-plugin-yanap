package cordovaPluginYanap;

interface IYanapPlayer {
    void play();
    void stop();
    void setVolume(float volume1, float volume2);
    void release();
}