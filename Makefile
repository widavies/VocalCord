WAKE_PHRASE := hey porcupine

# If you want to execute with make
#default: wake-engine
#	LD_LIBRARY_PATH="wake-engine/jni" gradle execute # LD_DEBUG=libs

wake-engine: wake-engine/Porcupine wake-engine/jni/libjava_porcupine.so src/main/java/wakeup/Porcupine.class

# This downloads the latest Porcupine Github repository
wake-engine/Porcupine:
	mkdir -p wake-engine
	cd wake-engine && git clone git@github.com:Picovoice/Porcupine.git
	cp wake-engine/Porcupine/lib/linux/x86_64/libpv_porcupine.so wake-engine/jni/libpv_porcupine.so

src/main/java/wakeup/Porcupine.class wake-engine/jni/wakeup_Porcupine.h: src/main/java/wakeup/Porcupine.java
	mkdir -p wake-engine/jni
	javac -h wake-engine/jni src/main/java/wakeup/Porcupine.java

# Compiles the c files
# https://stackoverflow.com/questions/14788345/how-to-install-the-jdk-on-ubuntu-linux
wake-engine/jni/libjava_porcupine.so: wake-engine/jni/wakeup_Porcupine.h src/main/c/porcupine.c
	gcc -shared -O3 \
		-I/usr/include \
		-I/usr/lib/jvm/java-8-openjdk-amd64/include \
		-I/usr/lib/jvm/java-8-openjdk-amd64/include/linux \
		-Iwake-engine/Porcupine/include \
		-Iwake-engine/jni \
		src/main/c/porcupine.c \
		wake-engine/Porcupine/lib/linux/x86_64/libpv_porcupine.so \
		-o wake-engine/jni/libjava_porcupine.so