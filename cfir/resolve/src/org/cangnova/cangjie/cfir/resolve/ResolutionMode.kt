package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import kotlin.jvm.JvmName

/**
 * 表达式或声明 body resolve 的解析模式。
 *
 * 解析模式携带期望类型、声明状态或接收者解析语义，用于控制当前节点是否必须完整完成解析。
 */
sealed class ResolutionMode(
    /**
     * 是否强制完成当前节点的完整解析。
     */
    val forceFullCompletion: Boolean,
) {
    /**
     * 供上下文敏感解析使用的类型提示。
     */
    open val hintForContextSensitiveResolution: ConeCangJieType? get() = null

    /**
     * 依赖外部上下文的解析模式。
     */
    open class ContextDependent(
        /**
         * 上下文敏感解析可使用的类型提示。
         */
        override val hintForContextSensitiveResolution: ConeCangJieType?,
    ) : ResolutionMode(forceFullCompletion = false) {
        /**
         * 不携带类型提示的默认上下文相关模式。
         */
        companion object : ContextDependent(hintForContextSensitiveResolution = null)
    }

    /**
     * 不依赖外部期望类型的完整解析模式。
     */
    data object ContextIndependent : ResolutionMode(forceFullCompletion = true)

    /**
     * 接收者解析模式。
     */
    sealed class ReceiverResolution(
        /**
         * 是否按 callable reference 的接收者规则解析。
         */
        val forCallableReference: Boolean,
    ) : ResolutionMode(forceFullCompletion = true) {
        /**
         * callable reference 接收者解析。
         */
        data object ForCallableReference : ReceiverResolution(forCallableReference = true)
        /**
         * 普通接收者解析。
         */
        companion object : ReceiverResolution(forCallableReference = false)
    }

    /**
     * 带期望类型的解析模式。
     */
    class WithExpectedType(
        /**
         * 当前节点的期望类型引用。
         */
        val expectedTypeRef: CfirResolvedTypeRef,
        /**
         * 当前节点是否是代码块最后一个语句。
         */
        val lastStatementInBlock: Boolean = false,
        /**
         * 期望类型是否来自类型转换表达式。
         */
        val fromCast: Boolean = false,
        /**
         * 数组字面量所在的特殊语法位置。
         */
        val arrayLiteralPosition: ArrayLiteralPosition? = null,
        /**
         * 上下文敏感解析可使用的类型提示。
         */
        override val hintForContextSensitiveResolution: ConeCangJieType? = null,
        forceFullCompletion: Boolean = true,
    ) : ResolutionMode(forceFullCompletion) {
        /**
         * 期望类型引用中的 cone 类型。
         */
        val expectedType: ConeCangJieType
            get() = expectedTypeRef.coneType

        /**
         * 派生新的带期望类型解析模式。
         */
        fun copy(
            expectedTypeRef: CfirResolvedTypeRef = this.expectedTypeRef,
            lastStatementInBlock: Boolean = this.lastStatementInBlock,
            forceFullCompletion: Boolean = this.forceFullCompletion,
        ): WithExpectedType = WithExpectedType(
            expectedTypeRef = expectedTypeRef,
            lastStatementInBlock = lastStatementInBlock,
            fromCast = fromCast,
            arrayLiteralPosition = arrayLiteralPosition,
            hintForContextSensitiveResolution = hintForContextSensitiveResolution,
            forceFullCompletion = forceFullCompletion,
        )
    }

    /**
     * 数组字面量参与解析的语法位置。
     */
    enum class ArrayLiteralPosition {
        /**
         * 注解实参位置。
         */
        AnnotationArgument,
        /**
         * 注解参数默认值位置。
         */
        AnnotationParameter,
    }

    /**
     * 带声明状态的解析模式。
     */
    class WithStatus(
        /**
         * 当前声明已经解析出的状态。
         */
        val status: CfirDeclarationStatus,
    ) : ResolutionMode(forceFullCompletion = false)

    /**
     * 只更新隐式类型引用的解析模式。
     */
    class UpdateImplicitTypeRef(
        /**
         * 要写回声明或表达式的已解析类型引用。
         */
        val newTypeRef: CfirResolvedTypeRef,
    ) : ResolutionMode(forceFullCompletion = false)
}

/**
 * 当前解析模式携带的有效期望类型。
 *
 * 来自 cast 的期望类型不参与普通表达式期望类型传播。
 */
val ResolutionMode.expectedType: ConeCangJieType?
    get() = when (this) {
        is ResolutionMode.WithExpectedType -> expectedType.takeIf { !fromCast }
        else -> null
    }

/**
 * 根据类型引用创建解析模式。
 */
fun withExpectedType(
    expectedTypeRef: CfirTypeRef,
    arrayLiteralPosition: ResolutionMode.ArrayLiteralPosition? = null,
    hintForContextSensitiveResolution: ConeCangJieType? = null,
): ResolutionMode = when (expectedTypeRef) {
    is CfirResolvedTypeRef -> ResolutionMode.WithExpectedType(
        expectedTypeRef = expectedTypeRef,
        arrayLiteralPosition = arrayLiteralPosition,
        hintForContextSensitiveResolution = hintForContextSensitiveResolution,
    )
    else -> ResolutionMode.ContextIndependent
}

/**
 * 根据可空 cone 类型创建解析模式。
 */
@JvmName("withExpectedNullableType")
fun withExpectedType(coneType: ConeCangJieType?, lastStatementInBlock: Boolean = false): ResolutionMode {
    return coneType?.let { withExpectedType(it, lastStatementInBlock) } ?: ResolutionMode.ContextDependent
}

/**
 * 根据非空 cone 类型创建带期望类型解析模式。
 */
fun withExpectedType(coneType: ConeCangJieType, lastStatementInBlock: Boolean = false): ResolutionMode {
    val typeRef = coneType.toCfirResolvedTypeRef()
    return ResolutionMode.WithExpectedType(typeRef, lastStatementInBlock)
}

/**
 * 将声明状态包装为解析模式。
 */
fun CfirDeclarationStatus.mode(): ResolutionMode =
    ResolutionMode.WithStatus(this)
