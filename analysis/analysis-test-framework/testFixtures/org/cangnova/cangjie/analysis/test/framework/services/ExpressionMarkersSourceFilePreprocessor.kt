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

internal class ExpressionMarkersSourceFilePreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
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

    private class SourceFileProcessor(val regex: Regex, val action: (String, IntRange) -> Unit)

    object TAGS {
        val SELECTION_REGEXP = "<(expr(?:_(\\w+))?)>(.*?)</\\1>".toRegex(RegexOption.DOT_MATCHES_ALL)
        val CARET_REGEXP = "<(caret(?:_(\\w+))?)>".toRegex()

        fun getCaretTagText(qualifier: String): String = getTagText("caret", qualifier)
        fun getSelectionTagText(qualifier: String): String = getTagText("expr", qualifier)

        private fun getTagText(tagName: String, qualifier: String): String {
            return if (qualifier.isEmpty()) "<$tagName>" else "<${tagName}_$qualifier>"
        }
    }
}

class ExpressionMarkerProvider : TestService {
    private val selections = FileMarkerStorage<String, TextRange>()
    private val carets = FileMarkerStorage<String, Int>()

    fun addSelection(file: TestFile, qualifier: String, range: TextRange) {
        selections.add(file.name, qualifier, range)
    }

    fun addCaret(file: TestFile, qualifier: String, caretOffset: Int) {
        carets.add(file.name, qualifier, caretOffset)
    }

    fun getCaretOrNull(file: PsiFile, qualifier: String = ""): Int? {
        return carets.get(file.name, qualifier)
    }

    @Throws(IllegalStateException::class)
    fun getCaret(file: PsiFile, qualifier: String = ""): Int {
        return getCaretOrNull(file, qualifier)
            ?: caretNotFoundError(getCaretTagText(qualifier))
    }

    fun getAllCarets(file: PsiFile): List<FileMarker<Int>> {
        return carets.getAll(file.name)
            .map { (qualifier, offset) -> FileMarker(qualifier, getCaretTagText(qualifier), offset) }
    }

    fun getSelectionOrNull(file: PsiFile, qualifier: String = ""): TextRange? {
        return selections.get(file.name, qualifier)
    }

    @Throws(IllegalStateException::class)
    fun getSelection(file: PsiFile, qualifier: String = ""): TextRange {
        return getSelectionOrNull(file, qualifier)
            ?: caretNotFoundError(getSelectionTagText(qualifier))
    }

    fun getAllSelections(file: PsiFile): List<FileMarker<TextRange>> {
        return selections.getAll(file.name)
            .map { (qualifier, range) -> FileMarker(qualifier, getSelectionTagText(qualifier), range) }
    }

    @Throws(NoSuchElementException::class)
    inline fun <reified T : PsiElement> getBottommostElementOfTypeAtCaret(file: PsiFile, qualifier: String = ""): T {
        return getBottommostElementOfTypeAtCaret(file, T::class, qualifier)
    }

    @Throws(NoSuchElementException::class)
    fun <T : PsiElement> getBottommostElementOfTypeAtCaret(file: PsiFile, type: KClass<T>, qualifier: String = ""): T {
        return getBottommostElementOfTypeAtCaretOrNull(file, type, qualifier)
            ?: throw NoSuchElementException("Found no element on ${getCaretTagText(qualifier)} with the type ${type.simpleName}")
    }

    inline fun <reified T : PsiElement> getBottommostElementOfTypeAtCaretOrNull(file: PsiFile, qualifier: String = ""): T? {
        return getBottommostElementOfTypeAtCaretOrNull(file, T::class, qualifier)
    }

    fun <T : PsiElement> getBottommostElementOfTypeAtCaretOrNull(file: PsiFile, type: KClass<T>, qualifier: String = ""): T? {
        val offset = getCaretOrNull(file, qualifier) ?: return null
        val element = file.findElementAt(offset)
        return PsiTreeUtil.getParentOfType(element, type.java, false)
    }

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

    inline fun <reified T : PsiElement> getBottommostElementsOfTypeAtCarets(
        testServices: TestServices,
        qualifier: String = "",
    ): Collection<Pair<T, PsiFile>> {
        return testServices.cjTestModuleStructure.mainModules
            .flatMap { getBottommostElementsOfTypeAtCarets<T>(it.psiFiles, qualifier) }
    }

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

    @Throws(IllegalStateException::class)
    fun getTopmostSelectedElement(file: CjFile, qualifier: String = ""): PsiElement {
        val elements = getTopmostSelectedElements(file, qualifier)
        return elements.singleOrNull() ?: singleElementError(elements)
    }

    @Throws(IllegalStateException::class)
    inline fun <reified T : PsiElement> getTopmostSelectedElementOfType(file: CjFile, qualifier: String = ""): T {
        return getTopmostSelectedElementOfType(file, T::class, qualifier)
    }

    @Throws(IllegalStateException::class)
    fun <T : PsiElement> getTopmostSelectedElementOfType(file: CjFile, type: KClass<T>, qualifier: String = ""): T {
        val elements = getTopmostSelectedElementsOfType(file, type, qualifier)
        return elements.singleOrNull() ?: singleElementError(elements)
    }

    @Throws(IllegalStateException::class)
    private fun <T : PsiElement> getTopmostSelectedElementOfTypeOrNull(file: CjFile, type: KClass<T>, qualifier: String = ""): T? {
        val elements = getTopmostSelectedElementsOfType(file, type, qualifier)
        return elements.singleOrNull()
    }

    @Throws(IllegalStateException::class)
    private fun <T : PsiElement> getTopmostSelectedElementsOfType(file: CjFile, type: KClass<T>, qualifier: String = ""): List<T> {
        return getTopmostSelectedElements(file, qualifier).mapNotNull { getChildOfTypeOrNull(it, type) }
    }

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

    @Throws(NoSuchElementException::class)
    fun <T : PsiElement> getBottommostSelectedElementOfType(file: CjFile, type: KClass<T>, qualifier: String = ""): T {
        return getBottommostSelectedElementOfTypeOrNull(file, type, qualifier)
            ?: throw NoSuchElementException("Found no element of type ${type.simpleName} inside ${getSelectionTagText(qualifier)}")
    }

    private fun <T : PsiElement> getBottommostSelectedElementOfTypeOrNull(file: CjFile, type: KClass<T>, qualifier: String = ""): T? {
        val element = getTopmostSelectedElements(file, qualifier).singleOrNull() ?: return null

        val result = generateSequence(element) { it.children.singleOrNull() }
            .filter { type.isInstance(it) }
            .last { it.textRange == element.textRange }

        @Suppress("UNCHECKED_CAST")
        return result as T
    }

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

    fun getTopmostSelectedElementOfTypeByDirective(
        file: CjFile,
        module: CjTestModule,
        defaultType: KClass<out PsiElement> = PsiElement::class,
        qualifier: String = "",
    ): PsiElement {
        val type = findExpectedTypeClass(module.testModule.directives) ?: defaultType
        return getTopmostSelectedElementOfType(file, type, qualifier)
    }

    fun getTopmostSelectedElementOfTypeByDirectiveOrNull(
        file: CjFile,
        module: CjTestModule,
        defaultType: KClass<out PsiElement> = PsiElement::class,
        qualifier: String = "",
    ): PsiElement? {
        val type = findExpectedTypeClass(module.testModule.directives) ?: defaultType
        return getTopmostSelectedElementOfTypeOrNull(file, type, qualifier)
    }

    fun getBottommostSelectedElementOfTypeByDirective(
        file: CjFile,
        module: CjTestModule,
        defaultType: KClass<out PsiElement> = PsiElement::class,
        qualifier: String = "",
    ): PsiElement {
        val type = findExpectedTypeClass(module.testModule.directives) ?: defaultType
        return getBottommostSelectedElementOfType(file, type, qualifier)
    }

    private fun findExpectedTypeClass(registeredDirectives: RegisteredDirectives): KClass<PsiElement>? {
        val expectedType = registeredDirectives.singleOrZeroValue(Directives.LOOK_UP_FOR_ELEMENT_OF_TYPE) ?: return null
        val cjPsiPackage = "org.cangnova.cangjie.psi."
        val expectedTypeFqName = cjPsiPackage + expectedType.removePrefix(cjPsiPackage)

        @Suppress("UNCHECKED_CAST")
        return Class.forName(expectedTypeFqName).kotlin as KClass<PsiElement>
    }

    private fun List<PsiElement>.trimWhitespaces(): List<PsiElement> =
        dropWhile { it is PsiWhiteSpace }
            .dropLastWhile { it is PsiWhiteSpace }

    @Throws(IllegalStateException::class)
    private fun caretNotFoundError(tagText: String): Nothing {
        error("No '$tagText' tag was found in the file")
    }

    @Throws(IllegalStateException::class)
    private fun singleElementError(elements: Collection<PsiElement>): Nothing {
        val foundElements = elements.joinToString { it::class.simpleName + ": " + it.text }
        error("Expected a single element but found ${elements.size} [$foundElements]")
    }

    object Directives : SimpleDirectivesContainer() {
        val LOOK_UP_FOR_ELEMENT_OF_TYPE by stringDirective("LOOK_UP_FOR_ELEMENT_OF_TYPE")
    }
}

data class FileMarker<T : Any>(
    val qualifier: String,
    val tagText: String,
    val value: T,
)

fun FileMarker<TextRange>.toCaretMarker(): FileMarker<Int> {
    return FileMarker(qualifier, getCaretTagText(qualifier), value.startOffset)
}

private class FileMarkerStorage<K : Any, T : Any> {
    private val markersByFile = mutableMapOf<K, FileMarkers<T>>()

    fun get(key: K, qualifier: String): T? {
        val fileMarkers = markersByFile[key] ?: return null
        return fileMarkers.get(qualifier)
    }

    fun getAll(key: K): Map<String, T> {
        return markersByFile[key]?.getAll().orEmpty()
    }

    fun add(key: K, qualifier: String, value: T) {
        val fileMarkers = markersByFile.getOrPut(key) { FileMarkers() }
        fileMarkers.add(qualifier, value)
    }

    private class FileMarkers<T : Any> {
        private val markersByTag = mutableMapOf<String, T>()

        fun get(qualifier: String): T? {
            return markersByTag[qualifier]
        }

        fun getAll(): Map<String, T> {
            return Collections.unmodifiableMap(markersByTag)
        }

        fun add(qualifier: String, value: T) {
            markersByTag[qualifier] = value
        }
    }
}

val TestServices.expressionMarkerProvider: ExpressionMarkerProvider by TestServices.testServiceAccessor()
