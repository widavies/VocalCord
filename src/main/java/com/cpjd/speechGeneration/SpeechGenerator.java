package com.cpjd.speechGeneration;

import sun.audio.AudioPlayer;

import javax.sound.sampled.AudioFileFormat;
import java.util.ArrayList;
import java.util.Stack;

public class SpeechGenerator implements AudioSendhandler {

    private VOICE voice;

    private Stack<String> queue;

    private byte[] pcm;

    private int frame;

    public enum VOICE {
        KEVIN("kevin16");

        private final String voiceName;
        VOICE(String voiceName) { this.voiceName = voiceName; }
        public String getVoiceName() { return voiceName; }

    }


    public SpeechGenerator(VOICE voice) {
        this.voice = voice;
    }

    public void talk(String message) {

    }

    /*
     * Generates speech from the specified text and saves it to the cache directory. This can then be converted
     * into a format that Discord understands, which will be provided by the speech generator
     */
    private void generateSpeech(String text) {
        FreeTTS freeTTS;
        AudioPlayer audioPlayer = null;
        String voiceName = "kevin16";

        VoiceManager voiceManager = VoiceManager.getInstance();
        Voice helloVoice = voiceManager.getVoice(voiceName);

        // if null

        helloVoice.allocate();

        audioPlayer = new SingleFileAudioPlayer("path", AudioFileFormat.Type.WAVE);
        helloVoice.setAudioPlayer(audioPlayer);

        helloVoice.speak(text);
        helloVoice.deallocate();
        audioPlayer.close();
    }

    @Override
    public void provide20MsAudio() {
        if(pcm != null) { // process chunk by chunk
            frame++;
            return null;
        }
    }

    @Override
    public boolean canProvideAudio() {
        // Process the queue
        if(!queue.empty() && pcm == null) {
            String message = queue.pop();
            generateSpeech(message);
            // convert
            pcm = null; // set
            frame = 0;
        }

        return pcm == null;
    }
} // end class
