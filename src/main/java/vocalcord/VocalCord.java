package vocalcord;

import com.google.cloud.texttospeech.v1beta1.SsmlVoiceGender;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

/**
 * This is the main class you will use to interact with VocalCord
 */
public class VocalCord {

    public interface Callbacks {
        /**
         * You should only use ONE onTranscribed(..) method. This callback is where you'll store all your voice commands.
         * This function is only called when onTranscribed() returns false, and will provide you the speaker user object
         * with a transcript of what they said. It is not recommended you use this callback
         * over the other for anything except the most simple bots.
         * @param user The user whose voice was transcribed into transcript
         * @param transcript A speech transcript of what the user said
         */
        default void onTranscribed(User user, String transcript) {
        }

        /**
         * You should only use ONE onTranscribed(..) method. This function is preferred.
         * This callback is where you'll store all your voice commands. Importantly, voice transcripts aren't always
         * 100% accurate. If you hard code a list of commands, being off by just one word wouldn't register the command,
         * or trying to use lots of String.contains(..) calls could easily intermix commands. This callback employs
         * CommandChain, which will generate document vectors and a document term matrix in order to compute the cosine
         * similarity between a candidate transcription. Essentially, CommandChain will automatically run an algorithm to
         * determine which command was most likely said. This means that a user doesn't have to be 100% accurate on matching a command,
         * and instead only needs to capture the meaning of a command.
         */
        default CommandChain onTranscribed() {
            return null;
        }

        /**
         * Checks whether the user has permission to use voice commands. This command is important because
         * every user VocalCord is using wake detection on is a drain on resources, returning true will
         * allow VocalCord to track the user's audio stream, but returning false will not track a user's audio
         * stream. Basically, only allow users to wake the bot that you trust, because Google Speech To Text can
         * cost money over a certain threshold, and universally returning "true" could make it easier for your
         * bot to get overloaded.
         * @param user The user object of a user who is talking in a voice channel
         * @return true if the user can use voice commands, false if they can't
         */
        boolean canWakeBot(User user);

        /**
         * This command is called when VocalCord detects a user has used a wake phrase.
         * @param userStream A wrapper around a User object, you can call sleep() to cancel voice recognition or getUser() to get the user
         * @param keywordIndex The index of the wake phrase that was used to wake the bot, this matches the order you provided wake phrase paths
         */
        void onWake(UserStream userStream, int keywordIndex);

        /**
         * Triggered when the bot has finished speaking the last phrase it was told to speak
         */
        default void onTTSCompleted() {}
    }

    private static Config CONFIG;

    public static Config newConfig(Callbacks callbacks) {
        CONFIG = new Config();
        CONFIG.callbacks = callbacks;
        return CONFIG;
    }

    public static Config getConfig() {
        return CONFIG;
    }

    public static class Config {
        public Callbacks callbacks;
        /*
         * Wake detection
         */
        public String jniLocation, porcupineLocation; // dynamic library locations
        public String porcupineParams;
        public String[] wakePhrasePaths;
        public float sensitivity;

        /*
         * TTS settings
         */
        String languageCode;
        boolean usingTTS, usingTTSCache;
        SsmlVoiceGender voiceGender;
        SendMultiplex sendMultiplex;

        /*
         * Phrase detection stuff
         */
        int postWakeLimit = 4000;
        int postPhraseTimeout = 600;
        int maxPhraseTime = 20;
        int userStreamLife = 15;

        public static class SendMultiplex {
            enum MultiplexMode {
                None,
                Switch,
                Blend;
            }

            MultiplexMode mode = MultiplexMode.None;
            AudioSendHandler[] handlers;
            float blendBalance;

            private SendMultiplex() {
            }

            public static SendMultiplex None() {
                return new SendMultiplex();
            }

            public static SendMultiplex Switch(AudioSendHandler sendHandler) {
                if(sendHandler == null) {
                    throw new RuntimeException("Send handler must not be null.");
                }
                SendMultiplex m = new SendMultiplex();
                m.handlers = new AudioSendHandler[1];
                m.handlers[0] = sendHandler;
                m.mode = MultiplexMode.Switch;
                return m;
            }

            /*
             * Blend ratio between 0 and 1
             */
            public static SendMultiplex Blend(float blendRatio, AudioSendHandler... sendHandlers) {
                throw new UnsupportedOperationException("Blend mode is not supported yet.");

//                if(blendRatio < 0 || blendRatio > 1) {
//                    throw new RuntimeException("Blend ratio must be between 0 and 1.");
//                } else if(sendHandlers == null || sendHandlers.length == 0) {
//                    throw new RuntimeException("Must provide at least one audio send handler.");
//                }
//
//                SendMultiplex m = new SendMultiplex();
//                m.handlers = sendHandlers;
//                m.mode = MultiplexMode.Blend;
//                m.blendBalance = blendRatio;
//                return m;
            }
        }

        private Config() {
        }

        /**
         * Settings for using wake detection
         * @param jniLocation The absolute path to your "libjni_porcupine.dll/libjni_porcupine.so" file
         * @param porcupineLocation The absolute path to your "libpv_porcupine.dll/libpv_porcupine.so" file
         * @param porcupineParams The absolute path to your "porcupine_params.pv" file
         * @param sensitivity A sensitivity between 0 and 1, 0 will leans towards false negatives, 1 will leans towards more false positives
         * @param wakePhrasePaths An array of abosolute paths to your "wake_phrase.ppn" files
         * @return Builder object
         */
        public Config withWakeDetection(String jniLocation, String porcupineLocation, String porcupineParams, float sensitivity, String... wakePhrasePaths) {
            this.jniLocation = jniLocation;
            this.porcupineLocation = porcupineLocation;
            this.sensitivity = sensitivity;
            this.porcupineParams = porcupineParams;
            this.wakePhrasePaths = wakePhrasePaths;
            return this;
        }

        /**
         * Optionally change the default settings for phrase detection. This allows you to fine-tune wake detection.
         * @param postWakeLimit After a user wakes the bot, they have this many milliseconds to start speaking a command before VocalCord will cancel the phrase detection and put the stream to sleep
         * @param postPhraseTimeout When the user is speaking a voice command, how many milliseconds of silents should occur before VocalCord should stop listening and start working on a transcript?
         * @param maxPhraseTime The maximum amount of time a voice command may last, in seconds
         * @return Builder object
         */
        public Config withPhraseDetectionSettings(int postWakeLimit, int postPhraseTimeout, int maxPhraseTime) {
            this.postWakeLimit = postWakeLimit;
            this.postPhraseTimeout = postPhraseTimeout;
            this.maxPhraseTime = maxPhraseTime;
            return this;
        }

        /**
         * What language TTS and STT should use. If you're using English, you don't need to call this
         * @param languageCode A language code from: https://www.cardinalpath.com/resources/tools/google-analytics-language-codes/
         * @return Builder object
         */
        public Config withLanguage(String languageCode) {
            this.languageCode = languageCode;
            return this;
        }

        /**
         * Enables TTS support
         * @param voiceGender The voice accent to use
         * @param useCaching Caching will cache frequent phrases to speed up TTS times, this is really helpful for things like a onWake(..)
         *                   whose speed will affect the overall speed of a voice command
         * @return Builder object
         */
        public Config withTTS(SsmlVoiceGender voiceGender, boolean useCaching) {
            usingTTS = true;
            this.usingTTSCache = useCaching;
            this.voiceGender = voiceGender;
            this.sendMultiplex = SendMultiplex.None();
            return this;
        }

        /**
         * JDA enforces a restriction of only ONE AudioSendHandler at a time. This is a bit tricky because TTS also
         * needs to use an AudioSendHandler. In order to fix this, VocalCord implements Multiplexing. I.E. if you want to
         * use a music bot or something, VocalCord is capable of mixing the signal with the TTS when needed.
         * @param voiceGender THe voice accent to use
         * @param useCaching Caching will cache frequent phrases to speed up TTS times, this is really helpful for things like a onWake(..)
         *                   whose speed will affect the overall speed of a voice command
         * @param sendMultiplex A send multiplexer defining how you want to mix the audio
         * @return Builder object
         */
        public Config withTTSMultiplex(SsmlVoiceGender voiceGender, boolean useCaching, SendMultiplex sendMultiplex) {
            withTTS(voiceGender, useCaching);
            this.sendMultiplex = sendMultiplex;
            return this;
        }

        /**
         * Constructs the VocalCord object
         * @return VocalCord object, you should connect to the channel after this
         */
        public VocalCord build() {
            // Verify arguments
            return new VocalCord();
        }

    }

    private TTSEngine ttsEngine;

    /**
     * Instructs VocalCord to say something in the channel, must have called "connect" beforehand
     * @param text TTS text
     */
    public void say(String text) {
        if(ttsEngine == null) {
            throw new RuntimeException("TTS not configured. Use withTTS(..) when configuring VocalCord to use TTS.");
        }

        try {
            ttsEngine.say(text);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Connects vocal cord to a voice channel
     * @param voiceChannel The voice channel to connect to
     */
    public void connect(VoiceChannel voiceChannel) {
        AudioManager manager = voiceChannel.getGuild().getAudioManager();
        manager.openAudioConnection(voiceChannel);

        Config cfg = VocalCord.getConfig();

        if(cfg.usingTTS) {
            ttsEngine = new TTSEngine();

            if(cfg.sendMultiplex.mode != Config.SendMultiplex.MultiplexMode.None) {
                manager.setSendingHandler(new AudioSendMultiplexer(ttsEngine, cfg.sendMultiplex));
            } else {
                manager.setSendingHandler(ttsEngine);
            }

            manager.setReceivingHandler(new STTEngine());
        }
    }

    // TODO
    // cheetah voice detection engine
    // continuation phrase
    // blend multiplexer
    // include documentation about disconnecting the bot
    // fix timings a bit
}
