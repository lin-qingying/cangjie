package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 无额外子节点的 argument list 基类。
 *
 * 该类适用于空参数列表或不需要在树变换中重写参数节点的轻量 argument list 实现。
 */
abstract class CfirAbstractArgumentList : CfirArgumentList() {
    /**
     * 默认不转换参数，直接返回当前 argument list。
     */
    override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirArgumentList {
        return this
    }

    /**
     * 默认没有需要访问的子节点。
     */
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        // DO NOTHING
    }

    /**
     * 默认没有需要转换的子节点。
     */
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement {
        return this
    }
}

/**
 * 已完成实参到形参映射的 argument list。
 *
 * 解析后的调用不再只关心源码顺序中的表达式列表，还需要保存 value 参数映射、
 * context 参数映射和原始 argument list，以便 call completer、checker 与 renderer 共享同一结果。
 */
abstract class CfirResolvedArgumentList : CfirArgumentList() {
    /** 原列表和被改写的 context 实参仍由此调用拥有，来源迁移不能只读当前 mapping keys。 */
    @org.cangnova.cangjie.cfir.CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        super.acceptSourceChildren(visitor, data)
        originalArgumentList?.accept(visitor, data)
        mappingIncludingContextArguments.keys.forEach { it.accept(visitor, data) }
    }
    /**
     * 已解析参数列表沿用原始参数列表的源码位置。
     */
    final override val source: CjSourceElement?
        get() = originalArgumentList?.source

    /** 已解析列表的源始终由原始实参列表拥有，不能新增旁路源槽位。 */
    @org.cangnova.cangjie.cfir.CfirImplementationDetail
    final override fun replaceSource(newSource: CjSourceElement?) {
        val original = originalArgumentList
        if (original == null) {
            require(newSource == null)
            return
        }
        original.replaceSource(newSource)
    }

    /**
     * 解析前的原始参数列表；错误恢复或 synthetic 调用中可能为空。
     */
    abstract val originalArgumentList: CfirArgumentList?

    /**
     * **值实参** 到 **值形参** 的映射。
     *
     * 迭代顺序对应源码中的原始实参顺序，但跳过 context 实参。
     *
     * 若需要包含 context 实参的完整映射，使用 [mappingIncludingContextArguments]。
     */
    abstract val mapping: LinkedHashMap<CfirExpression, CfirValueParameter>

    /**
     * 所有显式实参到 context/value 形参的映射。
     *
     * 该映射包含显式 context 实参，但不包含隐式 context 实参；迭代顺序对应源码中的原始实参顺序。
     *
     * 若只需要值实参映射，使用 [mapping]。
     */
    abstract val mappingIncludingContextArguments: LinkedHashMap<CfirExpression, CfirValueParameter>

    /** C ellipsis 的完成实参角色；extra 没有 formal，不能进入错误映射。 */
    abstract val cVariadicArguments: CfirCVariadicArguments<CfirExpression>?
    /** 已实际执行 Array 变参语法糖；与 C ellipsis 的函数类型位无关。 */
    abstract val usesCangjieVariadicCall: Boolean

    /**
     * 按 value 参数映射顺序暴露实参表达式。
     */
    abstract override val arguments: List<CfirExpression>


    /**
     * 转换已解析参数列表中的实参表达式。
     */
    abstract override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirArgumentList

    /**
     * 转换实参表达式并返回当前节点。
     */
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement {
        transformArguments(transformer, data)
        return this
    }

    /**
     * 按解析后的实参顺序访问子表达式。
     */
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        for (argument in arguments) {
            argument.accept(visitor, data)
        }
    }

}

/**
 * 正常调用的已解析参数列表实现。
 *
 * @property originalArgumentList 解析前的原始参数列表。
 * @property mapping value 实参到 value 形参的映射。
 * @property mappingIncludingContextArguments 包含显式 context 实参的完整映射。
 */
internal class CfirResolvedArgumentListImpl(
    /**
     * 解析前的原始参数列表。
     */
    override val originalArgumentList: CfirArgumentList?,
    mapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
    mappingIncludingContextArguments: LinkedHashMap<CfirExpression, CfirValueParameter> = mapping,
    cVariadicArguments: CfirCVariadicArguments<CfirExpression>? = null,
    arguments: List<CfirExpression> = mapping.keys.toList() + cVariadicArguments?.extraArguments.orEmpty(),
    usesCangjieVariadicCall: Boolean = false,
) : CfirResolvedArgumentList() {
    private var valueArguments: List<CfirExpression> = java.util.Collections.unmodifiableList(arguments.toList())
    override val arguments: List<CfirExpression> get() = valueArguments
    override var cVariadicArguments: CfirCVariadicArguments<CfirExpression>? = cVariadicArguments
        private set
    override var usesCangjieVariadicCall: Boolean = usesCangjieVariadicCall
        private set

    /** grouped owned-copy 第二遍完成所有实参/形参身份后才发布成功列表。 */
    internal fun replaceCompletedArgumentsForOwnedCopy(
        arguments: List<CfirExpression>,
        cVariadicArguments: CfirCVariadicArguments<CfirExpression>?,
        usesCangjieVariadicCall: Boolean,
    ) {
        valueArguments = java.util.Collections.unmodifiableList(arguments.toList())
        this.cVariadicArguments = cVariadicArguments
        this.usesCangjieVariadicCall = usesCangjieVariadicCall
        validateCompletedArguments()
    }

    private fun validateCompletedArguments() {
        val cArguments = cVariadicArguments
        require(cArguments == null || !usesCangjieVariadicCall)
        if (cArguments != null) require(mapping.size == cArguments.fixedParameterCount) {
            "A successful C ellipsis call must bind every fixed parameter"
        }
        val completed = mapping.keys.toList() + cArguments?.extraArguments.orEmpty()
        require(completed.size == valueArguments.size && completed.indices.all { completed[it] === valueArguments[it] }) {
            "Completed value arguments must preserve the ordered fixed and C-extra roles"
        }
        require(cArguments?.extraArguments?.none { it in mapping } != false)
    }
    /** 参数映射及 context 映射的共享身份属于同一可恢复事务。 */
    internal fun captureMutableState(): CfirArgumentListMutableState {
        val originalMapping = mapping
        val originalContextMapping = mappingIncludingContextArguments
        val values = LinkedHashMap(originalMapping)
        val contextValues = LinkedHashMap(originalContextMapping)
        val originalArguments = valueArguments
        val originalCArguments = cVariadicArguments
        val originalVariadicKind = usesCangjieVariadicCall
        return object : CfirArgumentListMutableState {
            override fun restore() {
                originalMapping.clear()
                originalMapping.putAll(values)
                if (originalContextMapping !== originalMapping) {
                    originalContextMapping.clear()
                    originalContextMapping.putAll(contextValues)
                }
                mapping = originalMapping
                mappingIncludingContextArguments = originalContextMapping
                valueArguments = originalArguments
                cVariadicArguments = originalCArguments
                usesCangjieVariadicCall = originalVariadicKind
            }
        }
    }
    /**
     * 包含显式 context 实参的完整映射。
     */
    override var mappingIncludingContextArguments: LinkedHashMap<CfirExpression, CfirValueParameter> =
        mappingIncludingContextArguments
        private set

    /**
     * value 实参到 value 形参的映射。
     */
    override var mapping: LinkedHashMap<CfirExpression, CfirValueParameter> = mapping
        private set

    init { validateCompletedArguments() }

    /**
     * 转换映射 key 中的实参表达式，并保持原有形参映射关系。
     *
     * 每个实参表达式实例只 transform 一次：[mapping] 与
     * [mappingIncludingContextArguments] 默认构造为同一实例（或持有相同 key 实例），
     * 若各做一遍 mapKeys，同一实参子树会被重复变换——嵌套调用深度 d 时子树遍历
     * 次数按 2^d 增长（llt/function/nest_function_call.cj 的 40 层 `F(F(...))`
     * 即触发指数爆炸，全量回归挂死在该 fixture 上）。
     */
    override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirArgumentList {
        val transformedKeys = java.util.IdentityHashMap<CfirExpression, CfirExpression>()
        fun transformKeyOnce(key: CfirExpression): CfirExpression =
            transformedKeys.getOrPut(key) { key.transformSingle(transformer, data) }

        val newArguments = valueArguments.map(::transformKeyOnce)
        val newCArguments = cVariadicArguments?.map(::transformKeyOnce)

        val aliased = mappingIncludingContextArguments === mapping
        val newMapping = LinkedHashMap<CfirExpression, CfirValueParameter>(mapping.size)
        for ((key, parameter) in mapping) {
            newMapping[transformKeyOnce(key)] = parameter
        }
        mapping = newMapping
        if (aliased) {
            mappingIncludingContextArguments = newMapping
        } else {
            val newMappingIncludingContext =
                LinkedHashMap<CfirExpression, CfirValueParameter>(mappingIncludingContextArguments.size)
            for ((key, parameter) in mappingIncludingContextArguments) {
                newMappingIncludingContext[transformKeyOnce(key)] = parameter
            }
            mappingIncludingContextArguments = newMappingIncludingContext
        }
        valueArguments = java.util.Collections.unmodifiableList(newArguments)
        cVariadicArguments = newCArguments
        validateCompletedArguments()
        return this
    }
}

/**
 * 错误调用使用的已解析参数列表实现。
 *
 * 错误调用可能存在无法绑定到有效形参的实参，因此内部 [_mapping] 允许形参为空；
 * 对外暴露的 [mapping] 会过滤掉这些空形参条目。
 */
internal class CfirResolvedArgumentListForErrorCall(
    /**
     * 解析前的原始参数列表。
     */
    override val originalArgumentList: CfirArgumentList?,
    /**
     * 错误调用的原始实参映射，值为 `null` 表示该实参没有可用形参。
     */
    private var _mapping: LinkedHashMap<CfirExpression, out CfirValueParameter?>,
) : CfirResolvedArgumentList() {
    override val cVariadicArguments: CfirCVariadicArguments<CfirExpression>? get() = null
    override val usesCangjieVariadicCall: Boolean get() = false
    /** 复制器读取全部实参，包括没有形参绑定的错误项。 */
    internal val rawMappingForOwnedCopy: Map<CfirExpression, CfirValueParameter?>
        get() = _mapping

    /** 新节点壳完成第二遍后，发布同一份完整映射与过滤投影。 */
    internal fun replaceMappingForOwnedCopy(values: LinkedHashMap<CfirExpression, CfirValueParameter?>) {
        _mapping = values
        mapping = computeMapping()
    }

    /** 保留没有对应形参的错误实参与原映射顺序，不能只保存过滤后的 mapping。 */
    internal fun captureMutableState(): CfirArgumentListMutableState {
        val original = _mapping
        val values = LinkedHashMap(original)
        val resolved = mapping
        val resolvedValues = LinkedHashMap(resolved)
        return object : CfirArgumentListMutableState {
            override fun restore() {
                _mapping = LinkedHashMap(values)
                resolved.clear()
                resolved.putAll(resolvedValues)
                mapping = resolved
            }
        }
    }

    /**
     * 过滤空形参后的 value 实参映射。
     */
    override var mapping: LinkedHashMap<CfirExpression, CfirValueParameter> = computeMapping()
        private set

    /**
     * 错误调用没有单独的 context 映射，对外复用 [mapping]。
     */
    override val mappingIncludingContextArguments: LinkedHashMap<CfirExpression, CfirValueParameter>
        get() = mapping

    /**
     * 从 [_mapping] 中过滤掉没有形参的错误实参条目。
     */
    private fun computeMapping(): LinkedHashMap<CfirExpression, CfirValueParameter> {
        @Suppress("UNCHECKED_CAST")
        return _mapping.filterValues { it != null } as LinkedHashMap<CfirExpression, CfirValueParameter>
    }

    /**
     * 错误调用保留所有原始实参表达式，包括未绑定到形参的条目。
     */
    override val arguments: List<CfirExpression>
        get() = _mapping.keys.toList()

    /**
     * 转换错误调用中的所有实参表达式，并重新计算过滤后的映射。
     */
    override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirResolvedArgumentListForErrorCall {
        _mapping = _mapping.mapKeys { (k, _) -> k.transformSingle(transformer, data) } as LinkedHashMap<CfirExpression, CfirValueParameter?>
        mapping = computeMapping()
        return this
    }
}

/**
 * 构建正常调用的已解析参数列表。
 */
fun buildResolvedArgumentList(
    originalArgumentList: CfirArgumentList,
    mapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
    mappingIncludingContextArguments: LinkedHashMap<CfirExpression, CfirValueParameter> = mapping,
    cVariadicArguments: CfirCVariadicArguments<CfirExpression>? = null,
    arguments: List<CfirExpression> = mapping.keys.toList() + cVariadicArguments?.extraArguments.orEmpty(),
    usesCangjieVariadicCall: Boolean = false,
): CfirResolvedArgumentList {
    return CfirResolvedArgumentListImpl(originalArgumentList, mapping, mappingIncludingContextArguments, cVariadicArguments, arguments, usesCangjieVariadicCall)
}

/**
 * 构建错误调用的已解析参数列表。
 *
 * [mapping] 中的 `null` 形参会在对外 [CfirResolvedArgumentList.mapping] 中被过滤。
 */
fun buildArgumentListForErrorCall(
    originalArgumentList: CfirArgumentList,
    mapping: LinkedHashMap<CfirExpression, out CfirValueParameter?>,
): CfirResolvedArgumentList {
    return CfirResolvedArgumentListForErrorCall(originalArgumentList, mapping)
}
