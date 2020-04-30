# VocalCord
Giving Discord bots a voice with speech recognition and text to speech.

# Donate
If you find this project useful, a donation helps out a lot!
[Donate](https://www.paypal.me/WillDaviesMN)

# How does it work?
- VocalCord is a _library_, not a standalone bot. VocalCord is built on the excellent [JDA](https://github.com/DV8FromTheWorld/JDA), providing a dead simple wrapper to receive voice transcripts and generate speech audio. VocalCord is a tool to build whatever you imagination decides.
- [Porcupine](https://github.com/Picovoice/porcupine) is used for wake detection, it's incredibly accurate and works consistently well.
- [Google Speech to Text](https://cloud.google.com/speech-to-text) is used for speech recognition, it's decently fast and provides accurate transcripts.
- [Google Text to Speech](https://cloud.google.com/text-to-speech/) is used for text to speech generation, it works great and is fast.
- VocalCord officially supports Windows and Linux
- Thanks to [Olical](https://github.com/Olical/clojure-wake-word-detection) for some great examples that really helped in developing the bot.

# Setup
### Porcupine wake phrases
Porcupine requires you to build a wake phrase AI model for every wake phrase you'd like to use. This process can take about 3 hours,
so if you're eager to get started, do this right away.

1) Create a Porcupine account at [Picovoice Console](https://console.picovoice.ai/ppn)
2) Under the "Wake Words" utility, enter your wake phrase into the "Phrase" box. I haven't had much feedback yet about how carried away you can get with wake words, but as it takes three hours, I would recommend trying to choose crisp, unambigious words that Porcupine is unlikely to get confused with similar words.
3) For Linux, select ```Linux (x86_64)```. For Windows, select ```Windows (x86_64)```.
4) Click "Train" to begin training the model. Check back in about 3 hours.
5) VocalCord supports multiple wake phrases at once or even different wake phrases for different users. Generate a wake phrase model for each wakeup phrase you'd like to use.
### Discord Bot
1) Go to the [Discord Developer Console](https://discordapp.com/developers/applications) and click "New application".
2) On the left sidebar of the application view, selected "Bot"
3) Click "Add Bot"
4) Click "Copy" under the token header. This is your Discord bot token, put it in a safe place (keep it secret!).
5) Select the "OAuth2" tab on the left sidebar
6) Under "Scopes" make sure "bot" is checked.
7) Enable any permissions your bot will utilize under the "Bot permissions" header. You will need to check ```Connect```, ```Speak```, and ```Use Voice Activity``` to use speech recognition and generation facilities.
8) Discord will auto generate a link for you, copy this link and paste it into your browser. From here, you may select which server you'd like to add the bot to.
### Google APIs
1) Navigate to [Google Cloud Console](https://console.cloud.google.com/)
2) In the lop left, select the projects drop down and create a new project.
3) Once your project is created, click the "APIs & Services" card.
4) From here, select the "Dashboard" tab on the left sidebar, click "Enable APIs and Services"
5) Search for and enable ```Cloud Speech-to-Text API``` and ```Cloud Text-to-Speech API```
6) On the left sidebar, select "Credentials", then under "Service Accounts", selected "Manage service accounts". Give your service account a name, and everything at its default. You will need to click the "Create Key" button, make sure JSON is selected, and hit "Create". This will download a JSON file. This is your credentials for using Google APIs, keep it secret! Save it to a location where you will remember where it is.
### Environment
#### Windows
1) You will need to add an environment variable named ```GOOGLE_APPLICATION_CREDENTIALS``` where its value is the path to the Google Credential JSON file you downloaded in the last step.
2) On Windows, open the start menu and search "Edit the system environment variables". Click "Environment Variables" and under System Variables, click "New"
3) For "Variable name", enter ```GOOGLE_APPLICATION_CREDENTIALS```
4) For "Variable value", enter the path to your Google Credentials JSON, for example: ```C:\Users\wdavi\IdeaProjects\VocalCord\vocalcord-gcs.json```. It does not matter where you put this .json file on your system, as long as the PATH points correctly to it.
#### Linux
1) Edit your ```.bashrc``` file by entering ```sudo nano ~/.bashrc```
2) Add the line ```export GOOGLE_APPLICATION_CREDENTIALS="path-to-google-creds.JSON"``` to the end of the file and save. Example: ```export GOOGLE_APPLICATION_CREDENTIALS="/mnt/c/Users/wdavi/IdeaProjects/VocalCord/vocalcord-gcs.json"```
3) Restart your terminal for this change to take effect.
### Java Project
The recommended IDE is [InteliJ IDEA](https://www.jetbrains.com/idea/download/).

1) Download [Java SDK 12.0.2](https://jdk.java.net/archive/) and extract to ```C:\Program Files\Java```. Your installation folder should be something like ```C:\Program Files\Java\jdk-12.0.2```. If you're on Linux, run ```sudo apt-get install openjdk-12-jdk```
1) Click ```New > New Project```
2) On the left side panel, select ```Gradle```, and check ```Java```.
3) Give the project a name and hit ```Finish```
4) Ensure you are using JDK 12
  - ```File > Settings > Build, Execution, Deployment > Gradle > Gradle JVM``` should be set to your JDK 12
  - ```Right click project > Open Module Settings > Project > Project SDK``` should be set to your JDK 12
  - ```Right click project > Open Module Settings > Project > Project language level``` should be ```12 - No new language features```
  - ```Right click project > Open Module Settings > Modules > Module SDK``` should be set to your JDK 12
5) Edit your ```build.gradle``` file to install ```VocalCord```:
  ```
  repositories {
      mavenCentral()
      maven { url 'https://jitpack.io' }
      jcenter()
  }

  dependencies {
      implementation 'net.dv8tion:JDA:4.1.1_136'
      implementation 'com.google.cloud:google-cloud-speech:1.22.6'
      implementation 'com.google.cloud:google-cloud-texttospeech:1.0.2'
      implementation 'com.github.wdavies973:VocalCord:2.2'
  }
  ```
### Dynamic Libraries
VocalCord uses Porcupine for wake detection, however Porcupine does not support Java. Instead, VocalCord uses the Java Native Interface (JNI) to wrap the Porcupine C library in Java bindings. You will need to obtain the Porcupine dynamic library, as well as the VocalCord wrapper dynamic library. VocalCord will load the wrapper library, which will in turn load the Porcupine dynamic library.
#### Linux
1) Create a folder with your root project directory called "native", within this create a subdirectory labeled "linux"
2) [Download libjni_porcupine.so](https://github.com/wdavies973/VocalCord/raw/master/native/linux/libjni_porcupine.so)
3) [Download libpv_porcupine.so](https://github.com/Picovoice/porcupine/raw/master/lib/linux/x86_64/libpv_porcupine.so)
4) [Download porcupine_params.pv](https://github.com/Picovoice/porcupine/raw/master/lib/common/porcupine_params.pv)
5) Move ```libjni_porcupine.so``` and ```libpv_porcupine.so``` into ```native/linux```
6) Move ```porcupine_params.pv``` into ```native```
7) You ```native``` directory should look like [this](https://imgur.com/a/tQJPF4n).
#### Windows
1) Create a folder with your root project directory called "native", within this create a subdirectory labeled "linux"
2) [Download libjni_porcupine.so](https://github.com/wdavies973/VocalCord/raw/master/native/windows/libjni_porcupine.dll)
3) [Download libpv_porcupine.so](https://github.com/Picovoice/porcupine/raw/master/lib/windows/x86_64/libpv_porcupine.dll)
4) [Download porcupine_params.pv](https://github.com/Picovoice/porcupine/raw/master/lib/common/porcupine_params.pv)
5) Move ```libjni_porcupine.so``` and ```libpv_porcupine.so``` into ```native/linux```
6) Move ```porcupine_params.pv``` into ```native```
7) You ```native``` directory should look like [this](https://imgur.com/a/tQJPF4n).
#### Porcupine
Once Porcupine's wake phrase training is done, you should also move your ```wake_phrase.ppn``` file to ```native/```
### Setup Complete!
You are now ready to configure your application and begin hacking.
# Running a basic example
You can find a basic example [here](https://github.com/wdavies973/VocalCord/blob/master/src/main/java/example/ExampleBot.java).
# Configuration
You can read up on most configuration options in the [VocalCord docs](https://github.com/wdavies973/VocalCord/blob/master/src/main/java/vocalcord/VocalCord.java)
# Using a music bot?
JDA enforces a restriction of only one ```AudioSendHandler``` at once. This introduces a problem if you want to use TTS and a music bot. To address this problem, VocalCord implements a audio send multliplexer, which essentially will mix the audio between your [music send handler](https://github.com/sedmelluq/lavaplayer#jda-integration) and VocalCord's internal TTS SendHandler. Currently, there are two send multiplex modes, ```Switch```, which will pause your music while TTS is occuring, and ```Blend``` which will lower the volume of your music bot while TTS is occuring. ```Blend``` is currently not implemented yet.
# Roadmap
Upcoming features:
- ```Blend``` multiplexing mode
- Option to use offline [Picovoice Cheetah](https://github.com/Picovoice/cheetah) voice recognition for faster voice recognition.
- Continuation phrases so the bot can carry out an ongoing conversation
- Improvements to command chain
# Issues/Suggestions/Help
If you need help or have any suggestions, feel free to contact me at wdavies973@gmail.com



