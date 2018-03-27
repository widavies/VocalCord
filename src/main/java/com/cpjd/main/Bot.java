package com.cpjd.main;

import com.cpjd.audio.SilenceAudioSendHandler;
import com.cpjd.audio.SpeechRecognitionReceiver;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.audio.AudioSendHandler;
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
        summon(event.getGuild().getVoiceChannelsByName("radio", true).get(0));
    }

    private void summon(VoiceChannel channel) {
        AudioManager manager = channel.getGuild().getAudioManager();
        manager.setReceivingHandler(new SpeechRecognitionReceiver("Hey JukeBot"));
        manager.setSendingHandler(new SilenceAudioSendHandler());
        manager.openAudioConnection(channel);
    }

    public static void main(String[] args) {
        new Bot();
    }
}
