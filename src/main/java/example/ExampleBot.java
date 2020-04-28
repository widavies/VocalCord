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

public class ExampleBot extends ListenerAdapter implements VocalCord.Callbacks {

    private final VocalCord cord;

    public ExampleBot() {
        /*
         * This code will create the bot, make sure to specify the absolute paths of the files you downloaded to native to ensure all libraries
         * are loaded correctly. This is also where you can config VocalCord settings.
         */

        // Windows
        cord = VocalCord.newConfig(this).withWakeDetection("C:\\Users\\wdavi\\IdeaProjects\\VocalCord\\native\\windows\\libjni_porcupine.dll",
                "C:\\Users\\wdavi\\IdeaProjects\\VocalCord\\native\\windows\\libpv_porcupine.dll", "C:\\Users\\wdavi\\IdeaProjects\\VocalCord\\native\\porcupine_params.pv",
                0.5f, "C:\\Users\\wdavi\\IdeaProjects\\VocalCord\\phrases\\windows.ppn").withTTS(SsmlVoiceGender.MALE, true).build();

        // Linux (using WSL)
//        cord = VocalCord.newConfig(this).withWakeDetection("/mnt/c/Users/wdavi/IdeaProjects/VocalCord/native/linux/libjni_porcupine.so",
//                "/mnt/c/Users/wdavi/IdeaProjects/VocalCord/native/linux/libpv_porcupine.so", "/mnt/c/Users/wdavi/IdeaProjects/VocalCord/native/porcupine_params.pv",
//                0.5f, "/mnt/c/Users/wdavi/IdeaProjects/VocalCord/phrases/linux.ppn").withTTS(SsmlVoiceGender.MALE, true).build();
    }

    public static void main(String[] args) throws Exception {
        // Creates a JDA Discord instance and makes your bot go online
        JDA api = JDABuilder.createDefault(args[0]).build();
        api.addEventListener(new ExampleBot());
    }

    /*
     * This callback defines which users are allowed to access VocalCord.
     * Note, you want to be restrictive on this, especially for large servers,
     * running wake word detection on like 50+ users simultaneously is untested and
     * may affect performance.
     */
    @Override
    public boolean canWakeBot(User user) {
        return true;
    }

    /*
     * This method is called when an authorized user (canWakeBot(..) returned true)
     * woke up the bot, the keywordIndex defines which keyword woke the bot (this depends
     * on the order you specified keywords to when setting up VocalCord) If you only have one
     * keyword, this will be 0. This method is useful for giving the user some feedback that the
     * bot is listening, here for example, the bot will say "Yes?" when it's woken up. Immediately after
     * this call, VocalCord will start generating a voice transcript of what the user said. If you want to cancel
     * voice recognition here, you can call userStream.sleep()
     */
    @Override
    public void onWake(UserStream userStream, int keywordIndex) {
        cord.say("Yes?");
    }

    /*
     * Note: There are two onTranscribed(..) methods, you should only use one of them (this one is better)
     * This callback is where you'll store all your voice commands. Importantly, voice transcripts aren't always
     * 100% accurate. If you hard code a list of commands, being off by just one word wouldn't register the command,
     * or trying to use lots of String.contains(..) calls could easily intermix commands. This callback employs
     * CommandChain, which will generate document vectors and a document term matrix in order to compute the cosine
     * similarity between a candidate transcription. Essentially, CommandChain will automatically run an algorithm to
     * determine which command was most likely said. This means that a user doesn't have to be 100% accurate on matching a command,
     * and instead only needs to capture the meaning of a command.
     */
    @Override
    public CommandChain onTranscribed() {
        return new CommandChain.Builder().add("hello world", (user, transcript) -> {
            cord.say(user.getName()+" said something");
        }).add("knock knock", (user, transcript) -> {
            cord.say("Who's there?");
        }).withFallback(((user, transcript) -> {
            cord.say("I'm sorry, I didn't get that");
        })).withMinThreshold(0.5f).build();
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

    @Override
    public void onTTSCompleted() {
        // If you want to do anything after the bot stops speaking
    }
}