package com.cpjd.speechRecognition;

import net.dv8tion.jda.core.entities.User;

/**
 * This interface allows the user to receive events from the SpeechReceiver, aside from
 * configuration of the SpeechReceiver, this is the only code that should be used to retrieve data
 * from it.
 *
 * @version 1.0.0
 * @author Will Davies
 */
public interface SpeechCallback {
    /**
     * This method will be called by SpeechReceiver if a command was detected after bot awakening.
     * The String command is VocalCord's (Google's) best guess of what the user said in Discord.
     * @param command The text form of the speech recognized by the bot
     */
    void commandReceived(String command);

    /**
     * This allows you to restrict bot usage to only certain users. If the bot detects a wakeup command,
     * it will call this method. The bot will ONLY start listening for a voice command if this method returns true.
     * You can run your checking code on the User object for names, roles, etc. You might also want to play a booping noise or a
     * text to speech response when the bot wakes up.
     * @param user The user(s) that woke up the bot, if combinedAudio==true, this is an array, if combinedAudio==false, it will only be user[0]
     * @return true if the bot should start listening to what the user has to say, or false to deny the wakeup request
     */
    boolean botAwakeRequest(User... user);
}
