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
 * This source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute it,
 * provided they adhere to the terms of the license.
 */

package org.cangnova.cangjie.psi.test

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * `.cj.d` 声明文件语法测试基类：`psi/testData/cjd` 下的 `.cj.d` 由 TestGeneratorForPsi 生成入口。
 *
 * 声明文件允许无体声明（抽象成员、接口成员、无体顶层函数），这是普通 `.cj` 不合法的形态；
 * 每个用例解析后断言树中不存在 [PsiErrorElement]。
 */
abstract class AbstractCjDeclarationParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = CangJieDeclarationFileType.DECLARATION_EXTENSION,
    fileType = CangJieDeclarationFileType,
    CangJieParserDefinition(),
) {

    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /**
     * 声明文件测试入口：解析 testData 文件并断言无错误元素。
     */
    protected fun runTest(testDataFilePath: String) {
        val file = resolveTestDataPath(testDataFilePath)
        val text = file.readText(Charsets.UTF_8).replace("\r\n", "\n")
        // 传入完整文件名：createPsiFile 仅在缺少声明扩展名时才追加，保证虚拟文件后缀为 .cj.d。
        val cjFile = createPsiFile(file.name, text) as CjFile
        assertNoErrorElements(cjFile, file.path)
    }

    private fun assertNoErrorElements(file: CjFile, path: String) {
        val errorElements = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        check(errorElements.isEmpty()) {
            val details = errorElements.joinToString("\n") { element ->
                "  [${element.textRange}] ${element.errorDescription}: ${element.text}"
            }
            "PSI contains ${errorElements.size} error element(s) in $path:\n$details"
        }
    }
}
