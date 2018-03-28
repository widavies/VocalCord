package com.cpjd.speechRecognition;

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
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * SpeechReceiver is an implementation of AudioReceiveHandler in the JDA library. This
 * handler will receive raw voice data from Discord in the form of PCM (pulse code modulation).
 * This data is scanned in small chunks for the wakeup phrase, if detected, the class will start listening
 * to speechRecognition input until it detects silence. The command will then be recognized and returned as a String via
 * the attached interface.
 *
 * The wakeup phrase is processed by CMUSphinx, a free, open source voice processing library. Since wakeup phrases are checked continuously,
 * this makes Sphinx a good choice because it's free. However, CMUSphinx has poor recognition accuracy, so actual commands are processed by
 * the Google Cloud Speech API, which costs a little bit if the user goes over 60 minutes worth of commands for the month.
 *
 * @version 1.0.0
 * @author Will Davies
 */
public class SpeechReceiver implements AudioReceiveHandler {
    /*
     *
     * Configuration settings
     * Use the construction to set these or use setters
     *
     */
    /**
     * This is an object reference to the callback interface that should receive events occurring in here.
     */
    private SpeechCallback callback;

    /**
     * The bot defaults to "sleeping" mode, during sleeping mode, the bot will analyze small chunks of sound data and listen for
     * the wakeup phrases defined below. If the 3 second chunk contains a wakeup phrase, the bot will "wake up" and start listening to the
     * user until the user stops talking, and then process dialog as a voice command.
     *
     * As you may notice, multiple wakeup phrases are supported. Use simple wakeup phrases that are voice recognition friendly.
     */
    private ArrayList<String> wakeupPhrases;

    /**
     * The bot can listen in two different ways, to collective channel speechRecognition (everybody's voices combined) or to a single user at a time.
     * If this option is false, only one user can use the bot at a time, and even if someone else interrupts them, the bot won't here it.
     * If true, other users can interrupt or add onto a command, which may be better for meme purposes.
     */
    private boolean combinedAudio;

    /**
     * If true, the bot will play it's own wakeup "boop" noise. You can disable this an implement your own if you'd like.
     * If you implement your own, you'll have to use JDA technologies or use the SpeechSender utility in VocalCord.
     */
    private boolean playBoop;

    /**
     * Wakeup sample size (seconds). This is the number of seconds to include in wakeup phrase speechRecognition chunk recognition.
     */
    private int wakeupChunkSize = 3;
    /*
     *
     * End configuration settings
     *
     */

    /*
     *
     * Internal parameters
     * You shouldn't need to edit these!
     *
     */

    /*
     * This stores the location of VocalCord's cache directory. This is where speechRecognition files will live and be processed from.
     * The cache will get auto-created on startup.
     * The files contained here are:
     * -audio.wav - audio chunk for speech recognition
     * -phrases.gram - a special file for helping CMUSphinx recognize the keyword better
     */
    private File cache;

    /*
     * Discord returns speechRecognition in 20 millisecond chunks, this array allows VocalCord to store multiple chunks for speech recognition
     */
    private byte[] pcm;

    /*
     * If true, the bot is awake and listening to a voice command.
     */
    private boolean awake;

    /*
     * If the combinedAudio option is false, that means that VocalCord can only listen to one user at a time.
     * If awake==true && combinedAudio==false, then only User can add speechRecognition data to pcm
     */
    private User user;

    /*
     * This is the CMUSphinx API, this is how wakeup phrases will be listened to. Important note:
     * CMUSphinx is shit. It's barely accurate. So, a custom grammar file is used so that the bot will only
     * know of the existence of the a language with only <wakeup phrase> amount of words in it. This means wakeup
     * phrases are much more likely to be detected. However, a grammar file has to be written and CMUSphinx reconfigured
     * whenever the user adds a wakeup phrase. No biggy.
     */
    private StreamSpeechRecognizer wakeupPhraseRecongizer;

    /**
     * Creates a SpeechReceiver object. This will initialize required directories and configure voice recognition
     * methods.
     * @param wakeupPhrase A wakeup phrase for the bot to wakeup to, you can add more with addWakeupPhrase(String).
     * @param callback The callback that will receive events occurring in this class.
     */
    public SpeechReceiver(String wakeupPhrase, SpeechCallback callback) {
        this.wakeupPhrases = new ArrayList<>();
        addWakeupPhrase(wakeupPhrase);
        this.callback = callback;

        // Create the cache directory, and d
        String osName = System.getProperty("os.name").toLowerCase();
        if(osName.contains("win")) {
            cache = new File((System.getenv("APPDATA") + File.separator + "Hidden" + File.separator));
        } else if(osName.contains("mac")) {
            cache = new File(System.getProperty("user.home") + "/Library/Application Support/Hidden"+File.separator);
        } else if(osName.contains("nux")) {
            cache = new File(System.getProperty("user.home"));
        }

        if(!cache.exists()) cache.mkdirs();

        Logger cmRootLogger = Logger.getLogger("default.config");
        cmRootLogger.setLevel(java.util.logging.Level.OFF);
        String conFile = System.getProperty("java.util.logging.config.file");
        if(conFile == null) {
            System.setProperty("java.util.logging.config.file", "ignoreAllSphinx4LoggingOutput");
        }
    }

    /**
     * Adds a wakeup phrase to bot's wakeup list. If the bot detects a wakeup phrase, the bot
     * will "wake up" and start processing as a voice command.
     * @param phrase The phrase the bot should wakeup to, make sure this is clear and easily recognizable by voice recognition software
     */
    public void addWakeupPhrase(String phrase) {
        // Add the phrase
        wakeupPhrases.add(phrase);

        // Create the custom wakeup grammar phrase file
        String[] lines = new String[4];
        lines[0] = "$JSGF V1.0;";
        lines[1] = "\n";
        lines[2] = "grammar wakeupPhrases;";
        StringBuilder builder = new StringBuilder("public <command> = (");
        for(int i = 0; i < wakeupPhrases.size(); i++) {
            builder.append(wakeupPhrases.get(i));
            String separator = (i == wakeupPhrases.size() - 1) ? ");" : " | ";
            builder.append(separator);
        }
        lines[3] = builder.toString();

        // Write the phrases.gram file for CMUSphinx to read
        File phrases = new File(cache+File.separator+"phrases.gram");
        try {
            if(!phrases.exists()) phrases.createNewFile();
            FileOutputStream fos = new FileOutputStream(phrases);
            PrintWriter writer = new PrintWriter(fos);
            for(String line : lines) writer.println(line);
        } catch(IOException e) {
            System.err.println("An error occurred while writing CMUSphinx grammar file. Err: "+e.getMessage());
        }

        // Reconfigure CMUSphinx with the new grammar file
        configureSphinx();
    }

    /**
     * @param combinedAudio {@link SpeechReceiver#combinedAudio}
     */
    public void setCombinedAudio(boolean combinedAudio) {
        this.combinedAudio = combinedAudio;
    }

    /**
     * @param wakeupChunkSize {@link SpeechReceiver#wakeupChunkSize}
     */
    public void setWakeupChunkSize(int wakeupChunkSize) {
        if(wakeupChunkSize < 0 || wakeupChunkSize > 10) return;
        this.wakeupChunkSize = wakeupChunkSize;

    }

    /**
     * @param playBoop {@link SpeechReceiver#playBoop}
     */
    public void setPlayBoop(boolean playBoop) {
        this.playBoop = playBoop;
    }

    /**
     * @param callback {@link SpeechReceiver#callback}
     */
    public void setSpeechCallback(SpeechCallback callback) {
        this.callback = callback;
    }

    /*
     * Reloads CMUSphinx, this will also reload the wakeup phrases file.
     */
    private void configureSphinx()  {
        Configuration configuration = new Configuration();

        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
        configuration.setGrammarPath(cache+File.separator+"phrases.gram");
        configuration.setGrammarName("wakeupPhrases");
        configuration.setUseGrammar(true);

        try {
            recognizer = new StreamSpeechRecognizer(configuration);
        } catch(Exception e) {
            System.err.println("Failed to configure CMUSphinx. Err: "+e.getMessage());
        }
    }

    /*
     * Processes the audio as wakeup phrase or command, and runs a few other checks on it (user verification, volume stop, etc)
     */
    private void processAudio(User user, byte[] receivedChunk) {
        concatenateChunk(receivedChunk);

        /*
         * The bot will process a command when 3 full seconds of silence have occurred.
         * Check the end of the byte array for this
         */
        if(awake && volumeRMS(receivedChunk) <= 0.20) { //TODO fix this condition && verify the same user
            if(callback != null) callback.commandReceived(speechRecognition(convertPCM()));
            awake = false;
            pcm = null;
        }
        else if(!awake && pcm.length >= 192000 * wakeupChunkSize) {
            if(wasWakeupSaid(convertPCM()) && callback != null && callback.botAwakeRequest(user)) {
                awake = true;
                if(!combinedAudio) {
                    user = user; // etc
                }
            }
            pcm = null;
        }
    }

    /*
     * Converts the current pcm array to a speechRecognition format compatible with the speech recognition APIs
     */
    private byte[] convertPCM() {
        try {
            AudioFormat target = new AudioFormat(16000f, 16, 1, true, false);
            AudioInputStream is = AudioSystem.getAudioInputStream(target, new AudioInputStream(new ByteArrayInputStream(pcm), AudioReceiveHandler.OUTPUT_FORMAT, pcm.length));
            AudioSystem.write(is, AudioFileFormat.Type.WAVE, new File(cache + File.separator + "audio.wav"));
            return IoUtils.toByteArray(new FileInputStream(new File(cache + File.separator + "audio.wav")));;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * Adds a 20 millisecond chunk of speechRecognition to the pcm byte[] array so speechRecognition can be processed in bigger chunks
     */
    private void concatenateChunk(byte[] chunk) {
        if(pcm == null) {
            pcm = new byte[chunk.length];
            return;
        }
        byte[] newPacket = new byte[pcm.length + chunk.length];
        System.arraycopy(pcm, 0, newPacket, 0, pcm.length);
        System.arraycopy(chunk, 0, newPacket, pcm.length, chunk.length);
        pcm = newPacket;
    }

    /*
     * Runs audio data through Google's Speech API and returns its top transcription prediction
     */
    public static String speechRecognition(byte[] pcm) {
        try (SpeechClient speech = SpeechClient.create()) {
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

            return results.get(0).getAlternativesList().get(0).getTranscript();
        }
    }

    /*
     * Runs audio data through CMUSphinx and searches the transcription for wakeup phrases.
     * This should not be used for command recognition because it's really inaccurate.
     *
     * Returns true if the audio data contained any of the wakeup phrases
     */
    private boolean wasWakeupSaid(byte[] pcm) throws Exception {
        recognizer.startRecognition(new ByteArrayInputStream(pcm));
        SpeechResult result;
        while ((result = recognizer.getResult()) != null) {
            for(String phrase : wakeupPhrases) {
                if(result.getHypothesis().toLowerCase().contains(phrase.toLowerCase())) return true;
            }
        }
        recognizer.stopRecognition();
        return false;
    }

    /*
     * Calculates the average volume (between -1 and 1) of the raw PCM.
     * This is used for detecting when the bot should stop listening for a voice command.
     */
    public double volumeRMS(byte[] raw) {
        double sum = 0d;
        if (raw.length==0) {
            return sum;
        } else {
            for (int ii=0; ii<raw.length; ii++) {
                sum += raw[ii];
            }
        }
        double average = sum/raw.length;

        double sumMeanSquare = 0d;
        for (int ii=0; ii<raw.length; ii++) {
            sumMeanSquare += Math.pow(raw[ii]-average,2d);
        }
        double averageMeanSquare = sumMeanSquare/raw.length;
        double rootMeanSquare = Math.sqrt(averageMeanSquare);

        return rootMeanSquare;
    }

    @Override
    public void handleCombinedAudio(CombinedAudio combinedAudio) {
        if(combinedAudio) processAudio(user, getAudioData(1.0));
    }

    @Override
    public void handleUserAudio(UserAudio userAudio) {
        if(!combinedAudio) processAudio(user, getAudioData(1.0));
    }

    @Override
    public boolean canReceiveCombined() {
        return combinedAudio;
    }

    @Override
    public boolean canReceiveUser() {
        return !combinedAudio;
    }
}
