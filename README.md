# VocalCord
Giving Discord bots a voice with speech recognition and text to speech.

# Under Construction
VocalCord is currently under a complete rewrite. Expect a new release soon.

# How does it work?
VocalCord receives audio data from Discord through the [JDA](https://github.com/DV8FromTheWorld/JDA) Java Discord API library. VocalCord will passively process audio data using [Porcupine](https://github.com/Picovoice/porcupine) to listen for the wake phrase. This library is free for non-commerical use, and works quite well. As soon as the wake phrase is detected, Porcupine will pass control to Google Cloud Speech to process the subsequent voice command. Google Cloud Speech API gives you 60 minutes of free audio recognition per month, with a small fee for going over. 

# Integrating
VocalCord abstracts you away from all the sound processing and provides a simple interface to configure your wake phrases, user permissions, and voice commands. VocalCord is not a complete bot, it won't work on its own. VocalCord is an existing plugin for any JDA bots you'd like to voice-enable and requires you to implement voice command procedures and any other functionality you'd like on your own. This is the most flexible for users. Development with VocalCord is very similar to developing a JDA bot. However, Porcupine does not have a Java version, so instead VocalCord uses the Java Native Interface to communicate with the Porcupine C binaries. The tooling for this is a bit complicated and requires you to use Linux (for ```make```, ```gcc```, and other utils), but will add hair to your chest. I developed VocalCord on Windows using WSL, which worked great for me. I may add native Windows support in the future, but as I suspect most bots are hosted on Linux anyway, I don't see this as a huge issue right now.

# Setup
1) Currently, Linux is required to compile the library. I highly recommend using [WSL](https://docs.microsoft.com/en-us/windows/wsl/install-win10) if you're on Windows.
2) Before you do anything, you need to train a model based off what you'd like your wake phrase to be. Make sure your wake phrase is non-ambigious ("bot" may get confused with "bought"). On the [Picovoice Console](https://console.picovoice.ai/ppn), draft a new wake word, enter your wake phrase and select ```Linux(x86_64)``` for the platform. You may train multiple wake phrases for your bot if you'd like. Make sure to do this right away if you're eager to get started, because the model takes around 3 hours to train before you can download it.
3) 
