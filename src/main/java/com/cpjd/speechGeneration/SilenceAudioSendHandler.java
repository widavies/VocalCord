package com.cpjd.speechGeneration;

import net.dv8tion.jda.core.audio.AudioSendHandler;

public class SilenceAudioSendHandler implements AudioSendHandler {
    private static final byte[] silenceBytes = new byte[] {(byte)0xF8, (byte)0xFF, (byte)0xFE};

    private boolean canProvide = true;
    private long startTime = System.currentTimeMillis();

    @Override
    public byte[] provide20MsAudio() {

        // send the silence only for 5 seconds
        if(((System.currentTimeMillis() - startTime) / 1000) > 5) {
            canProvide = false;
        }

        return silenceBytes;
    }

    @Override
    public boolean canProvide() {
        return canProvide;
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}
