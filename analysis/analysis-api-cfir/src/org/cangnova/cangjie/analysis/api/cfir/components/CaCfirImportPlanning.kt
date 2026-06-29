package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateStatus
import org.cangnova.cangjie.analysis.api.components.createUseSiteVisibilityChecker
import org.cangnova.cangjie.analysis.api.imports.CaImportOptimizationPlan
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningCommand
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningOperation
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningPlan
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.resolution.successfulFunctionCallOrNull
import org.cangnova.cangjie.analysis.api.resolution.symbol
import org.cangnova.cangjie.analysis.api.symbols.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType

/**
 * CFIR 补全候选决策实现。
 *
 * 它描述某个符号在指定位置是否可直接使用、是否需要导入，或者应被隐藏。
 */
internal class CaCfirCompletionCandidateDecisionImpl(
    /**
     * 当前被判定的公开 Analysis API 符号。
     */
    override val symbol: CaSymbol,
    /**
     * 符号在 use-site 的补全可用状态。
     */
    override val status: CaCompletionCandidateStatus,
    /**
     * 当符号需要新增导入后才能以短名使用时，对应的导入路径。
     */
    override val requiredImport: ImportPath?,
    /**
     * 约束本次补全判定结果生命周期的会话 token。
     */
    override val token: CaLifetimeToken,
) : CaCompletionCandidateDecision

/**
 * 引用缩短操作实现。
 */
internal class CaCfirReferenceShorteningOperationImpl(
    /**
     * 可以被短名替换的限定表达式。
     */
    override val expression: CjExpression,
    /**
     * 限定表达式当前解析到的目标符号。
     */
    override val target: CaSymbol,
    /**
     * 替换限定表达式时使用的短名。
     */
    override val shortName: Name,
    /**
     * 目标符号在当前位置的补全与导入判定。
     */
    override val decision: CaCompletionCandidateDecision,
    /**
     * 约束缩短操作对象生命周期的会话 token。
     */
    override val token: CaLifetimeToken,
) : CaReferenceShorteningOperation

/**
 * 引用缩短规划实现。
 */
internal class CaCfirReferenceShorteningPlanImpl(
    /**
     * 被分析的仓颉源文件。
     */
    override val file: CjFile,
    /**
     * 文件内所有可安全执行的引用缩短操作。
     */
    override val operations: List<CaReferenceShorteningOperation>,
    /**
     * 约束缩短规划生命周期的会话 token。
     */
    override val token: CaLifetimeToken,
) : CaReferenceShorteningPlan

/**
 * 指定选择范围的缩短命令实现。
 */
internal class CaCfirReferenceShorteningCommandImpl(
    /**
     * 命令作用的仓颉源文件。
     */
    override val file: CjFile,
    /**
     * 调用方指定的文本选择范围。
     */
    override val selection: TextRange,
    /**
     * 与选择范围相交、应由本命令执行的缩短操作。
     */
    override val operations: List<CaReferenceShorteningOperation>,
    /**
     * 执行缩短后为保持短名可见而需要补充的导入集合。
     */
    override val importsToAdd: Set<ImportPath>,
    /**
     * 约束缩短命令生命周期的会话 token。
     */
    override val token: CaLifetimeToken,
) : CaReferenceShorteningCommand

/**
 * 导入优化规划实现。
 */
internal class CaCfirImportOptimizationPlanImpl(
    /**
     * 被优化导入列表的仓颉源文件。
     */
    override val file: CjFile,
    /**
     * 优化后应继续保留的现有导入项。
     */
    override val retainedImports: List<CjImportInfo>,
    /**
     * 与前序导入路径重复、可删除的导入项。
     */
    override val duplicateImports: List<CjImportInfo>,
    /**
     * 在当前文件引用集合中未被使用、可删除的导入项。
     */
    override val unusedImports: List<CjImportInfo>,
    /**
     * 引用缩短或补全判定发现的缺失导入路径。
     */
    override val missingImports: List<ImportPath>,
    /**
     * 约束导入优化规划生命周期的会话 token。
     */
    override val token: CaLifetimeToken,
) : CaImportOptimizationPlan

/**
 * 在指定位置判定补全候选是否可直接使用、是否需要导入或应被隐藏。
 */
internal fun CaCfirSession.checkCompletionCandidate(
    symbol: CaSymbol,
    position: CjElement,
): CaCompletionCandidateDecision {
    val file = position.containingFile as? CjFile
        ?: error("补全候选判定只能在 CjFile 内执行：${position::class.simpleName}")
    val importPath = symbol.asTopLevelImportPath()
    val visible = when (symbol) {
        is CaDeclarationSymbol -> with(this) {
            createUseSiteVisibilityChecker(
                useSiteFile = file.symbol,
                position = position,
            ).isVisible(symbol)
        }
        is CaPackageSymbol -> true
        else -> false
    }
    val status = when {
        !visible -> CaCompletionCandidateStatus.HIDDEN
        symbol is CaFileSymbol -> CaCompletionCandidateStatus.HIDDEN
        symbol is CaPackageSymbol -> CaCompletionCandidateStatus.DIRECT
        isDirectlyReachable(symbol, file) -> CaCompletionCandidateStatus.DIRECT
        importPath != null -> CaCompletionCandidateStatus.REQUIRES_IMPORT
        else -> CaCompletionCandidateStatus.HIDDEN
    }
    return CaCfirCompletionCandidateDecisionImpl(
        symbol = symbol,
        status = status,
        requiredImport = importPath?.takeIf { status == CaCompletionCandidateStatus.REQUIRES_IMPORT },
        token = token,
    )
}

/**
 * 收集文件内可进行缩短的限定引用。
 */
internal fun CaCfirSession.collectReferenceShorteningPlan(file: CjFile): CaReferenceShorteningPlan {
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
    return CaCfirReferenceShorteningPlanImpl(
        file = file,
        operations = operations,
        token = token,
    )
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
    val currentImports = file.importDirectivesItem
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
    return CaCfirImportOptimizationPlanImpl(
        file = file,
        retainedImports = currentImports - duplicateImports - unusedImports.toSet(),
        duplicateImports = duplicateImports.toList(),
        unusedImports = unusedImports,
        missingImports = missingImports,
        token = token,
    )
}

/**
 * 解析限定表达式在缩短时真正要保留的目标符号。
 */
private fun CaCfirSession.resolveShorteningTarget(expression: CjDotQualifiedExpression): CaSymbol? {
    val selector = expression.selectorExpression
    return when (selector) {
        is CjCallExpression -> with(this) {
            selector.referenceExpression?.resolveToSymbol()
                ?: selector.resolveToCall()?.successfulFunctionCallOrNull()?.symbol
                ?: expression.resolveToCall()?.successfulFunctionCallOrNull()?.symbol
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

/**
 * 判断符号是否已经可通过文件作用域或当前包作用域以短名直接访问。
 */
private fun CaCfirSession.isDirectlyReachable(symbol: CaSymbol, file: CjFile): Boolean {
    val shortName = symbol.shortNameOrNull() ?: return false
    val visibleSymbols = buildList<CaSymbol> {
        with(this@isDirectlyReachable) {
            file.getFileScope().classifiers(shortName).forEach(::add)
            file.getFileScope().callables(shortName).forEach(::add)
        }
        getPackageScope(file.packageFqName)?.let { packageScope ->
            packageScope.classifiers(shortName).forEach(::add)
            packageScope.callables(shortName).forEach(::add)
        }
    }
    return visibleSymbols
        .any { visibleSymbol -> with(this) { visibleSymbol.isEquivalentTo(symbol) } }
}

/**
 * 提取符号可参与补全、导入和缩短判定的短名。
 */
private fun CaSymbol.shortNameOrNull(): Name? = when (this) {
    is CaClassLikeSymbol -> classId?.shortClassName
    is CaCallableSymbol -> callableId?.callableName
    is CaPackageSymbol -> name
    else -> name
}

/**
 * 将可独立导入的顶层符号转换为精确导入路径。
 */
private fun CaSymbol.asTopLevelImportPath(): ImportPath? = when (this) {
    is CaClassLikeSymbol -> classId?.let { ImportPath(it.asSingleFqName(), false) }
    is CaCallableSymbol -> callableId
        ?.takeIf { it.classId == null }
        ?.let { ImportPath(it.asSingleFqName(), false) }
    else -> null
}

/**
 * 从 PSI 导入节点恢复统一的 [ImportPath] 表示。
 */
private fun CjImportInfo.importPathOrNull(): ImportPath? {
    return when (this) {
        is org.cangnova.cangjie.psi.CjImportItem -> importPath
        else -> importedFqName?.let { ImportPath(it, isAllUnder, aliasName?.let(Name.Companion::identifier)) }
    }
}
