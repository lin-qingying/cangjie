package org.cangnova.cangjie.ide.completion.impl.shared.imports

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportPlan
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportTargetData
import org.cangnova.cangjie.ide.completion.api.lookup.ImportConflictKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
class PsiImportPlanApplierTest : CjParsingTestCase(
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
        val before = documentText(file)
        val report = applyPlans(file, mapOf("arrayList" to addImport("std.collection.ArrayList", "ArrayList")))
        assertEquals(listOf("std.collection.ArrayList"), report.skippedAsAlreadyImported)
        assertTrue(report.added.isEmpty())
        assertFalse(report.documentChanged)
        assertEquals(before, documentText(file))
    }

    /**
     * 冲突：短名被其它声明占用时不硬插 import，改走限定名并如实报告冲突。
     */
    @Test
    fun reportsShortNameConflictAndFallsBackToQualified() {
        val file = parse(FILE_WITH_FOO_IMPORT)
        val report = applyPlans(file, mapOf("otherFoo" to addImport("c.d.Foo", "c.d.Foo")))
        val conflict = report.conflicts.single()
        assertEquals(ImportConflictKind.SHORT_NAME_TAKEN_BY_OTHER, conflict.kind)
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
        val before = documentText(file)
        val report = applyPlans(
            file,
            mapOf(
                "otherFoo" to CompletionImportPlan.QualifyThenShorten(
                    CompletionImportTargetData(ImportPath.fromString("c.d.Foo"), "c.d.Foo"),
                ),
            ),
        )
        assertEquals("c.d.Foo", report.qualifiedExpressionText.values.single())
        assertTrue(report.added.isEmpty())
        assertEquals(before, documentText(file))
    }

    /**
     * 组织名与别名只出现在 import 文本里；无既有 import 时插在包声明之后。
     */
    @Test
    fun organizationAndAliasStayInImportText() {
        val file = parse(FILE_WITHOUT_IMPORTS)
        val report = applyPlans(file, mapOf("aliased" to addImport("org::pkg.Widget as W", "W")))
        assertEquals(listOf("org::pkg.Widget as W"), report.added)
        val text = documentText(file)
        assertTrue(text.contains("import org::pkg.Widget as W"), "组织名与别名必须原样写入 import，实际：\n$text")
        assertFalse(text.contains("W as W"), "表达式文本不得混入 import 文本")
    }

    /**
     * NoImport：不改文档、不报告任何内容。
     */
    @Test
    fun noImportPlanDoesNothing() {
        val file = parse(FILE_WITHOUT_IMPORTS)
        val before = documentText(file)
        val report = applyPlans(file, mapOf("widget" to CompletionImportPlan.NoImport))
        assertFalse(report.documentChanged)
        assertTrue(report.qualifiedExpressionText.isEmpty())
        assertEquals(before, documentText(file))
    }

    /**
     * 同包分组 import：新成员并入既有分组，而不是追加一行平行 import。
     *
     * 仓颉的 `import a.{b, c}` 与两条独立 import 语义等价，但前者是这个包在
     * 该文件里的既有书写形式；追加平行行会把它打散，review 时看着像重复导入。
     */
    @Test
    fun joinsExistingGroupedImport() {
        val file = parse(FILE_WITH_GROUPED_IMPORT)
        val report = applyPlans(file, mapOf("hashMap" to addImport("std.collection.HashMap", "HashMap")))
        assertEquals(listOf("std.collection.HashMap"), report.added)
        val text = documentText(file)
        assertTrue(
            text.contains("std.collection.{ArrayList, HashMap}") ||
                text.contains("std.collection.{ArrayList,HashMap}"),
            "新成员必须并入既有分组，实际 $text",
        )
        assertFalse(
            text.lines().any { it.trim() == "import std.collection.HashMap" },
            "不得同时追加平行 import，实际 $text",
        )
    }

    /**
     * 并组保持幂等：分组里已有该成员时不再改动文档。
     */
    @Test
    fun joiningGroupIsIdempotent() {
        val file = parse(FILE_WITH_GROUPED_IMPORT)
        val before = documentText(file)
        val report = applyPlans(file, mapOf("arrayList" to addImport("std.collection.ArrayList", "ArrayList")))
        assertEquals(emptyList(), report.added, "分组内已有成员时不得重复写入")
        assertTextEquals(before, documentText(file), "幂等场景下文档不得被改动")
    }

    /**
     * 不同包的 import 不得并入既有分组。
     */
    @Test
    fun doesNotJoinGroupAcrossPackages() {
        val file = parse(FILE_WITH_GROUPED_IMPORT)
        applyPlans(file, mapOf("option" to addImport("std.io.Option", "Option")))
        val text = documentText(file)
        assertTrue(
            text.contains("import std.io.Option"),
            "不同包的 import 必须独立成行，实际 $text",
        )
        assertFalse(
            text.contains("std.collection.{ArrayList, Option}"),
            "不得跨包并组，实际 $text",
        )
    }

    /**
     * 解析源码为仓颉文件。
     */
    private fun parse(text: String): CjFile = createPsiFile("imports", text) as CjFile

    /**
     * 文本相等断言。
     *
     * 刻意不写 `assertEquals(String, String, String)`：平台测试基类继承自 JUnit 3 的
     * `TestCase`，其 `assertEquals(String message, String expected, String actual)`
     * 是**成员函数**，在 Kotlin 里成员优先于导入，同名导入根本不会被选中——
     * 三个字符串参数会被静默按 (message, expected, actual) 解释，
     * 表现为「断言莫名其妙失败且实际值是一句提示语」。这里显式走 JUnit 5。
     */
    private fun assertTextEquals(expected: String, actual: String, message: String) =
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message)

    /**
     * 在 EDT 上读取文档文本。
     *
     * 轻量夹具的文档只在写命令内可用：测试线程直接读会拿到空文档，
     * 断言随之变成「文本莫名变化」。与 [applyPlans] 走同一条 EDT 路径。
     */
    private fun documentText(file: CjFile): String {
        var text = ""
        java.awt.EventQueue.invokeAndWait { text = documentOf(file).text }
        return text
    }

    /**
     * 取得文件文档；轻量夹具文件必须带文档，否则应用器无法写入。
     */
    private fun documentOf(file: CjFile): Document =
        requireNotNull(PsiDocumentManager.getInstance(project).getDocument(file)) { "夹具文件必须带文档" }

    /**
     * 导入写入必须落在调用方给的命令里，从而与候选文本共享一次撤销。
     *
     * 这是「完整支持单次撤销」的实际含义：补全插入是**一条**命令
     * （候选文本由平台替换、导入由应用器追加），若应用器自己开一条命令，
     * 用户按一次 Ctrl+Z 只会撤掉一半，留下一条指向不存在声明的 import。
     *
     * 断言的是**同一条命令**而不是撤销结果：撤销子系统在轻量解析夹具里没有注册，
     * `UndoManager.getInstance(project)` 直接返回 `null`，无法在这里真的按一次 Ctrl+Z。
     * 而「单次撤销」成立的条件恰恰是「两次写入属于同一条命令」——这条在夹具里可观察：
     * 应用器若自己开了命令，命令名就会被它改写。
     */
    @Test
    fun importWriteSharesTheEnclosingCommand() {
        val file = parse(FILE_WITH_IMPORT)
        var commandNameInsideApplier: String? = null

        java.awt.EventQueue.invokeAndWait {
            WriteCommandAction.runWriteCommandAction(
                project,
                ENCLOSING_COMMAND,
                null,
                Runnable {
                    PsiImportPlanApplier(project).apply(
                        file,
                        mapOf("k" to addImport("std.collection.HashMap", "HashMap")),
                    )
                    commandNameInsideApplier = CommandProcessor.getInstance().currentCommandName
                },
            )
        }

        assertTrue(
            documentText(file).contains("import std.collection.HashMap"),
            "写命令内必须真的写入 import",
        )
        // 用命名参数：`kotlin.test.assertEquals` 在「期望值与被测值都是字符串」时会选中
        // message-first 重载，位置参数会把断言消息当成被测值。
        assertEquals(
            expected = ENCLOSING_COMMAND,
            actual = commandNameInsideApplier,
            message = "应用器不得另开命令，否则一次撤销只会退回一半",
        )
    }

    /**
     * 在写命令内应用导入计划：应用器的调用契约就是写事务。
     *
     * PSI 文档写入要求 EDT + 命令上下文：测试工作线程必须切到 EDT，
     * 并用 WriteCommandAction 包裹，与产品里插入处理器所处的命令上下文一致。
     */
    private fun applyPlans(file: CjFile, plans: Map<String, CompletionImportPlan>): ImportApplyReport {
        var report: ImportApplyReport? = null
        java.awt.EventQueue.invokeAndWait {
            WriteCommandAction.runWriteCommandAction(project) {
                report = PsiImportPlanApplier(project).apply(file, plans)
            }
        }
        return requireNotNull(report) { "写命令必须产出报告" }
    }

    /**
     * 构造 AddImport 计划。
     */
    private fun addImport(importPath: String, expressionText: String): CompletionImportPlan =
        CompletionImportPlan.AddImport(
            CompletionImportTargetData(ImportPath.fromString(importPath), expressionText),
        )

    companion object {
        /** 外层命令名：应用器必须留在它里面。 */
        private const val ENCLOSING_COMMAND = "仓颉补全插入"

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

        /** 带一条分组 import 的夹具。 */
        private val FILE_WITH_GROUPED_IMPORT = """
            package completion.imports

            import std.collection.{ArrayList}

            func use(items: ArrayList<Int64>): Unit {}
        """.trimIndent()

        /** 无 import 的夹具：插入点应落到包声明之后。 */
        private val FILE_WITHOUT_IMPORTS = """
            package completion.imports

            func use(): Unit {}
        """.trimIndent()
    }
}
