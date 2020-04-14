package example;

import com.gmail.wdavies973.lib.VocalCord;
import com.gmail.wdavies973.lib.VocalEngine;
import com.google.cloud.texttospeech.v1beta1.*;
import com.google.protobuf.ByteString;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Random;

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
        private AudioFrame lastFrame;

        private AudioPlayerManager m;

        public Sender(AudioPlayerManager m, AudioPlayer player) {
            this.audioPlayer = player;
            this.m = m;
        }

        @Override
        public boolean canProvide() {
            lastFrame = audioPlayer.provide();
            return lastFrame != null;
        }

        @Nullable
        @Override
        public ByteBuffer provide20MsAudio() {
            return ByteBuffer.wrap(lastFrame.getData());
        }

        @Override
        public boolean isOpus() {
            return true;
        }

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
                        AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.MP3).build();

                // Perform the text-to-speech request on the text input with the selected voice parameters and
                // audio file type
                SynthesizeSpeechResponse response =
                        textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

                // Get the audio contents from the response
                ByteString audioContents = response.getAudioContent();

                try(OutputStream out = new FileOutputStream("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/output.mp3")) {
                    out.write(audioContents.toByteArray());
                    System.out.println("Audio content written to file \"output.mp3\"");
                }

                m.loadItem("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/output.mp3", new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        System.out.println("Loaded track");
                        audioPlayer.playTrack(track);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {

                    }

                    @Override
                    public void noMatches() {
                        System.out.println("No matches");
                    }

                    @Override
                    public void loadFailed(FriendlyException exception) {
                        System.out.println(exception.getMessage());
                    }
                });

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
        if(content.equals("!summon")) {
            event.getMessage().getChannel().sendMessage("On my way!").queue();
            try {
                VoiceChannel authorVoiceChannel = event.getMember().getVoiceState().getChannel();
                authorVoiceChannel.getGuild().getAudioManager().openAudioConnection(authorVoiceChannel);
                authorVoiceChannel.getGuild().getAudioManager().setSendingHandler(sender);
                authorVoiceChannel.getGuild().getAudioManager().setReceivingHandler(new VocalEngine(this));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

}