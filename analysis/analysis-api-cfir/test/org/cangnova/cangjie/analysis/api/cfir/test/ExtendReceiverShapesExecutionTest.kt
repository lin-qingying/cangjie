package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * extend 成员在全部接收者形状上的成员作用域可见性矩阵。
 *
 * 锁定 `CaType.scope` 家族入口的接收者覆盖完整性：基本类型、typealias、内建泛型指针、
 * 受上界约束的类型参数与普通 class-like 必须给出同一份成员结论。编译器调用解析走
 * `typeToScope` 接缝，本矩阵以同一接缝为对齐基线——任何一行退化成 null 都意味着
 * 成员通道与调用解析出现了新的不对称。
 */
@OptIn(CaExperimentalApi::class)
class ExtendReceiverShapesExecutionTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/extendReceivers",
) {
    /**
     * 使用 standalone CFIR 配置运行接收者形状矩阵。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 每个接收者形状必须能构造成员作用域，且 extend 注入的成员在其中可见。
     *
     * 预期失败面（修复前的基线）：基本类型、typealias、CPointer、受约束类型参数
     * 四行在 `CaType.scope` 上为 null；普通 class-like 一行为既有行为负对照。
     */
    @Test
    fun extendMemberVisibleAcrossReceiverShapes(mainFile: CjFile) {
        val expectations = listOf(
            // 接收者参数所在函数 → 必须可见的成员
            // 基本类型 / typealias / 内建泛型指针 / 受约束类型参数 → extend 注入成员
            "takesInt64" to "doubled",
            "takesShapedBox" to "shapedTag",
            "takesBounded" to "area",
            "takesPointer" to "pointeeTag",
            "takesAlias" to "unwrap",
        )

        analyzeForTest(mainFile) {
            for ((functionName, memberName) in expectations) {
                val receiverType = mainFile.function(functionName).symbol
                    .valueParameters.single().returnType

                val memberScope = receiverType.scope
                assertNotNull(
                    memberScope,
                    "`$functionName` 的接收者类型 ${receiverType::class.simpleName} 必须暴露成员作用域",
                )
                val members = memberScope!!.callables(Name.identifier(memberName)).toList()
                assertTrue(
                    members.isNotEmpty(),
                    "`$functionName` 的成员作用域必须包含 `$memberName`",
                )
            }
        }
    }

    /**
     * 负对照：不满足约束的实例上看不到受约束 extend 成员。
     *
     * `Box<Int64>` 不满足 `T <: Shaped`，`shapedTag` 不得出现；防止“全放行”式修复。
     * 正对照：同一类型上声明成员 `unwrap` 必须照常可见。
     */
    @Test
    fun extendMemberVisibleAcrossReceiverShapesNegative(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            val intBoxType = mainFile.function("takesBox").symbol
                .valueParameters.single().returnType

            val memberScope = intBoxType.scope
            assertNotNull(memberScope, "Box<Int64> 必须暴露成员作用域")
            assertTrue(
                memberScope!!.callables(Name.identifier("shapedTag")).toList().isEmpty(),
                "Box<Int64> 不满足 T <: Shaped，受约束 extend 成员不得出现",
            )
            assertTrue(
                memberScope.callables(Name.identifier("unwrap")).toList().isNotEmpty(),
                "正对照：Box<Int64> 的声明成员 unwrap 必须照常可见",
            )
        }
    }
}

/**
 * 按名取夹具里的命名函数。
 */
private fun CjFile.function(name: String): CjNamedFunction =
    PsiTreeUtil.findChildrenOfType(this, CjNamedFunction::class.java).single { it.name == name }
