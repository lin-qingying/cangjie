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

import org.cangnova.cangjie.lexer.CangJieLexer
import java.io.File

/**
 * 词法测试基类：`psi/testData/lexer` 下的 `.cj` 由 TestGeneratorForPsi 生成入口。
 *
 * 每个用例将源码输入 [CangJieLexer]，把完整 token 序列（含空白与注释）与
 * 同目录 sidecar `<name>.cj.tokens.txt` 比对。sidecar 缺失或不一致时，
 * 可用 `-Dupdate.test.data=true` 重新生成。
 *
 * 词法测试只依赖 Flex lexer，不需要 IntelliJ 应用 / PSI fixture，因此保持轻量。
 */
abstract class AbstractCjLexerTest {

    /**
     * 词法测试入口：lex 全量 token 并与 golden sidecar 比对。
     */
    protected fun runTest(testDataFilePath: String) {
        val file = resolveTestDataPath(testDataFilePath)
        val text = file.readText(Charsets.UTF_8).replace("\r\n", "\n")
        val actual = dumpTokens(text)
        val goldenFile = File(file.path.removeSuffix(".cj") + ".cj.tokens.txt")
        assertEqualsToFile(goldenFile, actual)
    }

    /**
     * 输出全部 token，每行格式：`TOKEN_TYPE "escaped text"`。
     */
    protected fun dumpTokens(text: String): String {
        val lexer = CangJieLexer()
        lexer.start(text)
        val sb = StringBuilder()
        while (lexer.tokenType != null) {
            val tokenType = lexer.tokenType!!
            val tokenText = text.substring(lexer.tokenStart, lexer.tokenEnd)
            sb.append(tokenType.toString())
            sb.append(" \"")
            sb.append(escapeTokenText(tokenText))
            sb.append('"')
            sb.append('\n')
            lexer.advance()
        }
        return sb.toString().trimEnd()
    }

    private fun escapeTokenText(text: String): String = buildString(text.length + 8) {
        for (ch in text) {
            when (ch) {
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                else -> append(ch)
            }
        }
    }
}
