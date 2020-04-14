//package com.gmail.wdavies973.lib;
//
//import com.google.cloud.speech.v1.*;
//import com.google.protobuf.ByteString;
//import net.dv8tion.jda.api.audio.AudioReceiveHandler;
//
//import javax.sound.sampled.AudioFileFormat;
//import javax.sound.sampled.AudioFormat;
//import javax.sound.sampled.AudioInputStream;
//import javax.sound.sampled.AudioSystem;
//import java.io.*;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.logging.Logger;
//
///**
// * SpeechReceiver is an implementation of {@link AudioReceiveHandler} in the JDA library. This
// * handler will receive raw voice data from Discord in the form of PCM (pulse code modulation).
// * This data is scanned in small chunks for the wakeup phrase, if detected, the class will start listening
// * to speechRecognition input until it detects silence. The command will then be recognized and returned as a String via
// * the attached interface.
// *
// * The wakeup phrase is processed by CMUSphinx, a free, open source voice processing library. Since wakeup phrases are checked continuously,
// * this makes Sphinx a good choice because it's free. However, CMUSphinx has poor recognition accuracy, so actual commands are processed by
// * the Google Cloud Speech API, which costs a little bit if the user goes over 60 minutes worth of commands for the month.
// *
// * @version 1.0.0
// * @author Will Davies
// */
//public class VocalCordEngine implements AudioReceiveHandler {
//    /*
//     *
//     * Configuration settings
//     * Use the constructor to set these or use setters
//     *
//     */
//    /**
//     * This is an object reference to the callback interface that should receive events occurring in here.
//     */
//    private VocalCord callback;
//
//    /**
//     * The bot defaults to "sleeping" mode, during sleeping mode, the bot will analyze small chunks of sound data and listen for
//     * the wakeup phrases defined below. If the 3 second chunk contains a wakeup phrase, the bot will "wake up" and start listening to the
//     * user until the user stops talking, and then process dialog as a voice command.
//     *
//     * As you may notice, multiple wakeup phrases are supported. Use simple wakeup phrases that are voice recognition friendly.
//     */
//    private ArrayList<String> wakeupPhrases;
//
//    /**
//     * The bot can listen in two different ways, to collective channel speechRecognition (everybody's voices combined) or to a single user at a time.
//     * If this option is false, only one user can use the bot at a time, and even if someone else interrupts them, the bot won't here it.
//     * If true, other users can interrupt or add onto a command, which may be better for meme purposes.
//     */
//    private boolean combinedAudio;
//
//    /**
//     * Wakeup sample size (seconds). This is the number of seconds to include in wakeup phrase speechRecognition chunk recognition.
//     */
//    private int wakeupChunkSize = 3;
//
//    /**
//     * This is the amount of seconds of silence (not perfect, just close quiet) that must occur  after a voice command to put the bot to sleep.
//     * After this timeout is reached, the voice command will be processed, and the bot put to sleep.
//     */
//    private int voiceCommandTimeout = 3;
//    /*
//     *
//     * End configuration settings
//     *
//     */
//
//    /*
//     *
//     * Internal variables
//     * You shouldn't need to edit these!
//     *
//     */
//
//    /*
//     * This stores the location of VocalCord's cache directory. This is where speechRecognition files will live and be processed from.
//     * The cache will get auto-created on startup.
//     * The files contained here are:
//     * -audio.wav - audio chunk for speech recognition
//     * -phrases.gram - a special file for helping CMUSphinx recognize the keyword better
//     */
//    private File cache;
//
//    /*
//     * Discord returns speechRecognition in 20 millisecond chunks, this array allows VocalCord to store multiple chunks for speech recognition
//     */
//    private byte[] pcm;
//
//    /*
//     * If true, the bot is awake and listening to a voice command.
//     */
//    private boolean awake;
//
//    /*
//     * If the combinedAudio option is false, that means that VocalCord can only listen to one user at a time.
//     * If awake==true && combinedAudio==false, then only User can add speechRecognition data to pcm
//     */
//    private User user;
//
//    /*
//     * This is the CMUSphinx API, this is how wakeup phrases will be listened to. Important note:
//     * CMUSphinx is shit. It's barely accurate. So, a custom grammar file is used so that the bot will only
//     * know of the existence of the a language with only <wakeup phrase> amount of words in it. This means wakeup
//     * phrases are much more likely to be detected. However, a grammar file has to be written and CMUSphinx reconfigured
//     * whenever the user adds a wakeup phrase. No biggy.
//     */
//    private StreamSpeechRecognizer wakeupPhraseRecognizer;
//
//    /*
//     * This is the length of the byte array of pcm containing 1 full second of audio, used for
//     * timeout / wakeup audio chunk sizes
//     */
//    private static final int BYTES_PER_SECOND = 192000;
//
//    /**
//     * Creates a SpeechReceiver object. This will initialize required directories and configure voice recognition
//     * methods.
//     * @param wakeupPhrase A wakeup phrase for the bot to wakeup to, you can add more with addWakeupPhrase(String).
//     * @param callback The callback that will receive events occurring in this class.
//     */
//    public VocalCordEngine(String wakeupPhrase, VocalCord callback) {
//        this.wakeupPhrases = new ArrayList<>();
//        this.callback = callback;
//
//        // Create the cache directory, and d
//        String osName = System.getProperty("os.name").toLowerCase();
//        if(osName.contains("win")) {
//            cache = new File((System.getenv("APPDATA") + File.separator + "VocalCord" + File.separator));
//        } else if(osName.contains("mac")) {
//            cache = new File(System.getProperty("user.home") + "/Library/Application Support/VocalCord"+File.separator);
//        } else if(osName.contains("nux")) {
//            cache = new File(System.getProperty("user.home"));
//        }
//
//        if(!cache.exists()) {
//            if(!cache.mkdirs() && !cache.mkdir()) System.err.println("Failed to create cache directory.");
//        }
//
//        addWakeupPhrase(wakeupPhrase);
//
//        configureSphinx();
//
//        Logger cmRootLogger = Logger.getLogger("default.config");
//        cmRootLogger.setLevel(java.util.logging.Level.OFF);
//        String conFile = System.getProperty("java.util.logging.config.file");
//        if(conFile == null) {
//            System.setProperty("java.util.logging.config.file", "ignoreAllSphinx4LoggingOutput");
//        }
//    }
//
//    /**
//     * Adds a wakeup phrase to bot's wakeup list. If the bot detects a wakeup phrase, the bot
//     * will "wake up" and start processing as a voice command.
//     * @param phrase The phrase the bot should wakeup to, make sure this is clear and easily recognizable by voice recognition software
//     */
//    public void addWakeupPhrase(String phrase) {
//        // Add the phrase
//        wakeupPhrases.add(phrase);
//
//        // Create the custom wakeup grammar phrase file
//        String[] lines = new String[4];
//        lines[0] = "#JSGF V1.0;";
//        lines[1] = "";
//        lines[2] = "grammar phrases;";
//        StringBuilder builder = new StringBuilder("public <command> = (");
//        /*
//         * This is a bit of a hack, but since we are using a grammar file, we need several more words in our
//         * "make shift" language so the wakeup phrases don't get said too much
//         */
//        builder.append("apple | car | dice | much | too | how | today | after | before | ");
//        for(int i = 0; i < wakeupPhrases.size(); i++) {
//            builder.append(wakeupPhrases.get(i));
//            String separator = (i == wakeupPhrases.size() - 1) ? ");" : " | ";
//            builder.append(separator);
//        }
//        lines[3] = builder.toString();
//
//        // Write the phrases.gram file for CMUSphinx to read
//        File phrases = new File(cache+File.separator+"phrases.gram");
//        try {
//            if(!phrases.exists()) {
//                if(!phrases.createNewFile()) System.err.println("Failed to create phrases.gram file.");
//            }
//            FileWriter writer = new FileWriter(phrases);
//            for(String line : lines) writer.write(line+"\n");
//            writer.close();
//        } catch(IOException e) {
//            System.err.println("An error occurred while writing CMUSphinx grammar file. Err: "+e.getMessage());
//        }
//
//        // Reconfigure CMUSphinx with the new grammar file
//        configureSphinx();
//    }
//
//    /**
//     * @param combinedAudio {@link VocalCordEngine#combinedAudio}
//     */
//    public void setCombinedAudio(boolean combinedAudio) {
//        this.combinedAudio = combinedAudio;
//    }
//
//    /**
//     * @param wakeupChunkSize {@link VocalCordEngine#wakeupChunkSize}
//     */
//    public void setWakeupChunkSize(int wakeupChunkSize) {
//        if(wakeupChunkSize < 0 || wakeupChunkSize > 10) return;
//        this.wakeupChunkSize = wakeupChunkSize;
//
//    }
//
//    /**
//     * @param callback {@link VocalCordEngine#callback}
//     */
//    public void setSpeechCallback(VocalCord callback) {
//        this.callback = callback;
//    }
//
//    /**
//     * @param voiceCommandTimeout {@link VocalCordEngine#voiceCommandTimeout}
//     */
//    public void setVoiceCommandTimeout(int voiceCommandTimeout) {
//        this.voiceCommandTimeout = voiceCommandTimeout;
//    }
//
//    /*
//     * Processes the audio as wakeup phrase or command, and runs a few other checks on it (user verification, volume stop, etc)
//     */
//    private void processAudio(byte[] receivedChunk, User ... users) {
//        concatenateChunk(receivedChunk);
//
//        /*
//         * This code is important, it handles voice commands. It needs to do two things:
//         *
//         * -If combinedAudio==false (bot only listens to one user), make sure that only that user gets to add to this array
//         * -Once 2 full seconds of silence have passed, put that bot back to sleep and listen for the commands
//         * -Ten second cap on voice commands
//         */
//        if(awake && (quietChunkDetected() || pcm.length >= BYTES_PER_SECOND * 10) && (combinedAudio || this.user.getId().equals(users[0].getId()))) {
//            if(callback != null) callback.onTranscribed(speechRecognition(convertPCM()));
//            awake = false;
//            pcm = null;
//            this.user = null;
//            configureSphinx(); // for some reason, sphinx needs to be reconfigured
//        }
//        else if(!awake && pcm.length >= BYTES_PER_SECOND * wakeupChunkSize) {
//            try {
//                if(callback != null && wasWakeupSaid(convertPCM()) && callback.isAuthorizedUser(users)) {
//                    awake = true;
//                    if(!combinedAudio) {
//                        this.user = users[0];
//                    }
//                }
//            } catch(Exception e) {
//                e.printStackTrace();
//            } finally {
//                pcm = null;
//            }
//        }
//    }
//
//    private boolean quietChunkDetected() {
//        int chunkSize = BYTES_PER_SECOND * voiceCommandTimeout;
//        if(pcm == null || chunkSize >= pcm.length) return false;
//
//        // Get the end of the array
//        byte[] temp = new byte[chunkSize];
//        System.arraycopy(pcm, pcm.length - chunkSize - 1, temp, 0, chunkSize);
//        return volumeRMS(temp) <= 15; // the audio volume threshold considered silence (this is arbitrary)
//    }
//
//    /*
//     * Converts the current pcm array to a speechRecognition format compatible with the speech recognition APIs
//     */
//    private byte[] convertPCM() {
//        try {
//            AudioFormat target = new AudioFormat(16000f, 16, 1, true, false);
//            AudioInputStream is = AudioSystem.getAudioInputStream(target, new AudioInputStream(new ByteArrayInputStream(pcm), AudioReceiveHandler.OUTPUT_FORMAT, pcm.length));
//            AudioSystem.write(is, AudioFileFormat.Type.WAVE, new File(cache + File.separator + "audio.wav"));
//            return IoUtils.toByteArray(new FileInputStream(new File(cache + File.separator + "audio.wav")));
//        } catch(Exception e) {
//            System.out.println("Failed to convert!");
//        }
//        return null;
//    }
//
//    /*
//     * Adds a 20 millisecond chunk of speechRecognition to the pcm byte[] array so speechRecognition can be processed in bigger chunks
//     */
//    private void concatenateChunk(byte[] chunk) {
//        if(pcm == null) pcm = chunk;
//        else {
//            byte[] newPacket = new byte[pcm.length + chunk.length];
//            System.arraycopy(pcm, 0, newPacket, 0, pcm.length);
//            System.arraycopy(chunk, 0, newPacket, pcm.length, chunk.length);
//            pcm = newPacket;
//        }
//    }
//
//    /*
//     * Runs audio data through Google's Speech API and returns its top transcription prediction
//     */
//    private String speechRecognition(byte[] pcm) {
//        System.out.println("Accessing Google Cloud Speech API.");
//
//        try (SpeechClient speech = SpeechClient.create()) {
//            ByteString audioBytes = ByteString.copyFrom(pcm);
//
//            // Configure request with local raw PCM speechRecognition
//            RecognitionConfig config = RecognitionConfig.newBuilder()
//                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
//                    .setLanguageCode("en-US")
//                    .setSampleRateHertz(16000)
//                    .build();
//            RecognitionAudio audio = RecognitionAudio.newBuilder()
//                    .setContent(audioBytes)
//                    .build();
//
//            // Use blocking call to get speechRecognition transcript
//            RecognizeResponse response = speech.recognize(config, audio);
//            List<SpeechRecognitionResult> results = response.getResultsList();
//
//            return results.get(0).getAlternativesList().get(0).getTranscript();
//        } catch(Exception e) {
//            System.err.println("Failed to run Google Cloud speech recognition. Err: "+e.getMessage());
//        }
//        return "";
//    }
//
//    /*
//     * Runs audio data through CMUSphinx and searches the transcription for wakeup phrases.
//     * This should not be used for command recognition because it's really inaccurate.
//     *
//     * Returns true if the audio data contained any of the wakeup phrases
//     */
//    private boolean wasWakeupSaid(byte[] pcm) {
//        System.out.println("Sleeping...");
//
//        wakeupPhraseRecognizer.startRecognition(new ByteArrayInputStream(pcm));
//        SpeechResult result;
//        while ((result = wakeupPhraseRecognizer.getResult()) != null) {
//            System.out.println(result.getHypothesis());
//            for(String phrase : wakeupPhrases) {
//                if(result.getHypothesis().toLowerCase().contains(phrase.toLowerCase())) {
//                    wakeupPhraseRecognizer.stopRecognition();
//                    return true;
//                }
//            }
//        }
//        wakeupPhraseRecognizer.stopRecognition();
//        return false;
//    }
//
//    /*
//     * Calculates the average volume of the raw PCM.
//     * This is used for detecting when the bot should stop listening for a voice command.
//     */
//    private double volumeRMS(byte[] raw) {
//        double sum = 0d;
//        if (raw.length==0) {
//            return sum;
//        } else {
//            for (byte aRaw : raw) {
//                sum += aRaw;
//            }
//        }
//        double average = sum/raw.length;
//
//        double sumMeanSquare = 0d;
//        for (byte aRaw : raw) {
//            sumMeanSquare += Math.pow(aRaw - average, 2d);
//        }
//        double averageMeanSquare = sumMeanSquare/raw.length;
//        return Math.sqrt(averageMeanSquare);
//    }
//
//    @Override
//    public void handleCombinedAudio(CombinedAudio combinedAudio) {
//        if(this.combinedAudio) processAudio(combinedAudio.getAudioData(1.0), combinedAudio.getUsers().toArray(new User[combinedAudio.getUsers().size()]));
//    }
//
//    @Override
//    public void handleUserAudio(UserAudio userAudio) {
//        if(!combinedAudio) processAudio(userAudio.getAudioData(1.0), userAudio.getUser());
//    }
//
//    @Override
//    public boolean canReceiveCombined() {
//        return combinedAudio;
//    }
//
//    @Override
//    public boolean canReceiveUser() {
//        return !combinedAudio;
//    }
//
//    public VocalCord getCallback() {
//        return callback;
//    }
//
//    public void setCallback(VocalCord callback) {
//        this.callback = callback;
//    }
//
//    public ArrayList<String> getWakeupPhrases() {
//        return wakeupPhrases;
//    }
//
//    public void setWakeupPhrases(ArrayList<String> wakeupPhrases) {
//        this.wakeupPhrases = wakeupPhrases;
//    }
//
//    public boolean isCombinedAudio() {
//        return combinedAudio;
//    }
//
//    public int getWakeupChunkSize() {
//        return wakeupChunkSize;
//    }
//
//    public int getVoiceCommandTimeout() {
//        return voiceCommandTimeout;
//    }
//}
