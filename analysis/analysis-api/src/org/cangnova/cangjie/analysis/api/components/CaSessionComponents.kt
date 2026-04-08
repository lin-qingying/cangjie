package org.cangnova.cangjie.analysis.api.components

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowInfo
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports
import org.cangnova.cangjie.analysis.api.imports.CaImportOptimizationPlan
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningCommand
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningPlan
import org.cangnova.cangjie.analysis.api.interop.CaInteropInfo
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForDebug
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRendererPosition
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForDebug
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.substitution.CaSubstitutedSignature
import org.cangnova.cangjie.analysis.api.substitution.CaTypeSubstitutor
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType
import org.cangnova.cangjie.analysis.api.types.CaTupleType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaUnionType
import org.cangnova.cangjie.analysis.api.types.pointers.CaTypePointer
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjReferenceExpression

/**
 * 解析协议。
 *
 * 这里只负责把源码元素映射到稳定的公开语义结果，
 * 不暴露底层候选、约束系统或后端专属解析细节。
 */
interface CaResolver : CaLifetimeOwner {
    fun CjReferenceExpression.resolveToSymbols(): Collection<CaSymbol>

    fun CjReferenceExpression.resolveToSymbol(): CaSymbol? = resolveToSymbols().singleOrNull()

    fun CjElement.resolveToCall(): CaCallInfo?
}

interface CaSymbolRelationProvider : CaLifetimeOwner {
    fun CaSymbol.isEquivalentTo(other: CaSymbol): Boolean
}

/**
 * 指针协议。
 *
 * 所有跨 `analyze {}` 传递的 symbol 都必须先降格为 pointer，
 * 避免直接泄漏 session 内部对象。
 */
interface CaSymbolInformationProvider : CaLifetimeOwner {
    fun CaSymbol.createPointer(): CaSymbolPointer<CaSymbol>
}

interface CaAnnotationProvider : CaLifetimeOwner {
    val CaDeclarationSymbol.annotations: List<CaAnnotation>
}

/**
 * callable 签名协议。
 *
 * 该层只暴露结构化语义签名，不再缓存源码文本快照。
 * 需要源码级文本时，应由 renderer 直接读取 PSI 或 source snapshot。
 */
interface CaSignatureProvider : CaLifetimeOwner {
    val CjCallableDeclaration.signature: CaSignature

    val CaCallableSymbol.signature: CaSignature?
}

interface CaTypeProvider : CaLifetimeOwner {
    val CaClassLikeSymbol.defaultType: CaType
}

/**
 * 类型附加信息协议。
 *
 * 该层负责类型对象与公开 symbol 之间的稳定关联，
 * 以及错误类型、指针等非声明级类型元信息。
 */
interface CaTypeInformationProvider : CaLifetimeOwner {
    fun CaType.createPointer(): CaTypePointer<CaType>

    val CaType.isErrorType: Boolean

    val CaType.classLikeSymbol: CaClassLikeSymbol?
}

interface CaTypeRelationChecker : CaLifetimeOwner {
    fun CaType.isSubTypeOf(superType: CaType): Boolean

    fun CaType.semanticallyEquals(other: CaType): Boolean
}

interface CaTypeCreator : CaLifetimeOwner {
    fun buildClassLikeType(
        classId: ClassId,
        typeArguments: List<CaType> = emptyList(),
    ): CaClassLikeType

    fun buildClassLikeType(
        symbol: CaClassLikeSymbol,
        typeArguments: List<CaType> = emptyList(),
    ): CaClassLikeType

    fun buildFunctionType(
        parameterTypes: List<CaType>,
        returnType: CaType,
        isCFunction: Boolean = false,
        isClosureType: Boolean = false,
        hasVariableLengthArgument: Boolean = false,
    ): CaFunctionType

    fun buildTupleType(
        elementTypes: List<CaType>,
    ): CaTupleType

    fun buildIntersectionType(
        conjuncts: List<CaType>,
    ): CaIntersectionType

    fun buildUnionType(
        alternatives: Collection<CaType>,
    ): CaUnionType
}

interface CaSubstitutorProvider : CaLifetimeOwner {
    fun createTypeSubstitutor(substitutions: Map<Name, CaType>): CaTypeSubstitutor

    fun CaSignature.createSubstitutor(typeArguments: List<CaType>): CaTypeSubstitutor
}

interface CaSignatureSubstitutor : CaLifetimeOwner {
    fun CaSignature.substitute(substitutor: CaTypeSubstitutor): CaSubstitutedSignature
}

interface CaExpressionTypeProvider : CaLifetimeOwner {
    val CjExpression.expressionType: CaType?

    val CjCallableDeclaration.returnType: CaType?
}

interface CaExpressionInformationProvider : CaLifetimeOwner {
    val CjExpression.isStatementLike: Boolean

    val CjExpression.isCompileTimeConstant: Boolean
}

interface CaEvaluator : CaLifetimeOwner {
    fun CjExpression.evaluate(): CaCompileTimeValue?
}

interface CaDataFlowProvider : CaLifetimeOwner {
    fun CjExpression.getDataFlowInfo(): CaDataFlowInfo
}

interface CaDiagnosticProvider : CaLifetimeOwner {
    fun CjElement.diagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>>

    fun CjFile.collectDiagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>>
}

interface CaScopeProvider : CaLifetimeOwner {
    fun CjFile.getFileScope(): CaScope

    fun getPackageScope(packageFqName: FqName): CaScope?

    val CaPackageSymbol.packageScope: CaScope

    val CaClassLikeSymbol.declaredMemberScope: CaScope

    val CaExtendSymbol.declaredMemberScope: CaScope

    val CaClassLikeSymbol.memberScope: CaScope

    val CaType.scope: CaScope?
}

interface CaAnalysisScopeProvider : CaLifetimeOwner {
    fun CaModule.analysisScope(): GlobalSearchScope
}

interface CaDefaultImportProvider : CaLifetimeOwner {
    val defaultImports: CaDefaultImports
}

interface CaCompletionCandidateChecker : CaLifetimeOwner {
    fun CaSymbol.checkCompletionCandidate(position: CjElement): CaCompletionCandidateDecision
}

interface CaVisibilityChecker : CaLifetimeOwner {
    fun CaSymbol.isVisible(): Boolean
}

interface CaReferenceShortener : CaLifetimeOwner {
    fun CjFile.collectReferenceShorteningPlan(): CaReferenceShorteningPlan

    /**
     * 按选择范围收集真正要执行的缩短命令。
     *
     * 该入口与 Kotlin `collectPossibleReferenceShortenings(file, selection)` 对位，
     * 但仓颉当前只暴露已经稳定的公共结果：
     * - 命中的操作
     * - 需要补齐的 imports
     */
    fun CjFile.collectReferenceShortenings(
        selection: TextRange = textRange,
    ): CaReferenceShorteningCommand

    /**
     * 按单个 PSI 元素范围收集缩短命令。
     */
    fun CjElement.collectReferenceShorteningsInElement(): CaReferenceShorteningCommand
}

interface CaImportOptimizer : CaLifetimeOwner {
    fun CjFile.collectImportOptimizationPlan(): CaImportOptimizationPlan
}

/**
 * 渲染协议。
 *
 * symbol 只提供语义模型，如何把它呈现为源码风格或调试风格文本，
 * 统一由 renderer 负责，避免把文本缓存重新塞回 symbol。
 */
interface CaRenderer : CaLifetimeOwner {
    fun CaSymbol.render(): String

    fun CaDeclarationSymbol.render(
        renderer: CaDeclarationRenderer = CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES,
    ): String

    fun CaDeclarationSymbol.renderDebug(
        renderer: CaDeclarationRenderer = CaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES,
    ): String = render(renderer)

    fun CaType.render(): String

    fun CaType.render(
        renderer: CaTypeRenderer = CaTypeRendererForSource.WITH_QUALIFIED_NAMES,
        position: CaTypeRendererPosition = CaTypeRendererPosition.INVARIANT,
    ): String

    fun CaType.renderDebug(
        renderer: CaTypeRenderer = CaTypeRendererForDebug.WITH_QUALIFIED_NAMES,
        position: CaTypeRendererPosition = CaTypeRendererPosition.INVARIANT,
    ): String = render(renderer, position)
}

interface CaOriginalPsiProvider : CaLifetimeOwner {
    fun CaSymbol.getOriginalPsi(): PsiElement?
}

interface CaSourceProvider : CaLifetimeOwner {
    fun CaSymbol.getContainingFile(): CjFile?
}

interface CaCInteropComponent : CaLifetimeOwner {
    fun CjElement.getInteropInfo(): CaInteropInfo?

    fun CaSymbol.getInteropInfo(): CaInteropInfo?
}

interface CaDocProvider : CaLifetimeOwner {
    fun CaSymbol.documentation(): String?
}
