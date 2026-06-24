package org.cangnova.cangjie.cfir.types.builder

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.impl.CfirErrorTypeRefImpl
import org.cangnova.cangjie.source.CjSourceElement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * [CfirErrorTypeRef] 的构建器。
 *
 * 错误类型引用在解析失败、类型恢复和 partially-resolved 类型链中都可能出现，
 * 因此构建器同时允许传入最终 cone type、委托类型引用和部分解析类型引用。
 */
@CfirBuilderDsl
class CfirErrorTypeRefBuilder {
    /**
     * 错误类型引用对应的源码位置。
     */
    var source: CjSourceElement? = null

    /**
     * 类型引用上的注解。
     */
    var annotations: MutableList<CfirAnnotation> = mutableListOf()

    /**
     * 已知的 cone 类型；为空时由 [diagnostic] 构造 [org.cangnova.cangjie.cfir.types.ConeErrorType]。
     */
    var coneType: ConeCangJieType? = null

    /**
     * 错误类型背后的委托类型引用。
     */
    var delegatedTypeRef: CfirTypeRef? = null

    /**
     * 在错误发生前已经部分解析出的类型引用。
     */
    var partiallyResolvedTypeRef: CfirTypeRef? = null

    /**
     * 产生错误类型的诊断；构建前必须写入。
     */
    lateinit var diagnostic: ConeDiagnostic

    /**
     * 构建错误类型引用实例。
     */
    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirErrorTypeRef {
        return CfirErrorTypeRefImpl(
            source = source,
            annotations = annotations.toMutableOrEmpty(),
            typeOrNull = coneType,
            delegatedTypeRef = delegatedTypeRef,
            diagnostic = diagnostic,
            partiallyResolvedTypeRef = partiallyResolvedTypeRef,
        )
    }
}

/**
 * 使用 DSL 构建 [CfirErrorTypeRef]。
 */
@OptIn(ExperimentalContracts::class)
inline fun buildErrorTypeRef(init: CfirErrorTypeRefBuilder.() -> Unit): CfirErrorTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirErrorTypeRefBuilder().apply(init).build()
}

/**
 * 以已有错误类型引用为模板构建副本。
 *
 * [init] 会在复制原始字段后执行，可用于定点替换诊断、注解或 partially-resolved 信息。
 */
@OptIn(ExperimentalContracts::class)
inline fun buildErrorTypeRefCopy(
    original: CfirErrorTypeRef,
    init: CfirErrorTypeRefBuilder.() -> Unit,
): CfirErrorTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirErrorTypeRefBuilder().apply {
        source = original.source
        coneType = original.coneType
        annotations = original.annotations.toMutableList()
        delegatedTypeRef = original.delegatedTypeRef
        diagnostic = original.diagnostic
        partiallyResolvedTypeRef = original.partiallyResolvedTypeRef
    }.apply(init).build()
}
