/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.test.framework.services

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.isAncestor
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.services.ExpressionMarkersSourceFilePreprocessor.TAGS.getCaretTagText
import org.cangnova.cangjie.analysis.test.framework.services.ExpressionMarkersSourceFilePreprocessor.TAGS.getSelectionTagText
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleOrZeroValue
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.SourceFilePreprocessor
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices
import java.util.Collections
import kotlin.reflect.KClass

/**
 * 解析测试源码中的 `<expr>` 和 `<caret>` 标记，并把标记位置登记到 [ExpressionMarkerProvider]。
 */
internal class ExpressionMarkersSourceFilePreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    /**
     * 从源码文本中移除标记并记录对应的 selection/caret 位置。
     */
    override fun process(file: TestFile, content: String): String {
        val processors = listOf(
            SourceFileProcessor(TAGS.SELECTION_REGEXP) { qualifier, range ->
                testServices.expressionMarkerProvider.addSelection(file, qualifier, TextRange(range.first, range.last + 1))
            },
            SourceFileProcessor(TAGS.CARET_REGEXP) { qualifier, range ->
                testServices.expressionMarkerProvider.addCaret(file, qualifier, range.first)
            },
        )

        return processText(content, processors)
    }

    /**
     * 按标记在文本中的先后顺序逐个执行处理器并返回去标记后的源码。
     */
    private fun processText(text: String, processors: List<SourceFileProcessor>): String {
        var result = text

        while (true) {
            val matches = sequence {
                for (processor in processors) {
                    val match = processor.regex.find(result) ?: continue
                    yield(processor to match)
                }
            }.sortedBy { it.second.range.first }

            val (processor, match) = matches.firstOrNull() ?: break
            val qualifier = match.groupValues[2]

            val startOffset = match.range.first
            val selectionGroup = if (match.groups.size >= 4) match.groups[3] else null

            val range = if (selectionGroup != null) {
                val delta = selectionGroup.range.first - startOffset
                IntRange(selectionGroup.range.first - delta, selectionGroup.range.last - delta)
            } else {
                IntRange(startOffset, startOffset)
            }

            processor.action(qualifier, range)

            val replacementText = selectionGroup?.value ?: ""
            result = result.replaceRange(match.range, replacementText)
        }

        return result
    }

    /**
     * 单类源码标记的正则与记录动作。
     */
    private class SourceFileProcessor(
        /**
         * 用于识别标记的正则表达式。
         */
        val regex: Regex,
        /**
         * 标记命中后写入 provider 的动作。
         */
        val action: (String, IntRange) -> Unit,
    )

    /**
     * 测试源码中表达式选择和光标标记的文本协议。
     */
    object TAGS {
        /**
         * `<expr>` 或 `<expr_name>` selection 标记正则。
         */
        val SELECTION_REGEXP = "<(expr(?:_(\\w+))?)>(.*?)</\\1>".toRegex(RegexOption.DOT_MATCHES_ALL)

        /**
         * `<caret>` 或 `<caret_name>` 光标标记正则。
         */
        val CARET_REGEXP = "<(caret(?:_(\\w+))?)>".toRegex()

        /**
         * 根据 qualifier 构造 caret 标记文本。
         */
        fun getCaretTagText(qualifier: String): String = getTagText("caret", qualifier)

        /**
         * 根据 qualifier 构造 selection 标记文本。
         */
        fun getSelectionTagText(qualifier: String): String = getTagText("expr", qualifier)

        /**
         * 构造带可选 qualifier 的标记文本。
         */
        private fun getTagText(tagName: String, qualifier: String): String {
            return if (qualifier.isEmpty()) "<$tagName>" else "<${tagName}_$qualifier>"
        }
    }
}

/**
 * 提供测试源码 selection/caret 标记查询能力的测试服务。
 */
class ExpressionMarkerProvider : TestService {
    /**
     * 按文件和 qualifier 保存的 selection 范围。
     */
    private val selections = FileMarkerStorage<String, TextRange>()

    /**
     * 按文件和 qualifier 保存的 caret 偏移。
     */
    private val carets = FileMarkerStorage<String, Int>()

    /**
     * 登记指定测试文件的 selection 范围。
     */
    fun addSelection(file: TestFile, qualifier: String, range: TextRange) {
        selections.add(file.name, qualifier, range)
    }

    /**
     * 登记指定测试文件的 caret 偏移。
     */
    fun addCaret(file: TestFile, qualifier: String, caretOffset: Int) {
        carets.add(file.name, qualifier, caretOffset)
    }

    /**
     * 返回指定 PSI 文件和 qualifier 对应的 caret 偏移。
     */
    fun getCaretOrNull(file: PsiFile, qualifier: String = ""): Int? {
        return carets.get(file.name, qualifier)
    }

    /**
     * 返回指定 PSI 文件和 qualifier 对应的 caret 偏移；不存在时失败。
     */
    @Throws(IllegalStateException::class)
    fun getCaret(file: PsiFile, qualifier: String = ""): Int {
        return getCaretOrNull(file, qualifier)
            ?: caretNotFoundError(getCaretTagText(qualifier))
    }

    /**
     * 返回指定 PSI 文件中登记的全部 caret 标记。
     */
    fun getAllCarets(file: PsiFile): List<FileMarker<Int>> {
        return carets.getAll(file.name)
            .map { (qualifier, offset) -> FileMarker(qualifier, getCaretTagText(qualifier), offset) }
    }

    /**
     * 返回指定 PSI 文件和 qualifier 对应的 selection 范围。
     */
    fun getSelectionOrNull(file: PsiFile, qualifier: String = ""): TextRange? {
        return selections.get(file.name, qualifier)
    }

    /**
     * 返回指定 PSI 文件和 qualifier 对应的 selection 范围；不存在时失败。
     */
    @Throws(IllegalStateException::class)
    fun getSelection(file: PsiFile, qualifier: String = ""): TextRange {
        return getSelectionOrNull(file, qualifier)
            ?: caretNotFoundError(getSelectionTagText(qualifier))
    }

    /**
     * 返回指定 PSI 文件中登记的全部 selection 标记。
     */
    fun getAllSelections(file: PsiFile): List<FileMarker<TextRange>> {
        return selections.getAll(file.name)
            .map { (qualifier, range) -> FileMarker(qualifier, getSelectionTagText(qualifier), range) }
    }

    /**
     * 返回 caret 位置处最内层的指定 PSI 元素。
     */
    @Throws(NoSuchElementException::class)
    inline fun <reified T : PsiElement> getBottommostElementOfTypeAtCaret(file: PsiFile, qualifier: String = ""): T {
        return getBottommostElementOfTypeAtCaret(file, T::class, qualifier)
    }

    /**
     * 返回 caret 位置处最内层的指定 PSI 元素。
     */
    @Throws(NoSuchElementException::class)
    fun <T : PsiElement> getBottommostElementOfTypeAtCaret(file: PsiFile, type: KClass<T>, qualifier: String = ""): T {
        return getBottommostElementOfTypeAtCaretOrNull(file, type, qualifier)
            ?: throw NoSuchElementException("Found no element on ${getCaretTagText(qualifier)} with the type ${type.simpleName}")
    }

    /**
     * 返回 caret 位置处最内层的指定 PSI 元素；不存在时返回 `null`。
     */
    inline fun <reified T : PsiElement> getBottommostElementOfTypeAtCaretOrNull(file: PsiFile, qualifier: String = ""): T? {
        return getBottommostElementOfTypeAtCaretOrNull(file, T::class, qualifier)
    }

    /**
     * 返回 caret 位置处最内层的指定 PSI 元素；不存在时返回 `null`。
     */
    fun <T : PsiElement> getBottommostElementOfTypeAtCaretOrNull(file: PsiFile, type: KClass<T>, qualifier: String = ""): T? {
        val offset = getCaretOrNull(file, qualifier) ?: return null
        val element = file.findElementAt(offset)
        return PsiTreeUtil.getParentOfType(element, type.java, false)
    }

    /**
     * 在多个 PSI 文件中查找指定 qualifier 的 caret 处元素。
     */
    inline fun <reified T : PsiElement> getBottommostElementsOfTypeAtCarets(
        files: List<PsiFile>,
        qualifier: String = "",
    ): List<Pair<T, PsiFile>> {
        return buildList {
            for (file in files) {
                val element = getBottommostElementOfTypeAtCaretOrNull<T>(file, qualifier) ?: continue
                add(element to file)
            }
        }
    }

    /**
     * 在当前测试模块结构的全部 PSI 文件中查找指定 qualifier 的 caret 处元素。
     */
    inline fun <reified T : PsiElement> getBottommostElementsOfTypeAtCarets(
        testServices: TestServices,
        qualifier: String = "",
    ): Collection<Pair<T, PsiFile>> {
        return testServices.cjTestModuleStructure.mainModules
            .flatMap { getBottommostElementsOfTypeAtCarets<T>(it.psiFiles, qualifier) }
    }

    /**
     * 返回 selection 覆盖范围内的最外层 PSI 元素集合。
     */
    private fun getTopmostSelectedElements(file: CjFile, qualifier: String = ""): List<PsiElement> {
        val range = getSelectionOrNull(file, qualifier) ?: return emptyList()
        val candidates = if (range.isEmpty) {
            file.collectDescendantsOfType<PsiElement> { element -> element.textRange == range }
        } else {
            PsiTreeUtil.collectElements(file) { element ->
                element.textRange?.let { range.contains(it) } == true
            }.toList()
        }

        return candidates
            .filter { candidate ->
                candidates.none { other -> other !== candidate && other.isAncestor(candidate, strict = true) }
            }
            .trimWhitespaces()
    }

    /**
     * 返回 selection 覆盖范围内唯一的最外层 PSI 元素。
     */
    @Throws(IllegalStateException::class)
    fun getTopmostSelectedElement(file: CjFile, qualifier: String = ""): PsiElement {
        val elements = getTopmostSelectedElements(file, qualifier)
        return elements.singleOrNull() ?: singleElementError(elements)
    }

    /**
     * 返回 selection 覆盖范围内唯一的最外层指定类型 PSI 元素。
     */
    @Throws(IllegalStateException::class)
    inline fun <reified T : PsiElement> getTopmostSelectedElementOfType(file: CjFile, qualifier: String = ""): T {
        return getTopmostSelectedElementOfType(file, T::class, qualifier)
    }

    /**
     * 返回 selection 覆盖范围内唯一的最外层指定类型 PSI 元素。
     */
    @Throws(IllegalStateException::class)
    fun <T : PsiElement> getTopmostSelectedElementOfType(file: CjFile, type: KClass<T>, qualifier: String = ""): T {
        val elements = getTopmostSelectedElementsOfType(file, type, qualifier)
        return elements.singleOrNull() ?: singleElementError(elements)
    }

    /**
     * 返回 selection 覆盖范围内唯一的最外层指定类型 PSI 元素；不存在时返回 `null`。
     */
    @Throws(IllegalStateException::class)
    private fun <T : PsiElement> getTopmostSelectedElementOfTypeOrNull(file: CjFile, type: KClass<T>, qualifier: String = ""): T? {
        val elements = getTopmostSelectedElementsOfType(file, type, qualifier)
        return elements.singleOrNull()
    }

    /**
     * 返回 selection 覆盖范围内的全部最外层指定类型 PSI 元素。
     */
    @Throws(IllegalStateException::class)
    private fun <T : PsiElement> getTopmostSelectedElementsOfType(file: CjFile, type: KClass<T>, qualifier: String = ""): List<T> {
        return getTopmostSelectedElements(file, qualifier).mapNotNull { getChildOfTypeOrNull(it, type) }
    }

    /**
     * 在元素自身或等长单子链中查找指定类型子元素。
     */
    private fun <T : PsiElement> getChildOfTypeOrNull(element: PsiElement, type: KClass<T>): T? {
        if (type.isInstance(element)) {
            @Suppress("UNCHECKED_CAST")
            return element as T
        }

        val result = generateSequence(element) { it.children.singleOrNull() }
            .takeWhile { it.textRange == element.textRange }
            .firstOrNull { type.isInstance(it) }

        @Suppress("UNCHECKED_CAST")
        return result as T?
    }

    /**
     * 返回 selection 覆盖范围内最内层的指定类型 PSI 元素。
     */
    @Throws(NoSuchElementException::class)
    fun <T : PsiElement> getBottommostSelectedElementOfType(file: CjFile, type: KClass<T>, qualifier: String = ""): T {
        return getBottommostSelectedElementOfTypeOrNull(file, type, qualifier)
            ?: throw NoSuchElementException("Found no element of type ${type.simpleName} inside ${getSelectionTagText(qualifier)}")
    }

    /**
     * 返回 selection 覆盖范围内最内层的指定类型 PSI 元素；不存在时返回 `null`。
     */
    private fun <T : PsiElement> getBottommostSelectedElementOfTypeOrNull(file: CjFile, type: KClass<T>, qualifier: String = ""): T? {
        val element = getTopmostSelectedElements(file, qualifier).singleOrNull() ?: return null

        val result = generateSequence(element) { it.children.singleOrNull() }
            .filter { type.isInstance(it) }
            .last { it.textRange == element.textRange }

        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    /**
     * 根据指令指定的 PSI 类型，从 selection 或 caret 中查找最内层元素。
     */
    fun getBottommostElementOfTypeByDirective(
        file: CjFile,
        module: TestModule,
        defaultType: KClass<out PsiElement> = PsiElement::class,
        qualifier: String = "",
    ): PsiElement {
        val type = findExpectedTypeClass(module.directives) ?: defaultType
        return getBottommostSelectedElementOfTypeOrNull(file, type, qualifier)
            ?: getBottommostElementOfTypeAtCaretOrNull(file, type, qualifier)
            ?: error("Neither <expr> marker nor <caret> were found in file")
    }

    /**
     * 根据指令指定的 PSI 类型，从 selection 中查找最外层元素。
     */
    fun getTopmostSelectedElementOfTypeByDirective(
        file: CjFile,
        module: CjTestModule,
        defaultType: KClass<out PsiElement> = PsiElement::class,
        qualifier: String = "",
    ): PsiElement {
        val type = findExpectedTypeClass(module.testModule.directives) ?: defaultType
        return getTopmostSelectedElementOfType(file, type, qualifier)
    }

    /**
     * 根据指令指定的 PSI 类型，从 selection 中查找最外层元素；不存在时返回 `null`。
     */
    fun getTopmostSelectedElementOfTypeByDirectiveOrNull(
        file: CjFile,
        module: CjTestModule,
        defaultType: KClass<out PsiElement> = PsiElement::class,
        qualifier: String = "",
    ): PsiElement? {
        val type = findExpectedTypeClass(module.testModule.directives) ?: defaultType
        return getTopmostSelectedElementOfTypeOrNull(file, type, qualifier)
    }

    /**
     * 根据指令指定的 PSI 类型，从 selection 中查找最内层元素。
     */
    fun getBottommostSelectedElementOfTypeByDirective(
        file: CjFile,
        module: CjTestModule,
        defaultType: KClass<out PsiElement> = PsiElement::class,
        qualifier: String = "",
    ): PsiElement {
        val type = findExpectedTypeClass(module.testModule.directives) ?: defaultType
        return getBottommostSelectedElementOfType(file, type, qualifier)
    }

    /**
     * 从 `LOOK_UP_FOR_ELEMENT_OF_TYPE` 指令中解析期望 PSI 类型。
     */
    private fun findExpectedTypeClass(registeredDirectives: RegisteredDirectives): KClass<PsiElement>? {
        val expectedType = registeredDirectives.singleOrZeroValue(Directives.LOOK_UP_FOR_ELEMENT_OF_TYPE) ?: return null
        val cjPsiPackage = "org.cangnova.cangjie.psi."
        val expectedTypeFqName = cjPsiPackage + expectedType.removePrefix(cjPsiPackage)

        @Suppress("UNCHECKED_CAST")
        return Class.forName(expectedTypeFqName).kotlin as KClass<PsiElement>
    }

    /**
     * 去掉元素列表首尾的空白 PSI。
     */
    private fun List<PsiElement>.trimWhitespaces(): List<PsiElement> =
        dropWhile { it is PsiWhiteSpace }
            .dropLastWhile { it is PsiWhiteSpace }

    /**
     * 构造缺失 caret/selection 标记时的错误。
     */
    @Throws(IllegalStateException::class)
    private fun caretNotFoundError(tagText: String): Nothing {
        error("No '$tagText' tag was found in the file")
    }

    /**
     * 构造 selection 结果不是单个元素时的错误。
     */
    @Throws(IllegalStateException::class)
    private fun singleElementError(elements: Collection<PsiElement>): Nothing {
        val foundElements = elements.joinToString { it::class.simpleName + ": " + it.text }
        error("Expected a single element but found ${elements.size} [$foundElements]")
    }

    /**
     * 表达式标记服务使用的测试指令。
     */
    object Directives : SimpleDirectivesContainer() {
        /**
         * 指定 selection/caret 应解析成的 PSI 元素类型短名或全限定名。
         */
        val LOOK_UP_FOR_ELEMENT_OF_TYPE by stringDirective("LOOK_UP_FOR_ELEMENT_OF_TYPE")
    }
}

/**
 * 单个文件标记的值对象。
 */
data class FileMarker<T : Any>(
    /**
     * 标记 qualifier，空字符串代表默认标记。
     */
    val qualifier: String,
    /**
     * 源码中对应的标记文本。
     */
    val tagText: String,
    /**
     * 标记承载的值。
     */
    val value: T,
)

/**
 * 将 selection 范围标记转换为 caret 偏移标记。
 */
fun FileMarker<TextRange>.toCaretMarker(): FileMarker<Int> {
    return FileMarker(qualifier, getCaretTagText(qualifier), value.startOffset)
}

/**
 * 按文件 key 和 qualifier 存储标记值。
 */
private class FileMarkerStorage<K : Any, T : Any> {
    /**
     * 文件 key 到该文件全部标记的映射。
     */
    private val markersByFile = mutableMapOf<K, FileMarkers<T>>()

    /**
     * 返回指定文件 key 与 qualifier 对应的标记值。
     */
    fun get(key: K, qualifier: String): T? {
        val fileMarkers = markersByFile[key] ?: return null
        return fileMarkers.get(qualifier)
    }

    /**
     * 返回指定文件 key 下的全部标记。
     */
    fun getAll(key: K): Map<String, T> {
        return markersByFile[key]?.getAll().orEmpty()
    }

    /**
     * 写入指定文件 key 与 qualifier 对应的标记值。
     */
    fun add(key: K, qualifier: String, value: T) {
        val fileMarkers = markersByFile.getOrPut(key) { FileMarkers() }
        fileMarkers.add(qualifier, value)
    }

    /**
     * 单个文件内按 qualifier 存储的标记集合。
     */
    private class FileMarkers<T : Any> {
        /**
         * qualifier 到标记值的映射。
         */
        private val markersByTag = mutableMapOf<String, T>()

        /**
         * 返回指定 qualifier 对应的标记值。
         */
        fun get(qualifier: String): T? {
            return markersByTag[qualifier]
        }

        /**
         * 返回不可变视图形式的全部标记。
         */
        fun getAll(): Map<String, T> {
            return Collections.unmodifiableMap(markersByTag)
        }

        /**
         * 写入指定 qualifier 对应的标记值。
         */
        fun add(qualifier: String, value: T) {
            markersByTag[qualifier] = value
        }
    }
}

/**
 * 当前测试服务容器中的表达式标记 provider。
 */
val TestServices.expressionMarkerProvider: ExpressionMarkerProvider by TestServices.testServiceAccessor()
