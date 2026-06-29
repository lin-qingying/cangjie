/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.NonLocalAnnotationVisitor
import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.expressions.withCfirEntry
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformerDispatcher
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.expressions.withCfirSymbolEntry
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment

/**
 * 校验 [typeRef] 已解析为 [CfirResolvedTypeRef]。
 */
internal inline fun checkTypeRefIsResolved(
    typeRef: CfirTypeRef,
    typeRefName: String,
    owner: CfirElementWithResolveState,
    acceptImplicitTypeRef: Boolean = false,
    extraAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    checkWithAttachment(
        typeRef is CfirResolvedTypeRef || acceptImplicitTypeRef && typeRef is CfirImplicitTypeRef,
        {
            buildString {
                append("Expected ${CfirResolvedTypeRef::class.simpleName}")
                if (acceptImplicitTypeRef) {
                    append(" or ${CfirImplicitTypeRef::class.simpleName}")
                }
                append(" for $typeRefName of ${owner::class.simpleName}(${(owner as? CfirDeclaration)?.origin}) but ${typeRef::class.simpleName} found")
            }
        }
    ) {
        withCfirEntry("typeRef", typeRef)
        withCfirEntry("cfirDeclaration", owner)
        extraAttachment()
    }
}

/**
 * 校验表达式 [type] 已经存在。
 */
internal inline fun checkExpressionTypeIsResolved(
    type: ConeCangJieType?,
    typeName: String,
    owner: CfirElement,
    extraAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    checkWithAttachment(
        type != null,
        {
            buildString {
                append("Expected resolved expression type")
                append(" for $typeName of ${owner::class.simpleName}(${(owner as? CfirDeclaration)?.origin})")
            }
        }
    ) {
        withCfirEntry("cfirDeclaration", owner)
        extraAttachment()
    }
}

/**
 * 校验 [annotationContainer] 中所有注解类型引用已经解析。
 */
internal fun <T> checkAnnotationTypeIsResolved(annotationContainer: T) where T : CfirAnnotationContainer, T : CfirElementWithResolveState {
    annotationContainer.annotations.forEach { annotation ->
        checkTypeRefIsResolved(annotation.typeRef, "annotation type", owner = annotationContainer) {
            withCfirEntry("cfirAnnotation", annotation)
        }
    }
}

/**
 * 校验 [function] 的 body block 已经具有表达式类型。
 */
internal fun checkBodyIsResolved(function: CfirFunction) {
    val block = function.body ?: return
    checkExpressionTypeIsResolved(block.coneTypeOrNull, "block type", function) {
        withCfirEntry("block", block)
    }
}

/**
 * 校验 [reference] 已解析为正常命名引用或错误命名引用。
 */
internal fun checkReferenceIsResolved(
    reference: CfirReference,
    owner: CfirResolvable,
    extraAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    checkWithAttachment(
        reference is CfirResolvedNamedReference || reference is CfirErrorNamedReference,
        {
            "Expected ${CfirNamedReference::class.simpleName} or " +
                    "${CfirErrorNamedReference::class.simpleName} " +
                    "but ${reference::class.simpleName} found"
        }
    ) {
        withCfirEntry("referenceOwner", owner)
        extraAttachment()
    }
}

/**
 * 校验 [variable] 的 initializer 已解析出表达式类型。
 */
internal fun checkInitializerIsResolved(variable: CfirVariable) {
    val initializer = variable.initializer ?: return
    checkExpressionTypeIsResolved(initializer.coneTypeOrNull, "initializer type", variable) {
        withCfirEntry("initializer", initializer)
    }
}

/**
 * 校验 [parameter] 的默认值表达式已解析出类型。
 */
internal fun checkDefaultValueIsResolved(parameter: CfirValueParameter) {
    val defaultValue = parameter.defaultValue ?: return
    checkExpressionTypeIsResolved(defaultValue.coneTypeOrNull, "default value type", parameter) {
        withCfirEntry("defaultValue", defaultValue)
    }
}

/**
 * 校验 [declaration] 的 deprecation provider 不再是未解析占位。
 */
internal fun checkDeprecationProviderIsResolved(declaration: CfirDeclaration, provider: DeprecationsProvider) {
    checkWithAttachment(
        provider !is UnresolvedDeprecationProvider,
        { "Unresolved deprecation provider found for ${declaration::class.simpleName}" }
    ) {
        withCfirEntry("declaration", declaration)
    }
}

/**
 * 校验 callable [declaration] 的返回类型引用已解析。
 */
internal fun checkReturnTypeRefIsResolved(declaration: CfirCallableDeclaration, acceptImplicitTypeRef: Boolean = false) {
    checkTypeRefIsResolved(declaration.returnTypeRef, typeRefName = "return type", declaration, acceptImplicitTypeRef)
}

/**
 * 校验成员 [declaration] 的状态已解析。
 */
internal fun checkDeclarationStatusIsResolved(declaration: CfirMemberDeclaration) {
    val status = declaration.status
    checkWithAttachment(
        status is CfirResolvedDeclarationStatus,
        { "Expected ${CfirResolvedDeclarationStatus::class.simpleName} but ${status::class.simpleName} found for ${declaration::class.simpleName}" }
    ) {
        withCfirEntry("declaration", declaration)
    }
}

/**
 * 校验 [typeRef] 及其嵌套注解中的注解调用已解析。
 */
internal fun checkAnnotationsAreResolved(owner: CfirAnnotationContainer, typeRef: CfirTypeRef) {
    checkWithAttachment(typeRef is CfirResolvedTypeRef, { "Unexpected type: ${typeRef::class.simpleName}" }) {
        withCfirEntry("owner", owner)
        withCfirEntry("type", typeRef)
    }

    typeRef.accept(AnnotationChecker, owner)
}

/**
 * 校验 [annotationCall] 在当前 body resolver 上下文中已经完成解析。
 */
internal fun CfirAbstractBodyResolveTransformerDispatcher.checkAnnotationCallIsResolved(
    symbol: CfirBasedSymbol<*>,
    annotationCall: CfirAnnotationCall,
) {
    val annotationContainer = context.containerIfAny ?: errorWithAttachment("Container cannot be found") {
        withCfirSymbolEntry("symbol", symbol)
        withCfirEntry("annotation", annotationCall)
    }

    checkAnnotationIsResolved(annotationCall, annotationContainer)
}

/**
 * 遍历非局部注解并逐个检查注解解析状态的 visitor。
 */
private object AnnotationChecker : NonLocalAnnotationVisitor<CfirAnnotationContainer>() {
    /**
     * 校验单个 [annotation] 在 [data] 容器中的解析状态。
     */
    override fun processAnnotation(annotation: CfirAnnotation, data: CfirAnnotationContainer) {
        checkAnnotationIsResolved(annotation, data)
    }
}

/**
 * 校验 [annotationContainer] 中所有注解调用和参数表达式已经解析。
 */
internal fun checkAnnotationsAreResolved(annotationContainer: CfirAnnotationContainer) {
    for (annotation in annotationContainer.annotations) {
        checkAnnotationIsResolved(annotation, annotationContainer)
    }
}

/**
 * 校验单个 [annotation] 在 [annotationContainer] 上下文中已经解析。
 */
internal fun checkAnnotationIsResolved(annotation: CfirAnnotation, annotationContainer: CfirAnnotationContainer) {
    if (annotation is CfirAnnotationCall) {
        checkWithAttachment(
            annotation.argumentList is CfirResolvedArgumentList,
            {
                buildString {
                    append("Expected ${CfirResolvedArgumentList::class.simpleName}")
                    append(" for ${annotation::class.simpleName} of ${annotationContainer::class.simpleName}(${(annotationContainer as? CfirDeclaration)?.origin})")
                    append(" but ${annotation.argumentList::class.simpleName} found")
                }
            }
        ) {
            withCfirEntry("cfirAnnotation", annotation)
            withCfirEntry("cfirDeclaration", annotationContainer)
        }
    }

    for (argument in annotation.arguments.filterIsInstance<CfirExpression>()) {
        checkExpressionTypeIsResolved(argument.coneTypeOrNull, "annotation argument", annotationContainer) {
            withCfirEntry("cfirAnnotation", annotation)
            withCfirEntry("cfirArgument", argument)
        }
    }
}

/**
 * 检查 [target] 是否已经解析到 [requestedPhase]。
 *
 * 如果目标已经完成对应阶段，会按需记录 ready phase 事件。
 *
 * @param target 正在分析的声明或元素。
 * @param containingDeclarations 从文件开始包围 [target] 的声明列表，传入该参数可避免重复计算。
 * @param requestedPhase 需要确认的目标解析阶段。
 *
 * @return 如果 [target] 已经达到 [requestedPhase] 则返回 `true`。
 */
internal fun checkAnalysisReadiness(
    target: CfirElementWithResolveState,
    containingDeclarations: List<CfirDeclaration>?,
    requestedPhase: CfirResolvePhase,
    currentPhase: CfirResolvePhase = target.resolvePhase
): Boolean {
    if (currentPhase >= requestedPhase) {
        if (shouldRecordReadyPhaseEvent(requestedPhase)) {
            if (containingDeclarations != null) {
                LLFlightRecorder.readyPhase(target, containingDeclarations, requestedPhase)
            } else {
                LLFlightRecorder.readyPhase(target, requestedPhase)
            }
        }
        return true
    }

    return false
}

/**
 * 判断请求 [requestedPhase] 时是否需要记录 ready phase 事件。
 */
private fun shouldRecordReadyPhaseEvent(requestedPhase: CfirResolvePhase): Boolean {
    return when (requestedPhase) {
        CfirResolvePhase.RAW_CFIR -> false

        CfirResolvePhase.IMPORTS -> {
            /**
             * Technically, we should record here [LLFlightRecorder.readyPhase] events here. However, imports for files are requested
             * too often. Moreover, import resolution is quite fast, so we won't be able to get any useful information from these events.
             */
            false
        }
        else -> true
    }
}
