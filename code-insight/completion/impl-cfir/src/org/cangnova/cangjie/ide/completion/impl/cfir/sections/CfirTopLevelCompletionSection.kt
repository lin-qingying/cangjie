package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.symbol
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.components.CaScopeKind
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.ImportCost
import org.cangnova.cangjie.ide.completion.impl.cfir.CfirCompletionSection
import org.cangnova.cangjie.ide.completion.impl.shared.context.positionContext
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind

/**
 * 顶层声明、类型与包候选。
 *
 * 候选来自 Analysis 的**导入作用域上下文**：它已经按编译器顺序组合了本文件顶层声明、
 * 同包成员、显式导入（含别名）与语言默认导入。按 PSI 声明列表自行拼候选会丢掉
 * 导入别名与来源优先级，也无法区分「已导入」与「需要导入」。
 *
 * 导入来源决定排序成本：已在本文件作用域可见的候选排在需要追加 import 的候选之前，
 * 这一区分由 `importCost` 表达，而不是由两个区段各自硬编码权重。
 *
 * 成员访问位置不使用本区段：那里的候选必须来自接收者类型。
 */
@OptIn(CaExperimentalApi::class)
class CfirTopLevelCompletionSection : CfirCompletionSection(
    id = ID,
    executionPriority = PRIORITY,
) {
    /**
     * 计算当前位置的顶层与导入候选。
     */
    override fun compute(request: CompletionRequest): List<CompletionSnapshot> {
        val position = request.completionPosition
        val context = request.positionContext.kind
        // 成员访问只补接收者成员；import 位置由导入区段负责。
        if (context == CompletionPositionKind.MEMBER_ACCESS) return emptyList()

        val file = position.containingCjFile ?: return emptyList()
        val prefix = request.prefix
        return analyze(file) {
            val importing = file.importingScopeContext
            buildList {
                for (entry in importing.scopes) {
                    // 词法层由局部区段负责；这里只取文件、包与导入来源。
                    if (entry.kind == CaScopeKind.LOCAL_SCOPE) continue
                    // 文件与包作用域的声明在当前文件已直接可达；导入层同样无需再写 import。
                    val origin = originFor(entry.kind)
                    for (classifier in entry.scope.classifiers { true }) {
                        for (snapshot in classifierCandidates(classifier, request, origin, context)) {
                            if (prefix.isEmpty() || snapshot.lookupString.startsWith(prefix)) add(snapshot)
                        }
                    }
                    for (callable in entry.scope.callables { true }) {
                        if (!callable.matchesPrefix(prefix)) continue
                        add(
                            callableSnapshot(
                                callable,
                                CompletionItemKind.TOP_LEVEL,
                                origin,
                                ImportCost.DIRECT,
                                documentStamp = request.documentStamp,
                            ),
                        )
                    }
                }
            }.distinctByIdentity()
        }
    }

    /**
     * 枚举一个类的全部构造器。
     *
     * **不能走 `declaredMemberScope.callables`**：那个通道里没有构造器（实测返回空），
     * 结果是调用位置上永远给不出构造器候选，且不报任何错。
     *
     * 这里走 PSI → 符号的**标准恢复路径**（`CjConstructor.symbol`）：
     * 类头的 `class Widget(alpha: Int64)` 是 primary constructor，
     * 类体里的 `init(...)` 是 secondary，两者都要收；两者都没有时
     * `hasPrimaryConstructor()` 仍为真，`primaryConstructor` 给出无参形态。
     */
    private fun CaSession.constructorsOf(classLike: CaClassLikeSymbol): List<CaConstructorSymbol> {
        val statement = classLike.psi as? CjTypeStatement ?: return emptyList()
        return buildList {
            statement.primaryConstructor?.let(::add)
            addAll(statement.secondaryConstructors)
        }.map { it.symbol }
    }

    /**
     * 一个分类器在当前位置上应当产出哪些候选。
     *
     * 位置决定形态：**调用位置**（表达式、函数体、代码片段）上，用户写的是「用这个类型造一个值」，
     * 因此给**构造器候选**——带括号、尾文本显示实参组合；**类型位置**上写的是类型本身，
     * 给类型候选（跟随左括号会让用户写出 `List(` 这种非法源码）。
     *
     * 一个类有多个构造器时逐个给：它们共享同一个 ClassId，靠尾文本区分。
     * 只给一个会让用户无从选择调用哪一种，而「参数组合」正是这里要传达的信息。
     */
    private fun CaSession.classifierCandidates(
        classifier: CaClassifierSymbol,
        request: CompletionRequest,
        origin: CompletionOrigin,
        positionKind: CompletionPositionKind,
    ): List<CompletionSnapshot> {
        val classLike = classifier as? CaClassLikeSymbol
        if (classLike != null && positionKind in CONSTRUCTIBLE_POSITIONS) {
            val constructors = constructorsOf(classLike)
            if (constructors.isNotEmpty()) {
                return constructors.map { constructor ->
                    classifierSnapshot(
                        classifier,
                        CompletionItemKind.TOP_LEVEL,
                        origin,
                        ImportCost.DIRECT,
                        documentStamp = request.documentStamp,
                        constructor = constructor,
                    )
                }
            }
        }
        return listOf(
            classifierSnapshot(
                classifier,
                CompletionItemKind.TOP_LEVEL,
                origin,
                ImportCost.DIRECT,
                documentStamp = request.documentStamp,
            ),
        )
    }

    /**
     * 把作用域来源映射为候选来源。
     *
     * 包成员与导入来源分开：前者是「同包直接可达」，后者是「靠 import 可达」，
     * 二者在需要追加 import 与冲突时的处理并不相同。
     */
    private fun originFor(kind: CaScopeKind): CompletionOrigin = when {
        kind.isImporting -> CompletionOrigin.IMPORT
        kind == CaScopeKind.PACKAGE_MEMBER_SCOPE -> CompletionOrigin.TOP_LEVEL
        else -> CompletionOrigin.TOP_LEVEL
    }

    companion object {
        /**
         * 区段标识。
         */
        const val ID: String = "cfir.topLevel"

        /**
         * 执行优先级：顶层候选需要作用域拼接，慢于词法与成员候选。
         */
        const val PRIORITY: Int = 30

        /**
         * 会产出**构造器候选**的位置。
         *
         * 判据是「用户在这里写的是一个值」：表达式、函数体与代码片段。
         * 类型位置不在其中——那里候选要能被当作类型名使用，跟随左括号会写出非法源码。
         */
        private val CONSTRUCTIBLE_POSITIONS = setOf(
            CompletionPositionKind.EXPRESSION,
            CompletionPositionKind.FUNCTION_BODY,
            CompletionPositionKind.CODE_FRAGMENT_EXPRESSION,
            CompletionPositionKind.CODE_FRAGMENT_BLOCK,
        )
    }
}

/**
 * 判断 callable 名称是否匹配补全前缀。
 */
@OptIn(CaExperimentalApi::class)
internal fun CaCallableSymbol.matchesPrefix(prefix: String): Boolean =
    prefix.isEmpty() || (this as? CaNamedSymbol)?.name?.asString()?.startsWith(prefix) == true

/**
 * 判断分类器名称是否匹配补全前缀。
 */
@OptIn(CaExperimentalApi::class)
internal fun CaClassifierSymbol.matchesClassifierPrefix(prefix: String): Boolean =
    prefix.isEmpty() || (this as? CaNamedSymbol)?.name?.asString()?.startsWith(prefix) == true