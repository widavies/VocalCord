# VocalCord
Giving Discord bots a voice with speech recognition and text to speech.

# How does it work?
VocalCord receives audio data from Discord through the [JDA](https://github.com/DV8FromTheWorld/JDA) Java Discord API library. VocalCord
will begin listening to small chunks of audio for its wakeup phrase. It uses CMUSphinx voice recognition for this, so the continous wakeup
phrase detection won't cost anything. However, CMUSphinx's accuracy has nothing on Google Cloud's Speech API. After the wakeup phrase is
detected, VocalCord will pass control of voice recongition to the Google Cloud Speech API, which can provide much more accurate transcriptions.
Google Cloud Speech API gives you 60 minutes of free audio recognition per month, with a small fee for going over. Since VocalCord is detects
voice commands, you shouldn't have too much of a problem fitting in this 60 minutes limit.

# Installation
### Prerequisites
1) Add the following to your build.gradle:
```java
compile 'net.dv8tion:JDA:3.5.1_339'
compile 'com.google.cloud:google-cloud-speech:0.32.0-alpha'
//compile "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version"
compile group: 'edu.cmu.sphinx', name: 'sphinx4-core', version:'5prealpha-SNAPSHOT'
compile group: 'edu.cmu.sphinx', name: 'sphinx4-data', version:'5prealpha-SNAPSHOT'
``` 
5) [Setup an authentication](https://cloud.google.com/speech/docs/reference/libraries) token for Google Cloud Speech API, you'll need to export this to your PATH.   
6) VocalCord is a library to assist you during bot creation, it's not a standalone bot. Thus, I'm assuming you already have a Discord bot token.

# Usage

