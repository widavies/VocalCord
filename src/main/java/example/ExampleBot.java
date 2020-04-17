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
import vocalcord.UserStream;
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
    public CommandChain onTranscribed() {
        return new CommandChain.Builder().add("what is the fact of the day", (user, transcript) -> {
            cord.say(user.getName()+" said something");
        }).withFallback(((user, transcript) -> {
            cord.say("I'm sorry, I didn't get that");
        })).build();
    }

    @Override
    public boolean canWakeBot(User user) {
        return true;
    }

    @Override
    public void onWake(UserStream user, int keywordIndex) {
        cord.say("Yes?");
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