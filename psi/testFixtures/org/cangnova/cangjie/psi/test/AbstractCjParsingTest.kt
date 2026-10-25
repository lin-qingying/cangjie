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
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * 语法（解析）测试基类：`psi/testData/parsing` 下的 `.cj` 由 TestGeneratorForPsi 生成入口。
 *
 * 每个用例走与 IDE 一致的链路（[CangJieParserDefinition] + [CangJieFileType]）解析出 PSI，
 * 并断言树中不存在 [PsiErrorElement]——即语法被完整接受、无恢复性错误节点。
 *
 * 需要断言语义形状（而非仅 smoke）时，在 [runTest] 基础上扩展具体断言，
 * 或为对应 testData 文件单独写继承本类的测试类。
 */
abstract class AbstractCjParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
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
     * 语法测试入口：解析 testData 文件并断言无错误元素。
     */
    protected fun runTest(testDataFilePath: String) {
        val file = resolveTestDataPath(testDataFilePath)
        val text = file.readText(Charsets.UTF_8).replace("\r\n", "\n")
        val cjFile = createPsiFile(file.nameWithoutExtension, text) as CjFile
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
