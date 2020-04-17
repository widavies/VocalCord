package wakeup;

import vocalcord.VocalCord;

// sudo apt-get install openjdk-8-jdk-headless
public class Porcupine {
    private final long object;

    private static final VocalCord.Config CONFIG = VocalCord.getConfig();

    static {
        // For both platforms, this will be set explicitly
        //System.load("C:\\Users\\wdavi\\IdeaProjects\\VocalCord\\native\\windows\\libjni_porcupine.dll");
        //System.load("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/native/linux/libjni_porcupine.so");
        System.load(CONFIG.jniLocation);
    }

    public Porcupine() throws Exception {
        try {
            // For linux, can just ignore first argument, just make sure both files are in the same directory
            //object = init("native\\windows\\libpv_porcupine.dll", modelFilePath, keywordFilePath, sens);
           // object = init("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/native/linux/libpv_porcupine.so", modelFilePath, keywordFilePath, sens);
            object = init(CONFIG.porcupineLocation, CONFIG.porcupineParams, CONFIG.sensitivity, CONFIG.wakePhrasePaths);
        } catch (Exception e) {
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
