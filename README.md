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
1) Make sure the JDA library is installed by adding 
```java
compile 'net.dv8tion:JDA:3.5.1_339'
``` 
to your build.gradle.
```
2) Make sure the Google Cloud Speech API is installed by adding 
```java
compile 'com.google.cloud:google-cloud-speech:0.32.0-alpha'
``` 
to your build.gradle
3) Make sure Kotlin is installed by adding 
```java
compile "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version"
``` 
to your build.gradle
Note: Kotlin is only used for a small audio fix, not for core library, so you don't need to know Kotlin to use this library.
4) Make sure CMUSphinx is installed by adding:  
```java
compile group: 'edu.cmu.sphinx', name: 'sphinx4-core', version:'5prealpha-SNAPSHOT'
compile group: 'edu.cmu.sphinx', name: 'sphinx4-data', version:'5prealpha-SNAPSHOT'
```
to your build.gradle
5) [Setup an authentication](https://cloud.google.com/speech/docs/reference/libraries) token for Google Cloud Speech API, you'll need to export this to the path  
6) VocalCord is a library to assist you during bot creation, it's not a standalone bot. Thus, I'm assuming you already have a Discord bot token.

# Usage

