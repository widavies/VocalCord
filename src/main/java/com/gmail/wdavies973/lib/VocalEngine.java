package com.gmail.wdavies973.lib;

import com.google.cloud.speech.v1.*;
import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.UserAudio;
import org.apache.commons.io.IOUtils;
import org.conscrypt.io.IoUtils;
import wakeup.Porcupine;

import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/*
 * -
 */

public class VocalEngine implements AudioReceiveHandler {

    private VocalCord cord;

    // Wake word detection library
    private Porcupine porcupine;

    private boolean collecting;

    private byte[] phrase;

    private boolean beginThreshold;
    private int startTimeout = 5;
    private int assTimeout = 2;
    private int maxLength = 30;

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

    BlockingQueue<byte[]> stream = new LinkedBlockingQueue<>(50 * 15);

    Thread t;

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
        if(!userAudio.getUser().getName().contains("tech")) return;

        if(collecting) {
            stream.add(userAudio.getAudioData(1));

            return;
        }

        try {
            PorcupineAdapter pa = new PorcupineAdapter(userAudio.getAudioData(1));
            while(pa.hasNext()) {
                if(porcupine.processFrame(pa.take())) {
                    System.out.println("WAKE WORD DETECTED!!!!!!!");

                    new Thread() {
                        byte[] pcm;

                        @Override
                        public void run() {
                            while(true) {
                                try {
                                    byte[] data = stream.poll(beginThreshold ? 800 : 4000, TimeUnit.MILLISECONDS);

                                    if(data == null) {
                                        if(!beginThreshold) {
                                            System.out.println("User didn't speak fast enough");
                                        } else {
                                            System.out.println("Audio is ready");

                                            // Send to google
                                            /*
                                             * Step 1: Convert byte array to shorts + LittleEndian
                                             */
//                                            byte[] raw = new byte[pcm.length];
//
//                                            for(int i = 0, j = 0; i < pcm.length; i += 2, j++) {
//                                                raw[j] = pcm[i+1];
//                                                raw[j+1] = pcm[i];
//                                            }
//
//                                            byte[] f = new byte[raw.length / 6];
//                                            for(int i = 0, j = 0; i < raw.length; i += 6, j++) {
//                                                f[j] = raw[i];
//                                            }
                                            try {
                                                AudioFormat target = new AudioFormat(16000f, 16, 1, true, false);
                                                AudioInputStream is = AudioSystem.getAudioInputStream(target, new AudioInputStream(new ByteArrayInputStream(pcm), AudioReceiveHandler.OUTPUT_FORMAT, pcm.length));
                                                AudioSystem.write(is, AudioFileFormat.Type.WAVE, new File("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/audio.wav"));
                                                cord.onTranscribed(null, speechRecognition(IOUtils.toByteArray(new FileInputStream(new File("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/audio.wav")))));
                                            } catch(Exception e) {
                                                e.printStackTrace();
                                                System.out.println("Failed to convert!");
                                            }

                                            reset();
                                            join();
                                        }
                                    } else {
                                        if(!beginThreshold) {
                                            beginThreshold = true;
                                            System.out.println("Threshold set");
                                        }

                                        // Concatenate
                                        if(pcm == null) pcm = data;
                                        else {
                                            byte[] newPacket = new byte[pcm.length + data.length];
                                            System.arraycopy(pcm, 0, newPacket, 0, pcm.length);
                                            System.arraycopy(data, 0, newPacket, pcm.length, data.length);
                                            pcm = newPacket;
                                        }
                                    }
                                } catch(Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                    }.start();

                    // Start collecting audio for Google recognition
                    collecting = true;
                    return;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private String speechRecognition(byte[] pcm) {
        System.out.println("Accessing Google Cloud Speech API.");

        try(SpeechClient speech = SpeechClient.create()) {
            ByteString audioBytes = ByteString.copyFrom(pcm);

            // Configure request with local raw PCM speechRecognition
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setLanguageCode("en-US")
                    .setSampleRateHertz(16000)
                    .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();

            // Use blocking call to get speechRecognition transcript
            RecognizeResponse response = speech.recognize(config, audio);
            List<SpeechRecognitionResult> results = response.getResultsList();

            System.out.println(response.getResultsCount());

            return results.get(0).getAlternativesList().get(0).getTranscript();
        } catch(Exception e) {
            e.printStackTrace();
            System.err.println("Failed to run Google Cloud speech recognition. Err: " + e.getMessage());
        }
        return "";
    }

    private double volumeRMS(byte[] raw) { // needs more testing
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

    private void reset() {
        collecting = false;
        phrase = null;
        beginThreshold = false;
    }


}
