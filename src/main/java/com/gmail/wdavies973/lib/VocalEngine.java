package com.gmail.wdavies973.lib;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.UserAudio;
import net.dv8tion.jda.api.entities.User;
import wakeup.Porcupine;

import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.HashMap;

public class VocalEngine implements AudioReceiveHandler {

    private VocalCord cord;

    private HashMap<User, byte[]> audioFeeds;

    public VocalEngine(VocalCord cord) {
        this.cord = cord;
    }

    @Override
    public boolean canReceiveUser() {
        return true;
    }

    @Override
    public void handleUserAudio(@Nonnull UserAudio userAudio) {

    }

    public static short bytePairToShort(byte a, byte b) {
        return (short) ((a << 8) | (b & 0xFF));
    }

    public static short[] bytesToShorts(byte[] buffer) {
        short[] arr = new short[buffer.length / 2];

        // Partition into two arrays
        for(int i = 0; i < buffer.length; i+=2) {
            arr[i]  = bytePairToShort(buffer[i], buffer[i+1]);
        }

        return arr;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(System.getenv("LD_LIBRARY_PATH"));
        Porcupine porcupine = new Porcupine("wake-engine/Porcupine/lib/common/porcupine_params.pv", "wake-engine/wake_phrase.ppn", 0.5f);
        System.out.println("Hello world");

        // Create microphone
//        AudioFormat audioFormat = new AudioFormat(16000, 16, 1, true, true);
//        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
//
//        if(!AudioSystem.isLineSupported(info)) {
//            throw new Error("AudioSystem: line is not supported");
//        }
//
//        TargetDataLine line = AudioSystem.getTargetDataLine(audioFormat);
//        line.open(audioFormat);
//        line.start();
//
//        int size = 1024;
//
//        byte[] buffer = new byte[1024];
//        System.out.println("Listening...");
//        while(true) {
//            if(line.read(buffer, 0, size) > 0) {
//                if(porcupine.processFrame(bytesToShorts(buffer))) {
//                    System.out.println("Wait word detected!");
//                }
//            }
//        }

    }
}
