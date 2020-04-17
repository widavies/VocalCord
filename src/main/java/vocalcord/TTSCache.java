package vocalcord;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.*;

class TTSCache {

    private final File cacheFile;

    private static final int FREQUENT_THRESHOLD = 15;

    private HashMap<String, byte[]> cachedPhrases;

    // Phrases that TTSCache is currently monitoring to decide if they are frequent enough to be cached
    private final HashMap<String, Integer> considerations = new HashMap<>();

    // Manages some caching related jobs, like IO or periodically cleaning up the hashmap
    private final ThreadPoolExecutor cacheService = new ThreadPoolExecutor(2, 4, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    static class CacheResponse {
        byte[] pcmIfCached;
        boolean shouldCache;

        public static CacheResponse phraseAlreadyCached(byte[] pcmIfCached) {
            CacheResponse r = new CacheResponse();
            r.pcmIfCached = pcmIfCached;
            return r;
        }

        public static CacheResponse shouldCachePhrase() {
            CacheResponse r = new CacheResponse();
            r.shouldCache = true;
            return r;
        }

        public static CacheResponse doNothing() {
            CacheResponse r = new CacheResponse();
            r.shouldCache = false;
            r.pcmIfCached = null;
            return r;
        }
    }

    public TTSCache() throws Exception {
         cacheFile = new File(TTSCache.class.getProtectionDomain().getCodeSource().getLocation().toURI() + File.separator + "vocalcord_phrases.cache");

         if(!cacheFile.exists()) {
             if(cacheFile.createNewFile()) {
                 cachedPhrases = new HashMap<>();
             } else {
                 cachedPhrases = load();
             }
         }

         // Clear considerations every day, this means that a phrase can only be frequent if FREQUENT_THRESHOLD
        // is acquired in a day
        ScheduledExecutorService streamDaemon = Executors.newScheduledThreadPool(1);
        streamDaemon.scheduleAtFixedRate(considerations::clear, 0, 1, TimeUnit.DAYS);
    }

    public CacheResponse checkCache(String phrase) {
        String cleaned = scrubPhrase(phrase);

        byte[] pcm = cachedPhrases.getOrDefault(cleaned, null);

        if(pcm == null) {
            int count = considerations.getOrDefault(cleaned, 0);
            considerations.put(cleaned, ++count);
            if(count >= FREQUENT_THRESHOLD) {
                return CacheResponse.shouldCachePhrase();
            } else {
                return CacheResponse.doNothing();
            }
        }

        return CacheResponse.phraseAlreadyCached(pcm);
    }

    public void cache(String phrase, byte[] pcm) {
        cachedPhrases.put(scrubPhrase(phrase), pcm);

        System.out.println("Phrase \""+phrase+"\" is now considered frequent and will be cached.");

        cacheService.execute(this::save);
    }

    private static String scrubPhrase(String phrase) {
        return phrase.toLowerCase().replaceAll("\\s+", "");
    }

    private void save() {
        try {
            FileOutputStream fos = new FileOutputStream(cacheFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(cachedPhrases);
            oos.close();
            fos.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, byte[]> load() {
        try {
            FileInputStream fis = new FileInputStream(cacheFile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            @SuppressWarnings("unchecked")
            HashMap<String, byte[]> map = (HashMap<String, byte[]>) ois.readObject();
            ois.close();
            fis.close();
            return map;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return new HashMap<>();
    }

}
