package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `getExtendSymbols(receiverType)` 公开契约回归。
 *
 * 锁定按接收者类型查询 extend 的完整覆盖面：基本类型走内建目标、`CPointer` 走
 * 非 ClassId 内建目标键、typealias 按展开后的语义目标查询。三个形状都必须与
 * 编译器 extend 索引同源——按 `ClassId` 的旧入口对前两者天然查不到。
 */
class ExtendSymbolsByReceiverTypeTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/extendReceiversApi",
) {
    /**
     * 使用 standalone CFIR 配置运行公开 API 契约测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 按接收者类型查询必须命中基本类型与内建指针两种内建形状的 extend。
     */
    @Test
    fun extendSymbolsByReceiverType(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            // `.symbol` / `.returnType` 是 session 作用域扩展，只能在 analyzeForTest 块内联调用。
            val int64Receiver = mainFile.namedFunction("takesInt64")
                .symbol.valueParameters.single().returnType
            val pointerReceiver = mainFile.namedFunction("takesPointer")
                .symbol.valueParameters.single().returnType
            val aliasReceiver = mainFile.namedFunction("takesAlias")
                .symbol.valueParameters.single().returnType

            // ① 基本类型：按内建目标命中。
            assertTrue(
                getExtendSymbols(int64Receiver).isNotEmpty(),
                "Int64 接收者必须能查到 extend Int64",
            )

            // ② 内建泛型指针：按非 ClassId 内建目标键命中。
            assertTrue(
                getExtendSymbols(pointerReceiver).isNotEmpty(),
                "CPointer<Int64> 接收者必须能查到 extend<T> CPointer<T>",
            )

            // ③ typealias：按展开后的语义目标（Box<Int64>）查询；
            //    该目标在夹具中无 extend，必须为空集合而不是报错。
            assertTrue(
                getExtendSymbols(aliasReceiver).isEmpty(),
                "无 extend 的目标必须返回空集合",
            )
        }
    }

    /**
     * 命中的 extend 符号必须能暴露自己的声明成员（与补全通道的消费方式一致）。
     *
     * 基本类型目标会同时命中标准库的大量 extend（Comparable / Hashable 等），
     * 源码 extend 必须按声明成员定位而不是假设唯一。
     */
    @Test
    fun extendSymbolsByReceiverTypeExposeMembers(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            val int64Receiver = mainFile.namedFunction("takesInt64")
                .symbol.valueParameters.single().returnType
            val int64Extends = getExtendSymbols(int64Receiver)
            val fixtureExtend = int64Extends.first { extend ->
                extend.declaredMemberScope.callables(Name.identifier("doubled")).count() == 1
            }
            val memberCount = fixtureExtend.declaredMemberScope
                .callables(Name.identifier("doubled"))
                .count()
            assertEquals(1, memberCount, "extend Int64 的声明成员 doubled 必须可查询")
        }
    }
}

/**
 * 按名取夹具里的唯一命名函数（纯 PSI 查找，不涉及 session 扩展）。
 */
private fun CjFile.namedFunction(name: String): CjNamedFunction =
    PsiTreeUtil.findChildrenOfType(this, CjNamedFunction::class.java).single { it.name == name }
