package org.cangnova.cangjie.psi.stubs

/**
 * 编译产物文件在 PSI / stub / decompiler 三层之间共享的错误文案协议。
 *
 * 这里收口的是“带语义约束的固定文案”，而不是普通展示字符串：
 * - decompiler 会用它生成 Invalid decompiled text
 * - compiled stub builder 会用它识别 Invalid file stub
 * - 上层工具会据此判断当前文件是否属于“可展示但不可反编译”的稳定错误态
 *
 * 因此必须集中定义，避免多个模块各自拷贝后逐渐漂移。
 */
object CangJieCompiledFileErrors {
    const val NEWER_VERSION_DECOMPILE_ERROR =
        "// This file was compiled with a newer version of CangJie compiler and can't be decompiled."
}
