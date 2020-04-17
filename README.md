# VocalCord
Giving Discord bots a voice with speech recognition and text to speech.

# How does it work?
- VocalCord is a _library_, not a standalone bot. VocalCord is built on the excellent [JDA](https://github.com/DV8FromTheWorld/JDA), providing a dead simple wrapper to receive voice transcripts and generate speech audio. VocalCord is a tool to build whatever you imagination decides.
- [Porcupine](https://github.com/Picovoice/porcupine) is used for wake detection, it's incredibly accurate and works consistently well.
- [Google Speech to Text](https://cloud.google.com/speech-to-text) is used for speech recognition, it's decently fast and provides accurate transcripts.
- [Google Text to Speech](https://cloud.google.com/text-to-speech/) is used for text to speech generation, it works great and is fast.
- VocalCord officially supports Windows and Linux

# Setup
### Porcupine Wake phrases
Porcupine requires you to build a wake phrase AI model for every wake phrase you'd like to use. This process can take about 3 hours,
so if you're eager to get started, do this right away.

1) Create a Porcupine account at [Picovoice Console](https://console.picovoice.ai/ppn)
2) Under the "Wake Words" utility, enter your wake phrase into the "Phrase" box. I haven't had much feedback yet about how carried away you can get with wake words, but as it takes three hours, I would recommend trying to choose crisp, unambigious words that Porcupine is unlikely to get confused with similar words.
3) For Linux, select ```Linux (x86_64)```. For Windows, select ```Windows (x86_64)```.
4) Click "Train" to begin training the model. Check back in about 3 hours.
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
5) Search for and enable ````Cloud Speech-to-Text API``` and ```Cloud Text-to-Speech API```
6) On the left sidebar, select "Credentials", then under "Service Accounts", selected "Manage service accounts". Give your service account a name, and everything at its default. You will need to click the "Create Key" button, make sure JSON is selected, and hit "Create". This will download a JSON file. This is your credentials for using Google APIs, keep it secret!


