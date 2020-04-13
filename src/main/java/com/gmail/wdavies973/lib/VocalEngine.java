package com.gmail.wdavies973.lib;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.UserAudio;
import wakeup.Porcupine;

import javax.annotation.Nonnull;

/*
 * -
 */

public class VocalEngine implements AudioReceiveHandler {

    private VocalCord cord;

    // Wake word detection library
    private Porcupine porcupine;

    private boolean collecting;

    private byte[] phrase;

    private int startTimeout = 5;
    private int assTimeout = 3;

    private static class PorcupineAdapter {

        private short[] pcm;
        private int index = 0;

        public PorcupineAdapter(byte[] discordPCM) {
            /*
             * Step 1: Convert byte array to shorts + LittleEndian
             */
            short[] raw = new short[discordPCM.length / 2];

            for(int i = 0, j = 0; i < discordPCM.length; i += 2, j++) {
                raw[j] = bytePairToShort(discordPCM[i], discordPCM[i + 1]);
            }

            // Do this by only keeping every 6th sample
            // should return as many 512 frames as it can
            pcm = new short[raw.length / 6];
            for(int i = 0, j = 0; i < raw.length; i += 6, j++) {
                pcm[j] = raw[i];
            }
        }

        public boolean hasNext() {
            return index < pcm.length;
        }

        public short[] take() {
            short[] frame = new short[512];

            for(int i = index; i < index + 512; i++) {
                if(i >= pcm.length) break;
                frame[i] = pcm[i];
            }

            index += 512;
            return frame;
        }

        // Combines two bytes into a short & converts to LittleEndian
        private short bytePairToShort(byte a, byte b) {
            return (short) ((a << 8) | (b & 0xFF));
        }
    }

    public VocalEngine(VocalCord cord) throws Exception {
        this.cord = cord;
        porcupine = new Porcupine("wake-engine/Porcupine/lib/common/porcupine_params.pv", "wake-engine/wake_phrase.ppn", 0.5f);

        if(porcupine.getFrameLength() != 512 || porcupine.getSampleRate() != 16000) {
            throw new RuntimeException("Porcupine data should be 512 length, 16000 Khz");
        }
    }

    @Override
    public boolean canReceiveUser() {
        return true;
    }

    @Override
    public void handleUserAudio(@Nonnull UserAudio userAudio) {
        System.out.println(userAudio.getAudioData(1)[0]);

        if(collecting) {
            // Start concatenating bytes
            concatenateChunk(userAudio.getAudioData(1));
            return;
        }

        try {
            PorcupineAdapter pa = new PorcupineAdapter(userAudio.getAudioData(1));
            while(pa.hasNext()) {
                if(porcupine.processFrame(pa.take())) {
                    System.out.println("WAKE WORD DETECTED!!!!!!!");

                    // Start collecting audio for Google recognition
                    collecting = true;
                    return;
                }
            }
            System.out.println("Yeemed");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private double volumeRMS(byte[] raw) {
        double sum = 0d;
        if(raw.length == 0) {
            return sum;
        } else {
            for(byte aRaw : raw) {
                sum += aRaw;
            }
        }
        double average = sum / raw.length;

        double sumMeanSquare = 0d;
        for(byte aRaw : raw) {
            sumMeanSquare += Math.pow(aRaw - average, 2d);
        }
        double averageMeanSquare = sumMeanSquare / raw.length;
        return Math.sqrt(averageMeanSquare);
    }

    private void concatenateChunk(byte[] chunk) {
        if(this.phrase == null) phrase = chunk;
        else {
            byte[] newPacket = new byte[phrase.length + chunk.length];
            System.arraycopy(phrase, 0, newPacket, 0, phrase.length);
            System.arraycopy(chunk, 0, newPacket, phrase.length, chunk.length);
            phrase = newPacket;
        }
    }

}
