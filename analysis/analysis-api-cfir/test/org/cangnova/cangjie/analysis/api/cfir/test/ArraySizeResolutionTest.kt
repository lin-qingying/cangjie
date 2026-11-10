package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * BINARIES 对照：`Array<Byte>.size` 必须能解析到属性符号。
 *
 * `CaCfirStandaloneAnalysisApiTestConfigurator` 的平台设置固定为 BINARIES，
 * 与 `ArraySizeStubsResolutionTest`（STUBS）成对，确保两条链路的结论可对照。
 */
class ArraySizeResolutionTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/diagnostics",
) {
    override val configurator = org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun arraySizeResolvesThroughBuiltinExtendMetadata(mainFile: CjFile) {
        // 说明：`CaCfirStandaloneAnalysisApiTestConfigurator` 的平台设置固定为 BINARIES，
        // 该用例是 **BINARIES 对照**；STUBS 的 `Array.size` 由
        // `ArraySizeStubsResolutionTest` 覆盖。
        assertEquals(
            org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin.BINARIES,
            org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings.getInstance(mainFile.project)
                .deserializedDeclarationsOrigin,
        )
        val resolved = analyzeForTest(mainFile) {
            val sizeExpr = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
                .firstOrNull { expression ->
                    expression.text == "size" && expression.parent?.text?.startsWith("bytes.size") == true
                }
            assertNotNull(sizeExpr, "fixture 必须包含 `bytes.size` 访问")
            // 符号对象不能逃出 analyze 生命周期，块内映射为类名字符串。
            sizeExpr!!.resolveToSymbols().map { symbol -> symbol::class.simpleName ?: "?" }
        }
        assertTrue(
            resolved.isNotEmpty(),
            "`Array<Byte>.size` 必须能解析到符号（builtins `.cjo` 的 extend 元数据必须可达）",
        )
        assertTrue(
            resolved.any { name -> name.contains("Property") },
            "`size` 必须解析为属性符号，实际：$resolved",
        )
    }
}
