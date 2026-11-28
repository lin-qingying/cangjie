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

package org.cangnova.cangjie.parsing

import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.parsing.CangJieParsing.Companion.createForTopLevel
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.parsing.AbstractCangJieParsing.ParsingContext
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjParserLanguageModuleNameProvider
import org.cangnova.cangjie.psi.parserLanguageModuleName
import org.cangnova.cangjie.psi.parsedLanguageModuleName
import org.jetbrains.annotations.NotNull

/**
 * 表示 `CangJieParser`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
 */
class CangJieParser(project: Project) : PsiParser {
    /**
     * 实现 `parse` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    @Deprecated("use Companion parse")
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        error("use Companion parse")
    }

    companion object {
        /** 同文本的目标编译产生独立真实树，原始 PSI 和旧诊断保持其原解析代次。 */
        fun parseCompilationFile(file: CjFile, context: CjmpParsingContext.Compilation,
            settings: CjParserSettings? = null): ParsedCangJiePsiFile = parsePsiCompilationFile(file, context, settings)

        /** 原生片段的入口角色与上下文由调用方显式提供，不能从旧文件最终角色猜测。 */
        fun parseCompilationFragment(
            file: CjCodeFragment,
            seed: CjParserSeed,
            targetContext: PsiElement?,
        ): ParsedCangJiePsiFile = parsePsiCompilationFragment(file, seed, targetContext)

        /**
         * 优先继承已完成解析的模块来源，已确认的空模块同样是有效结果。
         * 组织名属于官方 prefixPaths；不能把 org::std.foo 的语义包 std.foo 再解释为 std 模块。
         */
        fun languageModuleNameForContext(context: PsiElement?): String {
            val file = context?.containingFile as? CjFile ?: return ""
            if (file is CjCodeFragment) return file.fragmentParserSeed.languageModuleName
            file.parsedLanguageModuleName?.let { return it }

            // 该文件是调用方的既有语境。源码可能在这里首次建立 AST，解析完成后优先使用发布值；
            // stub 则直接提供源码 package 的标识符段，其中仍包含组织名前缀。
            val packageDirective = file.packageDirective
            file.parsedLanguageModuleName?.let { return it }
            val sourcePackageNames = packageDirective?.packageNames.orEmpty()
            val packageFqName = file.packageFqName
            val organizationName = if (sourcePackageNames.size > packageFqName.pathSegments().size) {
                sourcePackageNames.first().referencedNameAsName
            } else null
            return BuiltInAnnotationRegistry.sourceModuleName(packageFqName, organizationName)
        }

        @JvmStatic
        fun parseLambdaExpression(psiBuilder: PsiBuilder, seed: CjParserSeed): ASTNode =
            parseSeeded(psiBuilder, seed) { with(seed.context) { it.parseLambdaExpression() } }

        @JvmStatic
        fun parseBlockCodeFragment(psiBuilder: PsiBuilder, seed: CjParserSeed): ASTNode =
            parseSeeded(psiBuilder, seed, rootResultRequired = true) {
                with(seed.context) { it.parseBlockCodeFragment() }
            }

        internal fun parseBlockCodeFragment(psiBuilder: PsiBuilder, seed: CjParserSeed, file: CjCodeFragment?): ASTNode =
            parseSeeded(psiBuilder, seed, file, rootResultRequired = true) { with(seed.context) { it.parseBlockCodeFragment() } }

        @JvmStatic
        fun parseExpressionCodeFragment(psiBuilder: PsiBuilder, seed: CjParserSeed): ASTNode =
            parseSeeded(psiBuilder, seed, rootResultRequired = true) {
                with(seed.context) { it.parseExpressionCodeFragment() }
            }

        internal fun parseExpressionCodeFragment(psiBuilder: PsiBuilder, seed: CjParserSeed, file: CjCodeFragment?): ASTNode =
            parseSeeded(psiBuilder, seed, file, rootResultRequired = true) { with(seed.context) { it.parseExpressionCodeFragment() } }

        @JvmStatic
        fun parseMacroCodeFragment(psiBuilder: PsiBuilder, seed: CjParserSeed): ASTNode =
            parseSeeded(psiBuilder, seed, rootResultRequired = true) {
                with(seed.context) { it.parseMacroCodeFragment(checkNotNull(seed.macroFragment)) }
            }

        internal fun parseMacroCodeFragment(psiBuilder: PsiBuilder, seed: CjParserSeed, file: CjCodeFragment?): ASTNode =
            parseSeeded(psiBuilder, seed, file, rootResultRequired = true) {
                with(seed.context) { it.parseMacroCodeFragment(checkNotNull(seed.macroFragment)) }
            }

        @JvmStatic
        fun parseTypeCodeFragment(psiBuilder: PsiBuilder, seed: CjParserSeed): ASTNode =
            parseSeeded(psiBuilder, seed, rootResultRequired = true) {
                with(seed.context) { it.parseTypeCodeFragment() }
            }

        internal fun parseTypeCodeFragment(psiBuilder: PsiBuilder, seed: CjParserSeed, file: CjCodeFragment?): ASTNode =
            parseSeeded(psiBuilder, seed, file, rootResultRequired = true) { with(seed.context) { it.parseTypeCodeFragment() } }

        @NotNull
        @JvmStatic
        fun parse(psiBuilder: PsiBuilder, psiFile: PsiFile): ASTNode {
            psiBuilder.setDebugMode(true)
            require(psiFile is CjFile) { "A Cangjie file parser requires its actual CjFile input." }

            // 当前文件的 AST 尚未建立，不能在这里读取它自己的 packageFqName。
            // factory 已从外部 context 写入片段 seed；真实 package 由 parser 随后覆盖。
            psiFile.parsedLanguageModuleName = null
            psiFile.parsedFileFacts = null
            psiFile.parsedRoleSnapshot = null
            psiFile.parsedRoleRegion = null
            val configuredModuleName = psiFile.parserLanguageModuleName
            val languageModuleName = when {
                configuredModuleName.isNotEmpty() -> configuredModuleName
                psiFile is CjParserLanguageModuleNameProvider -> psiFile.parserLanguageModuleName()
                else -> configuredModuleName
            }
            // 文件语义模式：由文件本身推导，一次确定并随构造注入。
            // 不放进 ParsingContext —— 那是"用哪套文法"，与"文件是什么种类"正交。
            val entryKind = psiFile.sourceKind
            val sourceKind = psiFile.parserSourceKind ?: entryKind
            val context = when (entryKind) {
                CjSourceKind.MACRO_CALL -> ParsingContext.ANNOTATION_ONLY
                CjSourceKind.SOURCE, CjSourceKind.DECLARATION -> ParsingContext.DEFAULT
            }.copy(statementScope = CjScopeKind.TOPLEVEL)
            val compilation = psiFile.cjmpParsingContext
            val seed = psiFile.psiCompilationInput?.seed ?: CjParserSeed(
                sourceKind, languageModuleName, compilation,
                (compilation as? CjmpParsingContext.Compilation)?.initialFileRole ?: CjmpFileRole.NONE,
                0, context,
            )
            psiFile.putCopyableUserData(ROOT_PARSER_SEED_KEY, seed)
            return parseSeeded(psiBuilder, seed, psiFile) { cjParsing ->
                when (entryKind) {
                    CjSourceKind.MACRO_CALL -> cjParsing.parseMacroExpansionFile()
                    CjSourceKind.SOURCE, CjSourceKind.DECLARATION -> cjParsing.parseFile()
                }
            }
        }

        @JvmStatic
        fun parseBlockExpression(psiBuilder: PsiBuilder, seed: CjParserSeed): ASTNode =
            parseSeeded(psiBuilder, seed) { with(seed.context) { it.parseBlockExpression(seed.context.diagnosticScopeOwner) } }

        private inline fun parseSeeded(
            psiBuilder: PsiBuilder,
            seed: CjParserSeed,
            file: CjFile? = null,
            rootResultRequired: Boolean = file != null,
            action: (CangJieParsing) -> Unit,
        ): ASTNode {
            psiBuilder.setDebugMode(true)
            val ownBuilder = psiBuilder as? CangJiePsiBuilder
            val inputSource = ownBuilder?.sourceContext ?: seed.sourceContext
                ?: CjParserSourceContext.complete(psiBuilder.originalText)
            val lexicalInput = if (ownBuilder?.sourceContext != null) checkNotNull(ownBuilder.lexicalInput)
                else seed.lexicalInput?.also { require(it.source === inputSource.snapshot) }
                    ?: CjLexicalSourceInput(inputSource.snapshot)
            val semantic = SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder, inputSource, lexicalInput)
            val cjParsing = if (file?.psiCompilationInput == null) {
                createForTopLevel(semantic, seed.languageModuleName, seed.sourceKind, seed.cjmpContext, seed.settings)
            } else {
                CangJieParsing.createForTopLevelNonLazy(semantic, seed.languageModuleName, seed.sourceKind, seed.cjmpContext, seed.settings)
            }
            cjParsing.restoreSeed(seed)
            action(cjParsing)
            val result = if (!rootResultRequired) null else CjParsedFileResult(
                seed, cjParsing.languageModuleName, cjParsing.parsedFileFacts, cjParsing.macroFragmentInfo,
                sourceText = psiBuilder.originalText.toString(),
                sourceContext = inputSource,
            )
            val finished = psiBuilder.finishCangJieParsing(semantic.macroParsingJournal, result)
            if (file != null) file.publishParsedFileResult(checkNotNull(finished.fileResult))
            return finished.tree
        }
    }
}
