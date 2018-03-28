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
        smartSummon(event);
    }

    protected void smartSummon(GuildMessageReceivedEvent event) {
        VoiceChannel channel = event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel();
        summon(event, event.getGuild().getVoiceChannelsByName("radio", true).get(0));
    }

    private void summon(GuildMessageReceivedEvent event, VoiceChannel channel) {
        AudioManager manager = channel.getGuild().getAudioManager();
        manager.setSendingHandler(new SilenceAudioSendHandler());
        SpeechReceiver speechReceiver = new SpeechReceiver("okay bought", new SpeechCallback() {
            @Override
            public void commandReceived(String command) {
                if(!command.equals("")) event.getChannel().sendMessage("You said: "+command+". Is that right?").queue();
            }

            @Override
            public boolean botAwakeRequest(User... user) {
                System.out.println("Bot awakened!");
                return true;
            }
        });
        speechReceiver.setCombinedAudio(true);
        manager.setReceivingHandler(speechReceiver);
        manager.openAudioConnection(channel);
    }

    public static void main(String[] args) {
        new Bot();
    }
}
