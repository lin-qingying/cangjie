package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.idealExtendLookupTypes
import org.cangnova.cangjie.cfir.types.toPrimitiveTypeKindOrNull
import org.cangnova.cangjie.name.Name

/**
 * extend 成员 type scope。
 *
 * 官方 extend map 同时支持 nominal declaration 与 built-in type。这里以
 * [CfirExtendTargetKey] 为索引键，让 `CPointer<T>` / `CString` 这类没有
 * ClassId 的 built-in target 也能进入和普通类型相同的成员查询流程。
 */
class CfirExtendMemberScope(
    /**
     * 当前 scope 查询的 extend 目标 key，可以是 class-like 或 non-class built-in 类型。
     */
    private val targetKey: CfirExtendTargetKey,
    /**
     * 提供 extend 索引、可见性和包归属信息的 provider。
     */
    private val extendProvider: CfirExtendProvider,
    /**
     * 当前 use-site session。
     */
    private val session: CfirSession,
    /**
     * 成员查找时的实际 receiver 类型。
     */
    private val receiverType: ConeCangJieType,
    /**
     * 是否允许裸泛型静态 qualifier 暂缓 extend 泛型适用性判断。
     */
    private val allowBareGenericStaticQualifierExtends: Boolean = false,
    /**
     * 构造 extend 体内成员 scope 时需要排除的当前 extend 声明。
     */
    private val excludingExtend: CfirExtend? = null,
) : CfirTypeScope() {

    /**
     * 当前目标可见 extend 成员索引。
     */
    private val memberIndex: MemberIndex by lazy { buildIndex() }

    /**
     * 返回 extend 成员 callable 名称集合。
     */
    override fun getCallableNames(): Set<Name> = buildSet {
        addAll(memberIndex.functions.keys)
        addAll(memberIndex.properties.keys)
        addAll(memberIndex.variables.keys)
    }

    /**
     * 返回 extend 成员 classifier 名称集合。
     */
    override fun getClassifierNames(): Set<Name> = memberIndex.classifiers.keys

    /**
     * 按名称处理 extend classifier。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        memberIndex.classifiers[name]?.forEach(processor)
    }

    /**
     * 按名称处理 extend 函数。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        memberIndex.functions[name]?.forEach(processor)
    }

    /**
     * 按名称处理 extend 属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        memberIndex.properties[name]?.forEach(processor)
    }

    /**
     * 按名称处理所有 extend callable。
     */
    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        memberIndex.functions[name]?.forEach(processor)
        memberIndex.properties[name]?.forEach(processor)
        memberIndex.variables[name]?.forEach(processor)
    }

    /**
     * extend scope 自身不产生直接覆盖函数链。
     */
    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction = ProcessorAction.NONE

    /**
     * extend scope 自身不产生直接覆盖属性链。
     */
    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: CfirPropertySymbol,
        processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction = ProcessorAction.NONE

    /**
     * extend scope 不支持直接跨 session 替换。
     */
    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: org.cangnova.cangjie.cfir.ScopeSession,
    ): CfirTypeScope? = null

    /**
     * extend 成员索引。
     *
     * @property classifiers classifier 成员索引。
     * @property functions 函数成员索引。
     * @property properties 属性成员索引。
     * @property variables 字段变量成员索引。
     */
    private class MemberIndex(
        /**
         * extend classifier 成员索引。
         */
        val classifiers: Map<Name, List<CfirClassLikeSymbol<*>>>,
        /**
         * extend 函数成员索引。
         */
        val functions: Map<Name, List<CfirNamedFunctionSymbol>>,
        /**
         * extend 属性成员索引。
         */
        val properties: Map<Name, List<CfirPropertySymbol>>,
        /**
         * extend 字段变量成员索引。
         */
        val variables: Map<Name, List<CfirVariableSymbol<*>>>,
    )

    /**
     * 构建当前 receiver 可见的 extend 成员索引。
     */
    private fun buildIndex(): MemberIndex {
        val classifiers = HashMap<Name, MutableList<CfirClassLikeSymbol<*>>>()
        val functions = HashMap<Name, MutableList<CfirNamedFunctionSymbol>>()
        val properties = HashMap<Name, MutableList<CfirPropertySymbol>>()
        val variables = HashMap<Name, MutableList<CfirVariableSymbol<*>>>()

        val extends = extendsForTarget()
        for ((extend, concreteReceiverType) in extends) {
            if (extend === excludingExtend) continue
            if (!extend.isApplicableAtReceiver(concreteReceiverType)) continue
            for (declaration in extend.declarations) {
                indexDeclaration(
                    declaration = declaration,
                    classifiers = classifiers,
                    functions = functions,
                    properties = properties,
                    variables = variables,
                )
            }
        }
        return MemberIndex(classifiers, functions, properties, variables)
    }

    /**
     * 返回目标 key 与 ideal primitive 展开后对应的 extend 候选。
     */
    private fun extendsForTarget(): List<ExtendLookupCandidate> {
        val result = mutableListOf<ExtendLookupCandidate>()

        val primitiveKind = targetKey.classIdOrNull?.toPrimitiveTypeKindOrNull()
        if (primitiveKind != null) {
            /*
             * primitive qualifier 在符号层是 synthetic class-like，但官方 extend map
             * 以真实 builtin type 做适用性判断。先放入 ConePrimitiveType receiver，
             * 再合并 class-like 索引，避免 distinctBy 保留 synthetic receiver 后
             * 在 extend 目标匹配阶段把合法候选过滤掉。
             */
            val concreteReceiverTypes = receiverType.idealExtendLookupTypes.ifEmpty {
                listOf(ConePrimitiveType(primitiveKind))
            }
            for (concreteReceiverType in concreteReceiverTypes) {
                result += extendProvider.getExtendsForBuiltinType(concreteReceiverType.kind).map {
                    ExtendLookupCandidate(it, concreteReceiverType)
                }
            }
        }

        result += extendProvider.getExtendsForTarget(targetKey).map {
            ExtendLookupCandidate(it, receiverType)
        }

        return result.distinctBy { it.extend }
    }

    /**
     * extend member scope 必须和 use-site substitution/receiver 检查共享同一适用性规则。
     *
     * 只按目标 ClassId 建索引会把不满足泛型约束的 extend 成员暴露给调用解析；
     * 官方成员访问流程会在候选阶段删除这些目标。
     */
    private fun CfirExtend.isApplicableAtReceiver(concreteReceiverType: ConeCangJieType): Boolean {
        val targetPattern = extendedTypeRef.coneTypeOrNull ?: return false
        val substitution = createExtendDeclarationSubstitution(
            session = session,
            extend = this,
            targetPattern = targetPattern,
            concreteReceiverType = concreteReceiverType,
        )
        if (substitution != null) {
            return true
        }

        return canDeferBareGenericStaticQualifierApplicability(targetPattern, concreteReceiverType)
    }

    /**
     * `A.staticExtendMember(...)` 中的裸泛型类名 `A` 不携带实例类型实参。
     *
     * 官方调用检查会先保留该 static extend 成员，再把 extend 声明类型参数加入
     * 候选泛型映射，最终由调用实参/期望类型完成实例化。因此 scope 层不能在
     * `A<T>` 与裸 `A` 的目标匹配阶段提前删除候选。
     */
    private fun canDeferBareGenericStaticQualifierApplicability(
        targetPattern: ConeCangJieType,
        concreteReceiverType: ConeCangJieType,
    ): Boolean {
        if (!allowBareGenericStaticQualifierExtends) return false
        val bareReceiverType = concreteReceiverType as? ConeLookupTagBasedType ?: return false

        val patternType = targetPattern as? ConeLookupTagBasedType ?: return false
        if (patternType.typeArguments.isEmpty()) return false
        return patternType.classIdOrPrimitiveClassId == bareReceiverType.classIdOrPrimitiveClassId
    }

    /**
     * 将单个 extend 成员声明写入索引。
     */
    private fun indexDeclaration(
        declaration: CfirDeclaration,
        classifiers: HashMap<Name, MutableList<CfirClassLikeSymbol<*>>>,
        functions: HashMap<Name, MutableList<CfirNamedFunctionSymbol>>,
        properties: HashMap<Name, MutableList<CfirPropertySymbol>>,
        variables: HashMap<Name, MutableList<CfirVariableSymbol<*>>>,
    ) {
        when (declaration) {
            is CfirClassLikeDeclaration -> {
                val symbol = declaration.symbol as? CfirClassLikeSymbol<*> ?: return
                classifiers.getOrPut(symbol.name) { mutableListOf() }.add(symbol)
            }

            is CfirNamedFunction -> {
                val symbol = declaration.symbol ?: return
                val callableName = declaration.callableNameOrNull() ?: return
                functions.getOrPut(callableName) { mutableListOf() }.add(symbol)
            }

            is CfirProperty -> {
                val symbol = declaration.symbol as? CfirPropertySymbol ?: return
                properties.getOrPut(declaration.name) { mutableListOf() }.add(symbol)
            }

            is CfirFieldVariable -> {
                val symbol = declaration.symbol as? CfirVariableSymbol<*> ?: return
                variables.getOrPut(declaration.name) { mutableListOf() }.add(symbol)
            }

            else -> Unit
        }
    }

    /**
     * extend 查询候选。
     *
     * @property extend extend 声明。
     * @property concreteReceiverType 用于匹配该 extend 的具体 receiver 类型。
     */
    private data class ExtendLookupCandidate(
        /**
         * 当前查询命中的 extend 声明。
         */
        val extend: CfirExtend,
        /**
         * 用于匹配该 extend 的具体 receiver 类型。
         */
        val concreteReceiverType: ConeCangJieType,
    )
}
