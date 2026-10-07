package org.cangnova.cangjie.ide.completion.impl.shared

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.ide.completion.api.CompletionKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.impl.shared.dummy.CompletionDummyContext
import org.cangnova.cangjie.psi.CjElement

/**
 * 平台补全参数到仓颉补全请求的翻译结果。
 *
 * 该对象只保存 PSI、文本与计数：它是分析期与界面期之间可传递的最小集合。
 */
class PlatformCompletionRequest(
    override val originalPosition: CjElement,
    override val completionPosition: CjElement,
    override val prefix: String,
    override val kind: CompletionKind,
    override val invocationCount: Int,
    override val offset: Int,
    override val replaceStartOffset: Int,
    override val replaceEndOffset: Int,
    override val documentStamp: Long,
) : CompletionRequest {
    /**
     * 供日志与测试使用的稳定描述，不包含文档内容。
     */
    override fun toString(): String =
        "PlatformCompletionRequest(kind=$kind, prefix='$prefix', offset=$offset, " +
            "replace=[$replaceStartOffset,$replaceEndOffset), count=$invocationCount)"
}

/**
 * 从平台补全参数与 dummy 修正上下文构造仓颉补全请求。
 *
 * 位置、前缀与替换区间全部取自修正后的**文件副本**：
 * 平台原始位置仍带 `IntellijIdeaRulezzz`，直接采用会让作用域分析与候选前缀同时失真。
 * `offset` 单独保留编辑器偏移——轻量夹具中它与 PSI 偏移属于不同坐标系，不可混用。
 */
fun completionRequestFrom(
    parameters: CompletionParameters,
    dummy: CompletionDummyContext,
): CompletionRequest {
    val cangjiePosition = dummy.position
    // `originalPosition` 这里与 `completionPosition` 同值（都在 dummy 副本里）。
    //
    // 契约把它描述为「触发本次补全的原始 PSI 位置」并补充「未修正时等于 completionPosition」，
    // 设计 B2 则说「原件/副本」——两处措辞指向两种读法：**用户原件里的位置**，
    // 或**副本里修正前的位置**。目前两者都不满足（这里是修正后的位置），
    // 但**没有任何消费者**：语义复查走 `CompletionRecoveryService` 自己的 use-site 参数，
    // 插入校验用的是 `documentStamp`（那个确实取的是原件）。
    //
    // 因此不猜着改：等第一个真正的消费者出现时，按它需要的语义（大概率是「用户原件里的位置」，
    // 用于把候选写回文档）再定，并同时补上「两者必须可区分」的用例。
    // 在此之前，这里如实反映现状，不用一个看起来更对的值掩盖歧义。
    return PlatformCompletionRequest(
        originalPosition = cangjiePosition,
        completionPosition = cangjiePosition,
        prefix = dummy.prefix,
        kind = completionKindOf(parameters.completionType),
        invocationCount = invocationCountOf(cangjiePosition),
        offset = parameters.offset,
        replaceStartOffset = cangjiePosition.textOffset,
        replaceEndOffset = cangjiePosition.textOffset + cangjiePosition.textLength,
        // 版本戳取**原始文件**：分析副本上的 dummy 修正与用户文档无关，
        // 插入发生在原始文档上，只有原始文档的戳能反映「用户是否在弹窗期间改过文本」。
        documentStamp = parameters.originalFile.viewProvider.document?.modificationStamp ?: 0L,
    )
}

/** 用户数据键：按 PSI 元素累计补全触发次数。 */
private val INVOCATION_COUNT_KEY: com.intellij.openapi.util.Key<Int> =
    com.intellij.openapi.util.Key.create("CANGJIE_COMPLETION_INVOCATION_COUNT")

/**
 * 把平台补全类型翻译为仓颉补全种类。
 *
 * `CLASS_NAME` 是类名上下文专用类型：它仍属于基础补全，
 * 由仓颉侧的位置分类决定应给出哪些候选，不额外引入第三种语义。
 */
fun completionKindOf(type: CompletionType): CompletionKind = when (type) {
    CompletionType.BASIC, CompletionType.CLASS_NAME -> CompletionKind.BASIC
    CompletionType.SMART -> CompletionKind.SMART
}

/**
 * 计算当前 PSI 元素上的补全触发次数。
 */
private fun invocationCountOf(position: PsiElement): Int {
    val next = (position.getUserData(INVOCATION_COUNT_KEY) ?: 0) + 1
    position.putUserData(INVOCATION_COUNT_KEY, next)
    return next
}
