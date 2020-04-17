package vocalcord;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.entities.User;
import wakeup.Porcupine;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;

public class UserStream {

    private boolean awake;
    private byte[] phrase;
    private boolean phraseBegun;
    private long lastReceivedPacket;
    private int index;

    private Porcupine porcupine;

    /*
     * Converts Discord PCM to the audio format required by Porcupine
     */
    private static class PorcupineAdapter {
        private static final int AUDIO_FRAME = 512;
        private short[] pcm;
        private int index = 0;

        public PorcupineAdapter(byte[] raw) {
            // Down-samples audio to 16 KHz and combines bytes into shorts
            pcm = new short[raw.length / 12 + (AUDIO_FRAME - raw.length / 12 % AUDIO_FRAME)];

            for(int i = 0, j = 0; i < raw.length; i += 12, j++) {
                pcm[j] = (short) ((raw[i] << 8) | (raw[i + 1] & 0xFF));
            }
        }

        public boolean hasNext() {
            return index < pcm.length;
        }

        // Reclaim rest of audio data once wait word is detected
        public void reclaim() {

        }

        public short[] take() {
            short[] frame = new short[AUDIO_FRAME];
            System.arraycopy(pcm, index, frame, index, index + AUDIO_FRAME - index);
            index += AUDIO_FRAME;
            return frame;
        }
    }

    private User user;

    public UserStream() throws Exception {
        //porcupine = new Porcupine("C:\\Users\\wdavi\\IdeaProjects\\VocalCord\\wake-engine\\Porcupine\\lib\\common\\porcupine_params.pv", "C:\\Users\\wdavi\\IdeaProjects\\VocalCord\\phrases\\hey_bot_windows.ppn", 0.5f);
        //porcupine = new Porcupine("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/wake-engine/Porcupine/lib/common/porcupine_params.pv", "/mnt/c/Users/wdavi/IdeaProjects/VocalCord/phrases/hey_bot_linux.ppn", 0.5f);
        porcupine = new Porcupine();

        if(porcupine.getFrameLength() != 512 || porcupine.getSampleRate() != 16000) {
            throw new RuntimeException("The underlying porcupine binaries do not have the expected configuration.");
        }
    }

    public void putAudio(byte[] audio) {
        if(!awake) {
            try {
                PorcupineAdapter pa = new PorcupineAdapter(audio);
                if(porcupine.processFrame(pa.take()) != -1) {
                    System.out.println("WAKE WORD DETECTED");
                    if(VocalCord.getConfig().callbacks.onWake(user)) {
                        awake = true;
                        phrase = new byte[3840 * 50 * 5]; // by default, holds 5 seconds of data
                        phraseBegun = false;
                        lastReceivedPacket = System.nanoTime();
                        index = 0;
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            // Resize the array if needed
            if(index + audio.length >= phrase.length) {
                byte[] resized = new byte[phrase.length * 2];
                System.arraycopy(phrase, 0, resized, 0, phrase.length);
                phrase = resized;
            }

            // Concatenate on the audio data
            System.arraycopy(audio, 0, phrase, index, audio.length);
            index += audio.length;
            lastReceivedPacket = System.nanoTime();

            if(index >= 3840 * 50 / 2) { // need half a second of audio to consider phraseBegun
                phraseBegun = true;
            }
        }
    }

    public boolean readyForTranscription() {
        if(!awake) return false;

        // if no packet received in last 2 seconds & phraseBegun, then transcribe
        // if no packet received in last 5 seconds & !phraseBegun, then transcribe
        // if certain limit reached (15 seconds), transcribe
        // future: trailing audio volume is 30% or something less than the average of the whole

        long elapsedMs = (System.nanoTime() - lastReceivedPacket) / 1_000_000;

        if(phraseBegun && elapsedMs >= 120) {
            System.out.println("Phrase completed");
            return true;
        } else if(!phraseBegun && elapsedMs >= 5000) {
            System.out.println("Phrase never started");
            sleep();
            return false; // user never started talking after waking bot
        } else if(phrase.length > 3840 * 50 * 15) {
            return true;
        }

        return false;
    }

    public void sleep() {
        awake = false;
    }

    // export GOOGLE_APPLICATION_CREDENTIALS=/mnt/c/Users/wdavi/IdeaProjects/VocalCord/vocalcord-gcs.json
    public byte[] getAudioForGoogle() {
//        // Trim
//        byte[] trimmed = new byte[index];
//        System.arraycopy(phrase, 0, trimmed, 0, index);
//
//        // Stereo to mono
//        byte[] mono = new byte[trimmed.length / 2];
//        for(int i = 0, j = 0; i < trimmed.length; i += 4, j+=2) {
//            short a = (short) ((trimmed[i] << 8) | (trimmed[i + 1] & 0xFF));
//            short b = (short) ((trimmed[i + 2] << 8) | (trimmed[i + 3] & 0xFF));
//
//            short m = Short.reverseBytes((short) ((a + b) / 2));
//
//            byte high = (byte) (m >> 8);
//            byte low = (byte) (m & 0x00FF);
//
//            mono[j] = high;
//            mono[j+1] = low;
//        }
//
//        byte[] downsized = new byte[mono.length / 2];
//
//        // Convert to little endian
//        for(int i = 0, j = 0; i < mono.length; i += 4, j+=2 ) {
//            downsized[j] = mono[i];
//            downsized[j + 1] = mono[i+1];
//        }
//
//        return downsized;

        try {
            AudioFormat target = new AudioFormat(16000f, 16, 1, true, false);
            AudioInputStream is = AudioSystem.getAudioInputStream(target, new AudioInputStream(new ByteArrayInputStream(phrase), AudioReceiveHandler.OUTPUT_FORMAT, phrase.length));

            return is.readAllBytes();
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }

//        try {
//            AudioFormat target = ;
//            AudioInputStream is = AudioSystem.getAudioInputStream(target, new AudioInputStream(new ByteArrayInputStream(phrase), AudioReceiveHandler.OUTPUT_FORMAT, phrase.length));
//            AudioSystem.write(is, AudioFileFormat.Type.WAVE, new File("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/audio.wav"));
//            return IOUtils.toByteArray(new FileInputStream(new File("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/audio.wav")));
//        } catch(Exception e) {
//            e.printStackTrace();
//            System.out.println("Failed to convert!");
//        }

//        ByteBuffer bb = ByteBuffer.wrap(phrase);
//        bb.order(ByteOrder.LITTLE_ENDIAN);
//
//        byte[] pcm = bb.array();
//        byte[] downSample = new byte[pcm.length / 6];
//        for(int i = 0, j = 0; i < pcm.length; i += 6, j++) {
//            downSample[j] = pcm[i];
//        }

//        byte[] downsized = new byte[phrase.length / 6];
//
//        // Convert audio to little endian & down-sample to 16000Khz
//        for(int i = 0, j = 0; i < phrase.length / 6; i += 6, j += 2) {
//            short reversed = Short.reverseBytes((short) ((phrase[i] << 8) | (phrase[i + 1] & 0xFF)));
//            byte low = (byte) (reversed >> 8);
//            byte high = (byte) (reversed & 0x00FF);
//
//            downsized[j] = high;
//            downsized[j + 1] = low;
//        }

        // return downSample;

    }

}
