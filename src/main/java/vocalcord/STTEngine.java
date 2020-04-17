package vocalcord;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.UserAudio;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

class STTEngine implements AudioReceiveHandler {

    private final HashMap<String, UserStream> streams = new HashMap<>();

    private final ThreadPoolExecutor workPool = new ThreadPoolExecutor(8, 16, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>());

    private class StreamMonitor implements Runnable {

        @Override
        public void run() {
            for(String userId : streams.keySet()) {
                UserStream us = streams.get(userId);

                if(us.readyForTranscription()) {
                    byte[] audio = us.getAudioForGoogle();
                    us.sleep();

                    workPool.execute(() -> {
                        List<SpeechRecognitionResult> results = speechRecognition(audio);

                        VocalCord.Callbacks callbacks = VocalCord.getConfig().callbacks;

                        CommandChain chain = callbacks.onTranscribed();
                        if(chain != null) {
                            CommandChain.CommandCandidate max = null;
                            double maxSimilarity = -1;

                            for(SpeechRecognitionResult result : results) {
                                for(SpeechRecognitionAlternative sra : result.getAlternativesList()) {
                                    CommandChain.CommandCandidate ccs = chain.test(sra.getTranscript());
                                    if(ccs != null && ccs.similarity > maxSimilarity) {
                                        maxSimilarity = ccs.similarity;
                                        max = ccs;
                                    }
                                }
                            }

                            chain.execute(us.getUser(), max);
                        } else {
                            if(results.size() > 0 && results.get(0).getAlternativesList().size() > 0) {
                                callbacks.onTranscribed(us.getUser(), results.get(0).getAlternatives(0).getTranscript());
                            }
                        }
                    });
                } else if(us.shouldDestroy()) {
                    us.sleep();
                    us.destroy();
                    streams.remove(us.getUser().getId());
                }
            }
        }
    }

    public STTEngine() {
        ScheduledExecutorService streamDaemon = Executors.newScheduledThreadPool(1);
        streamDaemon.scheduleAtFixedRate(new StreamMonitor(), 0, 1000, TimeUnit.MICROSECONDS); // 1 ms
    }

    @Override
    public boolean canReceiveUser() {
        return true;
    }

    @Override
    public void handleUserAudio(@Nonnull UserAudio userAudio) {
        if(!streams.containsKey(userAudio.getUser().getId())) {
            try {
                /*
                 * Don't track the audio of users who aren't allowed to wake, otherwise resources would be needlessly wasted
                 * with another Porcupine instance
                 */
                if(VocalCord.getConfig().callbacks.canWakeBot(userAudio.getUser())) {
                    UserStream stream = new UserStream(userAudio.getUser());
                    stream.putAudio(workPool, userAudio.getAudioData(1));
                    streams.put(userAudio.getUser().getId(), stream);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            streams.get(userAudio.getUser().getId()).putAudio(workPool, userAudio.getAudioData(1));
        }
    }

    private List<SpeechRecognitionResult> speechRecognition(byte[] pcm) {
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

            return  response.getResultsList();
        } catch(Exception e) {
            e.printStackTrace();
            System.err.println("Failed to run Google Cloud speech recognition. Err: " + e.getMessage());
        }
        return new ArrayList<>();
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
