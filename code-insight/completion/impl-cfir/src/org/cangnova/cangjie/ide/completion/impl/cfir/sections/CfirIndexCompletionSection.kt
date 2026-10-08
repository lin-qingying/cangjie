@file:OptIn(org.cangnova.cangjie.analysis.api.CaExperimentalApi::class)

package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import com.intellij.openapi.progress.ProgressManager
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportPlan
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportTargetData
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.ImportCost
import org.cangnova.cangjie.ide.completion.api.lookup.ImportResolution
import org.cangnova.cangjie.ide.completion.impl.cfir.CfirCompletionSection
import org.cangnova.cangjie.ide.completion.impl.shared.context.positionContext
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.name.FqName

/**
 * 索引召回区段：补全未导入 / 跨模块的声明。
 *
 * 作用域路径看不到这些名字——导入作用域里根本没有它们，用户不手写包路径就补不出来。
 * 名称发现由 Analysis 完成，分两路：源码声明的名字来自**宿主名称索引**（按前缀），
 * 库侧（`.cjo`）声明的名字来自 CFIR 的只读名字视图。本区段只负责把两路接进同一套候选管线。
 *
 * 三条边界：
 * 1. **不自己解析名称**：名称发现给名字，符号恢复在 Analysis 层，候选与作用域路径同源；
 * 2. **可见性与可达性仍由 Analysis 判定**：索引范围不等于访问权限，
 *    private/protected 与遮蔽必须在使用点过滤；
 * 3. **索引不可用时不退化**：源码模块的候选必须缺席——它们只能靠索引找到，
 *    一旦出现就说明有人遍历了源码文件；禁止全工作区声明扫描兜底。
 *    库侧候选不依赖宿主索引，仍须照常给出：降级是「少给」，不是「不给」。
 */
class CfirIndexCompletionSection : CfirCompletionSection(
    id = ID,
    executionPriority = PRIORITY,
    isDelayed = IS_DELAYED,
) {
    /**
     * 计算索引召回候选。
     */
    override fun compute(request: CompletionRequest): List<CompletionSnapshot> {
        val position = request.completionPosition
        val kind = request.positionContext.kind
        if (kind !in SUPPORTED_POSITIONS) return emptyList()
        val file = position.containingCjFile ?: return emptyList()
        // 重复触发时放宽匹配：用户再次请求补全，通常意味着上一轮前缀太窄。
        val repeated = request.invocationCount > 1

        return analyze(file) {
            val visibility = createUseSiteVisibilityChecker(file.symbol, position = position)
            // 更内层作用域已经绑定的短名会遮蔽索引候选：用户写出那个名字时会解析到内层声明，
            // 此时把跨包同名声明也列出来，选中即得到一个指向别处的名字。
            // 作用域次序由 Analysis 的位置上下文给出，不在这里按 PSI 祖先自行推断。
            val claimed = file.scopeContext(position).scopes
                .flatMap { scopeEntry -> scopeEntry.scope.claimableNames() }
                .toSet()
            buildList {
                for (symbol in getDeclarationsByNamePrefix(request.prefix, repeated)) {
                    ProgressManager.checkCanceled()
                    // 类型位置只收类型：把函数名放进类型槽位，用户选中即编译失败。
                    if (kind == CompletionPositionKind.TYPE && symbol !is CaClassifierSymbol) continue
                    // 可见性按使用点判定：索引范围不是访问权限。
                    if (!visibility.isVisible(symbol)) continue
                    val name = symbol.name?.asString() ?: continue
                    // 同一身份已由内层作用域提供时由去重器合并，这里只挡**同名不同身份**的遮蔽。
                    if (name in claimed) continue
                    val packageFqName = symbol.enclosingPackageFqName() ?: continue
                    // 索引候选按定义就不在导入作用域里——在的话顶层区段已经给过、
                    // 会由身份去重合并掉。因此这里一律给「追加 import」计划，
                    // 已可达的重复候选由 `distinctByIdentity` 保留顶层区段那一份。
                    val importResolution = ImportResolution(
                        CompletionImportPlan.AddImport(
                            CompletionImportTargetData(ImportPath(packageFqName, false), name),
                        ),
                    )
                    add(
                        when (symbol) {
                            is CaClassifierSymbol -> classifierSnapshot(
                                symbol,
                                CompletionItemKind.TOP_LEVEL,
                                CompletionOrigin.TOP_LEVEL,
                                ImportCost.ADD_IMPORT,
                                request.documentStamp,
                                importResolution,
                            )

                            is CaCallableSymbol -> callableSnapshot(
                                symbol,
                                if (symbol is CaEnumConstructorSymbol) CompletionItemKind.ENUM_ENTRY
                                else CompletionItemKind.TOP_LEVEL,
                                CompletionOrigin.TOP_LEVEL,
                                ImportCost.ADD_IMPORT,
                                request.documentStamp,
                                importResolution,
                            )

                            else -> continue
                        },
                    )
                }
            }
        }
    }

    /**
     * 枚举作用域已绑定的短名。
     *
     * 只取名字不取符号：遮蔽判定只需要「这个名字在本层已被占用」，
     * 而是否同一身份由去重器按身份键判断。
     */
    private fun CaScope.claimableNames(): Sequence<String> = sequence {
        classifiers { true }.forEach { classifier ->
            (classifier as? CaNamedSymbol)?.name?.asString()?.let { yield(it) }
        }
        callables { true }.forEach { callable ->
            (callable as? CaNamedSymbol)?.name?.asString()?.let { yield(it) }
        }
    }

    /**
     * 取得声明所属的包名。
     *
     * 索引候选一律是**顶层**声明（或枚举构造器这类顶层类型体内声明），
     * 因此包身份足以写出 import；取不到包名的候选（局部、合成）不是索引召回的目标。
     */
    private fun CaDeclarationSymbol.enclosingPackageFqName(): FqName? = when (this) {
        is CaClassLikeSymbol -> classId?.packageFqName
        is CaCallableSymbol -> callableId?.packageName?.takeIf { !it.isRoot }
        else -> null
    }

    companion object {
        /**
         * 区段标识。
         */
        const val ID: String = "cfir.index"

        /**
         * 执行优先级：索引候选最慢，排在所有作用域候选之后。
         */
        const val PRIORITY: Int = 50

        /**
         * 属于**延迟阶段**：索引要枚举源码倒排表与 `.cjo` 名字视图，是整条管线里最慢的一步。
         * 用户应当先看到作用域候选，索引候选随后补上；继续输入时它应当整段被跳过。
         */
        const val IS_DELAYED: Boolean = true

        /**
         * 使用索引的位置。
         *
         * 成员访问、import、命名实参与文档位置不走索引：前两者的候选来自接收者与导入上下文，
         * 命名实参来自被调用方签名，文档标签来自语言；索引召回在这些位置只会给出无关名字。
         */
        private val SUPPORTED_POSITIONS = setOf(
            CompletionPositionKind.EXPRESSION,
            CompletionPositionKind.FUNCTION_BODY,
            CompletionPositionKind.TYPE,
            CompletionPositionKind.CODE_FRAGMENT_EXPRESSION,
            CompletionPositionKind.CODE_FRAGMENT_BLOCK,
        )
    }
}
