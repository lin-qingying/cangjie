package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.signatures.CaValueParameterSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjAnnotated
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjParameter

/**
 * CFIR 元数据协议实现。
 *
 * 这个文件统一承载 Analysis API 对外公开的三类稳定元数据模型：
 * - 注解
 * - 签名
 * - 默认导入
 *
 * 这样这些结构不会散落在不同组件里重复拼装。
 */
internal class CaCfirAnnotationImpl(
    override val classId: ClassId?,
    override val shortName: Name?,
    override val arguments: List<String>,
    override val renderedText: String,
    override val token: CaLifetimeToken,
) : CaAnnotation

internal class CaCfirValueParameterSignatureImpl(
    override val name: Name?,
    override val type: CaType?,
    override val typeText: String?,
    override val annotations: List<CaAnnotation>,
    override val token: CaLifetimeToken,
) : CaValueParameterSignature

internal open class CaCfirSignatureImpl(
    override val declarationName: Name?,
    override val typeParameters: List<Name>,
    override val valueParameters: List<CaValueParameterSignature>,
    override val returnType: CaType?,
    override val returnTypeText: String?,
    override val annotations: List<CaAnnotation>,
    override val token: CaLifetimeToken,
) : CaSignature

internal class CaCfirDefaultImportsImpl(
    override val regularImports: List<org.cangnova.cangjie.ImportPath>,
    override val lowPriorityImports: List<org.cangnova.cangjie.ImportPath>,
    override val excludedImports: List<org.cangnova.cangjie.name.FqName>,
    override val token: CaLifetimeToken,
) : CaDefaultImports

/**
 * 从公开声明符号读取注解。
 *
 * 当前阶段注解视图以源码声明为中心：
 * 只有能够恢复到源码声明 PSI 的公开符号，才能稳定生成注解快照。
 */
internal fun CaCfirSession.renderAnnotations(symbol: CaDeclarationSymbol): List<CaAnnotation> {
    val owner = symbol.psi as? CjAnnotated ?: return emptyList()
    return renderAnnotations(owner)
}

/**
 * 从源码声明节点读取注解。
 */
internal fun CaCfirSession.renderAnnotations(owner: CjAnnotated): List<CaAnnotation> {
    return getOrCreateDeclarationAnnotations(owner) {
        owner.annotationEntries.map { annotation ->
            annotation.asPublicAnnotation(this, token)
        }
    }
}

/**
 * 从源码 callable 声明构建结构化签名。
 */
internal fun CaCfirSession.renderSignature(declaration: CjCallableDeclaration): CaSignature {
    return getOrCreateDeclarationSignature(declaration) {
        CaCfirSignatureImpl(
            declarationName = declaration.nameAsName,
            typeParameters = declaration.typeParameters.mapNotNull { typeParameter -> typeParameter.nameAsName },
            valueParameters = declaration.valueParameters.map { parameter ->
                parameter.asPublicParameterSignature(this, token)
            },
            returnType = with(this) { declaration.returnType },
            returnTypeText = declaration.typeReference?.text,
            annotations = renderAnnotations(declaration),
            token = token,
        )
    }
}

/**
 * 从公开 callable 符号构建结构化签名。
 */
internal fun CaCfirSession.renderSignature(symbol: CaCallableSymbol): CaSignature? {
    val cfirSymbol = symbol as? CaCfirCallableSymbolImpl ?: return null
    val cacheKey = cfirSymbol.callableId?.let(::CaCfirCallableSymbolCacheKey) ?: return null
    return getOrCreateCallableSignature(cacheKey) {
        val declaration = cfirSymbol.psi as? CjCallableDeclaration ?: return@getOrCreateCallableSignature null
        renderSignature(declaration)
    }
}

/**
 * 从当前 use-site session 构建默认导入视图。
 */
internal fun CaCfirSession.renderDefaultImports(): CaDefaultImports {
    return getOrCreateDefaultImports {
        val provider = cfirSession.defaultImportsProvider
        CaCfirDefaultImportsImpl(
            regularImports = provider.getDefaultImports(includeLowPriorityImports = false),
            lowPriorityImports = provider.defaultLowPriorityImports,
            excludedImports = provider.excludedImports,
            token = token,
        )
    }
}

private fun CjAnnotation.asPublicAnnotation(
    session: CaCfirSession,
    token: CaLifetimeToken,
): CaAnnotation {
    return CaCfirAnnotationImpl(
        classId = resolveAnnotationClassId(session),
        shortName = shortName,
        arguments = valueArguments.map { argument ->
            argument.getArgumentExpression()?.text ?: argument.asElement().text
        },
        renderedText = text,
        token = token,
    )
}

/**
 * 从注解条目恢复其目标 class-like 标识。
 *
 * 这里复用既有的公开引用解析协议，而不是再引入一套注解专用解析逻辑。
 * 如果 callee 无法恢复为公开 class-like 符号，则明确返回 `null`。
 */
private fun CjAnnotation.resolveAnnotationClassId(session: CaCfirSession): ClassId? {
    val constructorReference = calleeExpression?.constructorReferenceExpression ?: return null
    val targetSymbol = with(session) {
        constructorReference.resolveToSymbol()
    }
    return (targetSymbol as? CaClassLikeSymbol)?.classId
}

private fun CjParameter.asPublicParameterSignature(
    session: CaCfirSession,
    token: CaLifetimeToken,
): CaValueParameterSignature {
    return CaCfirValueParameterSignatureImpl(
        name = nameAsName,
        type = session.queryValueParameterType(this)?.asCaType(token),
        typeText = typeReference?.text,
        annotations = session.renderAnnotations(this),
        token = token,
    )
}
