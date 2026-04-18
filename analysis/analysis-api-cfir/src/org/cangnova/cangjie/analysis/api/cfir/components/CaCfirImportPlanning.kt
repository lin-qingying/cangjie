package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.getPublicSymbol
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateStatus
import org.cangnova.cangjie.analysis.api.imports.CaImportOptimizationPlan
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningCommand
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningOperation
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningPlan
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.resolution.target
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportInfo
import org.cangnova.cangjie.psi.CjImportDirective
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType

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
 * 指定选择范围的缩短命令实现。
 */
internal class CaCfirReferenceShorteningCommandImpl(
    override val file: CjFile,
    override val selection: TextRange,
    override val operations: List<CaReferenceShorteningOperation>,
    override val importsToAdd: Set<ImportPath>,
    override val token: CaLifetimeToken,
) : CaReferenceShorteningCommand

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
                if (!target.canBeShortenedAsStandaloneReference()) return@mapNotNull null
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
 * 从文件级全量 plan 中投影出 selection 对应的缩短命令。
 *
 * 当前仓颉实现坚持一条统一主线：
 * 1. 先构造文件级 plan，稳定表达“哪些表达式可被缩短”；
 * 2. 再按 range / element 过滤出当前操作命令。
 *
 * 这样 whole-file、range、element 三种入口不会分裂出三套判定逻辑。
 */
internal fun CaCfirSession.collectReferenceShortenings(
    file: CjFile,
    selection: TextRange,
): CaReferenceShorteningCommand {
    val plan = collectReferenceShorteningPlan(file)
    val operations = plan.operations.filter { operation ->
        operation.expression.textRange.intersects(selection)
    }
    val importsToAdd = operations.mapNotNullTo(linkedSetOf()) { operation ->
        operation.decision.requiredImport
    }
    return CaCfirReferenceShorteningCommandImpl(
        file = file,
        selection = selection,
        operations = operations,
        importsToAdd = importsToAdd,
        token = token,
    )
}

/**
 * 结合当前文件引用和缩短规划结果，生成导入保留、去重、删除与补齐方案。
 */
internal fun CaCfirSession.collectImportOptimizationPlan(file: CjFile): CaImportOptimizationPlan {
    return getOrCreateImportOptimizationPlan(file) {
        val currentImports = file.importDirectivesItem
        // 导入优化只应统计“实际代码体中的引用名”，不能把 import 自身的短名误判为已使用。
        val referencedNames = PsiTreeUtil.collectElementsOfType(file, CjSimpleNameExpression::class.java)
            .filter { reference -> reference.getStrictParentOfType<CjImportDirective>() == null }
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
        is CjCallExpression -> with(this) {
            selector.referenceExpression?.resolveToSymbol()
                ?: selector.resolveToCall()?.target
                ?: expression.resolveToCall()?.target
        }
        is CjReferenceExpression -> with(this) { selector.resolveToSymbol() }
        else -> null
    }
}

/**
 * 引用缩短计划当前只建模“可以被独立短名替代”的公开语义：
 * 1. class-like 符号可以直接缩短到短类名；
 * 2. 顶层 callable 可以缩短到短函数名，并按需补 import；
 * 3. 成员 callable、包前缀等仍依赖接收者或层级结构，不进入本轮规划模型。
 */
private fun CaSymbol.canBeShortenedAsStandaloneReference(): Boolean = when (this) {
    is CaClassLikeSymbol -> true
    is CaCallableSymbol -> callableId?.classId == null
    else -> false
}

private fun CaCfirSession.isDirectlyReachable(symbol: CaSymbol, file: CjFile): Boolean {
    val shortName = symbol.shortNameOrNull() ?: return false
    val fileDeclaredScope = scopeQueries.queryFileDeclaredScope(file)
    val packageScope = scopeQueries.queryPackageScope(file.packageFqName)
    val visibleSymbols = buildList {
        fileDeclaredScope.processClassifiersByName(shortName) { add(it) }
        fileDeclaredScope.processCallablesByName(shortName) { add(it) }
        packageScope?.processClassifiersByName(shortName) { add(it) }
        packageScope?.processCallablesByName(shortName) { add(it) }
    }
    return visibleSymbols
        .map(::getPublicSymbol)
        .any { visibleSymbol -> with(this) { visibleSymbol.isEquivalentTo(symbol) } }
}

private fun CaSymbol.shortNameOrNull(): Name? = when (this) {
    is CaClassLikeSymbol -> classId?.shortClassName
    is CaCallableSymbol -> callableId?.callableName
    is CaPackageSymbol -> name
    else -> name
}

private fun CaSymbol.asTopLevelImportPath(): ImportPath? = when (this) {
    is CaClassLikeSymbol -> classId?.let { ImportPath(it.asSingleFqName(), false) }
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
