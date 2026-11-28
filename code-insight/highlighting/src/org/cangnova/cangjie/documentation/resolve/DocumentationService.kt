@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaNonPublicApi::class,
    org.cangnova.cangjie.psi.CjNonPublicApi::class,
    org.cangnova.cangjie.psi.CjImplementationDetail::class,
)

package org.cangnova.cangjie.documentation.resolve

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.containingDeclaration
import org.cangnova.cangjie.analysis.api.components.declaredMemberScope
import org.cangnova.cangjie.analysis.api.components.findCDoc
import org.cangnova.cangjie.analysis.api.components.renderAnnotated
import org.cangnova.cangjie.analysis.api.renderer.declarations.withSemanticAnnotations
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForQuickDocumentation
import org.cangnova.cangjie.analysis.api.resolution.CaCall
import org.cangnova.cangjie.analysis.api.resolution.CaCallableMemberCall
import org.cangnova.cangjie.analysis.api.resolution.CaErrorCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaSuccessCallInfo
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.psiSafe
import org.cangnova.cangjie.analysis.api.symbols.symbol
import org.cangnova.cangjie.analysis.api.symbols.bindingSymbol
import org.cangnova.cangjie.documentation.api.DocumentationBody
import org.cangnova.cangjie.documentation.api.DocumentationContainer
import org.cangnova.cangjie.documentation.api.DocumentationContent
import org.cangnova.cangjie.documentation.builtin.BasicTypeDocumentation
import org.cangnova.cangjie.documentation.cdoc.buildDocumentationDescription
import org.cangnova.cangjie.documentation.cdoc.buildDocumentationModel
import org.cangnova.cangjie.documentation.cdoc.findCDocByPsi
import org.cangnova.cangjie.idea.references.CjSimpleNameReference
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocCommentDescriptorImpl
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjAbstractClassBody
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjNamedPattern
import org.cangnova.cangjie.psi.CjBasicType
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjLightElement
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjMacroInput
import org.cangnova.cangjie.psi.CjModifierList
import org.cangnova.cangjie.psi.CjNameBasicReferenceExpression
import org.cangnova.cangjie.psi.CjNameReferenceExpression
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSecondaryConstructor
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjValueArgumentList
import java.util.concurrent.CancellationException

/** 定义完整文档与 HINT 的解析模式。 */
enum class DocumentationMode {
    FULL,
    HINT,
}

/** 一次文档请求携带的目标与 use-site 上下文。 */
data class DocumentationRequest(
    val element: PsiElement,
    val origin: PsiElement?,
    val useSiteFile: CjFile?,
    val mode: DocumentationMode,
)

/** 文档目标解析结果；每种失败原因都保留为独立状态。 */
sealed interface DocumentationResolution {
    data object NotTarget : DocumentationResolution

    data object MissingUseSiteContext : DocumentationResolution

    data object SymbolRecoveryFailed : DocumentationResolution

    data class Resolved(val content: DocumentationContent) : DocumentationResolution
}

/** 元素、公开符号与文档内容之间的唯一解析入口。 */
interface DocumentationService {
    context(_: CaSession)
    fun resolve(request: DocumentationRequest): DocumentationResolution
}

/** 无状态服务；宿主在 Analysis session 中直接调用，不注册平台服务。 */
object DefaultDocumentationService : DocumentationService {
    context(_: CaSession)
    override fun resolve(request: DocumentationRequest): DocumentationResolution {
        val target = request.element
        val sourceCall = request.origin?.findEnclosingCalleeCall()
        if (sourceCall != null && target.navigationElement is CjMacroExpression) {
            return resolveCall(sourceCall, target, request)
        }

        target.basicTypeDocumentationName()?.let { typeName ->
            return resolveBasicTypeDocumentation(typeName, request)
        }

        return when {
            target.isCangJieModifier() -> DocumentationResolution.NotTarget
            target is CjNamedPattern -> resolveBindingPattern(target, request)
            target is org.cangnova.cangjie.psi.CjCommandTypePattern || target is org.cangnova.cangjie.psi.CjTryResource ->
                resolveHostBinding(target as CjElement, request)
            target is CjMacroExpression -> {
                val declaration = target.referenceExpression?.resolveMacroDeclaration()
                    ?: return DocumentationResolution.NotTarget
                resolveElement(declaration, request)
            }

            target is CjNameReferenceExpression -> resolveNameReference(target, request)
            target is CjEnumConstructor -> resolveDeclaration(target, request, enumOrdinal(target, request.mode))
            target is CjDeclaration -> resolveDeclaration(target, request)
            target is CjLightElement<*, *> -> {
                val origin = target.cangjieOrigin ?: return DocumentationResolution.NotTarget
                resolveElement(origin, request.copy(element = origin))
            }

            target is CjValueArgumentList -> resolveValueArgumentList(target, request)
            target is CjCallExpression -> {
                val basicTypeReference = target.calleeReference() as? CjNameBasicReferenceExpression
                if (basicTypeReference != null) {
                    resolveBasicTypeDocumentation(basicTypeReference.referencedName, request)
                } else {
                    resolveCall(target, target, request)
                }
            }

            else -> DocumentationResolution.NotTarget
        }
    }

    private fun resolveBasicTypeDocumentation(
        typeName: String,
        request: DocumentationRequest,
    ): DocumentationResolution {
        val content = BasicTypeDocumentation.content(typeName, request.mode)
            ?: return DocumentationResolution.NotTarget
        return DocumentationResolution.Resolved(content)
    }

    private fun PsiElement.basicTypeDocumentationName(): String? {
        var current: PsiElement? = this
        while (current != null) {
            val element = current
            val typeName = when (element) {
                is CjBasicType -> element.name
                is CjNameBasicReferenceExpression -> element.referencedName
                else -> null
            }
            if (typeName != null) return typeName
            current = element.parent
        }
        return null
    }

    context(_: CaSession)
    private fun resolveElement(element: PsiElement, request: DocumentationRequest): DocumentationResolution = when (element) {
        is CjNamedPattern -> resolveBindingPattern(element, request.copy(element = element))
        is org.cangnova.cangjie.psi.CjCommandTypePattern, is org.cangnova.cangjie.psi.CjTryResource ->
            resolveHostBinding(element as CjElement, request.copy(element = element))
        is CjDeclaration -> resolveDeclaration(element, request.copy(element = element))
        is CjLightElement<*, *> -> {
            val origin = element.cangjieOrigin ?: return DocumentationResolution.NotTarget
            resolveElement(origin, request.copy(element = origin))
        }

        else -> DocumentationResolution.NotTarget
    }

    context(session: CaSession)
    private fun resolveNameReference(
        referenceExpression: CjNameReferenceExpression,
        request: DocumentationRequest,
    ): DocumentationResolution {
        val macroExpression = (referenceExpression.parent as? CjMacroExpression)
            ?.takeIf { it.referenceExpression == referenceExpression }
        if (macroExpression != null) {
            val macro = referenceExpression.resolveMacroDeclaration() ?: return DocumentationResolution.NotTarget
            return resolveElement(macro, request.copy(element = macro, origin = macroExpression))
        }

        val reference = referenceExpression.references.firstOrNull { it is CjSimpleNameReference } as? CjSimpleNameReference
            ?: return DocumentationResolution.NotTarget
        val declaration = reference.resolve() as? CjElement ?: return DocumentationResolution.NotTarget
        if (declaration === referenceExpression) return DocumentationResolution.NotTarget
        val origin = request.origin ?: referenceExpression
        // 引用点的 Analysis 会话最准确；尤其是库声明：声明 PSI 来自反编译文件，
        // 直接 `declaration.symbol` 会退化成 symbol recovery failure。
        val referenceSymbol = recoverSymbol {
            with(session) { referenceExpression.resolveToSymbol() as? CaDeclarationSymbol }
        }
        if (referenceSymbol != null) {
            return createResolved(referenceSymbol, declaration, request.copy(element = declaration, origin = origin))
        }
        return resolveElement(declaration, request.copy(element = declaration, origin = origin))
    }

    context(_: CaSession)
    private fun resolveValueArgumentList(
        argumentList: CjValueArgumentList,
        request: DocumentationRequest,
    ): DocumentationResolution {
        val reference = argumentList.prevSibling as? CjSimpleNameExpression ?: return DocumentationResolution.NotTarget
        val simpleNameReference = reference.references.firstOrNull { it is CjSimpleNameReference } as? CjSimpleNameReference
            ?: return DocumentationResolution.NotTarget
        val declaration = simpleNameReference.resolve() ?: return DocumentationResolution.NotTarget
        if (declaration !is CjCallableDeclaration && declaration !is CjEnumConstructor) {
            return DocumentationResolution.NotTarget
        }
        return resolveElement(declaration, request.copy(element = declaration))
    }

    context(_: CaSession)
    private fun resolveBindingPattern(
        bindingPattern: CjNamedPattern,
        request: DocumentationRequest,
    ): DocumentationResolution {
        val symbol = bindingPattern.bindingSymbol ?: return DocumentationResolution.NotTarget
        return createResolved(symbol, bindingPattern, request)
    }

    context(session: CaSession)
    private fun resolveHostBinding(element: CjElement, request: DocumentationRequest): DocumentationResolution {
        val symbol = with(session) {
            when (element) {
                is org.cangnova.cangjie.psi.CjCommandTypePattern -> element.bindingSymbol
                is org.cangnova.cangjie.psi.CjTryResource -> element.bindingSymbol
                else -> null
            }
        } ?: return DocumentationResolution.NotTarget
        return createResolved(symbol, element, request)
    }

    context(_: CaSession)
    private fun resolveDeclaration(
        declaration: CjDeclaration,
        request: DocumentationRequest,
        enumOrdinal: Int? = null,
    ): DocumentationResolution {
        val namedDeclaration = declaration as? CjNamedDeclaration
        // 反编译/库 PSI 的符号只能从它自己的 Analysis 会话恢复；按 use-site scope
        // 重建会因 PSI 实例不同（isEquivalentTo）而失败。
        if (namedDeclaration != null && (namedDeclaration.containingFile as? CjFile)?.isCompiled == true) {
            val symbol = recoverSymbol { namedDeclaration.symbol } as? CaDeclarationSymbol
                ?: return DocumentationResolution.SymbolRecoveryFailed
            return createResolved(symbol, namedDeclaration, request, enumOrdinal)
        }
        val requiresUseSiteFile = declaration.requiresUseSiteFile(request)
        val lookup = when {
            namedDeclaration == null -> recoverSymbol { declaration.symbol }
                ?.let(SymbolLookup::Found)
                ?: SymbolLookup.Failed

            requiresUseSiteFile && request.useSiteFile == null -> SymbolLookup.MissingUseSiteContext
            !requiresUseSiteFile -> recoverSymbol { declaration.symbol }
                ?.let { SymbolLookup.Found(it) }
                ?: SymbolLookup.Failed

            else -> namedDeclaration.toPublicSymbol(requireNotNull(request.useSiteFile))
                ?.let(SymbolLookup::Found)
                ?: SymbolLookup.Failed
        }

        val symbol = when (lookup) {
            SymbolLookup.MissingUseSiteContext -> return DocumentationResolution.MissingUseSiteContext
            SymbolLookup.Failed -> return DocumentationResolution.SymbolRecoveryFailed
            is SymbolLookup.Found -> lookup.symbol
        }
        return createResolved(symbol, declaration, request, enumOrdinal)
    }

    context(session: CaSession)
    private fun resolveCall(
        callExpression: CjCallExpression,
        target: PsiElement,
        request: DocumentationRequest,
    ): DocumentationResolution {
        val callInfo = recoverSymbol { with(session) { callExpression.resolveToCall() } }
        val callSymbol = when (callInfo) {
            is CaSuccessCallInfo -> callInfo.call.toDeclarationSymbol()
            is CaErrorCallInfo -> {
                val candidates = callInfo.candidateCalls.mapNotNull { call -> call.toDeclarationSymbol() }
                candidates.singleOrNull()
                    ?: candidates.singleOrNull { it.matchesTarget(target) }
                    ?: candidates.singleOrNull { it.matchesNavigationTarget(target) }
            }

            else -> null
        }
        val callReference = callExpression.calleeReference() as? CjReferenceExpression
        val referenceSymbol = recoverSymbol {
            with(session) {
                (callReference ?: target as? CjReferenceExpression)?.resolveToSymbol() as? CaDeclarationSymbol
            }
        }
        val targetDeclaration = target as? CjDeclaration
        val symbol = referenceSymbol ?: callSymbol ?: targetDeclaration
            ?.takeIf { it.isPhysical }
            ?.let { recoverSymbol { it.symbol } as? CaDeclarationSymbol }
            ?: return DocumentationResolution.NotTarget
        val declaration = symbol.psiSafe<CjElement>() ?: target as? CjElement
        return createResolved(symbol, declaration, request)
    }

    context(_: CaSession)
    private fun createResolved(
        symbol: CaDeclarationSymbol,
        declaration: CjElement?,
        request: DocumentationRequest,
        enumOrdinal: Int? = null,
    ): DocumentationResolution.Resolved {
        val renderedDefinition = symbol.renderAnnotated(
            CaDeclarationRendererForQuickDocumentation.WITH_SHORT_NAMES.withSemanticAnnotations(),
        )
        val descriptor = findDocumentation(symbol, declaration)
        val body = if (request.mode == DocumentationMode.HINT) {
            null
        } else {
            descriptor?.let {
                val typeParameterNames = (symbol as? CaTypeParameterOwnerSymbol)
                    ?.typeParameters
                    ?.map { it.name.asString() }
                    ?.toSet()
                    .orEmpty()
                DocumentationBody(
                    sections = buildDocumentationModel(it, typeParameterNames),
                    description = buildDocumentationDescription(it),
                    enumOrdinal = enumOrdinal,
                )
            } ?: DocumentationBody(emptyList(), enumOrdinal = enumOrdinal)
        }

        return DocumentationResolution.Resolved(
            DocumentationContent(
                definition = renderedDefinition.text,
                body = body,
                container = createContainer(symbol, declaration),
                definitionAnnotations = renderedDefinition.annotations,
            ),
        )
    }

    context(_: CaSession)
    private fun findDocumentation(
        symbol: CaDeclarationSymbol,
        declaration: CjElement?,
    ): CDocCommentDescriptor? {
        symbol.findCDoc()?.let { return it }
        val secondaryConstructor = (symbol.psi?.navigationElement as? CjSecondaryConstructor)
            ?: declaration as? CjSecondaryConstructor
            ?: return null
        val cdocContent = secondaryConstructor.getContainingTypeStatement().findCDocByPsi() ?: return null
        return CDocCommentDescriptorImpl(cdocContent.contentTag, cdocContent.sections)
    }

    context(_: CaSession)
    private fun createContainer(
        symbol: CaDeclarationSymbol,
        declaration: CjElement?,
    ): DocumentationContainer? {
        val classLike = symbol.containingDeclaration as? CaClassLikeSymbol
        val isTopLevel = symbol.location == CaSymbolLocation.TOP_LEVEL
        val packageName = (declaration?.containingFile as? CjFile)
            ?.packageFqName
            ?.takeUnless { it.isRoot }
            ?.asString()
        val fqName = classLike?.classId?.asFqNameString() ?: packageName ?: return null
        if (classLike != null) return DocumentationContainer.ClassLike(fqName)
        val fileName = if (isTopLevel) {
            (declaration?.navigationElement?.containingFile as? CjFile)?.name
        } else {
            null
        }
        return DocumentationContainer.Package(fqName, fileName)
    }

    context(session: CaSession)
    private fun CjNamedDeclaration.toPublicSymbol(useSiteFile: CjFile): CaDeclarationSymbol? {
        // 库/反编译声明的 PSI 来自另一份 decompiled file，按 use-site scope 重建符号时
        // `psi.isEquivalentTo` 会因 PSI 实例不同而失败。导航已经证明 `declaration.symbol`
        // 可直接恢复，因此 compiled file 优先走符号直取，不参与 use-site 重建。
        if (containingCjFile.isCompiled) {
            return recoverSymbol { symbol } as? CaDeclarationSymbol
        }
        return when (this) {
            is CjClassLikeDeclaration -> getClassId()?.let(session::getClassLikeSymbol)
            is CjCallableDeclaration -> restoreCallableSymbol(useSiteFile)
            else -> recoverSymbol { symbol } as? CaDeclarationSymbol
        }
    }

    context(session: CaSession)
    private fun CjCallableDeclaration.restoreCallableSymbol(useSiteFile: CjFile): CaCallableSymbol? {
        if (!useSiteFile.isValid) return null
        val declarationName = recoverSymbol { nameAsSafeName } ?: return null
        val classBody = containingClassBody()
        if (classBody != null) {
            val owner = classBody.parent as? CjTypeStatement ?: return null
            val memberScope = when (owner) {
                is CjExtend -> {
                    // `extend` 是独立声明容器且没有 ClassId，需从包级 extend 查询恢复其公开符号。
                    val ownerPackage = (owner.containingFile as? CjFile)?.packageFqName ?: return null
                    val ownerExtendId = owner.getExtendId()
                    val extendSymbol = session.getTopLevelExtendSymbols(ownerPackage)
                        .singleOrNull { candidate -> candidate.extendId == ownerExtendId }
                        ?: return null
                    extendSymbol.declaredMemberScope
                }

                else -> {
                    val ownerSymbol = owner.getClassId()?.let(session::getClassLikeSymbol) ?: return null
                    ownerSymbol.declaredMemberScope
                }
            }
            return memberScope.callables(declarationName)
                .singleOrNull { candidate -> candidate.psi?.isEquivalentTo(this) == true }
        }

        val declarationPackage = (containingFile as? CjFile)?.packageFqName ?: return null
        return session.getTopLevelCallableSymbols(declarationPackage, declarationName)
            .singleOrNull { candidate -> candidate.psi?.isEquivalentTo(this) == true }
    }

    /** 仅越过声明宏的透传节点查找所属类体，避免把宏包装的成员误判为顶层声明。 */
    private fun CjCallableDeclaration.containingClassBody(): CjAbstractClassBody? {
        var current: PsiElement? = parent
        while (current != null) {
            current = when (val currentElement = current) {
                is CjAbstractClassBody -> return currentElement
                is CjMacroInput -> currentElement.parent
                is CjMacroExpression -> {
                    if (currentElement.unwrappedDeclaration !== this) return null
                    currentElement.parent
                }

                else -> return null
            }
        }
        return null
    }

    private fun CaCall.toDeclarationSymbol(): CaDeclarationSymbol? =
        (this as? CaCallableMemberCall<*, *>)
            ?.partiallyAppliedSymbol
            ?.signature
            ?.symbol as? CaDeclarationSymbol

    private fun CaDeclarationSymbol.matchesTarget(target: PsiElement): Boolean =
        psi?.isEquivalentTo(target) == true

    private fun CaDeclarationSymbol.matchesNavigationTarget(target: PsiElement): Boolean =
        psi?.navigationElement?.isEquivalentTo(target.navigationElement) == true

    private fun CjNameReferenceExpression.resolveMacroDeclaration(): CjMacroDeclaration? =
        references.singleOrNull()?.resolve() as? CjMacroDeclaration

    private fun CjNameReferenceExpression.findEnclosingCallExpression(): CjCallExpression? {
        var current: PsiElement? = parent
        while (current != null) {
            val call = current as? CjCallExpression
            if (call?.calleeReference() == this) return call
            current = current.parent
        }
        return null
    }

    private fun PsiElement.findEnclosingCalleeCall(): CjCallExpression? {
        if (this is CjCallExpression) return this
        var current: PsiElement? = this
        while (current != null) {
            if (current is CjNameReferenceExpression) return current.findEnclosingCallExpression()
            current = current.parent
        }
        return null
    }

    private fun CjCallExpression.calleeReference(): CjSimpleNameExpression? =
        referenceExpression
            ?: (calleeExpression as? org.cangnova.cangjie.psi.CjQualifiedExpression)
                ?.selectorExpression as? CjSimpleNameExpression

    private fun CjDeclaration.requiresUseSiteFile(request: DocumentationRequest): Boolean {
        val sourceFile = request.useSiteFile ?: request.origin?.containingFile ?: request.element.containingFile
        return sourceFile != null && containingFile != sourceFile
    }

    private fun enumOrdinal(element: CjEnumConstructor, mode: DocumentationMode): Int? {
        if (mode == DocumentationMode.HINT) return null
        val ordinal = element.parentEnum?.constructor?.indexOf(element) ?: return null
        return ordinal.takeIf { it >= 0 }
    }

    private fun <T> recoverSymbol(block: () -> T): T? = try {
        block()
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private sealed interface SymbolLookup {
        data object MissingUseSiteContext : SymbolLookup
        data object Failed : SymbolLookup
        data class Found(val symbol: CaDeclarationSymbol) : SymbolLookup
    }
}

/** Modifier PSI predicate shared by target discovery and content resolution. */
fun PsiElement.isCangJieModifier(): Boolean =
    parent is CjModifierList && CjTokens.MODIFIER_KEYWORDS_ARRAY.any { token -> token.value == text }
