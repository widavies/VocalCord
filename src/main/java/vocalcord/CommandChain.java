package vocalcord;

import java.util.ArrayList;

public class CommandChain {

    // https://lmu-pms.github.io/irom-blog/posts/Doc2Vec.html

    private static class Command {
        private String phrase;
        private Runnable job;

        private Command() {}

        static Command create(String phrase, Runnable job) {
            Command cmd = new Command();
            cmd.phrase = phrase;
            cmd.job = job;



            return cmd;
        }
    }

    private double threshold;

    public CommandChain() {
        threshold = 0.5;
    }

    public CommandChain(double threshold) {
        this.threshold = threshold;
    }

    private ArrayList<Command> commands = new ArrayList<>();

    public CommandChain add(String phrase, Runnable job) {
        commands.add(Command.create(phrase, job));
        return this;
    }

    public void push(String phrase) {

    }

}
