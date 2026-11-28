package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.CfirRawDeclarationDataPolicies

/** helper 与原函数的非拥有关系；lazy 正文由原 owner 的正常重建事务物化。 */
data class CfirDefaultParameterHelperInfo(val owner: CfirFunction, val parameterIndex: Int)

var CfirNamedFunction.defaultParameterHelperInfo: CfirDefaultParameterHelperInfo? by
    CfirDeclarationDataRegistry.data(DefaultParameterHelperInfoKey)

internal object DefaultParameterHelperInfoKey : CfirDeclarationDataKey() {
    override val rawCopyPolicy = CfirRawDeclarationDataPolicies.referenceValue<CfirDefaultParameterHelperInfo>(
        "default parameter helper owner",
    ) { value, context ->
        value.copy(owner = context.state.referencedElement(value.owner))
    }
}

/**
 * 默认值的两个独立事实：HAS_INITIAL 与 FuncParamList.desugars 指向的辅助函数。
 * 辅助声明是 FuncDecl，不能把参数自身或默认值占位表达式当成该函数。
 */
data class CfirParameterDefaultValueMetadata(
    val hasInitial: Boolean,
    val function: CfirNamedFunction?,
)

/**
 * 参数原 assignment 的语法事实，在两条 raw builder 去除括号或改写负号之前保存。
 * literalConst 只证明原生 literal／ParenExpr 的 const 传播来源，不代表常量求值合法性。
 */
data class CfirDefaultAssignmentSyntaxFacts(
    val isLiteral: Boolean,
    val literalConst: Boolean,
    /** 语法归一化前的整个 assignment 范围；库声明没有此 SOURCE 输入。 */
    val source: org.cangnova.cangjie.source.CjSourceElement? = null,
)

/** 原始语法事实随参数 attributes 复制；缺少生产记录时不能把未知的 assignment const 当作 false。 */
var CfirValueParameter.defaultAssignmentSyntaxFacts: CfirDefaultAssignmentSyntaxFacts? by
    CfirDeclarationDataRegistry.data(DefaultAssignmentSyntaxFactsKey)

/** 在参数进入共享缓存前完整发布；供源码默认值降低阶段填充相同的事实。 */
var CfirValueParameter.defaultValueMetadata: CfirParameterDefaultValueMetadata? by
    CfirDeclarationDataRegistry.data(ParameterDefaultValueMetadataKey)

/** 自身 metadata 的拥有边与 effective default 的借用边分开；recipe 不替代 metadata 所有权。 */
val CfirValueParameter.ownedDefaultParameterHelper: CfirNamedFunction?
    get() = defaultValueMetadata?.function?.takeIf { helper ->
        helper.defaultParameterHelperInfo?.owner?.let { it.symbol === containingDeclarationSymbol } != false
    }

/** CJO 初始化位是权威事实；反序列化的占位表达式不能覆盖显式的 false。 */
val CfirValueParameter.hasInitialDefaultValue: Boolean
    get() = defaultValueMetadata?.hasInitial ?: (defaultValue != null)

internal object ParameterDefaultValueMetadataKey : CfirDeclarationDataKey() {
    override val rawCopyPolicy = CfirRawDeclarationDataPolicies.value<CfirParameterDefaultValueMetadata>(
        "parameter default helper",
        collect = { value, _, visit -> value.function?.let(visit) },
    ) { value, context ->
        value.copy(function = value.function?.let(context.state::copyOwned))
    }
}

internal object DefaultAssignmentSyntaxFactsKey : CfirDeclarationDataKey() {
    override val rawCopyPolicy = CfirRawDeclarationDataPolicies.value<CfirDefaultAssignmentSyntaxFacts>(
        "default assignment syntax",
    ) { value, context -> value.copy(source = value.source?.let(context::source)) }
}
