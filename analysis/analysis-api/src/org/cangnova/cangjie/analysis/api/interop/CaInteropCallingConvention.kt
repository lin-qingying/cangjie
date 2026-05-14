package org.cangnova.cangjie.analysis.api.interop

/**
 * 互操作调用约定。
 *
 * 仅在面向 C 这类 ABI 敏感的互操作后端时有意义;
 * 上层工具据此渲染签名,或在诊断时提示 ABI 不匹配。
 */
enum class CaInteropCallingConvention {
    /** C 默认调用约定。 */
    CDECL,

    /** Windows stdcall(被调用方清栈)。 */
    STDCALL,
}
