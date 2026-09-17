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
        /**
         * 优先继承已完成解析的模块来源，已确认的空模块同样是有效结果。
         * 组织名属于官方 prefixPaths；不能把 org::std.foo 的语义包 std.foo 再解释为 std 模块。
         */
        fun languageModuleNameForContext(context: PsiElement?): String {
            val file = context?.containingFile as? CjFile ?: return ""
            file.parsedLanguageModuleName?.let { return it }
            if (file is CjCodeFragment) return languageModuleNameForContext(file.getOriginalContext())

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
        @JvmOverloads
        fun parseLambdaExpression(psiBuilder: PsiBuilder, languageModuleName: String = ""): ASTNode {
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder),
                    languageModuleName,
                )

            with(ParsingContext.DEFAULT) {
                cjParsing.parseLambdaExpression()
            }

            return psiBuilder.treeBuilt
        }

        @JvmStatic
        @JvmOverloads
        fun parseBlockCodeFragment(psiBuilder: PsiBuilder, languageModuleName: String = ""): ASTNode {
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder),
                    languageModuleName,
                )

            with(ParsingContext.DEFAULT) {
                cjParsing.parseBlockCodeFragment()
            }

            return psiBuilder.treeBuilt
        }

        @JvmStatic
        @JvmOverloads
        fun parseExpressionCodeFragment(psiBuilder: PsiBuilder, languageModuleName: String = ""): ASTNode {
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder),
                    languageModuleName,
                )

            with(ParsingContext.DEFAULT) {
                cjParsing.parseExpressionCodeFragment()

            }


            return psiBuilder.treeBuilt
        }

        @JvmStatic
        @JvmOverloads
        fun parseTypeCodeFragment(psiBuilder: PsiBuilder, languageModuleName: String = ""): ASTNode {
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder),
                    languageModuleName,
                )

            with(ParsingContext.DEFAULT) {
                cjParsing.parseTypeCodeFragment()
            }

            return psiBuilder.treeBuilt
        }

        @NotNull
        @JvmStatic
        fun parse(psiBuilder: PsiBuilder, psiFile: PsiFile): ASTNode {
            psiBuilder.setDebugMode(true)

            // 当前文件的 AST 尚未建立，不能在这里读取它自己的 packageFqName。
            // factory 已从外部 context 写入片段 seed；真实 package 由 parser 随后覆盖。
            val languageModuleName = if (psiFile is CjFile) {
                psiFile.parsedLanguageModuleName = null
                psiFile.parserLanguageModuleName
            } else ""

            // 文件语义模式：由文件本身推导，一次确定并随构造注入。
            // 不放进 ParsingContext —— 那是"用哪套文法"，与"文件是什么种类"正交。
            // 非 CjFile（理论上到不了这里）回退到按文件名判定，保留能力而不留第二处判定。
            val sourceKind = (psiFile as? CjFile)?.sourceKind ?: CjSourceKind.fromFileName(psiFile.name)

            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder),
                    languageModuleName,
                    sourceKind,
                )


            // 入口分派：穷尽 when，无 else —— 新增文件种类时编译器会强制在此表态。
            when (sourceKind) {
                CjSourceKind.MACRO_CALL -> cjParsing.parseOnlyAnnotationFile()
                CjSourceKind.SOURCE,
                CjSourceKind.DECLARATION -> cjParsing.parseFile()
            }

            // 记录的是本次 parser 的真实来源；普通宏新 token 的空模块不得从词法宿主重新推导。
            if (psiFile is CjFile) psiFile.parsedLanguageModuleName = cjParsing.languageModuleName
            return psiBuilder.treeBuilt
        }

        @JvmStatic
        @JvmOverloads
        fun parseBlockExpression(psiBuilder: PsiBuilder, languageModuleName: String = ""): ASTNode {
            psiBuilder.setDebugMode(true)
            val cjParsing: CangJieParsing =
                createForTopLevel(
                    SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder),
                    languageModuleName,
                )

            with(ParsingContext.DEFAULT) {
                cjParsing.parseBlockExpression()
            }

            return psiBuilder.treeBuilt
        }
    }
}
