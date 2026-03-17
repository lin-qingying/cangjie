// JNI IR 构建器相关桥接实现。
#include "jni_utils.h"

#include <llvm-c/Core.h>
#include <algorithm>
#include <vector>

static std::vector<LLVMValueRef> jlong_array_to_values(JNIEnv* env, jlongArray array) {
    jsize size = env->GetArrayLength(array);
    std::vector<LLVMValueRef> values(static_cast<size_t>(size));
    jlong* raw = env->GetLongArrayElements(array, nullptr);
    for (jsize i = 0; i < size; ++i) {
        values[static_cast<size_t>(i)] = jlong_to_ptr<LLVMOpaqueValue>(raw[i]);
    }
    env->ReleaseLongArrayElements(array, raw, JNI_ABORT);
    return values;
}

static LLVMTypeRef callee_function_type(LLVMValueRef callee) {
    LLVMTypeRef callee_ty = LLVMTypeOf(callee);
    if (LLVMGetTypeKind(callee_ty) == LLVMPointerTypeKind) {
        return LLVMGetElementType(callee_ty);
    }
    return callee_ty;
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderCreateInContext(
    JNIEnv*,
    jclass,
    jlong context
) {
    return ptr_to_jlong(LLVMCreateBuilderInContext(jlong_to_ptr<LLVMOpaqueContext>(context)));
}

extern "C" JNIEXPORT void JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderDispose(
    JNIEnv*,
    jclass,
    jlong builder
) {
    if (builder == 0) return;
    LLVMDisposeBuilder(jlong_to_ptr<LLVMOpaqueBuilder>(builder));
}

extern "C" JNIEXPORT void JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderPositionAtEnd(
    JNIEnv*,
    jclass,
    jlong builder,
    jlong block
) {
    LLVMPositionBuilderAtEnd(
        jlong_to_ptr<LLVMOpaqueBuilder>(builder),
        jlong_to_ptr<LLVMOpaqueBasicBlock>(block)
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildRet(
    JNIEnv*,
    jclass,
    jlong builder,
    jlong value
) {
    return ptr_to_jlong(LLVMBuildRet(jlong_to_ptr<LLVMOpaqueBuilder>(builder), jlong_to_ptr<LLVMOpaqueValue>(value)));
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildRetVoid(
    JNIEnv*,
    jclass,
    jlong builder
) {
    return ptr_to_jlong(LLVMBuildRetVoid(jlong_to_ptr<LLVMOpaqueBuilder>(builder)));
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildBr(
    JNIEnv*,
    jclass,
    jlong builder,
    jlong block
) {
    return ptr_to_jlong(LLVMBuildBr(jlong_to_ptr<LLVMOpaqueBuilder>(builder), jlong_to_ptr<LLVMOpaqueBasicBlock>(block)));
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildCondBr(
    JNIEnv*,
    jclass,
    jlong builder,
    jlong condition,
    jlong then_block,
    jlong else_block
) {
    return ptr_to_jlong(
        LLVMBuildCondBr(
            jlong_to_ptr<LLVMOpaqueBuilder>(builder),
            jlong_to_ptr<LLVMOpaqueValue>(condition),
            jlong_to_ptr<LLVMOpaqueBasicBlock>(then_block),
            jlong_to_ptr<LLVMOpaqueBasicBlock>(else_block)
        )
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSwitch(
    JNIEnv* env,
    jclass,
    jlong builder,
    jlong value,
    jlong default_block,
    jlongArray case_values,
    jlongArray case_blocks
) {
    auto values = jlong_array_to_values(env, case_values);
    jsize block_size = env->GetArrayLength(case_blocks);
    std::vector<LLVMBasicBlockRef> blocks(static_cast<size_t>(block_size));
    jlong* raw_blocks = env->GetLongArrayElements(case_blocks, nullptr);
    for (jsize i = 0; i < block_size; ++i) {
        blocks[static_cast<size_t>(i)] = jlong_to_ptr<LLVMOpaqueBasicBlock>(raw_blocks[i]);
    }
    env->ReleaseLongArrayElements(case_blocks, raw_blocks, JNI_ABORT);

    auto* instruction = LLVMBuildSwitch(
        jlong_to_ptr<LLVMOpaqueBuilder>(builder),
        jlong_to_ptr<LLVMOpaqueValue>(value),
        jlong_to_ptr<LLVMOpaqueBasicBlock>(default_block),
        static_cast<unsigned>(std::min(values.size(), blocks.size()))
    );
    size_t size = std::min(values.size(), blocks.size());
    for (size_t i = 0; i < size; ++i) {
        LLVMAddCase(instruction, values[i], blocks[i]);
    }
    return ptr_to_jlong(instruction);
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildUnreachable(
    JNIEnv*,
    jclass,
    jlong builder
) {
    return ptr_to_jlong(LLVMBuildUnreachable(jlong_to_ptr<LLVMOpaqueBuilder>(builder)));
}

#define BINARY_OP(name, fn) \
extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_##name( \
    JNIEnv* env, jclass, jlong builder, jlong lhs, jlong rhs, jstring n \
) { \
    JniUtfChars value_name(env, n); \
    return ptr_to_jlong(fn( \
        jlong_to_ptr<LLVMOpaqueBuilder>(builder), \
        jlong_to_ptr<LLVMOpaqueValue>(lhs), \
        jlong_to_ptr<LLVMOpaqueValue>(rhs), \
        value_name.c_str())); \
}

BINARY_OP(builderBuildAdd, LLVMBuildAdd)
BINARY_OP(builderBuildSub, LLVMBuildSub)
BINARY_OP(builderBuildMul, LLVMBuildMul)
BINARY_OP(builderBuildSDiv, LLVMBuildSDiv)
BINARY_OP(builderBuildUDiv, LLVMBuildUDiv)
BINARY_OP(builderBuildSRem, LLVMBuildSRem)
BINARY_OP(builderBuildURem, LLVMBuildURem)
BINARY_OP(builderBuildFAdd, LLVMBuildFAdd)
BINARY_OP(builderBuildFSub, LLVMBuildFSub)
BINARY_OP(builderBuildFMul, LLVMBuildFMul)
BINARY_OP(builderBuildFDiv, LLVMBuildFDiv)
BINARY_OP(builderBuildAnd, LLVMBuildAnd)
BINARY_OP(builderBuildOr, LLVMBuildOr)
BINARY_OP(builderBuildXor, LLVMBuildXor)
BINARY_OP(builderBuildShl, LLVMBuildShl)
BINARY_OP(builderBuildAShr, LLVMBuildAShr)
BINARY_OP(builderBuildLShr, LLVMBuildLShr)

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFNeg(
    JNIEnv* env,
    jclass,
    jlong builder,
    jlong value,
    jstring name
) {
    JniUtfChars value_name(env, name);
    return ptr_to_jlong(
        LLVMBuildFNeg(
            jlong_to_ptr<LLVMOpaqueBuilder>(builder),
            jlong_to_ptr<LLVMOpaqueValue>(value),
            value_name.c_str()
        )
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildICmp(
    JNIEnv* env,
    jclass,
    jlong builder,
    jint predicate,
    jlong lhs,
    jlong rhs,
    jstring name
) {
    JniUtfChars value_name(env, name);
    return ptr_to_jlong(
        LLVMBuildICmp(
            jlong_to_ptr<LLVMOpaqueBuilder>(builder),
            static_cast<LLVMIntPredicate>(predicate),
            jlong_to_ptr<LLVMOpaqueValue>(lhs),
            jlong_to_ptr<LLVMOpaqueValue>(rhs),
            value_name.c_str()
        )
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFCmp(
    JNIEnv* env,
    jclass,
    jlong builder,
    jint predicate,
    jlong lhs,
    jlong rhs,
    jstring name
) {
    JniUtfChars value_name(env, name);
    return ptr_to_jlong(
        LLVMBuildFCmp(
            jlong_to_ptr<LLVMOpaqueBuilder>(builder),
            static_cast<LLVMRealPredicate>(predicate),
            jlong_to_ptr<LLVMOpaqueValue>(lhs),
            jlong_to_ptr<LLVMOpaqueValue>(rhs),
            value_name.c_str()
        )
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildAlloca(
    JNIEnv* env,
    jclass,
    jlong builder,
    jlong type,
    jstring name
) {
    JniUtfChars value_name(env, name);
    return ptr_to_jlong(
        LLVMBuildAlloca(
            jlong_to_ptr<LLVMOpaqueBuilder>(builder),
            jlong_to_ptr<LLVMOpaqueType>(type),
            value_name.c_str()
        )
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildLoad(
    JNIEnv* env,
    jclass,
    jlong builder,
    jlong type,
    jlong pointer,
    jstring name
) {
    JniUtfChars value_name(env, name);
    return ptr_to_jlong(
        LLVMBuildLoad2(
            jlong_to_ptr<LLVMOpaqueBuilder>(builder),
            jlong_to_ptr<LLVMOpaqueType>(type),
            jlong_to_ptr<LLVMOpaqueValue>(pointer),
            value_name.c_str()
        )
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildStore(
    JNIEnv*,
    jclass,
    jlong builder,
    jlong value,
    jlong pointer
) {
    return ptr_to_jlong(
        LLVMBuildStore(
            jlong_to_ptr<LLVMOpaqueBuilder>(builder),
            jlong_to_ptr<LLVMOpaqueValue>(value),
            jlong_to_ptr<LLVMOpaqueValue>(pointer)
        )
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildGep(
    JNIEnv* env,
    jclass,
    jlong builder,
    jlong element_type,
    jlong pointer,
    jlongArray indices,
    jboolean in_bounds,
    jstring name
) {
    JniUtfChars value_name(env, name);
    std::vector<LLVMValueRef> idx = jlong_array_to_values(env, indices);
    LLVMValueRef result = in_bounds == JNI_TRUE
        ? LLVMBuildInBoundsGEP2(
            jlong_to_ptr<LLVMOpaqueBuilder>(builder),
            jlong_to_ptr<LLVMOpaqueType>(element_type),
            jlong_to_ptr<LLVMOpaqueValue>(pointer),
            idx.data(),
            static_cast<unsigned>(idx.size()),
            value_name.c_str())
        : LLVMBuildGEP2(
            jlong_to_ptr<LLVMOpaqueBuilder>(builder),
            jlong_to_ptr<LLVMOpaqueType>(element_type),
            jlong_to_ptr<LLVMOpaqueValue>(pointer),
            idx.data(),
            static_cast<unsigned>(idx.size()),
            value_name.c_str());
    return ptr_to_jlong(result);
}

#define CAST_OP(name, fn) \
extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_##name( \
    JNIEnv* env, jclass, jlong builder, jlong value, jlong target_type, jstring n \
) { \
    JniUtfChars value_name(env, n); \
    return ptr_to_jlong(fn( \
        jlong_to_ptr<LLVMOpaqueBuilder>(builder), \
        jlong_to_ptr<LLVMOpaqueValue>(value), \
        jlong_to_ptr<LLVMOpaqueType>(target_type), \
        value_name.c_str())); \
}

CAST_OP(builderBuildTrunc, LLVMBuildTrunc)
CAST_OP(builderBuildZExt, LLVMBuildZExt)
CAST_OP(builderBuildSExt, LLVMBuildSExt)
CAST_OP(builderBuildFPTrunc, LLVMBuildFPTrunc)
CAST_OP(builderBuildFPExt, LLVMBuildFPExt)
CAST_OP(builderBuildFPToUI, LLVMBuildFPToUI)
CAST_OP(builderBuildFPToSI, LLVMBuildFPToSI)
CAST_OP(builderBuildUIToFP, LLVMBuildUIToFP)
CAST_OP(builderBuildSIToFP, LLVMBuildSIToFP)
CAST_OP(builderBuildPtrToInt, LLVMBuildPtrToInt)
CAST_OP(builderBuildIntToPtr, LLVMBuildIntToPtr)
CAST_OP(builderBuildBitCast, LLVMBuildBitCast)

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildCall(
    JNIEnv* env,
    jclass,
    jlong builder,
    jlong callee,
    jlongArray args,
    jstring name
) {
    JniUtfChars value_name(env, name);
    std::vector<LLVMValueRef> values = jlong_array_to_values(env, args);
    LLVMValueRef callee_value = jlong_to_ptr<LLVMOpaqueValue>(callee);
    return ptr_to_jlong(
        LLVMBuildCall2(
            jlong_to_ptr<LLVMOpaqueBuilder>(builder),
            callee_function_type(callee_value),
            callee_value,
            values.data(),
            static_cast<unsigned>(values.size()),
            value_name.c_str()
        )
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildPhi(
    JNIEnv* env,
    jclass,
    jlong builder,
    jlong type,
    jstring name
) {
    JniUtfChars value_name(env, name);
    return ptr_to_jlong(
        LLVMBuildPhi(
            jlong_to_ptr<LLVMOpaqueBuilder>(builder),
            jlong_to_ptr<LLVMOpaqueType>(type),
            value_name.c_str()
        )
    );
}

extern "C" JNIEXPORT void JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderAddIncoming(
    JNIEnv* env,
    jclass,
    jlong phi,
    jlongArray values,
    jlongArray blocks
) {
    std::vector<LLVMValueRef> incoming_values = jlong_array_to_values(env, values);
    jsize block_size = env->GetArrayLength(blocks);
    std::vector<LLVMBasicBlockRef> incoming_blocks(static_cast<size_t>(block_size));
    jlong* raw_blocks = env->GetLongArrayElements(blocks, nullptr);
    for (jsize i = 0; i < block_size; ++i) {
        incoming_blocks[static_cast<size_t>(i)] = jlong_to_ptr<LLVMOpaqueBasicBlock>(raw_blocks[i]);
    }
    env->ReleaseLongArrayElements(blocks, raw_blocks, JNI_ABORT);
    size_t size = std::min(incoming_values.size(), incoming_blocks.size());
    LLVMAddIncoming(
        jlong_to_ptr<LLVMOpaqueValue>(phi),
        incoming_values.data(),
        incoming_blocks.data(),
        static_cast<unsigned>(size)
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSelect(
    JNIEnv* env,
    jclass,
    jlong builder,
    jlong condition,
    jlong then_value,
    jlong else_value,
    jstring name
) {
    JniUtfChars value_name(env, name);
    return ptr_to_jlong(
        LLVMBuildSelect(
            jlong_to_ptr<LLVMOpaqueBuilder>(builder),
            jlong_to_ptr<LLVMOpaqueValue>(condition),
            jlong_to_ptr<LLVMOpaqueValue>(then_value),
            jlong_to_ptr<LLVMOpaqueValue>(else_value),
            value_name.c_str()
        )
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildExtractValue(
    JNIEnv* env,
    jclass,
    jlong builder,
    jlong aggregate,
    jint index,
    jstring name
) {
    JniUtfChars value_name(env, name);
    return ptr_to_jlong(
        LLVMBuildExtractValue(
            jlong_to_ptr<LLVMOpaqueBuilder>(builder),
            jlong_to_ptr<LLVMOpaqueValue>(aggregate),
            static_cast<unsigned>(index),
            value_name.c_str()
        )
    );
}

extern "C" JNIEXPORT jlong JNICALL Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildInsertValue(
    JNIEnv* env,
    jclass,
    jlong builder,
    jlong aggregate,
    jlong element,
    jint index,
    jstring name
) {
    JniUtfChars value_name(env, name);
    return ptr_to_jlong(
        LLVMBuildInsertValue(
            jlong_to_ptr<LLVMOpaqueBuilder>(builder),
            jlong_to_ptr<LLVMOpaqueValue>(aggregate),
            jlong_to_ptr<LLVMOpaqueValue>(element),
            static_cast<unsigned>(index),
            value_name.c_str()
        )
    );
}
