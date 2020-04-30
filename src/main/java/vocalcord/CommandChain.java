package vocalcord;

import net.dv8tion.jda.api.entities.User;

import java.util.*;
import java.util.regex.Pattern;

/**
 * The CommandChain class addresses a common problem with trying to match a particular voice command to a transcript of what the user said.
 * Consider the naive approach to this, such as using a ".equals()" or a ".contains()". The problem with this is voice transcripts aren't always
 * 100% accurate, so a direct ".equals()" may not always catch the command. A ".contains()" will make it easy for commands to get intermixed.
 * The CommandChain fixes this problem by using some document similarity maths to instead try to match the meaning of the transcript to a voice command
 * instead of word for word.
 */
public class CommandChain {

    private PhraseVector commandsVector; // a vector of ALL command words
    private double minThreshold = 0.5;
    private final ArrayList<VoiceCommand> commands = new ArrayList<>();
    private VoiceTask fallback;


    public interface VoiceTask {
        /**
         * This task is run when a voice command is detected
         *
         * @param user       The user that spoke the command
         * @param transcript The complete transcript of what the user said
         * @param args       A series of voice arguments, see {@link Builder#addPhrase(String, VoiceTask)}
         */
        void run(User user, String transcript, VoiceArgument<?>[] args);
    }

    public static class VoiceArgument<T> {
        T argument;

        public VoiceArgument(T argument) {
            this.argument = argument;
        }

        @Override
        public String toString() {
            return argument.toString();
        }
    }

    /**
     * User to construct a CommandChain
     */
    public static class Builder {
        private final CommandChain chain = new CommandChain();

        /**
         * Adds a voice command. If the specified {@code phrase} has a meaning similar to the transcript, {@code job} is run.
         * <p>
         * There are a few useful special characters that you can use for this command. Let's say you have a voice command
         * "kick user John" or "set volume to 100". Each commands takes an argument, "John" in the first case, and "100" in
         * the second case. Naturally, you'd want the voice command "kick user John" to also work for "kick user Mark" or the
         * voice command "set volume 100" to also work for "set volume 50". To facilitate this, you can use the following
         * special character sequences:
         * <p>
         * %s - matches a string argument
         * %i - matches an integer argument
         * %d - matches a double argument
         * <p>
         * So for example, in our present situation of kicking a user, you would specify the {@code phrase} as "kick user %s".
         * In this case, the voice command will match a user transcript that starts with "kick user" and ends with any string.
         * Then, when your VoiceTask is run, it will be given a VoiceArgument with the value of whatever "%s" was. You can
         * use as many of these special character sequences as you'd like. For example, "set volume to 100 and kick John" would
         * be represented as "set volume to %i and kick %s". If this command matches, your VoiceTask will be called with two
         * VoiceArguments, the first a volume integer, and the second a user to kick.
         *
         * Disclaimer: I handle these special characters in a semi-naive way, I could use a much better implementation using neural nets or
         *             something if you're interested. Let me know if it gets any really trivial cases wrong and I hope it works decently well for you!
         *
         * @param phrase The phrase that should trigger the command
         * @param task   The task that should be run when the phrase is detected
         * @return Builder object
         */
        public Builder addPhrase(String phrase, VoiceTask task) {
            chain.commands.add(new VoiceCommand(phrase, task));
            return this;
        }

        /**
         * If no voice command matches an incoming transcript with cosine similarity better than {@link CommandChain#minThreshold},
         * this fallback task is run. It can be used for things like the bot saying "Sorry, I didn't get that" If null, VocalCord
         * won't execute anything.
         *
         * @param task The task to run when the user said something, but no voice command matched close enough.
         * @return Builder object
         */
        public Builder withFallback(VoiceTask task) {
            chain.fallback = task;
            return this;
        }

        /**
         * Adjust the min cosine threshold for a voice command to be even considered to be a possible candidate
         *
         * @param minThreshold A value between 0 and 1, a value more towards 0 will let a voice transcript still trigger a voice command even if they are vastly different, a value of 1 will only allow essentially perfect matches
         * @return Builder object
         */
        public Builder withMinThreshold(float minThreshold) {
            chain.minThreshold = minThreshold;
            return this;
        }

        /**
         * Constructs the command chain object
         *
         * @return Returns the CommandChain object
         */
        public CommandChain build() {
            if(chain.commands.size() == 0) throw new RuntimeException("Must provide at least one command");

            chain.commandsVector = new PhraseVector(new HashMap<>());

            for(VoiceCommand cmd : chain.commands) {
                chain.commandsVector = chain.commandsVector.merge(cmd.phraseVector);
            }

            return chain;
        }
    }

    /*
     * Internal code
     */

    private CommandChain() {
    }

    // finds the best matching candidate voice command
    TaskCandidate score(String transcript) {
        double maxScore = -1;
        int maxIndex = -1;
        TaskCandidate leading = null;

        PhraseVector transcriptVector = new PhraseVector(transcript);
        PhraseVector docVector = commandsVector.merge(transcriptVector); // the entire document vector

        for(int i = 0; i < commands.size(); i++) {
            TaskCandidate candidate = new TaskCandidate(commands.get(i), transcript);

            double score = candidate.score(docVector);
            if(score > maxScore) {
                maxScore = score;
                maxIndex = i;
                leading = candidate;
            }
        }

        if(maxIndex == -1) {
            return null;
        } else {
            return leading;
        }
    }

    // runs the best matching candidate voice command
    void fulfillTaskCandidate(User user, TaskCandidate candidate) {
        if(candidate == null || candidate.score < minThreshold && fallback != null) {
            fallback.run(user, candidate == null ? "" : candidate.transcript, new VoiceArgument[0]);
        } else {
            // resolve voice arguments
            candidate.command.task.run(user, candidate.transcript, candidate.args);
        }
    }

    private static class VoiceCommand {
        String phrase;
        PhraseVector phraseVector;
        ArrayList<Param> params;
        VoiceTask task;

        public VoiceCommand(String phrase, VoiceTask task) {
            this.phrase = phrase;
            this.task = task;
            this.phraseVector = new PhraseVector(phrase);

            // Compute parameter contexts
                // ignore stop words
                // ignore other parameters
            params = new ArrayList<>();

            ArrayList<String> tokens = new ArrayList<>(Arrays.asList(phrase.replaceAll("[^a-zA-z0-9.% -]", "").trim().toLowerCase().split("\\s+")));

            // remove stop words
            tokens.removeIf(STOPS::contains);

            // create a parameter context
            for(int i = 0; i < tokens.size(); i++) {
                String word = tokens.get(i);

                if(word.equals("%s") || word.equals("%i") || word.equals("%d")) {
                    params.add(new Param(tokens, i));
                }
            }
        }
    }

    static class TaskCandidate {
        double score; // how closely the TaskCandidate matched a spoken transcript
        String transcript; // the exact transcript that was spoken
        VoiceCommand command;
        VoiceArgument<?>[] args; // the arguments to the VoiceCommand

        private PhraseVector resolvedVector; // a vector version of the command with all parameters resolved

        public TaskCandidate(VoiceCommand command, String transcript) {
            this.transcript = transcript;
            this.command = command;

            for(int i = 1; i < 6; i++) {
                this.args = resolve(i);

                if(this.args != null) {
                    break;
                }
            }

            if(this.args == null) {
                resolvedVector = command.phraseVector;
            }
        }

        private VoiceArgument<?>[] resolve(int numAllowableErrors) {
            /*
             * Resolve any parameters in VoiceCommand
             */

            // step 1, tokenize transcript
            ArrayList<String> tokens = new ArrayList<>(Arrays.asList(transcript.replaceAll("[^a-zA-z0-9. -]", "").trim().toLowerCase().split("\\s+")));

            // step 2, remove all stop words from transcript
            tokens.removeIf(STOPS::contains);

            // step 3, generate a "selection list" of parameter candidates, each with a index to where it occurs in tokens
            class ParamCandidate {
                final String token;
                final int position;
                public ParamCandidate(String token, int position) {
                    this.token = token;
                    this.position = position;
                }
            }

            ArrayList<ParamCandidate> paramCandidates = new ArrayList<>();

            for(int i = 0; i < tokens.size(); i++) {
                paramCandidates.add(new ParamCandidate(tokens.get(i), i));
            }

            // step 4, loop through parameters in the voice command
            VoiceArgument<?>[] args = new VoiceArgument[command.params.size()];

            int index = 0;

            for(Param p : command.params) {
                // The best way to think about this step is that Param "p" is going to take its most
                // desired pick from "paramCandidates", there are three criteria that make a paramCandidate more desirable:
                // It occurs near the beginning of paramCandidates ("p" will pick the closest satisfactory parameter to the start)
                // the types match
                // the param's context works
                for(int i = 0; i < paramCandidates.size(); i++) {
                    ParamCandidate candidate = paramCandidates.get(i);

                    // Do the types match?
                    if("%d".equals(p.param) && isDouble(candidate.token) && p.satisfiesContext(tokens, candidate.position, numAllowableErrors)) {
                        args[index] = new VoiceArgument<>(Double.parseDouble(candidate.token));
                        paramCandidates.remove(i);
                        break;
                    } else if("%i".equals(p.param) && isInteger(candidate.token) && p.satisfiesContext(tokens, candidate.position, numAllowableErrors)) {
                        args[index] = new VoiceArgument<>(Integer.parseInt(candidate.token));
                        paramCandidates.remove(i);
                        break;
                    } else if("%s".equals(p.param) && p.satisfiesContext(tokens, candidate.position, numAllowableErrors)) {
                        StringBuilder sb = new StringBuilder(candidate.token);

                        paramCandidates.remove(i);

                        for(int tmp = i; tmp < paramCandidates.size(); tmp++) {
                            ParamCandidate c = paramCandidates.get(tmp);

                            if(p.satisfiesContext(tokens, c.position, 1) && tmp < paramCandidates.size() - (command.params.size() - index - 1)) {
                                sb.append(" ").append(c.token);
                                paramCandidates.remove(tmp);
                                tmp--;
                            } else {
                                break;
                            }
                        }

                        args[index] = new VoiceArgument<>(sb.toString());

                        break;
                    }
                }

                index++;
            }

            // step 5, apply the parameter assignments to the command phrase and create a vector with it
            String resolvedPhrase = command.phrase;

            index = 0;
            for(Param p : command.params) {
                if(args[index] != null) {
                    resolvedPhrase = resolvedPhrase.replaceFirst(p.param, args[index].toString());
                } else {
                    return null;
                }

                index++;
            }
            this.resolvedVector = new PhraseVector(resolvedPhrase);
            return args;
        }

        public double score(PhraseVector documentVector) {
            this.score = new PhraseVector(transcript).cosine(documentVector.words, resolvedVector);
            return this.score;
        }
    }

    // stores the words in the voice command that occur before and after the voice command
    private static class Param {
        private final ArrayList<String> wordsBefore = new ArrayList<>(), wordsAfter = new ArrayList<>();
        private final String param;

        Param(ArrayList<String> words, int index) {
            for(int i = 0; i < index; i++) {
                if(words.get(i).equals("%s") || words.get(i).equals("%i") || words.get(i).equals("%d")) continue;
                wordsBefore.add(words.get(i));
            }
            param = words.get(index);

            for(int i = index + 1; i < words.size(); i++) {
                if(words.get(i).equals("%s") || words.get(i).equals("%i") || words.get(i).equals("%d")) continue;
                wordsAfter.add(words.get(i));
            }
        }

        public boolean satisfiesContext(ArrayList<String> context, int consideredToken, int numErrorsAllowed) {
            int errors = 0;

            // Create temporary sets for the words
            ArrayList<String> before = new ArrayList<>();
            ArrayList<String> after = new ArrayList<>();

            for(int i = 0; i < consideredToken; i++) {
                before.add(context.get(i));
            }

            for(int i = consideredToken + 1; i < context.size(); i++) {
                after.add(context.get(i));
            }

            for(String req : wordsBefore) {
                if(before.contains(req)) {
                    before.remove(req);
                } else {
                    errors++;
                }
            }

            for(String req : wordsAfter) {
                if(after.contains(req)) {
                    after.remove(req);
                } else {
                    errors++;
                }
            }

            return errors < numErrorsAllowed;
        }
    }

    private static class PhraseVector {
        private final HashMap<String, Integer> words;

        PhraseVector(String phrase) {
            words = new HashMap<>();

            ArrayList<String> tokens = tokenize(phrase);
            for(String word : tokens) {
                int count = words.getOrDefault(word, 0);
                words.put(word, count + 1);
            }
        }

        private PhraseVector(HashMap<String, Integer> words) {
            this.words = words;
        }

        private static ArrayList<String> tokenize(String phrase) {
            phrase = phrase.replaceAll("%s", "").replaceAll("%i", "").replaceAll("%d", "")
                    .replaceAll("[^a-zA-z0-9 -]", "").trim().toLowerCase();

            ArrayList<String> tokens = new ArrayList<>(Arrays.asList(phrase.split("\\s+")));

            tokens.removeIf(STOPS::contains);

            return tokens;
        }

        // Does not preserve frequencies
        PhraseVector merge(PhraseVector vec) {
            HashMap<String, Integer> merged = new HashMap<>();
            merged.putAll(words);
            merged.putAll(vec.words);
            return new PhraseVector(merged);
        }

        int[] asVector(HashMap<String, Integer> termMatrix) {
            int[] vector = new int[termMatrix.size()];

            int index = 0;
            for(String key : termMatrix.keySet()) {
                vector[index] = words.getOrDefault(key, 0);
                index++;
            }

            return vector;
        }

        double cosine(HashMap<String, Integer> termMatrix, PhraseVector vec) {
            int[] vec1 = asVector(termMatrix);
            int[] vec2 = vec.asVector(termMatrix);

            if(vec1.length != vec2.length) {
                throw new RuntimeException("Vector lengths must be the same.");
            }

            int innerProduct = 0;
            double vec1Length = 0;
            double vec2Length = 0;

            for(int i = 0; i < vec1.length; i++) {
                innerProduct += (vec1[i] * vec2[i]);

                vec1Length += (vec1[i] * vec1[i]);
                vec2Length += (vec2[i] * vec2[i]);
            }

            return (double) innerProduct / (Math.sqrt(vec1Length) * Math.sqrt(vec2Length));
        }
    }

    // common english words to remove
    private static final HashSet<String> STOPS = new HashSet<>();

    static {
        final String[] STOP_WORDS =
                {"i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself",
                        "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are", "was", "were",
                        "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at",
                        "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over",
                        "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no",
                        "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now"};

        STOPS.addAll(Arrays.asList(STOP_WORDS));
    }

    private static final Pattern R_INTEGER = Pattern.compile("^[-+]?\\d+$");
    private static final Pattern R_DOUBLE = Pattern.compile("\\d+\\.?\\d*");

    private static boolean isInteger(String s) {
        return R_INTEGER.matcher(s).matches();
    }

    private static boolean isDouble(String s) {
        return R_DOUBLE.matcher(s).matches();
    }
}
