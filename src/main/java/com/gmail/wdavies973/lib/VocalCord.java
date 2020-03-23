package com.gmail.wdavies973.lib;

import net.dv8tion.jda.api.entities.User;

/**
 * You should write a class that implements this Interface
 */
public interface VocalCord {
    /**
     * This method will be called by SpeechReceiver if a command was detected after bot awakening.
     * @param  user The user that issued the command
     * @param transcript VocalCord's (Google's) best guess of what the user said in Discord.
     */
    void onTranscribed(User user, String transcript);

    /**
     * This allows you to restrict bot usage to only certain users. If the bot detects a wakeup command,
     * it will call this method. The bot will ONLY start listening for a voice command if this method returns true.
     * You can run your checking code on the User object for names, roles, etc. You might also want to play a booping noise or a
     * text to speech response when the bot wakes up. Keep in mind, since Google Voice costs money, you want to be make sure
     * you trust which users are allowed to use voice recognition
     * @return true if the bot should start listening to what the user has to say, or false to deny the wakeup request
     */
    boolean isAuthorizedUser(User user);

}
