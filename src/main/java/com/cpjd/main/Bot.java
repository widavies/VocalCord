package com.cpjd.main;

import com.cpjd.speechGeneration.SilenceAudioSendHandler;
import com.cpjd.speechRecognition.SpeechCallback;
import com.cpjd.speechRecognition.SpeechReceiver;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;

/**
 * This is an example of how to use the VocalCord speech recognition library
 *
 * @author Will Davies
 */
public class Bot extends ListenerAdapter {

    public Bot() {
        try {
            new JDABuilder(AccountType.BOT).setToken(Constants.token).addEventListener(this)
                    .buildBlocking();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        summon(event, event.getGuild().getVoiceChannelsByName("General 1", true).get(0));
    }

    // Summons the bot to a voice channel, this is triggered by a message received in any channel
    private void summon(GuildMessageReceivedEvent event, VoiceChannel channel) {
        // Reference the audio manager
        AudioManager manager = channel.getGuild().getAudioManager();

        // This is required
        manager.setSendingHandler(new SilenceAudioSendHandler());

        // Setup the SpeechReceiver, you can initialize a wakeup phrase here, and the speech callback
        SpeechReceiver speechReceiver = new SpeechReceiver("okay bought", new SpeechCallback() {
            // This method is called when a voice command is processed, here it just sends the text back to the text channel
            @Override
            public void commandReceived(String command) {
                if(!command.equals("")) event.getChannel().sendMessage("You said: "+command+". Is that right?").queue();
            }

            /*
             * This is an important method, when a wakeup phrase is detected, this method will be called. The bot will only
             * start listening for a voice command if this returns true. This method returns a user variable parameter, which will contain a minimum of 0
             * to an x amount of users involved in the voice command. Here you can define certain roles or restrictions to who can
             * activate the bot.
             */
            @Override
            public boolean botAwakeRequest(User... user) {
                // If the bot is successfully activated, you might want to play a sound using JDA, send a message, or use TTS to
                // let the user know that the bot was successfully activated
                System.out.println("Bot awakened!");
                return true;
            }
        });

        // This must be true for now, it will be fixed when Discord fixes an audio issue
        speechReceiver.setCombinedAudio(true);
        // Set the speech handler
        manager.setReceivingHandler(speechReceiver);

        // Open the connection to the VoiceChannel
        manager.openAudioConnection(channel);
    }

    public static void main(String[] args) {
        new Bot();
    }
}
