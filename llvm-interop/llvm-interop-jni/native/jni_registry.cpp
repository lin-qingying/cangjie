// JNI 动态注册入口与方法表。
#include <jni.h>

extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetInitializeAll();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetDefaultTriple();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetCreateMachine();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetDisposeMachine();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetMachineEmitObjectFile();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetMachineEmitObjectBytes();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_contextCreate();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_contextDispose();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleCreateInContext();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleDispose();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleSetTargetTriple();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleSetDataLayout();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleAddFunction();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleAddGlobal();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_functionAppendBasicBlock();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_modulePrintToString();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleVerify();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleRunPasses();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_intTypeInContext();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_floatTypeInContext();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_doubleTypeInContext();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_voidTypeInContext();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_ptrTypeInContext();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_functionType();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_namedStructTypeInContext();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_structSetBody();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_arrayType();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_constInt();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_constReal();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_constNull();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_valueGetName();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_valueGetType();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderCreateInContext();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderDispose();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderPositionAtEnd();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildRet();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildRetVoid();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildBr();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildCondBr();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSwitch();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildUnreachable();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildAdd();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSub();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildMul();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSDiv();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildUDiv();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSRem();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildURem();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFNeg();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFAdd();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFSub();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFMul();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFDiv();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildAnd();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildOr();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildXor();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildShl();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildAShr();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildLShr();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildICmp();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFCmp();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildAlloca();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildLoad();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildStore();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildGep();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildTrunc();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildZExt();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSExt();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFPTrunc();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFPExt();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFPToUI();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFPToSI();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildUIToFP();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSIToFP();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildPtrToInt();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildIntToPtr();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildBitCast();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildCall();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildPhi();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderAddIncoming();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSelect();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildExtractValue();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildInsertValue();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_verifyFunction();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_writeBitcodeToFile();
extern "C" void Java_org_cangnova_cangjie_llvm_jni_LlvmNative_writeBitcodeToMemoryBuffer();

// JNINativeMethod 的 name/signature 字段在 JNI C API 中仍是 char*，
// 但 RegisterNatives 不会修改这里的静态字符串字面量。
#if defined(__clang__)
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wwritable-strings"
#endif
static JNINativeMethod kLlvmNativeMethods[] = {
    {"targetInitializeAll", "()V", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetInitializeAll)},
    {"targetDefaultTriple", "()Ljava/lang/String;", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetDefaultTriple)},
    {"targetCreateMachine", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;III)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetCreateMachine)},
    {"targetDisposeMachine", "(J)V", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetDisposeMachine)},
    {"targetMachineEmitObjectFile", "(JJLjava/lang/String;)V", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetMachineEmitObjectFile)},
    {"targetMachineEmitObjectBytes", "(JJ)[B", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_targetMachineEmitObjectBytes)},
    {"contextCreate", "()J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_contextCreate)},
    {"contextDispose", "(J)V", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_contextDispose)},
    {"moduleCreateInContext", "(Ljava/lang/String;J)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleCreateInContext)},
    {"moduleDispose", "(J)V", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleDispose)},
    {"moduleSetTargetTriple", "(JLjava/lang/String;)V", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleSetTargetTriple)},
    {"moduleSetDataLayout", "(JLjava/lang/String;)V", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleSetDataLayout)},
    {"moduleAddFunction", "(JLjava/lang/String;J)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleAddFunction)},
    {"moduleAddGlobal", "(JJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleAddGlobal)},
    {"functionAppendBasicBlock", "(JLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_functionAppendBasicBlock)},
    {"modulePrintToString", "(J)Ljava/lang/String;", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_modulePrintToString)},
    {"moduleVerify", "(J)V", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleVerify)},
    {"moduleRunPasses", "(JLjava/lang/String;J)V", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_moduleRunPasses)},
    {"intTypeInContext", "(JI)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_intTypeInContext)},
    {"floatTypeInContext", "(J)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_floatTypeInContext)},
    {"doubleTypeInContext", "(J)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_doubleTypeInContext)},
    {"voidTypeInContext", "(J)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_voidTypeInContext)},
    {"ptrTypeInContext", "(J)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_ptrTypeInContext)},
    {"functionType", "(J[JZ)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_functionType)},
    {"namedStructTypeInContext", "(JLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_namedStructTypeInContext)},
    {"structSetBody", "(J[JZ)V", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_structSetBody)},
    {"arrayType", "(JI)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_arrayType)},
    {"constInt", "(JJZ)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_constInt)},
    {"constReal", "(JD)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_constReal)},
    {"constNull", "(J)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_constNull)},
    {"valueGetName", "(J)Ljava/lang/String;", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_valueGetName)},
    {"valueGetType", "(J)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_valueGetType)},
    {"builderCreateInContext", "(J)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderCreateInContext)},
    {"builderDispose", "(J)V", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderDispose)},
    {"builderPositionAtEnd", "(JJ)V", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderPositionAtEnd)},
    {"builderBuildRet", "(JJ)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildRet)},
    {"builderBuildRetVoid", "(J)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildRetVoid)},
    {"builderBuildBr", "(JJ)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildBr)},
    {"builderBuildCondBr", "(JJJJ)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildCondBr)},
    {"builderBuildSwitch", "(JJJ[J[J)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSwitch)},
    {"builderBuildUnreachable", "(J)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildUnreachable)},
    {"builderBuildAdd", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildAdd)},
    {"builderBuildSub", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSub)},
    {"builderBuildMul", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildMul)},
    {"builderBuildSDiv", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSDiv)},
    {"builderBuildUDiv", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildUDiv)},
    {"builderBuildSRem", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSRem)},
    {"builderBuildURem", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildURem)},
    {"builderBuildFNeg", "(JJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFNeg)},
    {"builderBuildFAdd", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFAdd)},
    {"builderBuildFSub", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFSub)},
    {"builderBuildFMul", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFMul)},
    {"builderBuildFDiv", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFDiv)},
    {"builderBuildAnd", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildAnd)},
    {"builderBuildOr", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildOr)},
    {"builderBuildXor", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildXor)},
    {"builderBuildShl", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildShl)},
    {"builderBuildAShr", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildAShr)},
    {"builderBuildLShr", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildLShr)},
    {"builderBuildICmp", "(JIJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildICmp)},
    {"builderBuildFCmp", "(JIJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFCmp)},
    {"builderBuildAlloca", "(JJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildAlloca)},
    {"builderBuildLoad", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildLoad)},
    {"builderBuildStore", "(JJJ)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildStore)},
    {"builderBuildGep", "(JJJ[JZLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildGep)},
    {"builderBuildTrunc", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildTrunc)},
    {"builderBuildZExt", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildZExt)},
    {"builderBuildSExt", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSExt)},
    {"builderBuildFPTrunc", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFPTrunc)},
    {"builderBuildFPExt", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFPExt)},
    {"builderBuildFPToUI", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFPToUI)},
    {"builderBuildFPToSI", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildFPToSI)},
    {"builderBuildUIToFP", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildUIToFP)},
    {"builderBuildSIToFP", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSIToFP)},
    {"builderBuildPtrToInt", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildPtrToInt)},
    {"builderBuildIntToPtr", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildIntToPtr)},
    {"builderBuildBitCast", "(JJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildBitCast)},
    {"builderBuildCall", "(JJ[JLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildCall)},
    {"builderBuildPhi", "(JJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildPhi)},
    {"builderAddIncoming", "(J[J[J)V", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderAddIncoming)},
    {"builderBuildSelect", "(JJJJLjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildSelect)},
    {"builderBuildExtractValue", "(JJILjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildExtractValue)},
    {"builderBuildInsertValue", "(JJJILjava/lang/String;)J", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_builderBuildInsertValue)},
    {"verifyFunction", "(J)V", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_verifyFunction)},
    {"writeBitcodeToFile", "(JLjava/lang/String;)I", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_writeBitcodeToFile)},
    {"writeBitcodeToMemoryBuffer", "(J)[B", reinterpret_cast<void*>(Java_org_cangnova_cangjie_llvm_jni_LlvmNative_writeBitcodeToMemoryBuffer)},
};
#if defined(__clang__)
#pragma clang diagnostic pop
#endif

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) != JNI_OK) {
        return JNI_ERR;
    }

    jclass cls = env->FindClass("org/cangnova/cangjie/llvm/jni/LlvmNative");
    if (cls == nullptr) {
        return JNI_ERR;
    }

    if (env->RegisterNatives(
            cls,
            kLlvmNativeMethods,
            static_cast<jint>(sizeof(kLlvmNativeMethods) / sizeof(kLlvmNativeMethods[0]))) != 0) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_8;
}
