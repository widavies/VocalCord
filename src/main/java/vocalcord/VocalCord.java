package vocalcord;

import com.google.cloud.texttospeech.v1beta1.SsmlVoiceGender;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

public class VocalCord {

    public interface Callbacks {
        /**
         * This method will be called by SpeechReceiver if a command was detected after bot awakening.
         *
         * @param user       The user that issued the command
         * @param transcript VocalCord's (Google's) best guess of what the user said in Discord.
         */
        default void onTranscribed(User user, String transcript) {
        }

        default CommandChain onTranscribed() {
            return null;
        }

        boolean canWakeBot(User user);

        /**
         * This allows you to restrict bot usage to only certain users. If the bot detects a wakeup command,
         * it will call this method. The bot will ONLY start listening for a voice command if this method returns true.
         * You can run your checking code on the User object for names, roles, etc. You might also want to play a booping noise or a
         * text to speech response when the bot wakes up. Keep in mind, since Google Voice costs money, you want to be make sure
         * you trust which users are allowed to use voice recognition
         *
         * @return true if the bot should start listening to what the user has to say, or false to deny the wakeup request
         */
        void onWake(UserStream userStream, int keywordIndex);
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
        int beginTimeLimit = 4000;
        int endThreshold = 600;
        int maximumPhraseLength = 20;
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

        ;

        public Config withWakeDetectionDefaults(float sensitivity, String... wakePhrasePaths) {
            String os = System.getProperty("os.name").toLowerCase();

            if(os.contains("win")) {
                return withWakeDetection("native\\windows\\libjni_porcupine.dll", "native\\windows\\libpv_porcupine.dll",
                        "wake-engine\\Porcupine\\lib\\common\\porcupine_params.pv", sensitivity, wakePhrasePaths);
            } else {
                return withWakeDetection("native\\linux\\libjni_porcupine.so", "native\\linux\\libpv_porcupine.so",
                        "wake-engine\\Porcupine\\lib\\common\\porcupine_params.pv", sensitivity, wakePhrasePaths);
            }
        }

        public Config withWakeDetection(String jniLocation, String porcupineLocation, String porcupineParams, float sensitivity, String... wakePhrasePaths) {
            this.jniLocation = jniLocation;
            this.porcupineLocation = porcupineLocation;
            this.sensitivity = sensitivity;
            this.porcupineParams = porcupineParams;
            this.wakePhrasePaths = wakePhrasePaths;
            return this;
        }

        // https://www.cardinalpath.com/resources/tools/google-analytics-language-codes/
        public Config withLanguage(String languageCode) {
            this.languageCode = languageCode;
            return this;
        }

        public Config withTTS(SsmlVoiceGender voiceGender, boolean useCaching) {
            usingTTS = true;
            this.usingTTSCache = useCaching;
            this.voiceGender = voiceGender;
            this.sendMultiplex = SendMultiplex.None();
            return this;
        }

        public Config withTTSMultiplex(SsmlVoiceGender voiceGender, boolean useCaching, SendMultiplex sendMultiplex) {
            withTTS(voiceGender, useCaching);
            this.sendMultiplex = sendMultiplex;
            return this;
        }

        public VocalCord build() {
            // Verify arguments
            return new VocalCord();
        }

    }

    private TTSEngine ttsEngine;

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

    // setup guide
    // discord bot setup
    // gradle setup
    // dll setup
    // docs - need to update for javac
    // google cloud setup and configuration
    // cleanup api and shit
    // porcupine configuration

    // gradle deploy
    // documentation

    // release

    // cheetah voice detection engine
    // continuation phrase
    // blend multiplexer
}
