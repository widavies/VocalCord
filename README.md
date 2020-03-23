# VocalCord
Giving Discord bots a voice with speech recognition and text to speech.

# How does it work?
VocalCord receives audio data from Discord through the [JDA](https://github.com/DV8FromTheWorld/JDA) Java Discord API library. VocalCord
will begin listening to small chunks of audio for its wakeup phrase. It uses CMUSphinx voice recognition for this, so the continous wakeup
phrase detection won't cost anything. However, CMUSphinx's accuracy has nothing on Google Cloud's Speech API. After the wakeup phrase is
detected, VocalCord will pass control of voice recongition to the Google Cloud Speech API, which can provide much more accurate transcriptions.
Google Cloud Speech API gives you 60 minutes of free audio recognition per month, with a small fee for going over. Since VocalCord is only detecting
voice commands, you shouldn't have too much of a problem fitting in this 60 minutes limit.

# Installation
### Prerequisites
1) Add the following to your build.gradle:
```java
compile 'net.dv8tion:JDA:3.5.1_339'
compile 'com.google.cloud:google-cloud-speech:0.32.0-alpha'
compile group: 'edu.cmu.sphinx', name: 'sphinx4-core', version:'5prealpha-SNAPSHOT'
compile group: 'edu.cmu.sphinx', name: 'sphinx4-data', version:'5prealpha-SNAPSHOT'
``` 
5) [Setup an authentication](https://cloud.google.com/speech/docs/reference/libraries) token for Google Cloud Speech API, you'll need to export this to your PATH.   
6) VocalCord is a library to assist you during bot creation, it's not a standalone bot. Thus, I'm assuming you already have a Discord bot token.
7) Copy the ```SpeechCallback``` and ```SpeechReceiver``` classes from the ```speechRecognition``` package to your project and ```SilenceAudioSender``` from the ```speechGeneration``` package, you don't need to compile or setup a .jar. 
# Usage
See example usage [here](https://github.com/wdavies973/VocalCord/blob/master/src/main/java/com/cpjd/main/Bot.java).

IMPORTANT: Due to a Discord audio bug, as the VocalCord bot is connecting to the voice channel, you must make some noise through your micorophone to properly initialize audio recognition. Make 5 seconds worth of noise and the bot should work until you have to restart it.

The SilenceAudioSendHandler (the default, built in) will take care of this if you use it as your sending handler.

# VocalCord configuration settings
-```wakeupPhrases``` you can add wakeup phrases like so: ```vocalCordEngine.addWakeupPhrase("Yo Discord!");```. Wakeup phrases are certain phrases that will "awaken" the bot when said. You can add as many as you like, but <5 is the ideal amount. Choose phrases that are easily recognizable and make note of certain nuances. "hey bought" will get detected more often than "hey bot"   
-```combinedAudio``` this configures whether to collect audio from multiple users or restrict it to one user, currently, Discord developers have acknowledged a bug regarding user audio, so this MUST BE SET TO TRUE.  
-```wakeupChunkSize``` this is the size (in seconds) of sound that will be scanned for wakeup phrases. You shouldn't need to change it, but if the bot is having trouble waking up, give it a shot.  
-```voiceCommandTimeout``` when the bot is awake, it will listen to a command until 10 seconds have passed OR this many seconds of silence have occurred. You can change the amount of quiet needed to stop listening here.  

For questions, feedback, or bugs, contact me at wdavies973@gmail.com
