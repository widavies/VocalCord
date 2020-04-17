package vocalcord;

import com.google.cloud.texttospeech.v1beta1.SsmlVoiceGender;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import javax.annotation.Nonnull;

public class VocalCord {

    public interface Callbacks {
        /**
         * This method will be called by SpeechReceiver if a command was detected after bot awakening.
         *
         * @param user       The user that issued the command
         * @param transcript VocalCord's (Google's) best guess of what the user said in Discord.
         */
        void onTranscribed(User user, String transcript);

        /**
         * This allows you to restrict bot usage to only certain users. If the bot detects a wakeup command,
         * it will call this method. The bot will ONLY start listening for a voice command if this method returns true.
         * You can run your checking code on the User object for names, roles, etc. You might also want to play a booping noise or a
         * text to speech response when the bot wakes up. Keep in mind, since Google Voice costs money, you want to be make sure
         * you trust which users are allowed to use voice recognition
         *
         * @return true if the bot should start listening to what the user has to say, or false to deny the wakeup request
         */
        boolean onWake(User user);
    }

    public static class Builder {
        Callbacks callbacks;
        AudioSendHandler audioSendHandler;
        float sensitivity;
        String porcupineParams, keywordPath;
        String languageCode;
        boolean usingTTS;
        SsmlVoiceGender voiceGender;
        MultiplexMode multiplexMode;

        public enum MultiplexMode {
            Switch,
            Combine
        }

        public Builder(@Nonnull Callbacks callbacks) {
            this.callbacks = callbacks;
        }

        // volume mix percentage?
        public Builder withSendMultiplexer(AudioSendHandler audioSendHandler, MultiplexMode multiplexMode) {
            this.audioSendHandler = audioSendHandler;
            this.multiplexMode = multiplexMode;
            return this;
        }

        public Builder withWakeDetection(float sensitivity, String porcupineParams, String keywordPath) {
            this.sensitivity = sensitivity;
            this.porcupineParams = porcupineParams;
            this.keywordPath = keywordPath;
            return this;
        }

        // https://www.cardinalpath.com/resources/tools/google-analytics-language-codes/
        public Builder withLanguage(String languageCode) {
            this.languageCode = languageCode;
            return this;
        }

        public Builder withTTS(SsmlVoiceGender voiceGender) {
            usingTTS = true;
            this.voiceGender = voiceGender;
            return this;
        }

        public VocalCord build() {
            VocalCord cord = new VocalCord();
            cord.config = this;
            return cord;
        }

    }

    Builder config;

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

        if(config.usingTTS) {
            ttsEngine = new TTSEngine(config);
            manager.setSendingHandler(ttsEngine);
            manager.setReceivingHandler(new STTEngine(this));
            try {
                ttsEngine.cache("Yes?");
                ttsEngine.cache("hello");
            } catch(Exception e) {
                e.printStackTrace();
            }

        }
    }

// features - multiple keywords
// update packages
// gradle deploy
// command detection
// tts caching and auto-caching
// cancel bot
// continuation phrase
// listening mode
// pause in TTS
}
