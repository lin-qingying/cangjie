package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.types.*

/** 声明数据在 RAW 事务中的明确策略；只有声明该策略的非空 key 才能复制。 */
interface CfirRawDeclarationDataPolicy<V : Any> {
    val description: String
    fun decode(value: Any): V
    fun collectOwned(value: V, context: CfirRawCopyContext, visit: (CfirElement) -> Unit)
    fun copy(value: V, context: CfirRawCopyContext, install: (V) -> Unit)
}

/**
 * 策略在 key 的声明文件注册，core 不依赖 providers/resolve/analysis 的数据类型。
 * reified 检查仅做普通类型判定，不读取字段、不加载类、不执行反射。
 */
object CfirRawDeclarationDataPolicies {
    inline fun <reified V : Any> value(
        description: String,
        crossinline collect: (V, CfirRawCopyContext, (CfirElement) -> Unit) -> Unit = { _, _, _ -> },
        crossinline copyValue: (V, CfirRawCopyContext) -> V,
    ): CfirRawDeclarationDataPolicy<V> = object : CfirRawDeclarationDataPolicy<V> {
        override val description: String = description
        override fun decode(value: Any): V {
            require(value is V) { "RAW declaration-data value does not match $description" }
            return value
        }
        override fun collectOwned(value: V, context: CfirRawCopyContext, visit: (CfirElement) -> Unit) = collect(value, context, visit)
        override fun copy(value: V, context: CfirRawCopyContext, install: (V) -> Unit) = install(copyValue(value, context))
    }

    inline fun <reified V : Any> forbidden(description: String): CfirRawDeclarationDataPolicy<V> =
        value(description, collect = { _, _, _ -> error("RAW cannot import $description") }) { _, _ ->
            error("RAW cannot import $description")
        }

    inline fun <reified V : Any> borrowed(description: String): CfirRawDeclarationDataPolicy<V> =
        value(description, collect = { value, context, _ -> context.borrowedHandle(value) }) { value, context ->
            context.borrowedHandle(value)
        }

    inline fun <reified V : CfirElement> reference(description: String): CfirRawDeclarationDataPolicy<V> =
        referenceValue(description) { value, context -> context.state.referencedElement(value) }

    /** 含有借用边的值对象必须等所有拥有节点进入同一缓存后才重绑。 */
    inline fun <reified V : Any> referenceValue(
        description: String,
        crossinline rebind: (V, CfirRawCopyContext) -> V,
    ): CfirRawDeclarationDataPolicy<V> = object : CfirRawDeclarationDataPolicy<V> {
        override val description: String = description
        override fun decode(value: Any): V {
            require(value is V) { "RAW declaration-data reference does not match $description" }
            return value
        }

        override fun collectOwned(value: V, context: CfirRawCopyContext, visit: (CfirElement) -> Unit) {}

        override fun copy(value: V, context: CfirRawCopyContext, install: (V) -> Unit) {
            context.state.deferReference { install(rebind(value, context)) }
        }
    }

    val boolean: CfirRawDeclarationDataPolicy<Boolean> = value("Boolean source fact") { value, _ -> value }

    val classLookupTag: CfirRawDeclarationDataPolicy<ConeClassLikeLookupTag> =
        value("static member classifier lookup tag") { value, context -> context.lookupTag(value) }

    val evaluatedInitializer: CfirRawDeclarationDataPolicy<CfirEvaluatorResult> = value(
        "serialized/evaluated initializer",
        collect = { value, _, visit ->
            when (value) {
                is CfirEvaluatorResult.Evaluated -> visit(value.result)
                CfirEvaluatorResult.DuringEvaluation -> error("RAW cannot import an in-progress evaluator")
                CfirEvaluatorResult.NotEvaluated, CfirEvaluatorResult.DivisionByZero,
                CfirEvaluatorResult.RecursionInInitializer -> Unit
            }
        },
    ) { value, context ->
        when (value) {
            is CfirEvaluatorResult.Evaluated -> CfirEvaluatorResult.Evaluated(context.state.copyOwned(value.result))
            CfirEvaluatorResult.DuringEvaluation -> error("RAW cannot import an in-progress evaluator")
            CfirEvaluatorResult.NotEvaluated, CfirEvaluatorResult.DivisionByZero,
            CfirEvaluatorResult.RecursionInInitializer -> value
        }
    }

    val whereConstraints: CfirRawDeclarationDataPolicy<CfirTypeConstraintDiagnosticData> = value(
        "where constraint source/type-ref ownership",
        collect = { value, _, visit -> value.typeConstraints.forEach { it.boundTypeRefs.forEach(visit) } },
    ) { value, context ->
        CfirTypeConstraintDiagnosticData(value.typeConstraints.map { constraint ->
            CfirTypeConstraintReference(
                context.name(constraint.parameterName), context.source(constraint.source),
                constraint.boundTypeRefs.map { context.state.copyOwned(it) }, context.source(constraint.constraintSource),
            )
        })
    }

    val functionBodySource: CfirRawDeclarationDataPolicy<CfirFunctionBodyDiagnosticData> =
        value("function parameter-list source facts") { value, context ->
            CfirFunctionBodyDiagnosticData(value.valueParameterLists.map { CfirValueParameterListReference(context.source(it.source)) })
        }

    val ifAvailable: CfirRawDeclarationDataPolicy<CfirIfAvailableBranchContext> = value("IfAvailable source facts") { value, _ ->
        CfirIfAvailableBranchContext(value.kind, value.conditionName, value.conditionValue)
    }

    val annotationInfo: CfirRawDeclarationDataPolicy<CfirDeclarationAnnotationInfo> =
        value("declaration annotation source/wire facts") { value, _ -> value.frozenCopy() }

    val cjoPosition: CfirRawDeclarationDataPolicy<CfirCjoDeclarationPosition> =
        value("CJO source position") { value, _ -> value.copy() }

    /** CJO pool 下标是不可变 provenance，不能从声明名字反推。 */
    val cjoDeclarationIndex: CfirRawDeclarationDataPolicy<Int> = value("CJO declaration index") { value, _ -> value }

    val serializedAttributes: CfirRawDeclarationDataPolicy<CfirSerializedDeclarationAttributes> =
        value("CJO declaration attribute words") { value, _ -> CfirSerializedDeclarationAttributes(value.words.toList()) }

    val serializedInterop: CfirRawDeclarationDataPolicy<CfirSerializedInteropFacts> = value("CJO interop facts") { value, _ ->
        value.copy(attributeNames = value.attributeNames.toList())
    }

    val interop: CfirRawDeclarationDataPolicy<CfirInteropInfo> =
        value("canonical declaration interop snapshot") { value, _ -> value.frozenCopy() }
}
