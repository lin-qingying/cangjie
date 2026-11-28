package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperatorSignature
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceElementKind
import org.cangnova.cangjie.source.text

/**
 * 完成解析的同代来源节点事实。捕获器、linkage 分析与 CJO 内容写出共用这一不可变产品；
 * 节点对象只承担身份，子边、类型、引用及应用后签名单独冻结，发布前统一核验来源代次。
 */
data class CfirExportSourceElement(
    val identity: CfirElement,
    val context: CfirDeclaration,
    val children: List<CfirElement>,
    val type: ConeCangJieType?,
    val resolvedType: ConeCangJieType?,
    val target: CfirDeclaration?,
    val calleeReference: CfirReference?,
    val source: CjSourceElement?,
    val appliedReturnType: ConeCangJieType?,
    val appliedCallSiteType: ConeCangJieType?,
    val appliedParameterTypes: List<ConeCangJieType>,
    val builtinSignature: BuiltinPrimitiveOperatorSignature?,
    /** 节点角色的专属不可变输入；声明与类型引用由各自的冻结产品拥有。 */
    val payload: CfirExportSourcePayload?,
    /** 位置及 literal 拼写在捕获时取值；节点身份不授予消费者重读活动 PSI/LightTree 的能力。 */
    val span: CfirExportSourceSpan?,
    /** parser 拥有的注解参数仍完整冻结语法边，但不承诺普通正文的已解析类型或 callee。 */
    val isSyntax: Boolean = false,
)

/** 源锚点的冻结值；只为 literal 保留拼写，避免每个语法祖先重复持有整段正文。 */
data class CfirExportSourceSpan(
    val startOffset: Int,
    val endOffset: Int,
    val kind: CjSourceElementKind,
    val text: String?,
) {
    companion object {
        internal fun capture(source: CjSourceElement?, literalSpelling: Boolean): CfirExportSourceSpan? = source?.let {
            CfirExportSourceSpan(it.startOffset, it.endOffset, it.kind, if (literalSpelling) it.text?.toString() else null)
        }
    }
}

/** 类型来源的同代绑定及依赖边；写出闭包不能重新查询活动 symbol provider 或 alias 展开缓存。 */
data class CfirExportTypeElement(
    /** Cone 对象仅作为本次捕获身份；类型选择和编码只读取以下冻结字段。 */
    val identity: ConeCangJieType,
    /** 类型绑定的捕获域独立于声明所属 session；不能在填表时切换域重新选择 payload。 */
    val domain: CfirSession,
    val payload: CfirExportTypePayload,
    val children: List<ConeCangJieType>,
    val declaration: CfirDeclaration?,
    val abbreviatedDeclaration: CfirDeclaration?,
    val abbreviatedType: ConeCangJieType?,
    val expandedType: ConeCangJieType,
    val expandedDeclaration: CfirDeclaration?,
    /** ObtainsAliasType 的节点错误分支与编码器无法表示的类型分别表达。 */
    val containsError: Boolean,
)
