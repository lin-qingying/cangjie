package org.cangnova.cangjie.ide.completion.impl.shared.imports

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionImportPlan
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionImportTargetData
import org.cangnova.cangjie.ide.completion.api.lookup.CaImportConflictKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 导入计划应用器回归。
 *
 * 覆盖三条不变量：
 * 1. 幂等：已导入路径不重复写入，也不产生空行；
 * 2. 冲突不静默：短名被其它声明占用时退回限定名并如实报告，而不是硬插一条二义 import；
 * 3. 组织名与别名只出现在 import 里：表达式文本保持分析期结论。
 *
 * 写入结果以报告与文档文本双重断言：只信报告会让「报告说写了、文档没变」漏过去。
 */
class CaPsiImportPlanApplierTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    /**
     * 初始化平台测试环境。
     */
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    /**
     * 释放平台测试环境。
     */
    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /**
     * 追加缺失的 import：写在最后一条 import 之后，报告与文档同时成立。
     */
    @Test
    fun addsMissingImportAfterLastDirective() {
        val file = parse(FILE_WITH_IMPORT)
        val report = applyPlans(file, mapOf("hashMap" to addImport("std.collection.HashMap", "HashMap")))
        assertEquals(listOf("std.collection.HashMap"), report.added)
        assertTrue(report.skippedAsAlreadyImported.isEmpty())
        assertTrue(report.documentChanged)
        assertTrue(
            documentOf(file).text.contains("import std.collection.ArrayList\nimport std.collection.HashMap"),
            "新 import 必须紧跟在最后一条 import 之后，实际：\n${documentOf(file).text}",
        )
    }

    /**
     * 幂等：已导入路径不重复写入，文档保持不变。
     */
    @Test
    fun skipsAlreadyImportedPath() {
        val file = parse(FILE_WITH_IMPORT)
        val before = documentOf(file).text
        val report = applyPlans(file, mapOf("arrayList" to addImport("std.collection.ArrayList", "ArrayList")))
        assertEquals(listOf("std.collection.ArrayList"), report.skippedAsAlreadyImported)
        assertTrue(report.added.isEmpty())
        assertFalse(report.documentChanged)
        assertEquals(before, documentOf(file).text)
    }

    /**
     * 冲突：短名被其它声明占用时不硬插 import，改走限定名并如实报告冲突。
     */
    @Test
    fun reportsShortNameConflictAndFallsBackToQualified() {
        val file = parse(FILE_WITH_FOO_IMPORT)
        val report = applyPlans(file, mapOf("otherFoo" to addImport("c.d.Foo", "c.d.Foo")))
        val conflict = report.conflicts.single()
        assertEquals(CaImportConflictKind.SHORT_NAME_TAKEN_BY_OTHER, conflict.kind)
        assertEquals("a.b.Foo", conflict.conflictingBinding)
        assertEquals("c.d.Foo", report.qualifiedExpressionText["otherFoo"])
        assertTrue(report.added.isEmpty(), "冲突时不得写入会造成二义的 import")
        assertFalse(documentOf(file).text.contains("import c.d.Foo"))
    }

    /**
     * QualifyThenShorten：只报告表达式文本，不动文档（缩短由后续引用缩短负责）。
     */
    @Test
    fun qualifyThenShortenWritesNothing() {
        val file = parse(FILE_WITH_FOO_IMPORT)
        val before = documentOf(file).text
        val report = applyPlans(
            file,
            mapOf(
                "otherFoo" to CaCompletionImportPlan.QualifyThenShorten(
                    CaCompletionImportTargetData(ImportPath.fromString("c.d.Foo"), "c.d.Foo"),
                ),
            ),
        )
        assertEquals("c.d.Foo", report.qualifiedExpressionText.values.single())
        assertTrue(report.added.isEmpty())
        assertEquals(before, documentOf(file).text)
    }

    /**
     * 组织名与别名只出现在 import 文本里；无既有 import 时插在包声明之后。
     */
    @Test
    fun organizationAndAliasStayInImportText() {
        val file = parse(FILE_WITHOUT_IMPORTS)
        val report = applyPlans(file, mapOf("aliased" to addImport("org::pkg.Widget as W", "W")))
        assertEquals(listOf("org::pkg.Widget as W"), report.added)
        val text = documentOf(file).text
        assertTrue(text.contains("import org::pkg.Widget as W"), "组织名与别名必须原样写入 import，实际：\n$text")
        assertFalse(text.contains("W as W"), "表达式文本不得混入 import 文本")
    }

    /**
     * NoImport：不改文档、不报告任何内容。
     */
    @Test
    fun noImportPlanDoesNothing() {
        val file = parse(FILE_WITHOUT_IMPORTS)
        val before = documentOf(file).text
        val report = applyPlans(file, mapOf("widget" to CaCompletionImportPlan.NoImport))
        assertFalse(report.documentChanged)
        assertTrue(report.qualifiedExpressionText.isEmpty())
        assertEquals(before, documentOf(file).text)
    }

    /**
     * 解析源码为仓颉文件。
     */
    private fun parse(text: String): CjFile = createPsiFile("imports", text) as CjFile

    /**
     * 取得文件文档；轻量夹具文件必须带文档，否则应用器无法写入。
     */
    private fun documentOf(file: CjFile) =
        assertNotNull(PsiDocumentManager.getInstance(project).getDocument(file), "夹具文件必须带文档")

    /**
     * 在写命令内应用导入计划：应用器的调用契约就是写事务。
     *
     * PSI 文档写入要求 EDT + 命令上下文：测试工作线程必须切到 EDT，
     * 并用 WriteCommandAction 包裹，与产品里插入处理器所处的命令上下文一致。
     */
    private fun applyPlans(file: CjFile, plans: Map<String, CaCompletionImportPlan>): CaImportApplyReport {
        var report: CaImportApplyReport? = null
        java.awt.EventQueue.invokeAndWait {
            WriteCommandAction.runWriteCommandAction(project) {
                report = CaPsiImportPlanApplier(project).apply(file, plans)
            }
        }
        return assertNotNull(report, "写命令必须产出报告")
    }

    /**
     * 构造 AddImport 计划。
     */
    private fun addImport(importPath: String, expressionText: String): CaCompletionImportPlan =
        CaCompletionImportPlan.AddImport(
            CaCompletionImportTargetData(ImportPath.fromString(importPath), expressionText),
        )

    companion object {
        /** 带一条 import 的夹具。 */
        private val FILE_WITH_IMPORT = """
            package completion.imports

            import std.collection.ArrayList

            func use(items: ArrayList<Int64>): Unit {}
        """.trimIndent()

        /** 短名 `Foo` 已被占用的夹具。 */
        private val FILE_WITH_FOO_IMPORT = """
            package completion.imports

            import a.b.Foo

            func use(foo: Foo): Unit {}
        """.trimIndent()

        /** 无 import 的夹具：插入点应落到包声明之后。 */
        private val FILE_WITHOUT_IMPORTS = """
            package completion.imports

            func use(): Unit {}
        """.trimIndent()
    }
}
