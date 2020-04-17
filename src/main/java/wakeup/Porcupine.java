package wakeup;
// sudo apt-get install openjdk-8-jdk-headless
public class Porcupine {
    private final long object;

    static {
        // For both platforms, this will be set explicitly
        //System.load("C:\\Users\\wdavi\\IdeaProjects\\VocalCord\\native\\windows\\libjni_porcupine.dll");
        System.load("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/native/linux/libjni_porcupine.so");
    }

    public Porcupine(String modelFilePath, String keywordFilePath, float sens) throws Exception {
        try {
            // For linux, can just ignore first argument, just make sure both files are in the same directory
            //object = init("native\\windows\\libpv_porcupine.dll", modelFilePath, keywordFilePath, sens);
            object = init("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/native/linux/libpv_porcupine.so", modelFilePath, keywordFilePath, sens);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public boolean processFrame(short[] pcm) throws Exception {
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

    private native long init(String dllLocation, String modelFilePath, String keywordFilePaths, float sensitivities);

    private native boolean process(long object, short[] pcm);

    private native void delete(long object);
}
