/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbols

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolUnavailableException
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolUnavailabilityReason
import org.cangnova.cangjie.analysis.api.util.withPsiEntry
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.buildAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * 从 PSI 恢复 CFIR 实体（声明或符号）的结果。
 *
 * 恢复是**部分函数**：PSI 可能属于当前会话不可见的模块、可能尚未进入索引，
 * 也可能是本身没有全局身份的局部声明。这些情况都不是编程错误，因此必须作为
 * 一等结果返回，而不是抛出异常——异常只保留给“框架自身契约被破坏”。
 *
 * 该类型是 low-level 层的真相来源；上层（Analysis API、缓存、IDE 展示）据此决定
 * 降级方式，而不是依赖异常消息文本。
 */
sealed interface CfirResolution<out T> {
    /**
     * 恢复成功。
     */
    class Resolved<T>(
        /**
         * 恢复出的实体。
         */
        val value: T,
    ) : CfirResolution<T> {
        override fun toString(): String = "Resolved(${value?.toString() ?: "<null>"})"
    }

    /**
     * 恢复失败，[reason] 说明属于哪一类缺失。
     */
    class Unresolved(
        /**
         * 缺失原因分类。
         */
        val reason: UnresolvedReason,
        /**
         * 用于诊断的现场信息。
         */
        val diagnostics: ResolutionDiagnostics,
    ) : CfirResolution<Nothing> {
        override fun toString(): String = "Unresolved($reason)"
    }
}

/**
 * 恢复失败的原因分类。
 *
 * 复用 Analysis API 公共层的分类，使补全/提示/检查等上层模块不必依赖 low-level 实现，
 * 同时保证整个栈只有一份“缺失原因”枚举。
 */
typealias UnresolvedReason = CaSymbolUnavailabilityReason

/**
 * 恢复失败的现场信息。
 *
 * 与异常附件不同，该对象可以被日志、缓存否定结果与降级报告共同携带，
 * 不需要经过异常构造。
 */
class ResolutionDiagnostics(
    /**
     * 人类可读的失败描述。
     */
    val message: String,
    /**
     * 触发恢复的 PSI 元素。
     */
    val psi: PsiElement? = null,
    /**
     * 已定位到的 CFIR 元素（若存在）。
     */
    val cfir: CfirElement? = null,
    /**
     * 额外键值条目，按插入顺序输出。
     */
    private val extraEntries: List<Pair<String, String>> = emptyList(),
) {
    /**
     * 取指定名称的额外条目。
     */
    fun entry(name: String): String? = extraEntries.firstOrNull { it.first == name }?.second

    /**
     * 把现场信息写入异常附件。
     */
    fun writeTo(builder: ExceptionAttachmentBuilder) {
        if (cfir != null) {
            builder.withCfirEntry("cfir", cfir)
        }
        if (psi != null) {
            builder.withPsiEntry("psi", psi)
        }
        for ((name, value) in extraEntries) {
            builder.withEntry(name, value)
        }
    }

    /**
     * 按 [reason] 的语义把本次缺失写入日志。
     *
     * 可预期的缺失用 debug，配置或 PSI 生命周期问题用 warn——错误级别只留给真正的不一致。
     */
    fun log(logger: Logger, reason: UnresolvedReason) {
        val text = "CFIR resolution failed (${reason.name}): $message"
        if (reason.isExpectedAbsence) {
            logger.debug(text)
        } else {
            logger.warn(text)
        }
    }

    override fun toString(): String = message
}

/**
 * 构造一个“声明不可恢复”的异常。
 *
 * 该异常专供仍要求非空结果的旧调用点使用：它携带 [reason]，让上层可以按类型捕获，
 * 而不必再匹配异常消息。父类型 [CaSymbolUnavailableException] 位于 Analysis API 公共层，
 * 因此上层模块不需要依赖 low-level 实现即可完成分类降级。
 */
class CfirUnresolvedSymbolException(
    /**
     * 缺失原因分类。
     */
    reason: UnresolvedReason,
    /**
     * 失败现场。
     */
    val diagnostics: ResolutionDiagnostics,
) : CaSymbolUnavailableException(reason, diagnostics.message) {
    init {
        buildAttachment { diagnostics.writeTo(this) }
    }
}

/**
 * 恢复结果中的实体，缺失时为 `null`。
 */
val <T> CfirResolution<T>.valueOrNull: T?
    get() = when (this) {
        is CfirResolution.Resolved -> value
        is CfirResolution.Unresolved -> null
    }

/**
 * 恢复结果中的缺失原因，成功时为 `null`。
 */
val CfirResolution<*>.unresolvedReason: UnresolvedReason?
    get() = when (this) {
        is CfirResolution.Resolved -> null
        is CfirResolution.Unresolved -> reason
    }

/**
 * 要求实体必须存在，否则抛出 [CfirUnresolvedSymbolException]。
 *
 * 仅用于无法表达缺失语义的旧调用点；新代码应直接处理 [CfirResolution]。
 */
fun <T> CfirResolution<T>.getOrThrow(): T = when (this) {
    is CfirResolution.Resolved -> value
    is CfirResolution.Unresolved -> throw CfirUnresolvedSymbolException(reason, diagnostics)
}

/**
 * 在恢复成功时转换结果，缺失时原样向上传播缺失原因。
 */
inline fun <T, R> CfirResolution<T>.map(block: (T) -> R): CfirResolution<R> = when (this) {
    is CfirResolution.Resolved -> CfirResolution.Resolved(block(value))
    is CfirResolution.Unresolved -> this
}

/**
 * 在恢复成功时执行可能失败的下一步，缺失时原样传播。
 */
inline fun <T, R> CfirResolution<T>.flatMap(block: (T) -> CfirResolution<R>): CfirResolution<R> = when (this) {
    is CfirResolution.Resolved -> block(value)
    is CfirResolution.Unresolved -> this
}
