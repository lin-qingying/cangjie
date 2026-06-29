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

import org.cangnova.cangjie.utils.exceptions.ICangJieExceptionWithAttachments.Companion.withAttachmentsFrom
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.ExceptionWithAttachments
import java.nio.charset.StandardCharsets
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * 仓颉异常附件协议。
 *
 * 实现该接口的异常可以携带 IntelliJ [Attachment]，用于在 IDE 日志和错误报告中保留编译器现场信息。
 */
interface ICangJieExceptionWithAttachments : ExceptionWithAttachments {
    /**
     * 当前异常持有的可变附件列表。
     */
    val mutableAttachments: MutableList<Attachment>

    /**
     * 返回 IntelliJ 平台期望的附件数组。
     */
    override fun getAttachments(): Array<Attachment> = mutableAttachments.toTypedArray()

    /**
     * 添加一个文本附件并返回当前异常。
     */
    fun withAttachment(name: String, content: Any?): ICangJieExceptionWithAttachments {
        mutableAttachments.add(Attachment(name, content?.toString() ?: "<null>"))
        return this
    }

    /**
     * 附件复制与 cause 传播工具。
     */
    companion object {
        /**
         * 从 cause 异常中复制已有附件，并追加 cause 的堆栈文本。
         */
        internal fun ICangJieExceptionWithAttachments.withAttachmentsFrom(from: Throwable?) {
            if (from is ICangJieExceptionWithAttachments) {
                from.mutableAttachments.mapTo(mutableAttachments) { attachment ->
                    attachment.copyWithNewName("case_${attachment.path}")
                }
            }
            if (from != null) {
                withAttachment("causeThrowable", from.stackTraceToString())
            }
        }

        /**
         * 使用新名称复制附件内容。
         */
        private fun Attachment.copyWithNewName(newName: String): Attachment {
            val content = String(bytes, StandardCharsets.UTF_8)
            return Attachment(newName, content)
        }
    }
}

/**
 * 带附件的非法状态异常。
 */
open class CangJieIllegalStateExceptionWithAttachments : IllegalStateException, ICangJieExceptionWithAttachments {
    /**
     * 当前异常携带的附件集合。
     */
    final override val mutableAttachments = mutableListOf<Attachment>()

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        withAttachmentsFrom(cause)
    }
}

/**
 * 带附件的运行时异常。
 */
open class CangJieRuntimeExceptionWithAttachments : RuntimeException, ICangJieExceptionWithAttachments {
    /**
     * 当前异常携带的附件集合。
     */
    final override val mutableAttachments = mutableListOf<Attachment>()

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        withAttachmentsFrom(cause)
    }
}

/**
 * 带附件的非法参数异常。
 */
open class CangJieIllegalArgumentExceptionWithAttachments : IllegalArgumentException, ICangJieExceptionWithAttachments {
    /**
     * 当前异常携带的附件集合。
     */
    final override val mutableAttachments = mutableListOf<Attachment>()

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        withAttachmentsFrom(cause)
    }
}

/**
 * 带附件的参数前置条件检查。
 *
 * 条件失败时抛出 [CangJieIllegalArgumentExceptionWithAttachments]。
 */
@OptIn(ExperimentalContracts::class)
inline fun requireWithAttachment(
    condition: Boolean,
    message: () -> String,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    contract { returns() implies (condition) }

    if (!condition) {
        val exception = CangJieIllegalArgumentExceptionWithAttachments(message())
        exception.buildAttachment(attachmentName) { buildAttachment() }
        throw exception
    }
}
