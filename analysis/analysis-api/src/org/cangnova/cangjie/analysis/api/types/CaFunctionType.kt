package org.cangnova.cangjie.analysis.api.types

/**
 * 仓颉函数类型公开模型。
 *
 * 表示形如 `(P1, P2) -> R`、闭包类型、CFunc 类型等带形参列表与返回类型的函数式类型,
 * 区别于普通 class-like 类型,函数类型在公开层直接暴露 [parameterTypes] 与 [returnType] 等结构。
 *
 * 仓颉特有信息:
 * - [isCFunction] 区分 `CFunc<...>` 与一般的仓颉函数类型;
 * - [isClosureType] 标识闭包类型(可捕获上下文);
 * - [hasVariableLengthArgument] 标识形参列表是否包含可变长参数(VarArg)。
 *
 * 对齐 Kotlin Analysis API 的 `KaFunctionType`(由于仓颉函数类型不是 class-like type,
 * 这里没有像 Kotlin 那样继承 KaClassType)。
 */
interface CaFunctionType : CaType {
    /**
     * 函数形参类型列表,顺序与源码声明一致,不含接收者。
     */
    val parameterTypes: List<CaType>

    /**
     * 函数返回类型。
     */
    val returnType: CaType

    /**
     * 是否为 `CFunc<...>` 形式的 C 函数互操作类型。
     */
    val isCFunction: Boolean

    /**
     * 是否为闭包类型(可捕获外层变量)。
     */
    val isClosureType: Boolean

    /**
     * 形参列表中是否包含可变长参数(VarArg)。
     */
    val hasVariableLengthArgument: Boolean
}
