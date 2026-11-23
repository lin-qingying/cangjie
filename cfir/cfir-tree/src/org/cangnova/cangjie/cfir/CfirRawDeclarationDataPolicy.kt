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
        object : CfirRawDeclarationDataPolicy<V> {
            override val description: String = description
            override fun decode(value: Any): V {
                require(value is V) { "RAW declaration-data reference does not match $description" }
                return value
            }
            override fun collectOwned(value: V, context: CfirRawCopyContext, visit: (CfirElement) -> Unit) {}
            override fun copy(value: V, context: CfirRawCopyContext, install: (V) -> Unit) {
                // ancestor 尚在构造时不能假装它是 borrowed；所有 owned 节点记入同一 cache 后才安装。
                context.state.deferReference { install(context.state.referencedElement(value)) }
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

    val annotationInfo: CfirRawDeclarationDataPolicy<CfirDeclarationAnnotationInfo> = value("declaration annotation source/wire facts") { value, _ ->
        CfirDeclarationAnnotationInfo(
            value.isAnnotation, value.annotationTargets.toSet(), value.annotationTargetResolutionStatus,
            value.isIntrinsic, value.isConstSafe, value.isEnsurePreparedToMock, value.isMockSupported,
            value.attributes.toList(), value.overflowStrategy, value.runtimeVisible,
        )
    }

    val cjoPosition: CfirRawDeclarationDataPolicy<CfirCjoDeclarationPosition> = value("CJO source position") { value, _ ->
        CfirCjoDeclarationPosition(value.filePath, value.line, value.column, value.identifierLine, value.identifierColumn)
    }

    /** CJO pool 下标是不可变 provenance，不能从声明名字反推。 */
    val cjoDeclarationIndex: CfirRawDeclarationDataPolicy<Int> = value("CJO declaration index") { value, _ -> value }

    val serializedAttributes: CfirRawDeclarationDataPolicy<CfirSerializedDeclarationAttributes> =
        value("CJO declaration attribute words") { value, _ -> CfirSerializedDeclarationAttributes(value.words.toList()) }

    val serializedInterop: CfirRawDeclarationDataPolicy<CfirSerializedInteropFacts> = value("CJO interop facts") { value, _ ->
        CfirSerializedInteropFacts(
            value.hasExplicitC, value.callingConvention, value.overflowStrategy, value.isIntrinsic,
            value.isFastNative, value.isMockSupported, value.isJavaMirror, value.isJavaMirrorSubtype,
            value.hasJavaDefault, value.isJavaMirrorSyntheticWrapper, value.isJavaApplication,
            value.isJavaExtension, value.isJavaCjMapping, value.isJavaInterfaceForward, value.isJavaInterfaceDefault,
            value.isObjCMirror, value.isObjCMirrorSubtype, value.isObjCInit, value.isObjCOptional,
            value.isObjCMirrorSyntheticWrapper, value.isObjCCjMapping, value.isObjCInterfaceForward,
            value.attributeNames.toList(), value.cjmpTarget,
        )
    }

    val interop: CfirRawDeclarationDataPolicy<CfirInteropInfo> = value("canonical declaration interop snapshot") { value, _ ->
        CfirInteropInfo(
            abiRequest = CfirAbiRequest(value.abiRequest.isForeign, value.abiRequest.hasExplicitC, value.abiRequest.callingConvention, value.abiRequest.isFunction),
            resolvedAbi = CfirResolvedAbi(value.resolvedAbi.kind, value.resolvedAbi.isCFunction, value.resolvedAbi.effectiveCallingConvention),
            foreignName = value.foreignName,
            foreignGetterName = value.foreignGetterName,
            foreignSetterName = value.foreignSetterName,
            java = value.java?.let { info -> CfirJavaInteropInfo(
                info.isMirror, info.isMirrorSubtype, info.isImpl, info.hasDefault, info.isSyntheticWrapper,
                info.isApplication, info.isExtension, info.isInterfaceForward, info.isInterfaceDefault,
                info.externalName, info.generatedDeclarationIds.toList(),
            ) },
            objc = value.objc?.let { info -> CfirObjCInteropInfo(
                info.isMirror, info.isMirrorSubtype, info.isImpl, info.isInit, info.isOptional,
                info.isSyntheticWrapper, info.isInterfaceForward, info.externalName, info.generatedDeclarationIds.toList(),
            ) },
            cjmp = value.cjmp?.let { info -> CfirCjmpMappingInfo(
                info.target, info.declarationKind, info.isSupportedOnCommon, info.isSupportedOnSpecific,
            ) },
            overflowStrategy = value.overflowStrategy,
            isFastNative = value.isFastNative,
            isFrozen = value.isFrozen,
            externalSymbolName = value.externalSymbolName,
            ffiAnnotationKinds = value.ffiAnnotationKinds.toSet(),
            platformAnnotationKinds = value.platformAnnotationKinds.toSet(),
            ffiAnnotationNames = value.ffiAnnotationNames.toList(),
            platformAnnotationNames = value.platformAnnotationNames.toList(),
        )
    }
}
