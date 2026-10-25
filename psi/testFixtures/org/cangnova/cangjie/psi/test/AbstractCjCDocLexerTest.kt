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

import org.cangnova.cangjie.lexer.cdoc.lexer.CDocLexer
import java.io.File

/**
 * CDoc（文档注释）词法测试基类：`psi/testData/cdoc` 下的 `.cdoc` 由 TestGeneratorForPsi 生成入口。
 *
 * 每个用例把整段文档注释（含起始与结束定界符）输入 [CDocLexer]，
 * 将 token 序列与同目录 sidecar `<name>.cdoc.tokens.txt` 比对。
 */
abstract class AbstractCjCDocLexerTest {

    /**
     * CDoc 词法测试入口：lex 全量 token 并与 golden sidecar 比对。
     */
    protected fun runTest(testDataFilePath: String) {
        val file = resolveTestDataPath(testDataFilePath)
        val text = file.readText(Charsets.UTF_8).replace("\r\n", "\n")
        val actual = dumpTokens(text)
        val goldenFile = File(file.path.removeSuffix(".cdoc") + ".cdoc.tokens.txt")
        assertEqualsToFile(goldenFile, actual)
    }

    /**
     * 输出全部 token，每行格式：`TOKEN_TYPE "escaped text"`。
     */
    protected fun dumpTokens(text: String): String {
        val lexer = CDocLexer()
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
