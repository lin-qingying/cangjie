package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateStatus
import org.cangnova.cangjie.analysis.api.components.createUseSiteVisibilityChecker
import org.cangnova.cangjie.analysis.api.components.CaScopeKind
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.cfir.resolve.createFileLookupScopes
import org.cangnova.cangjie.analysis.api.imports.CaImportOptimizationPlan
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningCommand
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningOperation
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningPlan
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.resolution.successfulFunctionCallOrNull
import org.cangnova.cangjie.analysis.api.resolution.symbol
import org.cangnova.cangjie.analysis.api.symbols.*
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
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
    // 短名在**当前位置**的绑定（A1/A2 的位置 scope + shadow）：只查文件 lookup scope 会漏掉
    // 局部遮蔽与成员层，也就分不出「够不着」和「被别的声明占了」。
    val shortNameBinding =
        if (visible) resolveShortNameBinding(symbol, file, position) else ShortNameBinding.UNBOUND
    // 五种状态各按自己的理由给出，**不把「不适用」与「未知」并进 HIDDEN**（设计 §6.6）：
    // 权限/遮蔽是「确定看不见」，文件符号是「不是一类东西」，最后一种情况是「判定不出来」。
    // 后两者一旦并进 HIDDEN，消费者就再也分不出「该移除」与「不知道」。
    val status = when {
        !visible -> CaCompletionCandidateStatus.HIDDEN
        symbol is CaFileSymbol -> CaCompletionCandidateStatus.NOT_APPLICABLE
        symbol is CaPackageSymbol -> CaCompletionCandidateStatus.DIRECT
        shortNameBinding == ShortNameBinding.EQUIVALENT -> CaCompletionCandidateStatus.DIRECT
        // 短名被位置作用域里**别的**声明占了（局部变量、类型参数、成员……）：
        // 加 import 也救不回来——import 不参与遮蔽，写下的短名照样绑到那个声明上。
        // 因此这是「确定不可用」，与权限并列进 HIDDEN，而不是 REQUIRES_IMPORT。
        shortNameBinding == ShortNameBinding.SHADOWED -> CaCompletionCandidateStatus.HIDDEN
        importPath != null -> CaCompletionCandidateStatus.REQUIRES_IMPORT
        else -> CaCompletionCandidateStatus.UNKNOWN
    }
    return CaCfirCompletionCandidateDecisionImpl(
        symbol = symbol,
        status = status,
        requiredImport = importPath?.takeIf { status == CaCompletionCandidateStatus.REQUIRES_IMPORT },
        token = token,
    )
}

/**
 * 候选短名在当前位置作用域链上的绑定结果。
 */
private enum class ShortNameBinding {
    /** 作用域链上没有任何同名声明——短名可用性取决于是否需要 import。 */
    UNBOUND,

    /** 同名声明里包含本候选（同一层的合法重载/同名族都算命中）。 */
    EQUIVALENT,

    /** 同名声明被**别的**声明占了：局部遮蔽、类型参数、成员等。 */
    SHADOWED,
}

/**
 * 在 [position] 的作用域链上解析 [symbol] 短名的绑定。
 *
 * **位置层**（词法、类型/成员、片段顶层）只能从位置上下文取：这些层按真实查找次序
 * 从内到外排列，**首次绑定即结论**——内层先绑定就代表外层同名声明已被遮蔽。
 * 这与 `CfirLocalScopeCompletionSection` 的「首个同名者胜」同源，也是解析塔的真实次序——
 * 官方编译器实测（cjc 1.1.3）：`let Foo = 3` 之后连类型位置写 `Foo` 都报
 * `'Foo' is not a type`，即仓颉的类型名与值名共享一套遮蔽，不存在「值遮蔽不影响类型位置」
 * 的例外，所以这里不必按符号种类分情况。
 *
 * **文件层**（本文件顶层、包、显式/默认导入、内建）改走 resolver 共用的
 * `CfirFileLookupScopes`：位置上下文的公开投影只包装 names-aware 作用域，而导入作用域
 * （`CfirImportScope`）与内建原始类型作用域不是，于是投影里**没有这两层**——
 * 早先只查文件查找布局的实现之所以能看见 `import sample.*` 带来的名字，正是因为它是
 * 直接在 CFIR 作用域上查询的。这里保持同一通道，行为不因投影缺口而改变。
 *
 * 片段是例外：宿主文件顶层声明那一层**只存在于位置上下文**里，因此片段位置一并消费
 * 位置上下文的文件级带（导入层仍由上面的 CFIR 通道补齐）。
 *
 * 只按名字查询（classifiers / callables / typeParameters），不枚举作用域全部声明：
 * 成员作用域与包作用域的全量枚举代价随被查类型/包规模增长，而这些名字查询走的是
 * 各 scope 的按名通道。
 */
private fun CaCfirSession.resolveShortNameBinding(
    symbol: CaSymbol,
    file: CjFile,
    position: CjElement,
): ShortNameBinding {
    val shortName = symbol.shortNameForBinding() ?: return ShortNameBinding.UNBOUND

    val isCodeFragment = file is CjCodeFragment
    val positionEntries = with(this) { file.scopeContext(position) }.scopes
    for (entry in positionEntries) {
        if (entry.kind.indexInTower >= CaScopeKind.FILE_SCOPE.indexInTower && !isCodeFragment) continue
        val scope = entry.scope
        val bound = buildList<CaSymbol> {
            scope.classifiers(shortName).forEach { add(it) }
            scope.callables(shortName).forEach { add(it) }
            scope.typeParameters { it == shortName }.forEach { add(it) }
        }
        if (bound.isEmpty()) continue
        return if (bound.any { candidate -> with(this) { candidate.isEquivalentTo(symbol) } }) {
            ShortNameBinding.EQUIVALENT
        } else {
            ShortNameBinding.SHADOWED
        }
    }

    val currentCfirSession = cfirSession
    val cfirFile = file.getOrBuildCfirFile(resolutionFacade)
    val fileScopes = currentCfirSession.createFileLookupScopes(
        file = cfirFile,
        scopeSession = getScopeSessionFor(currentCfirSession),
    ).typeResolutionScopes
    for (scope in fileScopes) {
        val bound = buildList<CaSymbol> {
            scope.processClassifiersByName(shortName) { classifier ->
                add(cfirSymbolBuilder.buildSymbol(classifier))
            }
            scope.processCallablesByName(shortName) { callable ->
                add(cfirSymbolBuilder.buildSymbol(callable))
            }
        }
        if (bound.isEmpty()) continue
        return if (bound.any { candidate -> with(this) { candidate.isEquivalentTo(symbol) } }) {
            ShortNameBinding.EQUIVALENT
        } else {
            ShortNameBinding.SHADOWED
        }
    }
    return ShortNameBinding.UNBOUND
}

/**
 * 收集文件内可进行缩短的限定引用。
 */
internal fun CaCfirSession.collectReferenceShorteningPlan(file: CjFile): CaReferenceShorteningPlan {
    val operations = PsiTreeUtil.collectElementsOfType(file, CjDotQualifiedExpression::class.java)
        .filter { it.getStrictParentOfType<CjImportDirective>() == null }
        .mapNotNull { expression ->
            val target = resolveShorteningTarget(expression) ?: return@mapNotNull null
            if (!target.canBeShortenedAsStandaloneReference()) return@mapNotNull null
            val shortName = target.shortNameOrNull() ?: return@mapNotNull null
            if (expression.text == shortName.asString()) return@mapNotNull null
            val decision = checkCompletionCandidate(target, expression)
            // 缩短必须写下一个**一定能解析**的短名，因此这里要的是「确定可用」，
            // 不是 shouldHide 的反面：未知同样不能缩短（它与 HIDDEN 一样会被这一步挡掉，
            // 但理由不同——见两个属性的 KDoc）。
            if (!decision.status.isDeterminedUsable) return@mapNotNull null
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
 * 提取符号可参与补全、导入和缩短判定的短名。
 */
private fun CaSymbol.shortNameOrNull(): Name? = when (this) {
    is CaClassLikeSymbol -> classId?.shortClassName
    is CaCallableSymbol -> callableId?.callableName
    is CaPackageSymbol -> name
    else -> name
}

/**
 * 短名**绑定查询**用的名字。
 *
 * 与 [shortNameOrNull] 的差别只有局部声明这一类：局部变量/局部函数是 callable，但**没有**
 * callableId（不属于任何包/类），只按 callableId 取名字会把它们判成「没有短名」，
 * 于是位置作用域里的遮蔽与命中统统看不见——而这正是 A1/A2 要覆盖的那一层。
 */
private fun CaSymbol.shortNameForBinding(): Name? = when (this) {
    is CaClassLikeSymbol -> classId?.shortClassName
    is CaCallableSymbol -> callableId?.callableName ?: (this as? CaNamedSymbol)?.name
    else -> shortNameOrNull()
}

/**
 * 将可独立导入的顶层符号转换为精确导入路径。
 */
private fun CaSymbol.asTopLevelImportPath(): ImportPath? {
    val organizationName = (this as? CaCfirSymbol<*>)?.cfirSymbol
        ?.getContainingFile()?.packageDirective?.organizationName
    return when (this) {
        is CaClassLikeSymbol -> classId?.let { ImportPath(it.asSingleFqName(), false, organizationName = organizationName) }
        is CaCallableSymbol -> callableId
            ?.takeIf { it.classId == null }
            ?.let { ImportPath(it.asSingleFqName(), false, organizationName = organizationName) }
        else -> null
    }
}

/**
 * 从 PSI 导入节点恢复统一的 [ImportPath] 表示。
 */
private fun CjImportInfo.importPathOrNull(): ImportPath? {
    return when (this) {
        is org.cangnova.cangjie.psi.CjImportItem -> importPath
        else -> importedFqName?.let { ImportPath(it, isAllUnder, aliasName?.let(Name.Companion::identifier), organizationName) }
    }
}
