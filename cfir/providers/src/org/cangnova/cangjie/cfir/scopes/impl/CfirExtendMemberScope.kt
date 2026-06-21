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
 * Extend-member type scope.
 *
 * 官方 extend map 同时支持 nominal declaration 与 built-in type。这里以
 * [CfirExtendTargetKey] 为索引键，让 `CPointer<T>` / `CString` 这类没有
 * ClassId 的 built-in target 也能进入和普通类型相同的成员查询流程。
 */
class CfirExtendMemberScope(
    private val targetKey: CfirExtendTargetKey,
    private val extendProvider: CfirExtendProvider,
    private val session: CfirSession,
    private val receiverType: ConeCangJieType,
    private val allowBareGenericStaticQualifierExtends: Boolean = false,
) : CfirTypeScope() {

    private val memberIndex: MemberIndex by lazy { buildIndex() }

    override fun getCallableNames(): Set<Name> = buildSet {
        addAll(memberIndex.functions.keys)
        addAll(memberIndex.properties.keys)
        addAll(memberIndex.variables.keys)
    }

    override fun getClassifierNames(): Set<Name> = memberIndex.classifiers.keys

    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        memberIndex.classifiers[name]?.forEach(processor)
    }

    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        memberIndex.functions[name]?.forEach(processor)
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        memberIndex.properties[name]?.forEach(processor)
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        memberIndex.functions[name]?.forEach(processor)
        memberIndex.properties[name]?.forEach(processor)
        memberIndex.variables[name]?.forEach(processor)
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction = ProcessorAction.NONE

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: CfirPropertySymbol,
        processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction = ProcessorAction.NONE

    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: org.cangnova.cangjie.cfir.ScopeSession,
    ): CfirTypeScope? = null

    private class MemberIndex(
        val classifiers: Map<Name, List<CfirClassLikeSymbol<*>>>,
        val functions: Map<Name, List<CfirNamedFunctionSymbol>>,
        val properties: Map<Name, List<CfirPropertySymbol>>,
        val variables: Map<Name, List<CfirVariableSymbol<*>>>,
    )

    private fun buildIndex(): MemberIndex {
        val classifiers = HashMap<Name, MutableList<CfirClassLikeSymbol<*>>>()
        val functions = HashMap<Name, MutableList<CfirNamedFunctionSymbol>>()
        val properties = HashMap<Name, MutableList<CfirPropertySymbol>>()
        val variables = HashMap<Name, MutableList<CfirVariableSymbol<*>>>()

        val extends = extendsForTarget()
        for ((extend, concreteReceiverType) in extends) {
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

    private fun extendsForTarget(): List<ExtendLookupCandidate> {
        val result = mutableListOf<ExtendLookupCandidate>()
        result += extendProvider.getExtendsForTarget(targetKey).map {
            ExtendLookupCandidate(it, receiverType)
        }

        val primitiveKind = targetKey.classIdOrNull?.toPrimitiveTypeKindOrNull()
            ?: return result.distinctBy { it.extend }
        val concreteReceiverTypes = receiverType.idealExtendLookupTypes.ifEmpty {
            listOf(ConePrimitiveType(primitiveKind))
        }
        for (concreteReceiverType in concreteReceiverTypes) {
            result += extendProvider.getExtendsForBuiltinType(concreteReceiverType.kind).map {
                ExtendLookupCandidate(it, concreteReceiverType)
            }
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
        if (!extendProvider.isExtendAccessible(this)) return false
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
        if (bareReceiverType.typeArguments.isNotEmpty()) return false

        val patternType = targetPattern as? ConeLookupTagBasedType ?: return false
        if (patternType.typeArguments.isEmpty()) return false
        return patternType.classIdOrPrimitiveClassId == bareReceiverType.classIdOrPrimitiveClassId
    }

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

    private data class ExtendLookupCandidate(
        val extend: CfirExtend,
        val concreteReceiverType: ConeCangJieType,
    )
}
