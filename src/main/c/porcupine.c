#include <stdio.h>
#include <wakeup_Porcupine.h>
#include <pv_porcupine.h>

#ifdef _WIN32
    #include <windows.h>

    typedef int (__cdecl * porcupine_init)(const char*, int32_t,const char*const*,const float *, pv_porcupine_t**);
    typedef void (__cdecl * porcupine_delete)(pv_porcupine_t*);
    typedef pv_status_t (__cdecl * porcupine_process)(pv_porcupine_t*object,const int16_t*,int32_t*);
    typedef int32_t (__cdecl * porcupine_frame_length)();
    typedef int (__cdecl * porcupine_sample_rate)();

    porcupine_init f_init;
    porcupine_delete f_delete;
    porcupine_process f_process;
    porcupine_frame_length f_frame_length;
    porcupine_sample_rate f_sample_rate;

    bool loadDLL(const char * pv_porcupine_dll_location) {
        // Load DLL
        HINSTANCE proc = LoadLibrary(pv_porcupine_dll_location);

        if(!proc) {
            printf("Failed to load %s, is it in the right location?\n", pv_porcupine_dll_location);
            return false;
        }

        f_init = (porcupine_init)GetProcAddress(proc, "pv_porcupine_init");
        f_process = (porcupine_process)GetProcAddress(proc, "pv_porcupine_process");
        f_delete = (porcupine_delete)GetProcAddress(proc, "pv_porcupine_delete");
        f_sample_rate = (porcupine_sample_rate)GetProcAddress(proc, "pv_sample_rate");
        f_frame_length = (porcupine_frame_length)GetProcAddress(proc, "pv_porcupine_frame_length");

        if(!f_init || !f_process || !f_delete || !f_sample_rate || !f_frame_length) {
            printf("Failed to locate required functions from the DLL. Is it corrupt?\n");
            return false;
        } else {
            printf("Loaded porcupine JNI Porcupine DLL wrapper successfully.\n");
            return true;
        }
    }
#else // linux (maybe works for mac?)
    #include <dlfcn.h>

    int (*f_init)(const char*, int32_t,const char*const*,const float *, pv_porcupine_t**);
    void (*f_delete)(pv_porcupine_t*);
    pv_status_t (*f_process)(pv_porcupine_t*object,const int16_t*,int32_t*);
    int32_t (*f_frame_length)();
    int (*f_sample_rate)();

    bool loadSO(const char * pv_porcupine_so_location) {
        void * handle = dlopen(pv_porcupine_so_location, RTLD_LAZY);
        if(!handle) {
            printf("Failed to load %s, is it in the right location?\n", pv_porcupine_so_location);
            return false;
        }

        dlerror(); // clear any existing errors
        f_init = dlsym(handle, "pv_porcupine_init");
        f_process = dlsym(handle, "pv_porcupine_process");
        f_delete = dlsym(handle, "pv_porcupine_delete");
        f_sample_rate = dlsym(handle, "pv_sample_rate");
        f_frame_length = dlsym(handle, "pv_porcupine_frame_length");

        if(dlerror() != NULL) {
            printf("Failed to locate required functions from the SO. Is it corrupt?\n");
            return false;
        } else {
            printf("Loaded porcupine JNI Porcupine DLL wrapper successfully.\n");
            return true;
        }
    }
#endif

JNIEXPORT jint JNICALL Java_wakeup_Porcupine_getFrameLength (JNIEnv *env, jobject obj) {
    #ifdef _WIN32
        return f_frame_length();
    #else
        return (*f_frame_length)();
    #endif
}

JNIEXPORT jint JNICALL Java_wakeup_Porcupine_getSampleRate (JNIEnv *env, jobject obj) {
    #ifdef _WIN32
        return f_sample_rate();
    #else
        return (*f_sample_rate)();
    #endif
}

JNIEXPORT jlong JNICALL Java_wakeup_Porcupine_init
    (JNIEnv *env, jobject obj, jstring porcupine_location_raw, jstring model_raw, jstring keyword_raw, jfloat sens) {

    const char * porcupine_location = (*env)->GetStringUTFChars(env, porcupine_location_raw, 0);

    #ifdef _WIN32
        loadDLL(porcupine_location);
    #else
        loadSO(porcupine_location);
    #endif

    const char *model = (*env)->GetStringUTFChars(env, model_raw, 0);
    const char * keyword = (*env)->GetStringUTFChars(env, keyword_raw, 0);

    printf("Initializing Porcupine, using keyword directory %s...\n", (char *)keyword);
    printf("Settings file %s\n", model);

    pv_porcupine_t *handle;

    float sensArr[1];
    sensArr[0] = 1;

    const char * keyword_paths[1] = { keyword };

    #ifdef _WIN32
       const pv_status_t status = f_init(model, 1, keyword_paths, sensArr, &handle);
    #else
       const pv_status_t status = (*f_init)(model, 1, keyword_paths, sensArr, &handle);
    #endif

    if (status != PV_STATUS_SUCCESS) {
       printf("Error: Failed to initialise the Porcupine instance.");
    }

    (*env)->ReleaseStringUTFChars(env, model_raw, model);
    (*env)->ReleaseStringUTFChars(env, keyword_raw, keyword);
    (*env)->ReleaseStringUTFChars(env, porcupine_location_raw, porcupine_location);

    return (long long)handle;
}

JNIEXPORT jboolean JNICALL Java_wakeup_Porcupine_process (JNIEnv *env, jobject obj, jlong handle, jshortArray pcm_raw) {
    jshort *pcm = (*env)->GetShortArrayElements(env, pcm_raw, 0);
    int32_t keyword_index;

    #ifdef _WIN32
        f_process((pv_porcupine_t*)handle, (int16_t *)pcm, &keyword_index);
    #else
        (*f_process)((pv_porcupine_t*)handle, (int16_t *)pcm, &keyword_index);
    #endif

    (*env)->ReleaseShortArrayElements(env, pcm_raw, pcm, 0);

    bool result = keyword_index != -1;
    return result;
}

JNIEXPORT void JNICALL Java_wakeup_Porcupine_delete
  (JNIEnv *env, jobject obj, jlong handle) {
    #ifdef _WIN32
        f_delete((pv_porcupine_t*)handle);
    #else
        (*f_delete)((pv_porcupine_t*)handle);
    #endif
}

// For testing as a standalone executable without JNI
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