// JNI 桥接通用工具与异常辅助。
#pragma once

#include <jni.h>
#include <string>
#include <stdexcept>

template <typename T>
inline jlong ptr_to_jlong(T* ptr) {
    return static_cast<jlong>(reinterpret_cast<intptr_t>(ptr));
}

template <typename T>
inline T* jlong_to_ptr(jlong value) {
    return reinterpret_cast<T*>(static_cast<intptr_t>(value));
}

class JniUtfChars {
public:
    JniUtfChars(JNIEnv* env, jstring value) : env_(env), value_(value), chars_(nullptr) {
        if (value_ != nullptr) {
            chars_ = env_->GetStringUTFChars(value_, nullptr);
        }
    }

    ~JniUtfChars() {
        if (value_ != nullptr && chars_ != nullptr) {
            env_->ReleaseStringUTFChars(value_, chars_);
        }
    }

    const char* c_str() const {
        return chars_ == nullptr ? "" : chars_;
    }

private:
    JNIEnv* env_;
    jstring value_;
    const char* chars_;
};

inline void throw_llvm_exception(JNIEnv* env, const std::string& message) {
    jclass klass = env->FindClass("org/cangnova/cangjie/llvm/api/LlvmException");
    if (klass == nullptr) {
        klass = env->FindClass("java/lang/RuntimeException");
    }
    env->ThrowNew(klass, message.c_str());
}

#define CJ_LLVM_TRY try

#define CJ_LLVM_CATCH(env) \
    catch (const std::exception& e) { \
        throw_llvm_exception((env), e.what()); \
    } catch (...) { \
        throw_llvm_exception((env), "unexpected native LLVM error"); \
    }
