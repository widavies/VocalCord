#include <stdio.h>
#include <pv_porcupine.h>
#include <wakeup_Porcupine.h>

JNIEXPORT jlong JNICALL Java_wakeup_porcupine_Porcupine_init
  (JNIEnv *env, jobject obj, jstring model_raw, jstring keyword_raw, jfloat sens) {

   const char *model = (*env)->GetStringUTFChars(env, model_raw, 0);
   const char * const* keyword = (*env)->GetStringUTFChars(env, keyword_raw, 0);

   pv_porcupine_t *handle;

   float sensArr[1];
   sensArr[0] = sens;

   const pv_status_t status = pv_porcupine_init(model, 1, keyword, sensArr, &handle);

   if (status != PV_STATUS_SUCCESS) {
       printf("Error: Failed to initialise the Porcupine instance.");
   }

   (*env)->ReleaseStringUTFChars(env, model_raw, model);
   (*env)->ReleaseStringUTFChars(env, keyword_raw, keyword);

   return (long)handle;
}

JNIEXPORT void JNICALL Java_wakeup_porcupine_Porcupine_delete
  (JNIEnv *env, jobject obj, jlong handle) {
  pv_porcupine_delete((pv_porcupine_t*)handle);
}

JNIEXPORT jint JNICALL Java_wakeup_porcupine_Porcupine_getFrameLength
  (JNIEnv *env, jobject obj) {
  return pv_porcupine_frame_length();
}

JNIEXPORT jint JNICALL Java_wakeup_porcupine_Porcupine_getSampleRate
  (JNIEnv *env, jobject obj) {
  return pv_sample_rate();
}

JNIEXPORT jboolean JNICALL Java_wakeup_porcupine_Porcupine_process
  (JNIEnv *env, jobject obj, jlong handle, jshortArray pcm_raw) {
  jshort *pcm = (*env)->GetShortArrayElements(env, pcm_raw, 0);
  bool result;

  pv_porcupine_process((pv_porcupine_t*)handle, pcm, &result);

  (*env)->ReleaseShortArrayElements(env, pcm_raw, pcm, 0);

  return result;
}