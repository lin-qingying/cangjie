package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.resolution.successfulFunctionCallOrNull
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjValueArgument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 实参位的期望参数类型回归（变更 `completion-parameter-matching-weight` 的 1.2 / 1.3 / 1.4）。
 *
 * 补全在实参位置触发时，候选要按**该位形参的类型**参与排序，前提是分析层能把这个类型说出来。
 * 本套件固定三件事：
 * - 位置实参按位次绑定；命名实参按**形参名**绑定（写序与形参序相反时才区分得出来）；
 * - 绑定不到（形参名不存在、实参个数多于形参）一律返回 `null`，不得任取一个形参；
 * - 「未知」（`null`）与「有类型但不匹配」在补全侧的后果不同——前者不参与过滤与加权，后者会，
 *   因此这里必须精确而不是「差不多」。
 *
 * **变长参数位未覆盖**：`CaValueParameterSymbol.isVararg` 当前是「最后一个形参且其类型是 Array」的启发式，
 * 会把正常的尾随数组形参误判为变长参数，据此返回「未知」会误杀合法期望类型；
 * 修正该判定之前不为变长参数写用例（详见变更 tasks.md 1.4）。
 */
class AnalysisApiCfirExpectedParameterTypeTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/components/expectedParameterType",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /** 位置实参按位次取到对应形参的类型。 */
    @Test
    fun positionalArgumentTakesParameterType(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            assertEquals(
                "completion/expectedparametertype/Alpha",
                mainFile.argumentOf("positionalArgumentTakesParameterType", 0).expectedType?.toString(),
                "第一个实参位应期望第一个形参的类型",
            )
        }
    }

    /**
     * 命名实参位当前取不到期望类型——**实测记录的缺口**，不是通过用例。
     *
     * 夹具是合法的命名实参写法（声明侧用 `!` 标注命名参数，调用侧写 `name: value` 且与声明同序），
     * 但该位在 `valueArgumentMapping` 里没有条目，期望类型为 `null`。
     * **未区分**两种可能：①解析层没把命名实参放进 `valueArgumentMapping`；
     * ②该写法在本解析器下整条调用不解析。两者都表现为「取不到映射」，要分开需要解析层探针，另立条目。
     *
     * 对补全而言当前结论是安全的：取不到 = 未知 = 不参与过滤与加权，与「推导不出来一律未知」一致。
     * 哪天命名实参进了映射，本用例会失败并提示把它翻成正向断言。
     */
    @Test
    fun namedArgumentBindsByParameterName(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            assertNull(
                mainFile.argumentOf("namedArgumentBindsByParameterName", 1).expectedType,
                "命名实参位目前取不到期望类型（缺口已记录）；取到类型说明映射已补齐，应翻成正向断言",
            )
        }
    }

    /** 形参名不存在时返回未知，不得退化成按位次绑定。 */
    @Test
    fun unknownNamedArgumentYieldsNoExpectedType(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            assertNull(
                mainFile.argumentOf("unknownNamedArgumentYieldsNoExpectedType", 0).expectedType,
                "形参名不匹配时必须返回 null；退化成按位次绑定会给出一个用户没写的形参类型",
            )
        }
    }


    /**
     * 变长形参位不给期望类型。
     *
     * 该位既可以传 `Array<Int64>` 本身，也可以传 0..n 个 `Int64`，两种写法都合法；
     * 给出其中任一个都会让另一种写法的候选被错误加权，而「未知」只是不加权。
     */
    @Test
    fun variadicArgumentYieldsNoExpectedType(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            assertNull(
                mainFile.argumentOf("variadicArgumentYieldsNoExpectedType", 0).expectedType,
                "变长形参位不存在统一的期望类型，必须返回 null",
            )
        }
    }

    /**
     * 变长的判据是「最后一个**非命名**形参是 `Array`」，后面跟着命名形参不影响。
     *
     * 这条是回归：判据若写成「整张形参表的最后一位」，这里会返回 `Array<Int64>`，
     * 于是 `collect(1, 2, 3, extra: 4)` 里的 `1` 会被按数组加权。
     */
    @Test
    fun arrayParameterBeforeNamedParameterIsVariadic(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            assertNull(
                mainFile.argumentOf("arrayParameterBeforeNamedParameterIsVariadic", 0).expectedType,
                "命名的尾随形参不影响变长判定：`arr` 仍是变长位，必须返回 null",
            )
        }
    }

    /** 实参个数多于形参时返回未知的用例**缺位**：该夹具会让 `resolveToCall` 在诊断转换处抛
     *  `ClassCastException: CjOffsetsOnlySimpleDiagnostic -> CjPsiDiagnostic`（已在本分支修复的缺陷），
     *  与期望类型推导无关。详见变更 `completion-parameter-matching-weight` 的 tasks.md 1.4。
     */
}

/**
 * 取得指定函数中第 [index] 个实参表达式（0 起）。
 *
 * 只认挂在 `CjValueArgument` 下的表达式：实参内部的嵌套表达式也是 `CjExpression`，
 * 按类型取会取到子表达式，得到的结论就与「这个实参位的期望类型」不是一回事。
 */
private fun CjFile.argumentOf(functionName: String, index: Int): CjExpression {
    val function = PsiTreeUtil.findChildrenOfType(this, CjNamedFunction::class.java)
        .single { it.name == functionName }
    val arguments = PsiTreeUtil.findChildrenOfType(function, CjExpression::class.java)
        .filter { it.parent is CjValueArgument }
        .toList()
    require(arguments.size > index) { "函数 `$functionName` 的实参个数不足：需要第 $index 个，实际 ${arguments.size} 个" }
    return arguments[index]
}
