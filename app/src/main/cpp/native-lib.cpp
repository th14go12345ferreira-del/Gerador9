#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include "whisper.h"

#define TAG "TranscribeTranslate"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static whisper_context * g_context = nullptr;

static size_t asset_read(void * ctx, void * output, size_t read_size) {
    return AAsset_read(static_cast<AAsset *>(ctx), output, read_size);
}

static bool asset_eof(void * ctx) {
    return AAsset_getRemainingLength64(static_cast<AAsset *>(ctx)) <= 0;
}

static void asset_close(void * ctx) {
    AAsset_close(static_cast<AAsset *>(ctx));
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_thiago_transcribetranslate_NativeBridge_loadModelFromAsset(
        JNIEnv * env, jobject, jobject assetManager, jstring assetPath) {
    if (g_context) {
        whisper_free(g_context);
        g_context = nullptr;
    }

    const char * path = env->GetStringUTFChars(assetPath, nullptr);
    AAssetManager * manager = AAssetManager_fromJava(env, assetManager);
    AAsset * asset = AAssetManager_open(manager, path, AASSET_MODE_STREAMING);
    env->ReleaseStringUTFChars(assetPath, path);

    if (!asset) {
        LOGE("Não foi possível abrir o modelo");
        return JNI_FALSE;
    }

    whisper_model_loader loader{};
    loader.context = asset;
    loader.read = asset_read;
    loader.eof = asset_eof;
    loader.close = asset_close;

    g_context = whisper_init_with_params(&loader, whisper_context_default_params());
    return g_context ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_thiago_transcribetranslate_NativeBridge_transcribe(
        JNIEnv * env, jobject, jfloatArray samples, jstring language) {
    if (!g_context) {
        return env->NewStringUTF("ERRO: modelo não carregado.");
    }

    jsize count = env->GetArrayLength(samples);
    if (count <= 0) return env->NewStringUTF("ERRO: áudio vazio.");

    jfloat * pcm = env->GetFloatArrayElements(samples, nullptr);
    const char * lang = env->GetStringUTFChars(language, nullptr);

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.language = lang;
    params.n_threads = 4;

    int result = whisper_full(g_context, params, pcm, count);

    env->ReleaseStringUTFChars(language, lang);
    env->ReleaseFloatArrayElements(samples, pcm, JNI_ABORT);

    if (result != 0) {
        return env->NewStringUTF("ERRO: a transcrição falhou.");
    }

    std::string output;
    const int segments = whisper_full_n_segments(g_context);
    for (int i = 0; i < segments; ++i) {
        const char * text = whisper_full_get_segment_text(g_context, i);
        if (text) output += text;
    }
    return env->NewStringUTF(output.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_thiago_transcribetranslate_NativeBridge_releaseModel(JNIEnv *, jobject) {
    if (g_context) {
        whisper_free(g_context);
        g_context = nullptr;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_thiago_transcribetranslate_NativeBridge_status(JNIEnv * env, jobject) {
    return env->NewStringUTF(g_context ? "Modelo Whisper carregado" : "Modelo não carregado");
}
