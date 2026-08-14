#include <jni.h>
#include <string>
#include <vector>
#include <fstream>
#include <sstream>
#include <iomanip>
#include <android/log.h>

#define LOG_TAG "NativeScannerEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Simple hash / string signature matching native engine for demonstration/high-performance checking
extern "C" JNIEXPORT jstring JNICALL
Java_com_shieldcore_security_nativeengine_NativeScannerBridge_computeFileHash(
        JNIEnv *env,
        jobject /* this */,
        jstring filePath) {
    
    const char *path = env->GetStringUTFChars(filePath, nullptr);
    if (!path) return env->NewStringUTF("ERROR_PATH");

    std::ifstream file(path, std::ios::binary);
    if (!file.is_open()) {
        env->ReleaseStringUTFChars(filePath, path);
        return env->NewStringUTF("FILE_NOT_ACCESSIBLE");
    }

    // High performance stream processing (64KB buffer)
    char buffer[65536];
    uint64_t simpleHash = 14695981039346656037ULL; // FNV-1a basis

    while (file.read(buffer, sizeof(buffer)) || file.gcount() > 0) {
        std::streamsize bytesRead = file.gcount();
        for (std::streamsize i = 0; i < bytesRead; ++i) {
            simpleHash ^= static_cast<uint8_t>(buffer[i]);
            simpleHash *= 1099511628211ULL;
        }
    }
    file.close();
    env->ReleaseStringUTFChars(filePath, path);

    std::stringstream stream;
    stream << std::hex << std::setw(16) << std::setfill('0') << simpleHash;
    return env->NewStringUTF(stream.str().c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_shieldcore_security_nativeengine_NativeScannerBridge_scanBinarySignatures(
        JNIEnv *env,
        jobject /* this */,
        jstring filePath,
        jobjectArray signaturePatterns) {

    const char *path = env->GetStringUTFChars(filePath, nullptr);
    if (!path) return JNI_FALSE;

    std::ifstream file(path, std::ios::binary);
    if (!file.is_open()) {
        env->ReleaseStringUTFChars(filePath, path);
        return JNI_FALSE;
    }

    jsize patternCount = env->GetArrayLength(signaturePatterns);
    std::vector<std::string> patterns;
    for (jsize i = 0; i < patternCount; ++i) {
        auto patternStr = (jstring) env->GetObjectArrayElement(signaturePatterns, i);
        const char *patternC = env->GetStringUTFChars(patternStr, nullptr);
        if (patternC) {
            patterns.push_back(std::string(patternC));
            env->ReleaseStringUTFChars(patternStr, patternC);
        }
    }

    // Buffer chunk reading for signature inspection
    char buffer[32768];
    bool matchFound = false;

    while (file.read(buffer, sizeof(buffer)) || file.gcount() > 0) {
        std::string chunk(buffer, file.gcount());
        for (const auto &pat : patterns) {
            if (!pat.empty() && chunk.find(pat) != std::string::npos) {
                matchFound = true;
                break;
            }
        }
        if (matchFound) break;
    }

    file.close();
    env->ReleaseStringUTFChars(filePath, path);
    return matchFound ? JNI_TRUE : JNI_FALSE;
}
