package org.cangnova.cangjie.cfir.resolve.calls.tower

import org.cangnova.cangjie.cfir.calls.ReceiverValue
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 为 fresh type variable 接收者提供成员签名候选。
 *
 * 官方 Cangjie 在 lambda 参数类型未知时通过 `MemSig -> Mem2Decls` 反查拥有对应成员的声明，
 * 再把 lambda 参数类型变量约束到候选接收者构造器。CFIR 在 tower 层把这一步表达为一个
 * 专用 tower level：候选仍然来自普通 use-site member scope，后续参数、receiver、可见性检查
 * 继续走统一 call-resolution 管线。
 */
internal class CfirTypeVariableReceiverMemberScopeTowerLevel(
    /**
     * 构造成员作用域所需的 body resolve 组件集合。
     */
    private val components: BodyResolveComponents,
    /**
     * 当前调用的 dispatch receiver，类型必须是 fresh type variable 才会启用该 tower level。
     */
    private val dispatchReceiver: ReceiverValue,
) : CfirTowerLevel {
    /**
     * 按成员名缓存候选接收者类型作用域的 provider。
     */
    private val memberScopeProvider = CfirTypeVariableReceiverMemberScopeProvider(components)

    /**
     * 处理可调用成员查找。
     */
    override fun processCallablesByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        if (!dispatchReceiver.type.isFreshTypeVariable()) return ProcessResult.SCOPE_EMPTY
        return processScopes(info.name) { scope ->
            ScopeBasedTowerLevel(
                components = components,
                scope = scope,
                dispatchReceiver = dispatchReceiver,
            ).processCallablesByName(info, processor)
        }
    }

    /**
     * 处理函数成员查找。
     */
    override fun processFunctionsByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        if (!dispatchReceiver.type.isFreshTypeVariable()) return ProcessResult.SCOPE_EMPTY
        return processScopes(info.name) { scope ->
            ScopeBasedTowerLevel(
                components = components,
                scope = scope,
                dispatchReceiver = dispatchReceiver,
            ).processFunctionsByName(info, processor)
        }
    }

    /**
     * 依次在所有可能包含目标成员名的接收者作用域中执行处理。
     */
    private inline fun processScopes(
        name: Name,
        processScope: (CfirTypeScope) -> ProcessResult,
    ): ProcessResult {
        var result = ProcessResult.SCOPE_EMPTY
        for (scope in memberScopeProvider.memberScopesForName(name)) {
            result += processScope(scope)
        }
        return result
    }

    /**
     * 判断类型是否为尚未绑定原始类型参数的 fresh type variable。
     */
    private fun ConeCangJieType.isFreshTypeVariable(): Boolean =
        this is ConeTypeVariableType && typeConstructor.originalTypeParameter == null
}

/**
 * 为 fresh type variable 接收者按成员名收集可用的 use-site member scope。
 */
private class CfirTypeVariableReceiverMemberScopeProvider(
    /**
     * 访问符号 provider、session 和类型作用域构造依赖的组件集合。
     */
    private val components: BodyResolveComponents,
) {
    /**
     * 成员名到候选类型作用域列表的缓存。
     */
    private val scopesByName = hashMapOf<Name, List<CfirTypeScope>>()

    /**
     * 返回可能包含指定成员名的所有类型作用域。
     */
    fun memberScopesForName(name: Name): List<CfirTypeScope> =
        scopesByName.getOrPut(name) { collectMemberScopesForName(name) }

    /**
     * 扫描顶层分类器索引，收集声明了指定成员名的 use-site member scope。
     */
    private fun collectMemberScopesForName(name: Name): List<CfirTypeScope> {
        val packageNames = components.symbolProvider.symbolNamesProvider
            .getPackageNamesWithTopLevelClassifiers()
            ?: return emptyList()
        val result = mutableListOf<CfirTypeScope>()
        val seenClassIds = linkedSetOf<ClassId>()

        for (packageName in packageNames) {
            val packageFqName = FqName(packageName)
            val classifierNames = components.symbolProvider.symbolNamesProvider
                .getTopLevelClassifierNamesInPackage(packageFqName)
                .orEmpty()
            for (classifierName in classifierNames) {
                val classId = ClassId(packageFqName, classifierName)
                if (!seenClassIds.add(classId)) continue
                val classSymbol = components.symbolProvider.getClassLikeSymbolByClassId(classId) ?: continue
                val scope = createUseSiteMemberScope(classSymbol) ?: continue
                if (name in scope.getCallableNames()) {
                    result += scope
                }
            }
        }

        return result
    }

    /**
     * 为类样符号创建带 use-site 替换的成员作用域。
     */
    private fun createUseSiteMemberScope(classSymbol: CfirClassLikeSymbol<*>): CfirTypeScope? {
        if (!classSymbol.isBound) return null
        classSymbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val ownerType = classSymbol.declarationSelfType()
        val rawScope = CfirClassUseSiteMemberScope(
            session = components.session,
            classSymbol = classSymbol,
            symbolProvider = components.symbolProvider,
            extendProvider = components.session.extendProvider,
            directSupertypeProvider = components.session.directSupertypeProviderOrNull,
            ownerType = ownerType,
            dispatchReceiverType = ownerType,
            scopeKind = CfirClassMemberScopeKind.USE_SITE,
        )
        return CfirClassSubstitutionScope(
            session = components.session,
            useSiteMemberScope = rawScope,
            dispatchReceiverType = ownerType,
            substitutionOwnerType = ownerType,
        )
    }

    /**
     * 构造分类器声明自身对应的 owner type。
     */
    private fun CfirClassLikeSymbol<*>.declarationSelfType(): ConeCangJieType {
        val typeArguments: List<ConeTypeProjection> = cfir.typeParameters.map { typeParameter ->
            ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())
        }
        return constructType(typeArguments)
    }
}
