package example;

import com.gmail.wdavies973.lib.VocalCord;
import com.gmail.wdavies973.lib.VocalEngine;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

// Create a template JDA bot using this guide: https://github.com/DV8FromTheWorld/JDA/wiki/3)-Getting-Started
public class ExampleBot extends ListenerAdapter {

    public static void main(String[] args) throws Exception {

        // Constants.token won't be defined for you because it's not tracked by git, this is where you'll put your
        // bot's authentication token
        JDA api = JDABuilder.createDefault(Constants.TOKEN).build();
        api.addEventListener(new ExampleBot());

    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Don't process messages from other bots
        if(event.getAuthor().isBot()) return;

        Message message = event.getMessage();
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
                authorVoiceChannel.getGuild().getAudioManager().setReceivingHandler(new VocalEngine(new VocalCord() {
                    @Override
                    public void onTranscribed(User user, String transcript) {

                    }

                    @Override
                    public boolean isAuthorizedUser(User user) {
                        return false;
                    }
                }));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

}