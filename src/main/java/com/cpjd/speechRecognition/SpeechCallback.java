package com.cpjd.speechRecognition;

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
     * You can run your checking code on the User object for names, roles, etc.
     * @param user The user that woke up the bot
     * @return true if the bot should start listening to what the user has to say
     */
    boolean botAwakeRequest(User user);
} // end class
