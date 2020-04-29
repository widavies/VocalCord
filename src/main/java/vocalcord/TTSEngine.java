package vocalcord;

import com.google.cloud.texttospeech.v1beta1.*;
import com.google.protobuf.ByteString;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

/*
 * Text to speech engine
 */
class TTSEngine implements AudioSendHandler {

    public static final int AUDIO_FRAME = 3840; // 48000 / 50 (number of 20 ms in a second) * 2 (16-bit samples) * 2 (channels)

    private byte[] out;
    private int index;
    private ByteBuffer lastFrame;

    private TTSCache ttsCache;

    TTSEngine() {
        this.out = new byte[0];

        // Load the cache
        if(VocalCord.getConfig().usingTTS && VocalCord.getConfig().usingTTSCache) {
            try {
                ttsCache = new TTSCache();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    byte[] tts(String text) throws Exception {
        try(TextToSpeechClient client = TextToSpeechClient.create()) {
            SynthesisInput input = SynthesisInput.newBuilder().setSsml(text).build();

            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder().setLanguageCode(VocalCord.getConfig().languageCode).setSsmlGender(VocalCord.getConfig().voiceGender).build();

            AudioConfig audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.LINEAR16).setSampleRateHertz(48_000).build();

            SynthesizeSpeechResponse response = client.synthesizeSpeech(input, voice, audioConfig);

            ByteString audioContents = response.getAudioContent();

            byte[] pcm = audioContents.toByteArray();

            // Three things need to happen - big endian, stereo, pad to a multiple of 3840
            byte[] converted = new byte[pcm.length * 2 + (AUDIO_FRAME - pcm.length * 2 % AUDIO_FRAME)]; // ensures converted is a multiple of AUDIO_FRAME
            for(int i = 0; i < pcm.length; i += 2) {
                short reversed = Short.reverseBytes((short) ((pcm[i] << 8) | (pcm[i + 1] & 0xFF)));
                byte low = (byte) (reversed >> 8);
                byte high = (byte) (reversed & 0x00FF);

                // reverse bytes and double to convert to stereo
                converted[i * 2] = low;
                converted[i * 2 + 1] = high;
                converted[i * 2 + 2] = low;
                converted[i * 2 + 3] = high;
            }

            return converted;
        }
    }

    void say(String phrase) throws Exception {
        if(ttsCache != null) {
            TTSCache.CacheResponse response = ttsCache.checkCache(phrase);

            byte[] data = tts(phrase);

            if(response.pcmIfCached != null) {
                this.out = response.pcmIfCached;
            } else {
                this.out = data;
            }

            if(response.shouldCache) {
                ttsCache.cache(phrase, data);
            }
        } else {
            this.out = tts(phrase);
        }

        this.index = 0;
    }

    @Override
    public boolean canProvide() {
        boolean provide = index < out.length;

        if(provide) {
            lastFrame = ByteBuffer.wrap(out, index, AUDIO_FRAME);
            index += AUDIO_FRAME;

            if(index >= out.length) {
                VocalCord.getConfig().callbacks.onTTSCompleted();
            }
        }

        return provide;
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        return lastFrame;
    }

    @Override
    public boolean isOpus() {
        return false;
    }
}
