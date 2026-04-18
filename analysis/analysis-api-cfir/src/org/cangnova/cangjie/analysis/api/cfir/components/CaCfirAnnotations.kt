package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.annotations.CaNamedAnnotationValue
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.evaluate.asPublicNamedAnnotationValue
import org.cangnova.cangjie.analysis.api.impl.base.annotations.CaBaseAnnotationImpl
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjAnnotated
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjValueArgument

/**
 * CFIR 注解公开语义映射。
 *
 * Kotlin FIR 侧把“注解对象构建”和“注解值转换”拆在独立文件中。
 * 这里沿用同样的职责边界：本文件只负责注解公开模型，不再混放签名和默认导入逻辑。
 */
internal fun CaCfirSession.renderAnnotations(symbol: CaDeclarationSymbol): List<CaAnnotation> {
    val owner = symbol.psi as? CjAnnotated ?: return emptyList()
    return renderAnnotations(owner)
}

/**
 * 直接从源码注解容器构建公开注解快照。
 */
internal fun CaCfirSession.renderAnnotations(owner: CjAnnotated): List<CaAnnotation> {
    return getOrCreateDeclarationAnnotations(owner) {
        owner.annotationEntries.map { annotation -> annotation.asPublicAnnotation(this, token) }
    }
}

private fun CjAnnotation.asPublicAnnotation(
    session: CaCfirSession,
    token: CaLifetimeToken,
): CaAnnotation {
    val constructorSymbol = resolveAnnotationConstructorSymbol(session)
    return CaBaseAnnotationImpl(
        classId = resolveAnnotationClassId(session),
        shortName = shortName,
        psi = this,
        lazyArguments = lazy(LazyThreadSafetyMode.NONE) {
            buildPublicNamedArguments(
                session = session,
                token = token,
                constructorSymbol = constructorSymbol,
            )
        },
        constructorSymbol = constructorSymbol,
        token = token,
    )
}

/**
 * 从注解调用点恢复其目标 class-like 标识。
 */
private fun CjAnnotation.resolveAnnotationClassId(session: CaCfirSession): ClassId? {
    val constructorReference = calleeExpression?.constructorReferenceExpression ?: return null
    val targetSymbol = with(session) { constructorReference.resolveToSymbol() }
    return (targetSymbol as? CaClassLikeSymbol)?.classId
}

/**
 * 恢复注解调用解析到的构造器符号。
 */
private fun CjAnnotation.resolveAnnotationConstructorSymbol(session: CaCfirSession): CaConstructorSymbol? {
    val constructorReference = calleeExpression?.constructorReferenceExpression ?: return null
    return with(session) { constructorReference.resolveToSymbol() as? CaConstructorSymbol }
}

/**
 * 构建注解“命名参数 + 值对象”列表。
 */
private fun CjAnnotation.buildPublicNamedArguments(
    session: CaCfirSession,
    token: CaLifetimeToken,
    constructorSymbol: CaConstructorSymbol?,
): List<CaNamedAnnotationValue> {
    return valueArguments.mapIndexed { index, argument ->
        (argument as CjValueArgument).asPublicNamedAnnotationValue(
            session = session,
            token = token,
            fallbackName = constructorSymbol?.valueParameters?.getOrNull(index)?.name,
            position = index,
        )
    }
}
