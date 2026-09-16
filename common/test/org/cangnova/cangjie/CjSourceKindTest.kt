package org.cangnova.cangjie

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 锁定 [CjSourceKind.fromFileName] 的后缀判定契约。
 *
 * 判定规则对齐官方 `HasCJDExtension`（`FileUtil.cpp`）：**字面后缀匹配**，且后缀之前必须存在
 * 非空文件名（等价于 `fileIt != fileEnd`）。这条"非空文件名"约束容易被忽略，
 * 而它决定了 `.cj.d` 本身**不是**声明文件 —— 缺少它会静默地把无主文件名判成声明文件。
 */
class CjSourceKindTest {
    /** 合法的 `.cj.d` 文件名判定为声明文件。 */
    @Test
    fun declarationFileNamesAreRecognized() {
        assertEquals(CjSourceKind.DECLARATION, CjSourceKind.fromFileName("a.cj.d"))
        assertEquals(CjSourceKind.DECLARATION, CjSourceKind.fromFileName("ohos.arkui.cj.d"))
        assertEquals(CjSourceKind.DECLARATION, CjSourceKind.fromFileName("a.cj.d.cj.d"))
    }

    /**
     * 不满足"非空文件名"或"逐字后缀"的输入一律退回 [CjSourceKind.SOURCE]。
     *
     * 与 v3 设计文档 6.3 的后缀判定矩阵逐条对应。
     */
    @Test
    fun nonDeclarationFileNamesFallBackToSource() {
        // 后缀之前没有文件主体：官方 HasCJDExtension 的 fileIt != fileEnd 检查
        assertEquals(CjSourceKind.SOURCE, CjSourceKind.fromFileName(".cj.d"))
        // 缺少 `.cj.d` 的分隔点
        assertEquals(CjSourceKind.SOURCE, CjSourceKind.fromFileName("a.cjd"))
        // 大小写敏感
        assertEquals(CjSourceKind.SOURCE, CjSourceKind.fromFileName("a.CJ.D"))
        assertEquals(CjSourceKind.SOURCE, CjSourceKind.fromFileName("a.Cj.D"))
        // 后缀不是行尾
        assertEquals(CjSourceKind.SOURCE, CjSourceKind.fromFileName("a.cj.d.txt"))
        // 普通实现文件
        assertEquals(CjSourceKind.SOURCE, CjSourceKind.fromFileName("a.cj"))
        assertEquals(CjSourceKind.SOURCE, CjSourceKind.fromFileName("a"))
        // 目录名里出现 `.cj.d` 不构成声明文件
        assertEquals(CjSourceKind.SOURCE, CjSourceKind.fromFileName("dir.cj.d/x.cj"))
    }

    /** 宏调用文件是独立的第三类，且优先于声明文件判定。 */
    @Test
    fun macroCallFileNamesAreRecognized() {
        assertEquals(CjSourceKind.MACRO_CALL, CjSourceKind.fromFileName("a.cj.macrocall"))
        assertEquals(CjSourceKind.MACRO_CALL, CjSourceKind.fromFileName("a.cj.d.cj.macrocall"))
        // 同样要求非空文件名
        assertEquals(CjSourceKind.SOURCE, CjSourceKind.fromFileName(".cj.macrocall"))
        assertEquals(CjSourceKind.SOURCE, CjSourceKind.fromFileName("a.macrocall"))
    }

    /** `isDeclaration` 只对声明文件为真。 */
    @Test
    fun isDeclarationOnlyMatchesDeclarationKind() {
        assertTrue(CjSourceKind.DECLARATION.isDeclaration)
        assertFalse(CjSourceKind.SOURCE.isDeclaration)
        assertFalse(CjSourceKind.MACRO_CALL.isDeclaration)
    }
}
