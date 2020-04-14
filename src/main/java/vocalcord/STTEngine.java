package vocalcord;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.UserAudio;
import wakeup.Porcupine;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class STTEngine implements AudioReceiveHandler {

    private VocalCord cord;
    private UserStream us;

    private Timer timer;

    private Porcupine porcupine;

    public STTEngine(VocalCord cord) {
        this.cord = cord;

        try {
            this.us = new UserStream(cord.config);
        } catch(Exception e) {
            e.printStackTrace();
        }

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(us.readyForTranscription()) {
                    byte[] audio = us.getAudioForGoogle();
                    us.sleep();
                    cord.config.callbacks.onTranscribed(null, speechRecognition(audio)); // thread pool for this? TODO
                }
            }
        }, 0, 1);
    }

    @Override
    public boolean canReceiveUser() {
        return true;
    }

    @Override
    public void handleUserAudio(@Nonnull UserAudio userAudio) {
        if(!userAudio.getUser().getName().contains("tech")) return;

        us.putAudio(userAudio.getAudioData(1));
    }

    private String speechRecognition(byte[] pcm) {
        System.out.println("Accessing Google Cloud Speech API.");

        try(SpeechClient speech = SpeechClient.create()) {
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

            System.out.println(response.getResultsCount());

            return results.get(0).getAlternativesList().get(0).getTranscript();
        } catch(Exception e) {
            e.printStackTrace();
            System.err.println("Failed to run Google Cloud speech recognition. Err: " + e.getMessage());
        }
        return "";
    }

    private double volumeRMS(byte[] raw) { // needs more testing
        double sum = 0d;
        if(raw.length == 0) {
            return sum;
        } else {
            for(byte aRaw : raw) {
                sum += aRaw;
            }
        }
        double average = sum / raw.length;

        double sumMeanSquare = 0d;
        for(byte aRaw : raw) {
            sumMeanSquare += Math.pow(aRaw - average, 2d);
        }
        double averageMeanSquare = sumMeanSquare / raw.length;
        return Math.sqrt(averageMeanSquare);
    }

}
