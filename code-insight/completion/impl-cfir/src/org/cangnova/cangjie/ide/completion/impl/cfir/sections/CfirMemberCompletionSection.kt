package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateStatus
import org.cangnova.cangjie.analysis.api.completion.CaExtensionApplicabilityResult
import org.cangnova.cangjie.analysis.api.components.CaTypeAccessKind
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.types.CaErrorType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportPlan
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportTargetData
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.ImportResolution
import org.cangnova.cangjie.ide.completion.impl.cfir.CfirCompletionSection
import org.cangnova.cangjie.ide.completion.impl.shared.context.positionContext
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.psi.CjSuperExpression
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.analysis.api.components.type
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression

/**
 * 显式接收者成员候选。
 *
 * 候选来自 Analysis 的类型作用域与 extend 提供者：
 * 成员集合、继承链与 extend 注入都由编译器路径决定，
 * 在此按 PSI 遍历类体会漏掉继承成员与 extend 成员，也会把不可见成员混进来。
 *
 * 使用点形态（`Type.member` 还是 `receiver.member`）由 Analysis 判定，不靠语法猜测：
 * 仓颉 PSI 里两种写法都是 `receiver.selector`，语法上无法区分；
 * 靠首字母大写之类的启发式会在小写命名的类型或大写命名的变量上给出错误候选集合。
 *
 * 可见性由 use-site 可见性检查器判定，extend 是否适用由 extend 适用性检查判定；
 * 两者都返回「未知」时不据此过滤候选，避免把本可判定的成员一起丢掉。
 */
@OptIn(CaExperimentalApi::class)
class CfirMemberCompletionSection : CfirCompletionSection(
    id = ID,
    executionPriority = PRIORITY,
) {
    /**
     * 计算当前位置的接收者成员候选。
     */
    override fun compute(request: CompletionRequest): List<CompletionSnapshot> {
        val position = request.completionPosition
        val context = request.positionContext
        if (context.kind != CompletionPositionKind.MEMBER_ACCESS) return emptyList()
        val receiver = context.receiverExpression as? CjElement ?: return emptyList()
        val prefix = request.prefix
        val file = position.containingCjFile ?: return emptyList()

        return analyze(file) {
            // `super` 必须**先判**：它不是普通接收者，`expressionType` 会给出错误类型
            // 而不是 null，判在后面前面的分支就把结果占住了（与未导入 receiver 那次同一形态）。
            val resolved = resolveSuperTarget(receiver, position) ?: resolveAccessTarget(receiver)
            // 触发条件不能只判 null：未解析引用的 `expressionType` 返回的是**错误类型**而不是 null，
            // 只判 null 会让恢复路径永远不触发（成员候选恒为空，且不报任何错）。
            val recovered = if (resolved == null || resolved.type is CaErrorType) {
                recoverUnimportedReceiver(receiver)
            } else {
                null
            }
            val target = recovered?.target ?: resolved ?: return@analyze emptyList()
            // 未导入 receiver 的成员要能插出去，必须同时把**接收者类型**导入进来：
            // 只插入 `Type.member` 而 `Type` 仍不可解析，得到的是一段编译不过的代码。
            val receiverImport = recovered?.importResolution ?: ImportResolution.NoImport
            val receiverType = target.type

            // 先收集适用且可见的 extend 成员，再枚举成员通道。
            //
            // 顺序不能反：use-site 成员作用域**已经**注入了 extend 成员，
            // 先枚举成员通道会把它们标成 MEMBER，之后 extend 通道的同名候选被去重丢掉，
            // 结果是「extend 成员出现了但类别标错」。先建立 extend 身份集合才能正确归类。
            val extendMembers = applicableExtendMembers(receiverType, receiver, file, prefix)

            buildList {
                for (query in receiverType.memberScopeQueries(target.accessKind)) {
                    val memberScope = query.scope ?: continue
                    for (symbol in memberScope.callables { true }) {
                        if (!symbol.matchesPrefix(prefix)) continue
                        if (!isVisibleAt(symbol, file, receiver)) continue
                        val fromExtend = callableIdentityKey(symbol) in extendMembers
                        add(
                            callableSnapshot(
                                symbol,
                                kindFor(query.accessedThroughTypeName, fromExtend),
                                if (fromExtend) CompletionOrigin.EXTEND else CompletionOrigin.MEMBER,
                                documentStamp = request.documentStamp,
                                importResolution = receiverImport,
                            ),
                        )
                    }
                }
                // extend 成员若尚未被成员通道覆盖（例如成员通道受静态限定），
                // 在此补齐：适用且可见的 extend 成员一个都不能少。
                for (symbol in extendMembers.values) {
                    if (!symbol.matchesPrefix(prefix)) continue
                    if (!isVisibleAt(symbol, file, receiver)) continue
                    if (none { it.identity.deduplicationKey == callableIdentityKey(symbol) }) {
                        add(
                            callableSnapshot(
                                symbol,
                                CompletionItemKind.EXTEND_MEMBER,
                                CompletionOrigin.EXTEND,
                                documentStamp = request.documentStamp,
                                importResolution = receiverImport,
                            ),
                        )
                    }
                }
            }.distinctByIdentity()
        }
    }

    /**
     * 解析 `super.` 的接收者类型；接收者不是 `super` 时返回 `null`。
     *
     * 仓颉的 `super` 只指向**直接父类**，因此只取超类型列表里的第一个类型引用：
     * 接口不参与 super 成员查找，把列表里其余条目混进来会让用户看到父类上根本没有的成员。
     *
     * 判据是 PSI 类型（`CjSuperExpression`）而不是文本 `"super"`：
     * 仓颉允许 `` `super` `` 作为转义标识符，按文本判会把一个普通变量当成 super。
     */
    private fun CaSession.resolveSuperTarget(receiver: CjElement, position: CjElement): AccessTarget? {
        if (receiver !is CjSuperExpression) return null
        val owner = PsiTreeUtil.getParentOfType(position, CjTypeStatement::class.java, false) ?: return null
        val superTypeReference = owner.superTypeListEntries.firstNotNullOfOrNull { it.typeReference } ?: return null
        return AccessTarget(superTypeReference.type, CaTypeAccessKind.INSTANCE_THROUGH_RECEIVER)
    }

    /**
     * 收集对给定接收者**适用且可见**的 extend 成员。
     *
     * 返回「候选去重键 → 符号」的映射：去重键用于与成员通道对齐身份，
     * 符号本身用于在成员通道未覆盖时补齐候选。
     *
     * extend 候选按**接收者类型**查询而不是降级成 `ClassId`：基本类型在公开
     * 模型里不是 `CaClassLikeType`（`CaPrimitiveType` 刻意不暴露 `classId`），
     * `CPointer` / `CString` 甚至没有 ClassId——按 ClassId 查询会把这两类接收者
     * 的 extend 通道整体判空。`getExtendSymbols(receiverType)` 内部镜像编译器
     * extend 索引的目标键语义，保证补全候选与调用解析同源。
     */
    private fun CaSession.applicableExtendMembers(
        receiverType: CaType,
        useSite: CjElement,
        file: CjFile,
        prefix: String,
    ): Map<String, CaCallableSymbol> {
        val result = mutableMapOf<String, CaCallableSymbol>()
        for (extend in getExtendSymbols(receiverType)) {
            if (!appliesTo(extend, receiverType, useSite)) continue
            for (symbol in extend.declaredMemberScope.callables { true }) {
                if (!symbol.matchesPrefix(prefix)) continue
                if (!isVisibleAt(symbol, file, useSite)) continue
                result.putIfAbsent(callableIdentityKey(symbol), symbol)
            }
        }
        return result
    }

    /**
     * 成员访问的使用点目标。
     *
     * @property type 接收者类型：类型限定时是分类器的默认类型，实例访问时是表达式类型。
     * @property accessKind 访问形态。
     */
    private data class AccessTarget(val type: CaType, val accessKind: CaTypeAccessKind)

    /**
     * 判定接收者表示类型限定还是实例。
     *
     * 只有「解析结果恰好是唯一分类器、且没有任何变量绑定」时才判为类型限定：
     * 同时存在变量绑定说明这个名字在该位置是值而不是类型名，按实例访问处理才正确。
     */
    private fun CaSession.resolveAccessTarget(receiver: CjElement): AccessTarget? {
        val reference = receiver as? CjReferenceExpression
        if (reference != null) {
            val symbols = reference.resolveToSymbols()
            val classifier = symbols.filterIsInstance<CaClassLikeSymbol>().singleOrNull()
            if (classifier != null && symbols.none { it is CaVariableSymbol }) {
                return AccessTarget(classifier.defaultType, CaTypeAccessKind.STATIC_THROUGH_TYPE_NAME)
            }
        }
        val expressionType = (receiver as? CjExpression)?.expressionType ?: return null
        return AccessTarget(expressionType, CaTypeAccessKind.INSTANCE_THROUGH_RECEIVER)
    }

    /**
     * 未导入 receiver 的类型恢复。
     *
     * 使用点写在 `Type.member`，而 `Type` 尚未导入时，接收者解析必然失败，
     * 用户于是拿不到任何成员候选——但类型本身就在依赖模块里，完全可以找到。
     *
     * 三条边界都是刻意的，缺一条就会把「有界的恢复」变成「猜」：
     * 1. **只认裸标识符**：用类型检查表达，不看文本。receiver 是 `CjSimpleNameExpression`
     *    才继续；写成 `a.b.c` 时它就是限定表达式，直接放弃——
     *    任意深度的表达式链搜索不属于本恢复的范围，代价与收益完全不成比例。
     * 2. **只认精确同名**：索引接口按前缀召回（长前缀退化为子串匹配），
     *    这里必须再按「短名完全相等」过滤一次，否则 `Foo` 会把 `FooBar` 也带进来。
     * 3. **必须唯一**：同名类型来自多个包时给不出一个确定的接收者类型，
     *    与其随便挑一个、把用户带到错误的成员集合，不如不给。
     */
    private fun CaSession.recoverUnimportedReceiver(receiver: CjElement): RecoveredReceiver? {
        val simpleName = receiver as? CjSimpleNameExpression ?: return null
        val name = simpleName.referencedName ?: return null
        com.intellij.openapi.progress.ProgressManager.checkCanceled()
        val classifier = getDeclarationsByNamePrefix(name)
            .filterIsInstance<CaClassLikeSymbol>()
            .filter { it.name?.asString() == name }
            .singleOrNull()
            ?: return null
        val decision = classifier.checkCompletionCandidate(receiver)
        val importResolution = when (decision.status) {
            CaCompletionCandidateStatus.DIRECT -> ImportResolution.NoImport
            CaCompletionCandidateStatus.REQUIRES_IMPORT -> ImportResolution(
                CompletionImportPlan.AddImport(
                    CompletionImportTargetData(
                        importPath = checkNotNull(decision.requiredImport) {
                            "REQUIRES_IMPORT 候选必须提供 requiredImport: ${decision.symbol}"
                        },
                        expressionText = name,
                    ),
                ),
            )
            CaCompletionCandidateStatus.HIDDEN,
            CaCompletionCandidateStatus.NOT_APPLICABLE,
            CaCompletionCandidateStatus.UNKNOWN,
            -> return null
        }
        return RecoveredReceiver(
            AccessTarget(classifier.defaultType, CaTypeAccessKind.STATIC_THROUGH_TYPE_NAME),
            importResolution,
        )
    }

    /**
     * 恢复出的接收者：类型与它需要的导入计划。
     */
    private data class RecoveredReceiver(
        val target: AccessTarget,
        val importResolution: ImportResolution,
    )

    /**
     * 判断候选是否在当前位置可见。
     *
     * 可见性未知时保留候选：过滤责任在可见性组件，
     * 这里不代替它做「不可见即丢弃」的判断。
     */
    private fun CaSession.isVisibleAt(symbol: CaCallableSymbol, file: CjFile, useSite: CjElement): Boolean {
        val fileSymbol: CaFileSymbol = file.symbol
        val checker = createUseSiteVisibilityChecker(fileSymbol, null, useSite)
        return checker.isVisible(symbol)
    }

    /**
     * 判断 extend 是否对给定接收者适用。
     *
     * `Unknown` 视为可能适用：把未判定的 extend 一起丢掉会让补全缺失合法候选。
     */
    private fun CaSession.appliesTo(
        extend: CaExtendSymbol,
        receiverType: CaType,
        useSite: CjElement,
    ): Boolean = when (val result = checkExtensionApplicability(extend, receiverType, useSite)) {
        is CaExtensionApplicabilityResult.ReceiverMismatch -> false
        is CaExtensionApplicabilityResult.ConstraintUnsatisfied -> false
        else -> true
    }

    /**
     * 按通道区分成员类别。
     *
     * 静态成员与实例成员在展示与插入上不同：静态成员通过类型名限定访问，
     * 混为一类会让插入后得到不可解析的调用。
     */
    private fun kindFor(accessedThroughTypeName: Boolean, fromExtend: Boolean): CompletionItemKind = when {
        fromExtend -> CompletionItemKind.EXTEND_MEMBER
        accessedThroughTypeName -> CompletionItemKind.STATIC_MEMBER
        else -> CompletionItemKind.MEMBER
    }

    companion object {
        /**
         * 区段标识。
         */
        const val ID: String = "cfir.member"

        /**
         * 执行优先级：成员候选慢于词法候选，但快于需要索引的顶层候选。
         */
        const val PRIORITY: Int = 20
    }
}
