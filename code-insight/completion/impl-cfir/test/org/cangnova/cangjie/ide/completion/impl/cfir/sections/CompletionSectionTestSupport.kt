package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.ide.completion.api.CompletionKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.impl.shared.CompletionResultSink
import org.cangnova.cangjie.ide.completion.impl.shared.CompletionSection
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression

/**
 * 通过区段的**公开执行入口**收集候选。
 *
 * 走 `execute` 而不是内部计算方法：执行入口才是区段的真实契约，
 * 同时验证「区段只推自己批次、收尾由 runner 负责」的分工。
 */
fun CompletionSection.collectCandidates(request: CompletionRequest): List<CompletionSnapshot> {
    val collected = mutableListOf<CompletionSnapshot>()
    val batches = mutableListOf<Boolean>()
    val sink = object : CompletionResultSink {
        /**
         * 收集批次并记录每批的收尾标记。
         */
        override fun consume(batch: List<CompletionSnapshot>, isLast: Boolean) {
            collected += batch
            batches += isLast
        }
    }
    execute(request, sink, NoProgressIndicator)
    require(batches.isNotEmpty()) { "区段必须至少推出一批结果" }
    // 区段擅自声明 isLast 会让多区段流提前截断。
    require(batches.none { it }) { "区段不得自行声明收尾，实际 $batches" }
    return collected
}

/**
 * 取得候选显示名序列。
 */
fun List<CompletionSnapshot>.lookupNames(): List<String> = map { it.lookupString }

/**
 * 在最后一个引用指定名字的位置构造补全请求。
 */
@OptIn(CaExperimentalApi::class)
fun CjFile.requestAtReference(referenceName: String, prefix: String = ""): CompletionRequest {
    val position = PsiTreeUtil.findChildrenOfType(this, CjSimpleNameExpression::class.java)
        .last { it.referencedName == referenceName }
    return TestCompletionRequest(position, prefix)
}

/**
 * 无进度的空指示器：区段只用它做取消检查。
 */
private object NoProgressIndicator : com.intellij.openapi.progress.EmptyProgressIndicatorBase() {
    override fun cancel() = Unit
    override fun isCanceled(): Boolean = false
    override fun start() = Unit
    override fun stop() = Unit
    override fun isRunning(): Boolean = false
}

/**
 * 在声明标识符处构造补全请求。
 *
 * 声明名不是引用表达式，必须经 `PsiNameIdentifierOwner` 定位；
 * 用引用查找会得到空集合，把「声明名位置无候选」误判成位置分类失败。
 */
fun CjFile.requestAtDeclarationName(declarationName: String, prefix: String = ""): CompletionRequest {
    val owner = PsiTreeUtil
        .findChildrenOfType(this, PsiNameIdentifierOwner::class.java)
        .last { it.name == declarationName }
    // 标识符本身是叶子元素，不是仓颉 PSI；用声明本身作为位置，
    // 位置分类按「标识符的父节点」判定，仍会落到声明名形态。
    requireNotNull(owner.nameIdentifier) { "声明 `$declarationName` 缺少标识符" }
    return TestCompletionRequest(owner as CjElement, prefix)
}

/**
 * 测试用补全请求。
 *
 * 直接实现请求接口，避免在测试里依赖平台 `CompletionParameters`：
 * 测试要验证的是区段行为，而不是平台参数翻译。
 */
class TestCompletionRequest(
    private val position: CjElement,
    override val prefix: String,
    override val kind: CompletionKind = CompletionKind.BASIC,
    override val invocationCount: Int = 1,
    override val offset: Int = position.textOffset,
    override val replaceStartOffset: Int = position.textOffset,
    override val replaceEndOffset: Int = position.textOffset + position.textLength,
    /**
     * 测试请求默认没有真实文档版本：区段测试验证候选生成，不验证插入校验；
     * 插入校验由 InsertionValidator 的专项测试覆盖。
     */
    override val documentStamp: Long = 0L,
) : CompletionRequest {
    /** 原始位置与分析位置在测试中相同：dummy 修正由共享入口单独验证。 */
    override val originalPosition: CjElement get() = position

    /** 分析位置。 */
    override val completionPosition: CjElement get() = position
}
