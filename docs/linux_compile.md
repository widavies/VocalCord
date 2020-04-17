### Introduction
The following guide describes how to create the ```native/linux/libjni_porcupine.so``` dynamic library. This library is a light JNI
wrapper over the ```libpv_porcupine.so``` Porcupine dynamic library. Many thanks to [Olical](https://github.com/Olical/clojure-wake-word-detection) for
this compilation process. If someone wants to make a script that does this all automatically, have at it.

### Compiling for Linux
1) First, make a directory for Porcupine to reside in: ```mkdir -p wake-engine```
2) Clone: ```cd wake-engine && git clone git@github.com:Picovoice/Porcupine.git```
2) Ensure back in top directory: ```VocalCord/```
3) Create the JNI header file: ```javac -h wake-native/jni src/main/java/wakeup/Porcupine.java```
4) Compile: ```gcc -shared -O3 -I/usr/include -I/usr/lib/jvm/{YOUR-JVM-EDITION}/include -I/usr/lib/jvm/{YOUR_JVM-EDITION}/include/linux -Iwake-engine/Porcupine/include -Inative/jni src/main/c/porcupine.c -o native/linux/libjni_porcupine.so -fPIC```
    - Example: ```gcc -shared -O3 -I/usr/include -I/usr/lib/jvm/java-14-oracle/include -I/usr/lib/jvm/java-14-oracle/include/linux -Iwake-engine/Porcupine/include -Iwake-engine/jni src/main/c/porcupine.c -o native/linux/libjni_porcupine.so -fPIC```
