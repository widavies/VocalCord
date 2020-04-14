#include <stdio.h>
#include <pv_porcupine.h>
#include <wakeup_Porcupine.h>

JNIEXPORT jint JNICALL Java_wakeup_Porcupine_getFrameLength
  (JNIEnv *env, jobject obj) {
  return pv_porcupine_frame_length();
}

JNIEXPORT jint JNICALL Java_wakeup_Porcupine_getSampleRate
  (JNIEnv *env, jobject obj) {
  return pv_sample_rate();
}

JNIEXPORT jlong JNICALL Java_wakeup_Porcupine_init
  (JNIEnv *env, jobject obj, jstring model_raw, jstring keyword_raw, jfloat sens) {

    const char *model = (*env)->GetStringUTFChars(env, model_raw, 0);
    const char * keyword = (*env)->GetStringUTFChars(env, keyword_raw, 0);

    printf("Initializing Porcupine, using keyword directory %s...\n", (char *)keyword);
    printf("Settings file %s\n", model);
    printf("Testing");

    pv_porcupine_t *handle;

    float sensArr[1];
    sensArr[0] = 1;

    const char * keyword_paths[1] = { keyword };

    const pv_status_t status = pv_porcupine_init(model, 1, keyword_paths, sensArr, &handle);

    if (status != PV_STATUS_SUCCESS) {
       printf("Error: Failed to initialise the Porcupine instance.");
    }

    (*env)->ReleaseStringUTFChars(env, model_raw, model);
    (*env)->ReleaseStringUTFChars(env, keyword_raw, keyword);

    return (long)handle;
}

JNIEXPORT jboolean JNICALL Java_wakeup_Porcupine_process
  (JNIEnv *env, jobject obj, jlong handle, jshortArray pcm_raw) {
  jshort *pcm = (*env)->GetShortArrayElements(env, pcm_raw, 0);
  int32_t keyword_index;

  pv_porcupine_process((pv_porcupine_t*)handle, pcm, &keyword_index);

  (*env)->ReleaseShortArrayElements(env, pcm_raw, pcm, 0);

  bool result = keyword_index != -1;
  return result;
}

JNIEXPORT void JNICALL Java_wakeup_Porcupine_delete
  (JNIEnv *env, jobject obj, jlong handle) {
  pv_porcupine_delete((pv_porcupine_t*)handle);
}
