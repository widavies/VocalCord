### Introduction
The following guide describes how to create the ```native/windows/libjni_porcupine.dll``` dynamic library. This library is a light JNI
wrapper over the ```libpv_porcupine.dll``` Porcupine dynamic library. If someone wants to make a script that does this all automatically, have at it.

### Compiling for Windows
1) First, make a directory for Porcupine to reside in: ```mkdir -p wake-engine```
2) Clone: ```cd wake-engine && git clone git@github.com:Picovoice/Porcupine.git```
2) Ensure back in top directory: ```VocalCord/```
3) Create the JNI header file: ```javac -h native/jni src/main/java/wakeup/Porcupine.java```
4) You will need to have Microsoft Visual Studio installed using C/C++ build tools. Then, click start, go to the Visual Studio 2019 folder and
launch ```x86_x64 Cross Tools Command Prompt for VS 2019```. Then, within that prompt, CD to the ```VocalCord/``` directory. You can also optionally use MinGW 64-bit for Windows (skip to step 8).
5) Compile using: ```cl /I "wake-engine\Porcupine\include" /I "native\jni" /I "C:\Program Files\Java\{YOUR-JAVA-VERSION-HERE}\include" /I "C:\Program Files\Java\{YOUR-JAVA-VERSION-HERE}\include\win32" /LD src/main/c/porcupine.c```
    - Example: ```cl /I "wake-engine\Porcupine\include" /I "native\jni" /I "C:\Program Files\Java\jdk-14.0.1\include" /I "C:\Program Files\Java\jdk-14.0.1\include\win32" /LD src/main/c/porcupine.c```
7) This will create a bunch of files in the ```VocalCord``` directory. I can't figure out how to get them to go to ```native/windows``` automatically (```/OUT``` isn't working for me),
so you will need to manually rename ```porcupine.dll``` to ```libjni_porcupine.dll``` and move it into the ```native/windows``` directory.
8) If you are using the [64-bit version of MinGW](http://mingw-w64.org/doku.php/download/mingw-builds) for Windows, you can compile using: ```gcc -shared -O3 -I "C:\Program Files\Java\{YOUR-JAVA-VERSION}\include" -I "C:\Program Files\Java\{YOUR-JAVA-VERSION}\include\win32" -I wake-engine/Porcupine/include -I native/jni src/main/c/porcupine.c -o native/windows/libjni_porcupine.dll```
    - Example: ```gcc -shared -O3 -I "C:\Program Files\Java\jdk-14.0.1\include" -I "C:\Program Files\Java\jdk-14.0.1\include\win32" -I wake-engine/Porcupine/include -I wake-native/jni src/main/c/porcupine.c -o native/windows/libjni_porcupine.dll``` 