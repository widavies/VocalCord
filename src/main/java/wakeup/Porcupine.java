package wakeup;

import vocalcord.VocalCord;

// sudo apt-get install openjdk-8-jdk-headless
public class Porcupine {
    private final long object;

    private static final VocalCord.Config CONFIG = VocalCord.getConfig();

    static {
        System.load(CONFIG.jniLocation);
    }

    public Porcupine() throws Exception {
        try {
            object = init(CONFIG.porcupineLocation, CONFIG.porcupineParams, CONFIG.sensitivity, CONFIG.wakePhrasePaths);
        } catch(Exception e) {
            throw new Exception(e);
        }
    }

    public int processFrame(short[] pcm) throws Exception {
        try {
            return process(object, pcm);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public void delete() {
        delete(object);
    }

    public native int getFrameLength();

    public native int getSampleRate();

    private native long init(String dllLocation, String modelFilePath, float sensitivities, String[] wakePhrasePaths);

    private native int process(long object, short[] pcm);

    private native void delete(long object);
}
