package com.gmail.wdavies973.lib;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.UserAudio;
import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nonnull;
import java.util.HashMap;

public class VocalEngine implements AudioReceiveHandler {

    private VocalCord cord;

    private HashMap<User, byte[]> audioFeeds;

    public VocalEngine(VocalCord cord) {
        this.cord = cord;
    }

    @Override
    public boolean canReceiveUser() {
        return true;
    }

    @Override
    public void handleUserAudio(@Nonnull UserAudio userAudio) {

    }

    public static void main(String[] args) {
        new Porcupine().print();
    }
}
