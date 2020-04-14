package example;

import com.google.cloud.texttospeech.v1beta1.*;
import com.google.protobuf.ByteString;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import vocalcord.VocalCord;
import vocalcord.VocalEngine;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

// Create a template JDA bot using this guide: https://github.com/DV8FromTheWorld/JDA/wiki/3)-Getting-Started
public class ExampleBot extends ListenerAdapter implements VocalCord {

    public static void main(String[] args) throws Exception {

        // Constants.token won't be defined for you because it's not tracked by git, this is where you'll put your
        // bot's authentication token
        JDA api = JDABuilder.createDefault(Constants.TOKEN).build();
        api.addEventListener(new ExampleBot());

        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerLocalSource(playerManager);
        AudioPlayer player = playerManager.createPlayer();

        sender = new Sender(playerManager, player);
    }

    @Override
    public void onTranscribed(User user, String transcript) {
        System.out.println(transcript);

        transcript = transcript.toLowerCase();

    }

    @Override
    public boolean isAuthorizedUser(User user) {
        return false;
    }

    public static class Sender implements AudioSendHandler {
        private final AudioPlayer audioPlayer;
        private ByteBuffer lastFrame;

        private AudioPlayerManager m;

        private byte[] pcm;
        private int index = 0;

        public Sender(AudioPlayerManager m, AudioPlayer player) {
            this.audioPlayer = player;
            this.m = m;
        }

        @Override
        public boolean canProvide() {
            if(index + 3840 < pcm.length) {
                byte[] array = new byte[3840];
                for(int i = index, j = 0; i < index + 3840; j++, i++) {
                    array[j] = pcm[i];
                }

                lastFrame = ByteBuffer.wrap(array);
                index += 3840;
            } else {
                lastFrame = null;
            }

            return lastFrame != null;
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

        private short bytePairToShort(byte a, byte b) {
            return (short) ((a << 8) | (b & 0xFF));
        }

        // export "GOOGLE_APPLICATION_CREDENTIALS=/mnt/c/Users/wdavi/IdeaProjects/VocalCord/vocalcord-gcs.json"
        public void say(String text) {
            try(TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
                // Set the text input to be synthesized
                SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

                // Build the voice request, select the language code ("en-US") and the ssml voice gender
                // ("neutral")
                VoiceSelectionParams voice =
                        VoiceSelectionParams.newBuilder()
                                .setLanguageCode("en-US")
                                .setSsmlGender(SsmlVoiceGender.NEUTRAL)
                                .build();

                // Select the type of audio file you want returned
                AudioConfig audioConfig =
                        AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.LINEAR16).setSampleRateHertz(48000).build();

                // Perform the text-to-speech request on the text input with the selected voice parameters and
                // audio file type
                SynthesizeSpeechResponse response =
                        textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

                // Get the audio contents from the response
                ByteString audioContents = response.getAudioContent();

                // 16-bit, linear, little-endian, signed, 1 channel, 48KHz
                byte[] pcm = audioContents.toByteArray();

                // Convert to big endian
                short[] combinedPCM = new short[pcm.length / 2];
                for(int i = 0, j = 0; i < pcm.length; j++, i += 2) {
                    combinedPCM[j] = Short.reverseBytes(bytePairToShort(pcm[i], pcm[i + 1]));
                }

                // Create stereo channel
                short[] stereo = new short[combinedPCM.length * 2];
                int k = 0;
                for(int i = 0; i < combinedPCM.length; i++) {
                    stereo[k] = combinedPCM[i];
                    stereo[k + 1] = combinedPCM[i];
                    k += 2;
                }

                // Split out into bytes
                byte[] data = new byte[stereo.length * 2];
                data = new byte[stereo.length * 2];
                k = 0;
                for(int i = 0; i < stereo.length; i++) {
                    byte a = (byte)(stereo[i] >> 8);
                    byte b = (byte)(stereo[i] & 0x00FF);
                    data[k] = a;
                    data[k+1] = b;
                    k += 2;
                }

                this.pcm = data;

                System.out.println(this.pcm.length+" samples");

                this.index = 0;

                // Write to a file
//                try {
//                    AudioFormat target = new AudioFormat(48000f, 16, 2, true, true);
//                    AudioInputStream is = AudioSystem.getAudioInputStream(target, new AudioInputStream(new ByteArrayInputStream(this.pcm), AudioReceiveHandler.OUTPUT_FORMAT, this.pcm.length));
//                    AudioSystem.write(is, AudioFileFormat.Type.WAVE, new File("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/converted.wav"));
//                } catch(Exception e) {
//                    System.out.println("Failed to convert!");
//                }

//                try(OutputStream out = new FileOutputStream("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/output.wav")) {
//                    out.write(this.pcm);
//                    System.out.println("Audio content written to file \"output.ogg\"");
//                }
//
//                m.loadItem("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/output.mp3", new AudioLoadResultHandler() {
//                    @Override
//                    public void trackLoaded(AudioTrack track) {
//                        System.out.println("Loaded track");
//                        audioPlayer.playTrack(track);
//                    }
//
//                    @Override
//                    public void playlistLoaded(AudioPlaylist playlist) {
//
//                    }
//
//                    @Override
//                    public void noMatches() {
//                        System.out.println("No matches");
//                    }
//
//                    @Override
//                    public void loadFailed(FriendlyException exception) {
//                        System.out.println(exception.getMessage());
//                    }
//                });

                // Write the response to the output file.
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    static Sender sender;

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
            sender.say(content.substring(5));
        }

        if(content.equals("!summon")) {
            event.getMessage().getChannel().sendMessage("On my way!").queue();
            try {
                VoiceChannel authorVoiceChannel = event.getMember().getVoiceState().getChannel();
                authorVoiceChannel.getGuild().getAudioManager().openAudioConnection(authorVoiceChannel);
                authorVoiceChannel.getGuild().getAudioManager().setSendingHandler(sender);
                authorVoiceChannel.getGuild().getAudioManager().setReceivingHandler(new VocalEngine(this));
                sender.say("testing the audio system");
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

}