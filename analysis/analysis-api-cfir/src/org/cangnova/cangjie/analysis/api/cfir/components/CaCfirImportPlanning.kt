package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateStatus
import org.cangnova.cangjie.analysis.api.imports.CaImportOptimizationPlan
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningOperation
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningPlan
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.resolution.target
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportInfo
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression

/**
 * CFIR 补全候选决策实现。
 *
 * 它描述某个符号在指定位置是否可直接使用、是否需要导入，或者应被隐藏。
 */
internal class CaCfirCompletionCandidateDecisionImpl(
    override val symbol: CaSymbol,
    override val status: CaCompletionCandidateStatus,
    override val requiredImport: ImportPath?,
    override val token: CaLifetimeToken,
) : CaCompletionCandidateDecision

/**
 * 引用缩短操作实现。
 */
internal class CaCfirReferenceShorteningOperationImpl(
    override val expression: CjExpression,
    override val target: CaSymbol,
    override val shortName: Name,
    override val decision: CaCompletionCandidateDecision,
    override val token: CaLifetimeToken,
) : CaReferenceShorteningOperation

/**
 * 引用缩短规划实现。
 */
internal class CaCfirReferenceShorteningPlanImpl(
    override val file: CjFile,
    override val operations: List<CaReferenceShorteningOperation>,
    override val token: CaLifetimeToken,
) : CaReferenceShorteningPlan

/**
 * 导入优化规划实现。
 */
internal class CaCfirImportOptimizationPlanImpl(
    override val file: CjFile,
    override val retainedImports: List<CjImportInfo>,
    override val duplicateImports: List<CjImportInfo>,
    override val unusedImports: List<CjImportInfo>,
    override val missingImports: List<ImportPath>,
    override val token: CaLifetimeToken,
) : CaImportOptimizationPlan

/**
 * 在指定位置判定补全候选是否可直接使用、是否需要导入或应被隐藏。
 */
internal fun CaCfirSession.checkCompletionCandidate(
    symbol: CaSymbol,
    position: CjElement,
): CaCompletionCandidateDecision {
    return getOrCreateCompletionDecision(symbol, position) {
        val file = position.containingFile as? CjFile
            ?: error("补全候选判定只能在 CjFile 内执行：${position::class.simpleName}")
        val importPath = symbol.asTopLevelImportPath()
        val visible = with(this) { symbol.isVisible() }
        val status = when {
            !visible -> CaCompletionCandidateStatus.HIDDEN
            symbol is CaFileSymbol -> CaCompletionCandidateStatus.HIDDEN
            symbol is CaPackageSymbol -> CaCompletionCandidateStatus.DIRECT
            isDirectlyReachable(symbol, file) -> CaCompletionCandidateStatus.DIRECT
            importPath != null -> CaCompletionCandidateStatus.REQUIRES_IMPORT
            else -> CaCompletionCandidateStatus.HIDDEN
        }
        CaCfirCompletionCandidateDecisionImpl(
            symbol = symbol,
            status = status,
            requiredImport = importPath?.takeIf { status == CaCompletionCandidateStatus.REQUIRES_IMPORT },
            token = token,
        )
    }
}

/**
 * 收集文件内可进行缩短的限定引用。
 */
internal fun CaCfirSession.collectReferenceShorteningPlan(file: CjFile): CaReferenceShorteningPlan {
    return getOrCreateReferenceShorteningPlan(file) {
        val operations = PsiTreeUtil.collectElementsOfType(file, CjDotQualifiedExpression::class.java)
            .mapNotNull { expression ->
                val target = resolveShorteningTarget(expression) ?: return@mapNotNull null
                val shortName = target.shortNameOrNull() ?: return@mapNotNull null
                if (expression.text == shortName.asString()) return@mapNotNull null
                val decision = checkCompletionCandidate(target, expression)
                if (decision.status == CaCompletionCandidateStatus.HIDDEN) return@mapNotNull null
                CaCfirReferenceShorteningOperationImpl(
                    expression = expression,
                    target = target,
                    shortName = shortName,
                    decision = decision,
                    token = token,
                )
            }
        CaCfirReferenceShorteningPlanImpl(
            file = file,
            operations = operations,
            token = token,
        )
    }
}

/**
 * 结合当前文件引用和缩短规划结果，生成导入保留、去重、删除与补齐方案。
 */
internal fun CaCfirSession.collectImportOptimizationPlan(file: CjFile): CaImportOptimizationPlan {
    return getOrCreateImportOptimizationPlan(file) {
        val currentImports = file.importDirectivesItem
        val referencedNames = PsiTreeUtil.collectElementsOfType(file, CjSimpleNameExpression::class.java)
            .map(CjSimpleNameExpression::referencedNameAsName)
            .toSet()
        val duplicateImports = linkedSetOf<CjImportInfo>()
        val seenImports = linkedSetOf<ImportPath>()
        currentImports.forEach { importInfo ->
            val importPath = importInfo.importPathOrNull() ?: return@forEach
            if (!seenImports.add(importPath)) {
                duplicateImports += importInfo
            }
        }
        val shorteningPlan = collectReferenceShorteningPlan(file)
        val missingImports = shorteningPlan.operations
            .mapNotNull { operation -> operation.decision.requiredImport }
            .distinct()
            .filterNot(seenImports::contains)
        val unusedImports = currentImports.filter { importInfo ->
            if (importInfo in duplicateImports) return@filter false
            val importPath = importInfo.importPathOrNull() ?: return@filter false
            if (importPath.isAllUnder) return@filter false
            val importedName = importPath.importedName ?: return@filter false
            importedName !in referencedNames && importPath !in missingImports
        }
        CaCfirImportOptimizationPlanImpl(
            file = file,
            retainedImports = currentImports - duplicateImports - unusedImports.toSet(),
            duplicateImports = duplicateImports.toList(),
            unusedImports = unusedImports,
            missingImports = missingImports,
            token = token,
        )
    }
}

private fun CaCfirSession.resolveShorteningTarget(expression: CjDotQualifiedExpression): CaSymbol? {
    val selector = expression.selectorExpression
    return when (selector) {
        is CjReferenceExpression -> with(this) { selector.resolveToSymbol() }
        is CjCallExpression -> with(this) { selector.resolveToCall()?.target }
        else -> null
    }
}

private fun CaCfirSession.isDirectlyReachable(symbol: CaSymbol, file: CjFile): Boolean {
    val shortName = symbol.shortNameOrNull() ?: return false
    return queryFileScope(file)
        .getSymbols(shortName)
        .map(::getPublicSymbol)
        .any { visibleSymbol -> with(this) { visibleSymbol.isEquivalentTo(symbol) } }
}

private fun CaSymbol.shortNameOrNull(): Name? = when (this) {
    is CaClassLikeSymbol -> classId.shortClassName
    is CaCallableSymbol -> callableId?.callableName
    is CaPackageSymbol -> name.takeUnless(String::isEmpty)?.let(Name.Companion::identifier)
    else -> name?.let(Name.Companion::identifier)
}

private fun CaSymbol.asTopLevelImportPath(): ImportPath? = when (this) {
    is CaClassLikeSymbol -> ImportPath(classId.asSingleFqName(), false)
    is CaCallableSymbol -> callableId
        ?.takeIf { it.classId == null }
        ?.let { ImportPath(it.asSingleFqName(), false) }
    else -> null
}

private fun CjImportInfo.importPathOrNull(): ImportPath? {
    return when (this) {
        is org.cangnova.cangjie.psi.CjImportItem -> importPath
        else -> importedFqName?.let { ImportPath(it, isAllUnder, aliasName?.let(Name.Companion::identifier)) }
    }
}
