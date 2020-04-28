package vocalcord;

import net.dv8tion.jda.api.audio.AudioSendHandler;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

// Used for multiplex mode Switch and Blend
public class AudioSendMultiplexer implements AudioSendHandler {

    private final VocalCord.Config.SendMultiplex sendMultiplex;
    private final TTSEngine ttsEngine;

    private AudioSendHandler currentProvider;

    public AudioSendMultiplexer(TTSEngine engine, VocalCord.Config.SendMultiplex multiplex) {
        this.ttsEngine = engine;
        this.sendMultiplex = multiplex;
    }

    @Override
    public boolean canProvide() {
        if(sendMultiplex.mode == VocalCord.Config.SendMultiplex.MultiplexMode.Switch) {
            if(ttsEngine.canProvide()) {
                currentProvider = ttsEngine;
                return true;
            } else if(sendMultiplex.handlers[0].canProvide()) {
                currentProvider = sendMultiplex.handlers[0];
                return true;
            }
        }

        return false;
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        return currentProvider.provide20MsAudio();
    }

    @Override
    public boolean isOpus() {
        return currentProvider.isOpus();
    }
}
