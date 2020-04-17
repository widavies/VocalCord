#include <stdio.h>
#include <windows.h>
#include <wakeup_Porcupine.h>
#include <pv_porcupine.h>

typedef int (__cdecl * porc_init)(const char*, int32_t,const char*const*,const float *, pv_porcupine_t**);
typedef void (__cdecl * porc_delete)(pv_porcupine_t*);
typedef pv_status_t (__cdecl * porc_process)(pv_porcupine_t*object,const int16_t*,int32_t*);
typedef int32_t (__cdecl * porc_frame_length)();
typedef int (__cdecl * porc_sample_rate)();

porc_init f_init;
porc_delete f_delete;
porc_process f_process;
porc_frame_length f_frame_length;
porc_sample_rate f_sample_rate;

void loadDLL(const char * pv_porcupine_dll_location) {
    // Load DLL
    HINSTANCE proc = LoadLibrary(pv_porcupine_dll_location);

    if(!proc) {
        printf("Failed to libpv_porcupine.dll, is it in the right location?\n");
        return;
    } else {
        printf("Loaded porcupine JNI DLL wrapper successfully.\n");
    }

    f_init = (porc_init)GetProcAddress(proc, "pv_porcupine_init");
    f_process = (porc_process)GetProcAddress(proc, "pv_porcupine_process");
    f_delete = (porc_delete)GetProcAddress(proc, "pv_porcupine_delete");
    f_sample_rate = (porc_sample_rate)GetProcAddress(proc, "pv_sample_rate");
    f_frame_length = (porc_frame_length)GetProcAddress(proc, "pv_porcupine_frame_length");

    if(!f_init || !f_process || !f_delete || !f_sample_rate || !f_frame_length) {
        printf("Failed to locate required functions from the DLL.\n");
        return;
    }
}

JNIEXPORT jint JNICALL Java_wakeup_Porcupine_getFrameLength
  (JNIEnv *env, jobject obj) {
  return f_frame_length();
}

JNIEXPORT jint JNICALL Java_wakeup_Porcupine_getSampleRate
  (JNIEnv *env, jobject obj) {
  return f_sample_rate();
}

JNIEXPORT jlong JNICALL Java_wakeup_Porcupine_init
  (JNIEnv *env, jobject obj, jstring dll_location_raw, jstring model_raw, jstring keyword_raw, jfloat sens) {

    const char * dll_location= (*env)->GetStringUTFChars(env, dll_location_raw, 0);

    loadDLL(dll_location);

    const char *model = (*env)->GetStringUTFChars(env, model_raw, 0);
    const char * keyword = (*env)->GetStringUTFChars(env, keyword_raw, 0);

    printf("Initializing Porcupine, using keyword directory %s...\n", (char *)keyword);
    printf("Settings file %s\n", model);

    pv_porcupine_t *handle;

    float sensArr[1];
    sensArr[0] = 1;

    const char * keyword_paths[1] = { keyword };

    const pv_status_t status = f_init(model, 1, keyword_paths, sensArr, &handle);

    if (status != PV_STATUS_SUCCESS) {
       printf("Error: Failed to initialise the Porcupine instance.");
    }

    (*env)->ReleaseStringUTFChars(env, model_raw, model);
    (*env)->ReleaseStringUTFChars(env, keyword_raw, keyword);
    (*env)->ReleaseStringUTFChars(env, dll_location_raw, dll_location);

    return (long long)handle;
}

JNIEXPORT jboolean JNICALL Java_wakeup_Porcupine_process (JNIEnv *env, jobject obj, jlong handle, jshortArray pcm_raw) {
  jshort *pcm = (*env)->GetShortArrayElements(env, pcm_raw, 0);
  int32_t keyword_index;

  f_process((pv_porcupine_t*)handle, (int16_t *)pcm, &keyword_index);

  (*env)->ReleaseShortArrayElements(env, pcm_raw, pcm, 0);

  bool result = keyword_index != -1;
  return result;
}

JNIEXPORT void JNICALL Java_wakeup_Porcupine_delete
  (JNIEnv *env, jobject obj, jlong handle) {
  f_delete((pv_porcupine_t*)handle);
}

// For testing as a
//int main() {
//    loadDLL();
//
//    const char *model = "C:\\Users\\wdavi\\IdeaProjects\\VocalCord\\wake-engine\\Porcupine\\lib\\common\\porcupine_params.pv";
//    const char * keyword = "C:\\Users\\wdavi\\IdeaProjects\\VocalCord\\wake-engine\\wake_phrase_win32.ppn";
//
//    printf("Initializing Porcupine, using keyword directory %s...\n", (char *)keyword);
//    printf("Settings file %s\n", model);
//
//    pv_porcupine_t *handle;
//
//    float sensArr[1];
//    sensArr[0] = 1;
//
//    const char * keyword_paths[1] = { keyword };
//
//    const pv_status_t status = f_init(model, 1, keyword_paths, sensArr, &handle); //pv_porcupine_init(model, 1, keyword_paths, sensArr, &handle);
//
//    if (status != PV_STATUS_SUCCESS) {
//       printf("Error: Failed to initialise the Porcupine instance.");
//    }
//
//    int32_t keyword_index;
//
//    const int16_t pcm[512];
//
//    f_process((pv_porcupine_t*)handle, pcm, &keyword_index);
//    printf("%d", keyword_index);
//
//    return (long long)handle;
//}