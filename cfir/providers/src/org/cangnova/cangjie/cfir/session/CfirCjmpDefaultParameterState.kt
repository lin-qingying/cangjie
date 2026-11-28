package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.defaultValueMetadata
import org.cangnova.cangjie.cfir.declarations.hasInitialDefaultValue
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.scopes.impl.typeAliasConstructorInfo
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.session.requiresDeferredCjmpFinalMatching
import org.cangnova.cangjie.resolve.calls.mpp.CjmpParameterDefaultState

/** 返回参数自己的默认值声明事实，不解析候选，也不触发 CJMP 阶段。 */
fun CfirValueParameter.declaredCjmpDefaultParameterState(): CjmpParameterDefaultState<CfirBasedSymbol<*>> {
    val metadata = defaultValueMetadata
    return CjmpParameterDefaultState(
        hasInitial = hasInitialDefaultValue,
        desugaredDefaultSource = metadata?.function?.symbol,
        canBeReadThrough = metadata?.function != null || metadata == null && defaultValue != null,
    )
}

/**
 * 返回参数对 owning callable 可见的默认值事实。
 * specific 参数自身没有默认值时，先解析其匹配阶段，再从该 callable 的不可变 family 结果读取转移事实。
 */
fun CfirValueParameter.cjmpDefaultParameterState(): CjmpParameterDefaultState<CfirBasedSymbol<*>> {
    val owner = containingDeclarationSymbol.cfir as? CfirCallableDeclaration
        ?: error("A parameter with a missing default must belong to a bound callable")
    val ownerParameters = when (owner) {
        is CfirFunction -> owner.valueParameters
        is CfirEnumConstructor -> owner.valueParameters
        else -> error("A value parameter must belong to a function or enum constructor")
    }
    val parameterIndex = ownerParameters.indexOfFirst { it === this }
    check(parameterIndex >= 0) {
        "A default-state query must use a parameter owned by its callable: " +
            "$symbol, callable=${owner.symbol}, parameterOwner=$containingDeclarationSymbol, parameterCount=${ownerParameters.size}"
    }

    // 签名副本按原 callable 和相同 ordinal 读取默认事实；HAS_INITIAL/stub 不授予原 helper 的拥有权。
    var originalOwner = owner
    val visited = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CfirCallableDeclaration, Boolean>())
    while (true) {
        check(visited.add(originalOwner)) { "A copied callable has cyclic default provenance" }
        val original = originalOwner.unwrapSubstitutionOverrides()
        if (original !== originalOwner) {
            originalOwner = original
            continue
        }
        val aliasOriginal = (originalOwner as? CfirFunction)?.typeAliasConstructorInfo?.originalConstructor
        if (aliasOriginal != null) {
            originalOwner = aliasOriginal
            continue
        }
        break
    }
    if (originalOwner !== owner) {
        val originalParameters = when (originalOwner) {
            is CfirFunction -> originalOwner.valueParameters
            is CfirEnumConstructor -> originalOwner.valueParameters
            else -> error("A copied value parameter must retain a function or enum constructor provenance")
        }
        check(originalParameters.size == ownerParameters.size) { "A copied callable must retain its original parameter ordinals" }
        return originalParameters[parameterIndex].cjmpDefaultParameterState()
    }
    val declaredState = declaredCjmpDefaultParameterState()
    if (declaredState.hasInitial) return declaredState

    val session = owner.moduleData.session
    if (owner.isLocal || !session.isCjmpSpecificCompilationEnabled() ||
        !owner.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)
    ) return declaredState

    val familyDefaults = if (session.requiresDeferredCjmpFinalMatching()) {
        session.cjmpFamilyAggregate.defaultParameterStatesForBootstrap(owner)
    } else {
        owner.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        checkNotNull(session.cjmpFamilyAggregate.matchingGroupFor(originalOwner).defaults[originalOwner.symbol]) {
            "A matched callable group must publish its ordered default states"
        }
    }
    return familyDefaults.getOrNull(parameterIndex)
        ?: error("A matched callable group must publish a default state for every parameter")
}
