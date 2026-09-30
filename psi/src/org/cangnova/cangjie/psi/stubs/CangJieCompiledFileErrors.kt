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
    /**
     * 保存 `NEWER_VERSION_DECOMPILE_ERROR`，供PSI Stub流程读取节点结构或语义信息。
     */
    const val NEWER_VERSION_DECOMPILE_ERROR =
        "// This file was compiled with a newer version of CangJie compiler and can't be decompiled."

    /**
     * 反编译链无法产出文本时的占位首行前缀。
     */
    const val DECOMPILE_FAILURE_PREFIX = "// Could not decompile the file: "

    /**
     * 占位文案固定不变的上报指引行。
     */
    const val DECOMPILE_FAILURE_REPORT_LINE =
        "// Please report an issue: https://github.com/lin-qingying/cangjie/issues"

    /**
     * 按同源两行构造反编译失败占位文本。
     *
     * compiled stub 的 `forInvalid` 路径与 `filetype.decompiler` 的文本路径都会产出这段文本，
     * 两层必须逐字一致，因此文案只在这里定义。
     *
     * @param cause 失败原因，必须是当前可观测到的真实条件（缺少 stub、缺少 decompiler、无项目上下文等），
     *   不要据此构造推测性诊断。
     */
    fun decompileFailureText(cause: String): String =
        "$DECOMPILE_FAILURE_PREFIX$cause\n$DECOMPILE_FAILURE_REPORT_LINE"
}
