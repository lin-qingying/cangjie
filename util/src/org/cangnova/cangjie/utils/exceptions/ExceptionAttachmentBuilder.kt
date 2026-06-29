/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.utils.exceptions


import org.cangnova.cangjie.utils.getElementTextWithContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * 在带附件异常上构建一个文本附件。
 */
inline fun ICangJieExceptionWithAttachments.buildAttachment(
    name: String = "info.txt",
    buildContent: ExceptionAttachmentBuilder.() -> Unit,
): ICangJieExceptionWithAttachments {
    return withAttachment(name, ExceptionAttachmentBuilder().apply(buildContent).buildString())
}

/**
 * 构造带附件的参数错误异常。
 */
inline fun buildErrorWithAttachment(
    message: String,
    cause: Exception? = null,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
): Throwable {
    val exception = CangJieIllegalArgumentExceptionWithAttachments(message, cause)
    exception.buildAttachment(attachmentName) { buildAttachment() }
    return exception
}

/**
 * 带附件的状态检查。
 *
 * 条件失败时抛出 [CangJieIllegalStateExceptionWithAttachments]，并把 [buildAttachment] 生成的内容写入附件。
 */
@OptIn(ExperimentalContracts::class)
inline fun checkWithAttachment(
    condition: Boolean,
    message: () -> String,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    contract { returns() implies (condition) }

    if (!condition) {
        val exception = CangJieIllegalStateExceptionWithAttachments(message())
        exception.buildAttachment(attachmentName) { buildAttachment() }
        throw exception
    }
}

/**
 * 使用 IntelliJ [Logger] 记录带附件错误。
 */
inline fun Logger.logErrorWithAttachment(
    message: String,
    cause: Exception? = null,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    this.error(buildErrorWithAttachment(message, cause, attachmentName, buildAttachment))
}

/**
 * 异常附件文本内容构建器。
 *
 * 构建器以分组条目的方式记录对象类型、渲染值和 PSI 上下文，便于错误报告定位现场。
 */
class ExceptionAttachmentBuilder {
    /**
     * 附件文本的内部缓冲区。
     */
    private val sb = StringBuilder()

    /**
     * 添加一个对象条目，并使用 [render] 输出非空值的详细文本。
     */
    fun <T> withEntry(name: String, value: T, render: (T & Any) -> String) {
        sb.appendLine("- $name:")
        sb.appendLine("  Class: ${value?.let { it::class.java.name } ?: "<null>"}")
        sb.appendLine("  Value:")
        sb.appendLine("    ${value?.let(render) ?: "<null>"}")
        sb.appendLine(SEPARATOR)
    }

    /**
     * 添加一个字符串条目。
     */
    fun withEntry(name: String, value: String?) {
        sb.appendLine("- $name:")
        sb.appendLine("    ${value ?: "<null>"}")
        sb.appendLine(SEPARATOR)
    }

    /**
     * 使用 [StringBuilder] DSL 构造并添加字符串条目。
     */
    fun withEntry(name: String, buildValue: StringBuilder.() -> Unit) {
        withEntry(name, StringBuilder().apply(buildValue).toString())
    }

    /**
     * 添加一个嵌套条目组。
     */
    fun withEntryGroup(groupName: String, build: ExceptionAttachmentBuilder.() -> Unit) {
        val builder = ExceptionAttachmentBuilder().apply(build)
        withEntry(groupName, builder) { it.buildString() }
    }

    /**
     * 返回当前已构造的附件文本。
     */
    fun buildString(): String = sb.toString()

    /**
     * 附件格式化常量。
     */
    private companion object {
        /**
         * 条目之间的文本分隔线。
         */
        private const val SEPARATOR = "========"
    }
}

/**
 * 将 PSI 元素的上下文文本写入附件条目。
 */
fun ExceptionAttachmentBuilder.withPsiEntry(name: String, psi: PsiElement?) {
    withEntry(name, psi) { psiElement ->
        getElementTextWithContext(psiElement)
    }
}



/**
 * 抛出带附件的参数错误异常。
 */
inline fun errorWithAttachment(
    message: String,
    cause: Throwable? = null,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
): Nothing {
    throw buildErrorWithAttachment(message, cause, attachmentName, buildAttachment)
}

/**
 * 构造带附件的参数错误异常。
 *
 * 该重载接受任意 [Throwable] 作为 cause，用于保留非 Exception 类型的底层错误。
 */
inline fun buildErrorWithAttachment(
    message: String,
    cause: Throwable? = null,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
): Throwable {
    val exception = CangJieIllegalArgumentExceptionWithAttachments(message, cause)
    exception.buildAttachment(attachmentName) { buildAttachment() }
    return exception
}
