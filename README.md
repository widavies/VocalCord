# VocalCord
Giving Discord bots a voice with speech recognition and text to speech.

# Under Construction
VocalCord is currently under a complete rewrite. Expect a new release soon.

# How does it work?
VocalCord receives audio data from Discord through the [JDA](https://github.com/DV8FromTheWorld/JDA) Java Discord API library. VocalCord
will begin listening to small chunks of audio for its wakeup phrase. It uses CMUSphinx voice recognition for this, so the continous wakeup
phrase detection won't cost anything. However, CMUSphinx's accuracy has nothing on Google Cloud's Speech API. After the wakeup phrase is
detected, VocalCord will pass control of voice recongition to the Google Cloud Speech API, which can provide much more accurate transcriptions.
Google Cloud Speech API gives you 60 minutes of free audio recognition per month, with a small fee for going over. Since VocalCord is only detecting
voice commands, you shouldn't have too much of a problem fitting in this 60 minutes limit.

# Setup
1) Currently, Linux is required to compile the library. I highly recommend using [WSL](https://docs.microsoft.com/en-us/windows/wsl/install-win10) if you're on Windows.
