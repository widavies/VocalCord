package vocalcord;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.entities.User;
import wakeup.Porcupine;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.util.concurrent.ThreadPoolExecutor;

public class UserStream {

    private boolean awake;
    private byte[] phrase;
    private boolean phraseBegun;
    private long lastReceivedPacket;
    private int index;
    private long lastAudioReceived = -1;

    private final VocalCord.Config config = VocalCord.getConfig();

    private Porcupine porcupine;

    /*
     * Converts Discord PCM to the audio format required by Porcupine
     */
    private static class PorcupineAdapter {
        private static final int AUDIO_FRAME = 512;
        private final short[] pcm;
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

        public short[] take() {
            short[] frame = new short[AUDIO_FRAME];
            System.arraycopy(pcm, index, frame, index, index + AUDIO_FRAME - index);
            index += AUDIO_FRAME;
            return frame;
        }
    }

    private final User user;

    public UserStream(User user) throws Exception {
        this.user = user;

        if(!VocalCord.getConfig().captioning) {
            porcupine = new Porcupine();

            if(porcupine.getFrameLength() != 512 || porcupine.getSampleRate() != 16000) {
                throw new RuntimeException("The underlying porcupine binaries do not have the expected configuration.");
            }
        }
    }

    public void putAudio(ThreadPoolExecutor workPool, byte[] audio) {
        lastAudioReceived = System.nanoTime();

        if(!awake && !VocalCord.getConfig().captioning) {
            try {
                PorcupineAdapter pa = new PorcupineAdapter(audio);
                while(pa.hasNext()) {
                    int keywordIndex = porcupine.processFrame(pa.take());

                    if(keywordIndex != -1) {
                        workPool.execute(() -> VocalCord.getConfig().callbacks.onWake(this, keywordIndex));
                        wakeup();
                    }
                }

            } catch(Exception e) {
                e.printStackTrace();
            }
        } else if(!awake && VocalCord.getConfig().captioning) {
            wakeup();
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

    private void wakeup() {
        awake = true;
        phrase = new byte[3840 * 50 * 5]; // by default, holds 5 seconds of data
        phraseBegun = false;
        lastReceivedPacket = System.nanoTime();
        index = 0;
    }

    boolean readyForTranscription() {
        if(!awake) return false;

        // if no packet received in last 2 seconds & phraseBegun, then transcribe
        // if no packet received in last 5 seconds & !phraseBegun, then don't transcribe
        // if captioning, transcribe every 5 minutes anyway
        // if certain limit reached (15 seconds), transcribe
        // future: trailing audio volume is 30% or something less than the average of the whole

        long elapsedMs = (System.nanoTime() - lastReceivedPacket) / 1_000_000;

        if(phraseBegun && elapsedMs >= config.postPhraseTimeout) {
            return true;
        } else if(!phraseBegun && elapsedMs >= config.postWakeLimit) {
            sleep();
            return false; // user never started talking after waking bot
        } else if(phraseBegun && VocalCord.getConfig().captioning && phrase.length > 3840 * 50 * VocalCord.getConfig().captioningChunkSize) {
            return true;
        } else if(phrase.length > 3840 * 50 * config.maxPhraseTime) {
            return true;
        }

        return false;
    }

    boolean shouldDestroy() {
        double elapsedMinutes = (System.nanoTime() - lastAudioReceived) / 1_000_000_000.0 / 60.0;

        return !awake && elapsedMinutes > 3840 * 50 * config.userStreamLife;
    }

    public void sleep() {
        awake = false;
    }

    byte[] getAudioForGoogle() {

        try {
            AudioFormat target = new AudioFormat(16000f, 16, 1, true, false);
            AudioInputStream is = AudioSystem.getAudioInputStream(target,
                    new AudioInputStream(new ByteArrayInputStream(phrase), AudioReceiveHandler.OUTPUT_FORMAT,
                            phrase.length));

            return is.readAllBytes();
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    void destroy() {
        porcupine.delete();
    }

    public User getUser() {
        return user;
    }
}
