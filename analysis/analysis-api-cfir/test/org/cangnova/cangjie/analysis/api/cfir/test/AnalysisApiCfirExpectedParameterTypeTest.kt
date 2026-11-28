package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.resolution.CaFunctionCall
import org.cangnova.cangjie.analysis.api.resolution.calls
import org.cangnova.cangjie.analysis.api.resolution.successfulFunctionCallOrNull
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.symbol
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
     * 命名实参（`name: value`）按形参名绑定时能取到该位的期望类型。
     *
     * 这条是回归：CFIR 的实参映射对命名实参给的是 `name: value` **节点**，
     * 它的 PSI 是 `CjValueArgument` 而不是表达式——投影时若不拆到值表达式，
     * 命名实参会被静默跳过，整个映射为空（实测：调用能解析、`isNamed()==true`，但 `valueArgumentMapping` size 0）。
     */
    @Test
    fun namedArgumentBindsByParameterName(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            assertEquals(
                "completion/expectedparametertype/Alpha",
                mainFile.argumentOf("namedArgumentBindsByParameterName", 1).expectedType?.toString(),
                "命名实参位必须能取到该形参的类型（取不到说明命名实参没进实参映射）",
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

    /**
     * `()` 运算符是官方例外中**允许**变长的两个之一。
     *
     * 断言走**真实调用**后的实参位：`callable(1, 2, 3)` 里第一个实参位没有统一的期望类型
     * （元素可以是 `Int64`，也可以整传 `Array<Int64>`）。若 `()` 被误当成「不许变长的运算符」，
     * 该位会返回 `Array<Int64>`——因此这条用例真的在判别运算符例外。
     *
     * **未覆盖**：除 `()`/`[]` 外的运算符（如 `+`）**不得**变长这一支没有用例。
     * 判别它需要在运算符使用点取到实参位，而二元写法 `a + b` 在公开解析 API 里拿不到实参映射；
     * 直接查声明符号又会因为形参类型未解析而一律得到 `false`（实测）——那样的用例会因为错误的原因通过，故不留。
     */
    @Test
    fun invokeOperatorParameterIsVariadic(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            assertNull(
                mainFile.argumentOf("invokeOperatorParameterIsVariadic", 0).expectedType,
                "`()` 运算符允许变长：该位不给期望类型（判成不许变长会返回 Array<Int64>）",
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
