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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.psi.psiUtil.getElementTextWithContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.messages.Topic
import java.util.LinkedHashSet

/**
 * 表示 `CjCodeFragment`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
abstract class CjCodeFragment(
    viewProvider: FileViewProvider,
    imports: String?, // Should be separated by KtCodeFragment.IMPORT_SEPARATOR
    elementType: IElementType,
    /**
     * 保存 `context` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private val context: PsiElement?,
) : CjFile(
    viewProvider,
    false,
),
    CjCodeFragmentBase {
    /**
     * 保存 `viewProvider` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private var viewProvider = super.getViewProvider() as SingleRootFileViewProvider

    constructor(
        project: Project,
        name: String,
        text: CharSequence,
        imports: String?,
        elementType: IElementType,
        context: PsiElement?,
    ) : this(
        createFileViewProviderForLightFile(project, name, text),
        imports,
        elementType,
        context,
    )

    /**
     * Parses raw [rawImports] and appends them to the list of code fragment imports.
     *
     * Import strings must be separated by the [IMPORT_SEPARATOR].
     * Each import must be either a qualified name to import (e.g., 'foo.bar'), or a complete text representation of an import directive
     * (e.g., 'import foo.bar as baz').
     *
     * Note that already present import directives will be ignored.
     *
     * @return `true` if new import directives were added.
     */
    private fun appendImports(rawImports: String): Boolean {
        if (rawImports.isEmpty()) {
            return false
        }

        var hasNewImports = false

        for (rawImport in rawImports.split(IMPORT_SEPARATOR)) {
            val importDirectiveString = if (rawImport.startsWith("import ")) rawImport else "import $rawImport"
            if (importDirectiveStrings.add(importDirectiveString) && !hasNewImports) {
                hasNewImports = true
            }
        }

        return hasNewImports
    }

    init {
        @Suppress("LeakingThis")
        getViewProvider().forceCachedPsi(this)
        init(TokenType.CODE_FRAGMENT, elementType)
        if (imports != null) {
            appendImports(imports)
        }
    }

    /**
     * 保存 `importDirectiveStrings` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private var importDirectiveStrings = LinkedHashSet<String>()

    /**
     * 保存 `forcedResolveScope` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private var forcedResolveScope: GlobalSearchScope? = null
    /**
     * 实现 `getForcedResolveScope` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getForcedResolveScope(): GlobalSearchScope? = forcedResolveScope



    /**
     * 实现 `forceResolveScope` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun forceResolveScope(scope: GlobalSearchScope?) {
        forcedResolveScope = scope
    }

    /**
     * 保存 `isPhysical` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private var isPhysical = true

    /**
     * 提供 `getContentElement` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    abstract fun getContentElement(): CjElement?

    /**
     * 实现 `isPhysical` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isPhysical() = isPhysical

    /**
     * 实现 `isValid` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isValid() = true

    /**
     * 实现 `getContext` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getContext(): PsiElement? {
        if (context != null && context !is CjElement) {
            val logInfoForContextElement =
                (context as? PsiFile)?.virtualFile?.path ?: context.getElementTextWithContext()
            LOG.warn("CodeFragment with non-cangjie context should have fakeContextForJavaFile set: \noriginalContext = $logInfoForContextElement")
            return null
        }

        return context
    }

    /**
     * 实现 `getResolveScope` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getResolveScope() = context?.resolveScope ?: super.getResolveScope()

    /**
     * 实现 `clone` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun clone(): CjCodeFragment {
        val elementClone = calcTreeElement().clone() as FileElement
        return (cloneImpl(elementClone) as CjCodeFragment).apply {
            isPhysical = false
            myOriginalFile = this@CjCodeFragment
            importDirectiveStrings = LinkedHashSet(this@CjCodeFragment.importDirectiveStrings)
            viewProvider = SingleRootFileViewProvider(
                PsiManager.getInstance(project),
                LightVirtualFile(name, CangJieFileType.INSTANCE, text),
                false,
            )
            viewProvider.forceCachedPsi(this)
        }
        return (cloneImpl(elementClone) as CjCodeFragment).apply {
            isPhysical = false
            myOriginalFile = this@CjCodeFragment
            importDirectiveStrings = LinkedHashSet(this@CjCodeFragment.importDirectiveStrings)

            viewProvider = SingleRootFileViewProvider(
                PsiManager.getInstance(project),
                LightVirtualFile(name, CangJieFileType.INSTANCE, text),
                false,
            )
            viewProvider.forceCachedPsi(this)
        }
    }

    /**
     * 提供 `importsToString` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun importsToString(): String {
        return importDirectiveStrings.joinToString(IMPORT_SEPARATOR)
    }

    /**
     * 提供 `getViewProvider` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    final override fun getViewProvider() = viewProvider

    /**
     * 提供 `addImportsFromString` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun addImportsFromString(imports: String?) {
        val notifyChanged = viewProvider.isEventSystemEnabled && project !is MockProject

        if (imports != null && appendImports(imports)) {
            if (notifyChanged) {
                // This forces the code fragment to be re-highlighted.
                add(CjPsiFactory(project).createColon()).delete()
            }

            clearCaches()

            if (notifyChanged) {
                project.messageBus
                    .syncPublisher(IMPORT_MODIFICATION)
                    .onCodeFragmentImportsModification(this)
            }
        }
    }

    @Deprecated(
        "Use 'addImportsFromString()w' instead",
        ReplaceWith("addImportsFromString(import)"),
        level = DeprecationLevel.WARNING,
    )
    /**
     * 提供 `addImport` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun addImport(import: String) {
        addImportsFromString(import)
    }

    /**
     * 提供 `importsAsImportList` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun importsAsImportList(): CjImportList? {
        if (importDirectiveStrings.isNotEmpty() && context != null) {
            val ktPsiFactory = CjPsiFactory.contextual(context)
            val fileText = importDirectiveStrings.joinToString("\n")
            return ktPsiFactory.createFile("imports_for_codeFragment.kt", fileText).importList
        }

        return null
    }

    /**
     * 暴露 `importDirectivesItem`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val importDirectivesItem: List<CjImportInfo>
        get() = importsAsImportList()?.imports?.flatMap { it.importItems } ?: emptyList()

    /**
     * 提供 `getContextContainingFile` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getContextContainingFile(): CjFile? {
        return getOriginalContext()?.takeIf { it.isValid }?.getContainingCjFile()
    }

    /**
     * 提供 `getOriginalContext` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getOriginalContext(): CjElement? {
        val contextElement = getContext() as? CjElement
        val contextFile = contextElement?.containingFile as? CjFile
        if (contextFile is CjCodeFragment) {
            return contextFile.getOriginalContext()
        }
        return contextElement
    }

    companion object {
        const val IMPORT_SEPARATOR: String = ","

        @Suppress("UnstableApiUsage")
        val IMPORT_MODIFICATION: Topic<CangJieCodeFragmentImportModificationListener> =
            Topic(CangJieCodeFragmentImportModificationListener::class.java, Topic.BroadcastDirection.TO_CHILDREN, true)

        val FAKE_CONTEXT_FOR_JAVA_FILE: Key<Function0<CjElement>> = Key.create("FAKE_CONTEXT_FOR_JAVA_FILE")

        private val LOG = Logger.getInstance(CjCodeFragment::class.java)
        fun createFileViewProviderForLightFile(
            project: Project,
            name: String,
            text: CharSequence,
        ): FileViewProvider {
            val psiManager = PsiManager.getInstance(project) as PsiManagerEx
            return psiManager.fileManager.createFileViewProvider(
                LightVirtualFile(name, CangJieFileType.INSTANCE, text),
                /* eventSystemEnabled = */
                true,
            )
        }
    }
}

/**
 * 提供 `interface` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun interface CangJieCodeFragmentImportModificationListener {
    fun onCodeFragmentImportsModification(codeFragment: CjCodeFragment)
}
