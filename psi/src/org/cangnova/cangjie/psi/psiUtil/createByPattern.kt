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

package org.cangnova.cangjie.psi.psiUtil

import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.render
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil
import org.jetbrains.annotations.TestOnly
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashMap

/**
 * 表示 `PlainTextArgumentType`，承载PSI 工具中的语法节点、索引桩或辅助模型。
 */
private class PlainTextArgumentType<T : Any>(klass: Class<T>, val toPlainText: (T) -> String) : ArgumentType<T>(klass)
/**
 * 提供 `PsiChildRangeArgumentType` 单例，集中承载PSI 工具的共享状态、工厂或工具行为。
 */
private object PsiChildRangeArgumentType :
    PsiElementPlaceholderArgumentType<PsiChildRange, CjElement>(PsiChildRange::class.java, CjElement::class.java) {
    /**
     * 实现 `replacePlaceholderElement` 的PSI 工具协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun replacePlaceholderElement(placeholder: CjElement, argument: PsiChildRange, reformat: Boolean): PsiChildRange {
        val project = placeholder.project

        return if (!argument.isEmpty) {
            val first = placeholder.parent?.addRangeBefore(argument.first!!, argument.last!!, placeholder)
            val last = placeholder.prevSibling
            placeholder.delete()

            if (reformat) {
                val codeStyleManager = CodeStyleManager.getInstance(project)
                if (first != null) {
                    codeStyleManager.reformatNewlyAddedElement(first.node.treeParent, first.node)
                }
                if (last != first) {
                    codeStyleManager.reformatNewlyAddedElement(last.node.treeParent, last.node)
                }
            }
            PsiChildRange(first, last)
        } else {
            placeholder.delete()
            PsiChildRange.EMPTY
        }
    }
}

/**
 * 保存 `CREATE_BY_PATTERN_MAY_NOT_REFORMAT`，供PSI 工具流程读取节点结构或语义信息。
 */
@get:TestOnly
@set:TestOnly
var CREATE_BY_PATTERN_MAY_NOT_REFORMAT = false
/**
 * 表示 `PsiElementArgumentType`，承载PSI 工具中的语法节点、索引桩或辅助模型。
 */
private class PsiElementArgumentType<T : PsiElement>(klass: Class<T>) : PsiElementPlaceholderArgumentType<T, T>(klass, klass) {
    /**
     * 实现 `replacePlaceholderElement` 的PSI 工具协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun replacePlaceholderElement(placeholder: T, argument: T, reformat: Boolean): PsiChildRange {
        var result = if (placeholder is CjExpressionImplStub<*>) {
            CjExpressionImpl.replaceExpression(placeholder, argument, reformat, placeholder::rawReplace)
        } else {
            placeholder.replace(argument)
        }
        // if argument element has generated flag then it has not been formatted yet and we should do this manually
        // (because we cleared this flag for the whole tree above and PostprocessReformattingAspect won't format anything)
        if (CodeEditUtil.isNodeGenerated(argument.node)) {
            result = CodeStyleManager.getInstance(result.project).reformat(result, true)
        }
        return PsiChildRange.singleElement(result)
    }
}
/**
 * 保存 `SUPPORTED_ARGUMENT_TYPES` 的内部状态，供PSI 工具实现维护节点缓存或解析上下文。
 */
private val SUPPORTED_ARGUMENT_TYPES = listOf(
    PsiElementArgumentType(CjExpression::class.java),
    PsiElementArgumentType(CjTypeReference::class.java),
    PlainTextArgumentType(String::class.java, toPlainText = { it }),
    PlainTextArgumentType(Name::class.java, toPlainText = Name::render),
    PsiChildRangeArgumentType,
)
/**
 * 表示 `Placeholder`，承载PSI 工具中的语法节点、索引桩或辅助模型。
 */
private data class Placeholder(val range: TextRange, val text: String)
/**
 * 表示 `PsiElementPlaceholderArgumentType`，承载PSI 工具中的语法节点、索引桩或辅助模型。
 */
private abstract class PsiElementPlaceholderArgumentType<T : Any, TPlaceholder : PsiElement>(
    klass: Class<T>,
    /**
     * 保存 `placeholderClass`，供PSI 工具流程读取节点结构或语义信息。
     */
    val placeholderClass: Class<TPlaceholder>,
) : ArgumentType<T>(klass) {
    /**
     * 提供 `replacePlaceholderElement` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
     */
    abstract fun replacePlaceholderElement(placeholder: TPlaceholder, argument: T, reformat: Boolean): PsiChildRange
}
/**
 * 表示 `ArgumentType`，承载PSI 工具中的语法节点、索引桩或辅助模型。
 */
private abstract class ArgumentType<T : Any>(val klass: Class<T>)
/**
 * 表示 `PatternData`，承载PSI 工具中的语法节点、索引桩或辅助模型。
 */
private data class PatternData(val processedText: String, val placeholders: Map<Int, List<Placeholder>>)
/**
 * 提供 `createExpressionByPattern` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjPsiFactory.createExpressionByPattern(pattern: String, vararg args: Any, reformat: Boolean = true): CjExpression =
    createByPattern(pattern, *args, reformat = reformat) { createExpression(it) }
/**
 * 执行 `processPattern` 内部辅助逻辑，支撑PSI 工具节点的结构解析与访问。
 */
private fun processPattern(pattern: String, args: List<Any>): PatternData {
    val ranges = LinkedHashMap<Int, MutableList<Placeholder>>()

    fun charOrNull(i: Int) = if (0 <= i && i < pattern.length) pattern[i] else null

    fun check(condition: Boolean, message: String) {
        if (!condition) {
            throw IllegalArgumentException("Invalid pattern '$pattern' - $message")
        }
    }

    val text = buildString {
        var i = 0
        while (i < pattern.length) {
            val c = pattern[i]

            if (c == '$') {
                val nextChar = charOrNull(++i)
                if (nextChar == '$') {
                    append(nextChar)
                } else {
                    check(nextChar?.isDigit() ?: false, "unclosed '$'")

                    val lastIndex = (i until pattern.length).firstOrNull { !pattern[it].isDigit() } ?: pattern.length
                    val n = pattern.substring(i, lastIndex).toInt()
                    check(n >= 0, "invalid placeholder number: $n")
                    i = lastIndex

                    val arg: Any? = if (n < args.size) args[n] else null
                    val placeholderText = if (charOrNull(i) != ':' || charOrNull(i + 1) != '\'') {
                        arg as? String ?: "xyz"
                    } else {
                        check(arg !is String, "do not specify placeholder text for $$n - plain text argument passed")
                        i += 2 // skip ':' and '\''
                        val endIndex = pattern.indexOf('\'', i)
                        check(endIndex >= 0, "unclosed placeholder text")
                        check(endIndex > i, "empty placeholder text")
                        val text = pattern.substring(i, endIndex)
                        i = endIndex + 1
                        text
                    }

                    append(placeholderText)
                    val range = TextRange(length - placeholderText.length, length)
                    ranges.getOrPut(n) { ArrayList() }.add(Placeholder(range, placeholderText))
                    continue
                }
            } else {
                append(c)
            }
            i++
        }
    }

    if (ranges.isNotEmpty()) {
        val max = ranges.keys.maxOrNull()!!
        for (i in 0..max) {
            check(ranges.contains(i), "no '$$i' placeholder")
        }
    }

    if (args.size != ranges.size) {
        throw IllegalArgumentException("Wrong number of arguments, expected: ${ranges.size}, passed: ${args.size}")
    }

    return PatternData(text, ranges)
}
/**
 * 提供 `createByPattern` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun <TElement : CjElement> createByPattern(
    pattern: String,
    vararg args: Any,
    reformat: Boolean = true,
    factory: (String) -> TElement,
): TElement {
    val argumentTypes = args.map { arg ->
        SUPPORTED_ARGUMENT_TYPES.firstOrNull { it.klass.isInstance(arg) }
            ?: throw IllegalArgumentException(
                "Unsupported argument type: ${arg::class.java}, should be one of: " +
                    SUPPORTED_ARGUMENT_TYPES.joinToString { it.klass.simpleName },
            )
    }

    // convert arguments that can be converted into plain text
    @Suppress("NAME_SHADOWING")
    val args = args.zip(argumentTypes).map {
        val (arg, type) = it

        arg
    }

    val (processedText, allPlaceholders) = processPattern(pattern, args)

    var resultElement: CjElement = factory(processedText.trim())
    val project = resultElement.project

    val start = resultElement.startOffset

    val pointerManager = SmartPointerManager.getInstance(project)

    val pointers = HashMap<SmartPsiElementPointer<PsiElement>, Int>()

    PlaceholdersLoop@
    for ((n, placeholders) in allPlaceholders) {
        val arg = args[n]
        if (arg is String) continue // already in the text
        val expectedElementType = (argumentTypes[n] as? PsiElementPlaceholderArgumentType<*, *>)?.placeholderClass ?: continue

        for ((range, _) in placeholders) {
            val token = resultElement.findElementAt(range.startOffset)!!
            for (element in token.parentsWithSelf) {
                val elementRange = element.textRange.shiftRight(-start)
                if (elementRange == range && expectedElementType.isInstance(element)) {
                    val pointer = pointerManager.createSmartPsiElementPointer(element)
                    pointers[pointer] = n
                    break
                }

                if (!range.contains(elementRange)) {
                    throw IllegalArgumentException("Invalid pattern '$pattern' - no ${expectedElementType.simpleName} found for $$n, text = '$processedText'")
                }
            }
        }
    }

    val codeStyleManager = CodeStyleManager.getInstance(project)

    if (reformat) {
        if (CREATE_BY_PATTERN_MAY_NOT_REFORMAT) {
            throw java.lang.IllegalArgumentException("Reformatting is not allowed in the current context; please change the invocation to use reformat=false")
        }
        val stringPlaceholderRanges = allPlaceholders
            .filter { args[it.key] is String }
            .flatMap { it.value }
            .map { it.range }
            .filterNot { it.isEmpty }
            .sortedByDescending { it.startOffset }

        // reformat whole text except for String arguments (as they can contain user's formatting to be preserved)
        resultElement = if (stringPlaceholderRanges.none()) {
            codeStyleManager.reformat(resultElement, true) as CjElement
        } else {
            var bound = resultElement.endOffset - 1
            for (range in stringPlaceholderRanges) {
                // we extend reformatting range by 1 to the right because otherwise some of spaces are not reformatted
                resultElement = codeStyleManager.reformatRange(resultElement, range.endOffset + start, bound + 1, true) as CjElement
                bound = range.startOffset + start
            }
            codeStyleManager.reformatRange(resultElement, start, bound + 1, true) as CjElement
        }

        // do not reformat the whole expression in PostprocessReformattingAspect
        CodeEditUtil.setNodeGeneratedRecursively(resultElement.node, false)
    }
    for ((pointer, n) in pointers) {
        var element = pointer.element!!

        @Suppress("UNCHECKED_CAST")
        val argumentType = argumentTypes[n] as PsiElementPlaceholderArgumentType<in Any, in PsiElement>
        val range = argumentType.replacePlaceholderElement(element, args[n], reformat)

        if (element == resultElement) {
            assert(range.first == range.last)
            resultElement = range.first as CjElement
        }
    }

    if (reformat) {
        codeStyleManager.adjustLineIndent(resultElement.containingFile, resultElement.textRange)
    }

    @Suppress("UNCHECKED_CAST")
    return resultElement as TElement
}
