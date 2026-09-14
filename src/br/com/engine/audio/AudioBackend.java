package br.com.engine.audio;

/** Internal playback contract kept behind the stable {@link AudioClip} API. */
interface AudioBackend extends AutoCloseable
{
    void play();
    void stop();
    void setVolume(double volume);
    @Override void close();
}
