package com.cpjd.audio;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import io.grpc.internal.IoUtils;
import net.dv8tion.jda.core.audio.AudioReceiveHandler;
import net.dv8tion.jda.core.audio.CombinedAudio;
import net.dv8tion.jda.core.audio.UserAudio;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

/**
 * SpeechRecognitionReceiver is an implementation of AudioReceiverHandler in the JDA
 * library. It will listen into a particular Discord voice channel and run speech
 * recognition on the received audio.
 *
 * @version 1
 * @author Will Davies
 */
public class SpeechRecognitionReceiver implements AudioReceiveHandler {

    /**
     * The bot will not process audio as a command until it hears it's name. Specify it's name here, make sure it's something that can be easily recognized
     */
    private String botName;

    /**
     * This stores the current audio sample, this is the raw audio data that the bot will be processing.
     */
    private byte[] pcm;

    /**
     * The size of chunks, in seconds, to analyze for the keyword
     */
    private static final int KEYWORD_SAMPLE_SIZE = 3;

    private boolean listening;

    private StreamSpeechRecognizer recognizer;

    /**
     * Creates a SpeechRecognitionReceiver, this class will listen to audio in a Discord
     * channel and run speech recognition on it.
     * @param botName The bot will not process audio as a command until it hears it's name. Specify it's name here, make sure it's something that can be easily recognized
     */
    public SpeechRecognitionReceiver(String botName) {
        this.botName = botName;

        Configuration configuration = new Configuration();

        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
        configuration.setGrammarPath("file:src");
        configuration.setGrammarName("hello");
        configuration.setUseGrammar(true);

        try {
            recognizer = new StreamSpeechRecognizer(configuration);
        } catch(Exception e) {
            e.printStackTrace();
        }

        Logger cmRootLogger = Logger.getLogger("default.config");
        cmRootLogger.setLevel(java.util.logging.Level.OFF);
        String conFile = System.getProperty("java.util.logging.config.file");
        if (conFile == null) {
            System.setProperty("java.util.logging.config.file", "ignoreAllSphinx4LoggingOutput");
        }
    }

    @Override
    public boolean canReceiveCombined() {
        return true;
    }

    @Override
    public boolean canReceiveUser() {
        return false;
    }

    @Override
    public void handleCombinedAudio(CombinedAudio combinedAudio) {
        byte[] received = combinedAudio.getAudioData(1.0);
        if(pcm == null) pcm = new byte[received.length];
        else {
            byte[] newPacket = new byte[pcm.length + received.length];
            System.arraycopy(pcm, 0, newPacket, 0, pcm.length);
            System.arraycopy(received, 0, newPacket, pcm.length, received.length);
            pcm = newPacket;

            /*
             * If the bot is listening for commands, detect if the most recent packet is mostly silence,
             * if so, process the command.
             */
        }

        if(!listening && pcm.length >= 192000 * KEYWORD_SAMPLE_SIZE) {
            AudioFormat target = new AudioFormat(16000f, 16, 1, true, false);
            AudioInputStream is = AudioSystem.getAudioInputStream(target, new AudioInputStream(new ByteArrayInputStream(pcm), AudioReceiveHandler.OUTPUT_FORMAT, pcm.length));

            try {
                AudioSystem.write(is, AudioFileFormat.Type.WAVE, new File("C:\\Users\\Will Davies\\Downloads\\filename.wav"));
                byte[] data = IoUtils.toByteArray(new FileInputStream(new File("C:\\Users\\Will Davies\\Downloads\\filename.wav")));

                System.out.println("Was wakeup phrase said: "+wasWakeupSaid(data));

            } catch(Exception e) {
                e.printStackTrace();
            }

            pcm = null;
        }
    }

    public static String speechRecognition(byte[] pcm) throws Exception {
        try (SpeechClient speech = SpeechClient.create()) {
            ByteString audioBytes = ByteString.copyFrom(pcm);

            // Configure request with local raw PCM audio
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setLanguageCode("en-US")
                    .setSampleRateHertz(16000)
                    .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();

            // Use blocking call to get audio transcript
            RecognizeResponse response = speech.recognize(config, audio);
            List<SpeechRecognitionResult> results = response.getResultsList();

            return results.get(0).getAlternativesList().get(0).getTranscript();
        }
    }

    private boolean wasWakeupSaid(byte[] pcm) throws Exception {
        recognizer.startRecognition(new ByteArrayInputStream(pcm));
        SpeechResult result;
        while ((result = recognizer.getResult()) != null) {
            System.out.format("Hypothesis: %s\n", result.getHypothesis());
            if(result.getHypothesis().toLowerCase().contains(botName.toLowerCase())) return true;
        }
        recognizer.stopRecognition();
        return false;
    }

    @Override
    public void handleUserAudio(UserAudio userAudio) {

    }
}
