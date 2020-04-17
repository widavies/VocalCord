package example;

import com.google.cloud.texttospeech.v1beta1.SsmlVoiceGender;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import vocalcord.CommandChain;
import vocalcord.VocalCord;

// Create a template JDA bot using this guide: https://github.com/DV8FromTheWorld/JDA/wiki/3)-Getting-Started
public class ExampleBot extends ListenerAdapter implements VocalCord.Callbacks {

    private VocalCord cord;

    public ExampleBot() {
        // Windows
        cord = VocalCord.newConfig(this).withWakeDetection("C:\\Users\\wdavi\\IdeaProjects\\VocalCord\\native\\windows\\libjni_porcupine.dll",
                "C:\\Users\\wdavi\\IdeaProjects\\VocalCord\\native\\windows\\libpv_porcupine.dll", "C:\\Users\\wdavi\\IdeaProjects\\VocalCord\\native\\porcupine_params.pv",
                0.5f, "C:\\Users\\wdavi\\IdeaProjects\\VocalCord\\phrases\\hey_bot_windows.ppn").withTTS(SsmlVoiceGender.MALE, true).build();
        
        // Linux (using WSL)
//        cord = VocalCord.newConfig(this).withWakeDetection("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/native/linux/libjni_porcupine.so",
//                "/mnt/c/Users/wdavi/IdeaProjects/VocalCord/native/linux/libpv_porcupine.so", "/mnt/c/Users/wdavi/IdeaProjects/VocalCord/native/porcupine_params.pv",
//                0.5f, "/mnt/c/Users/wdavi/IdeaProjects/VocalCord/phrases/hey_bot_linux.ppn").withTTS(SsmlVoiceGender.MALE).build();
    }

    public static void main(String[] args) throws Exception {
        // Constants.token won't be defined for you because it's not tracked by git, this is where you'll put your
        // bot's authentication token
        JDA api = JDABuilder.createDefault(Constants.TOKEN).build();
        api.addEventListener(new ExampleBot());

        System.out.println("Hello world!");
    }

    @Override
    public CommandChain onTranscribed(User user, String transcript) {
        System.out.println(transcript);

        transcript = transcript.toLowerCase();

        // cord.say(transcript);
        return new CommandChain();
    }

    @Override
    public boolean canWakeBot(User user) {
        return true;
    }

    @Override
    public void onWake(User user, int keywordIndex) {
        cord.say("Yes?");
    }
//
//    public static class Sender implements AudioSendHandler {
//        private ByteBuffer lastFrame;
//
//        private byte[] pcm;
//        private int index = 0;
//
//        @Override
//        public boolean canProvide() {
//            if(index + 3840 < pcm.length) {
//                byte[] array = new byte[3840];
//                for(int i = index, j = 0; i < index + 3840; j++, i++) {
//                    array[j] = pcm[i];
//                }
//
//                lastFrame = ByteBuffer.wrap(array);
//                index += 3840;
//            } else {
//                lastFrame = null;
//            }
//
//            return lastFrame != null;
//        }
//
//        @Nullable
//        @Override
//        public ByteBuffer provide20MsAudio() {
//            return lastFrame;
//        }
//
//        @Override
//        public boolean isOpus() {
//            return false;
//        }
//
//        private short bytePairToShort(byte a, byte b) {
//            return (short) ((a << 8) | (b & 0xFF));
//        }
//
//        // export "GOOGLE_APPLICATION_CREDENTIALS=/mnt/c/Users/wdavi/IdeaProjects/VocalCord/vocalcord-gcs.json"
//        public void say(String text) {
//            try(TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
//                // Set the text input to be synthesized
//                SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();
//
//                // Build the voice request, select the language code ("en-US") and the ssml voice gender
//                // ("neutral")
//                VoiceSelectionParams voice =
//                        VoiceSelectionParams.newBuilder()
//                                .setLanguageCode("en-US")
//                                .setSsmlGender(SsmlVoiceGender.NEUTRAL)
//                                .build();
//
//                // Select the type of audio file you want returned
//                AudioConfig audioConfig =
//                        AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.LINEAR16).setSampleRateHertz(48000).build();
//
//                // Perform the text-to-speech request on the text input with the selected voice parameters and
//                // audio file type
//                SynthesizeSpeechResponse response =
//                        textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
//
//                // Get the audio contents from the response
//                ByteString audioContents = response.getAudioContent();
//
//                // 16-bit, linear, little-endian, signed, 1 channel, 48KHz
//                byte[] pcm = audioContents.toByteArray();
//
//                // Convert to big endian
//                short[] combinedPCM = new short[pcm.length / 2];
//                for(int i = 0, j = 0; i < pcm.length; j++, i += 2) {
//                    combinedPCM[j] = Short.reverseBytes(bytePairToShort(pcm[i], pcm[i + 1]));
//                }
//
//                // Create stereo channel
//                short[] stereo = new short[combinedPCM.length * 2];
//                int k = 0;
//                for(int i = 0; i < combinedPCM.length; i++) {
//                    stereo[k] = combinedPCM[i];
//                    stereo[k + 1] = combinedPCM[i];
//                    k += 2;
//                }
//
//                // Split out into bytes
//                byte[] data = new byte[stereo.length * 2];
//                data = new byte[stereo.length * 2];
//                k = 0;
//                for(int i = 0; i < stereo.length; i++) {
//                    byte a = (byte)(stereo[i] >> 8);
//                    byte b = (byte)(stereo[i] & 0x00FF);
//                    data[k] = a;
//                    data[k+1] = b;
//                    k += 2;
//                }
//
//                this.pcm = data;
//
//                System.out.println(this.pcm.length+" samples");
//
//                this.index = 0;
//
//                // Write the response to the output file.
//            } catch(Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

    static Message message;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Don't process messages from other bots
        if(event.getAuthor().isBot()) return;

        message = event.getMessage();
        String content = message.getContentRaw();

        /*
         * This is a basic summon command that will summon the bot to
         * whatever voice channel the author is in, this is a really basic
         * summon command, but you can develop more advanced scenarios where
         * the bot follows you around or whatever.
         */
        if(content.startsWith("!say")) {
            cord.say(content.substring(5));
        }

        if(content.equals("!summon")) {
            event.getMessage().getChannel().sendMessage("On my way!").queue();
            try {
                VoiceChannel authorVoiceChannel = event.getMember().getVoiceState().getChannel();
                cord.connect(authorVoiceChannel);

            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

}