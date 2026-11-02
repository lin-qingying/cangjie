package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic

/**
 * raw CFIR 构建期的类型诊断。
 *
 * 官方 parser 在语法写坏时产出 `InvalidType`（`src/Parse/ParseType.cpp:206-209`、
 * `include/cangjie/AST/Node.h:735-743`），错误类型本身不带消息；raw CFIR 需要一个稳定的
 * reason 让 PSI 与 LightTree 两条构建路径渲染一致，否则同一份源码两侧 golden 会分叉。
 */
object RawCfirTypeDiagnostics {
    /**
     * 类型引用存在但类型元素缺失的调试文案。
     *
     * 对应官方 `InvalidType`：语法上要求类型、也写出了类型引用的外壳，但没有解析出任何
     * 类型元素（例如类型参数列表缺右括号、`(x: Int64 = 1) -> Bool`）。与"类型注解整体
     * 缺席"不同，后者是 implicit type ref。
     */
    const val INVALID_TYPE: String = "Invalid type"

    /**
     * 构造 [INVALID_TYPE] 对应的 cone 诊断。
     *
     * 语法错误在解析阶段已经报给用户（`CjParsingBundle` 的解析诊断，如
     * `Unclosed delimiter: '('`），CFIR 侧不能再造一条语义诊断重复上报，否则同一个
     * 源码位置会出现"解析错误 + Invalid type"两条。仓库已有的表达是
     * [ConeUnreportedDuplicateDiagnostic]：诊断映射层对它直接返回空
     * （`coneDiagnosticToCfirDiagnostic.kt` 的 `toCfirDiagnostics` 首行），
     * 而 `ConeSimpleDiagnostic` 会走 `mapOtherDiagnostic` 的 reason 表 —— 依赖字符串表
     * 兜底太脆，这里显式声明"解析期已报过"。
     */
    fun invalidTypeDiagnostic(): ConeDiagnostic =
        ConeUnreportedDuplicateDiagnostic(ConeSimpleDiagnostic(INVALID_TYPE))
}
