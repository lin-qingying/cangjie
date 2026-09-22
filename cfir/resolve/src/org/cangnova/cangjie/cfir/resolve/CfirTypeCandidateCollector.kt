package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.declarationAvailabilityProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.providers.lookupOriginForAccessibility
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.accessibilityChecker
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * 类型与构造目标 classifier 的统一候选收集器。
 *
 * scope 只提供结构符号、替换器与查找来源；本收集器在每个候选进入解析器时，使用完整
 * [CfirAccessContext] 调用 session 级可访问性服务。类型解析、tower classifier、限定符
 * 成员和构造调用因此不会再各自回放 package/import/provider 查找。
 */
internal class CfirTypeCandidateCollector(
    private val session: CfirSession,
    private val context: CfirAccessContext,
) {
    /** 单个 classifier 候选及其 use-site 结构元数据。 */
    data class TypeCandidate(
        val symbol: CfirClassifierSymbol<*>,
        val substitutor: ConeSubstitutor?,
        val lookupOrigin: CfirLookupOrigin,
    )

    /**
     * 返回第一个含可访问候选的 scope 层，并保留该层全部候选。
     *
     * 不可发现的 classifier 不会阻断低优先级 scope；同层多个可访问候选由上层解析器
     * 继续执行歧义处理，不能在 collector 内任意选择一个符号。
     */
    fun firstVisibleScopeCandidates(
        scopes: Iterable<CfirScope>,
        name: Name,
    ): List<TypeCandidate> {
        for (scope in scopes) {
            val candidates = candidatesFromScope(scope, name)
            if (candidates.isNotEmpty()) return candidates
        }
        return emptyList()
    }

    /**
     * 类型位置简单名在 scope 链上的解析结果。
     */
    sealed interface SimpleTypeNameLookup {
        /** 命中候选；保留首个含候选 scope 层级的全部可访问 classifier。 */
        data class Classifiers(val candidates: List<TypeCandidate>) : SimpleTypeNameLookup

        /**
         * 名字被更内层作用域中的非类型声明遮蔽。
         *
         * 官方 `PreCheck::GetTyFromASTType` 在筛掉非类型声明后 candidates 为空，
         * 而原始声明集合非空，此时报 `not_a_type`。
         */
        data object ShadowedByNonTypeDeclaration : SimpleTypeNameLookup

        /** 所有作用域都没有该名字；官方报 `undeclared_type_name`。 */
        data object NotDeclared : SimpleTypeNameLookup
    }

    /**
     * 按官方统一命名空间规则解析类型位置上的简单名。
     *
     * 官方先取同名声明的完整集合，再筛出类型声明，因此"本层没有 classifier"并不等于
     * "可以继续向外层找类型"：更内层的同名值成员或函数已经挡住了外层类型名。这里把
     * 遮蔽事实和候选一起返回，交给 [org.cangnova.cangjie.cfir.resolve.CfirTypeResolver]
     * 区分 `not_a_type` 与 `undeclared_type_name`。
     *
     * [exemptDeclarationTypeRef] 是当前正在解析的类型引用，对应官方 `IsNodeInVarDecl`：
     * 声明自身的签名允许引用与自身同名的外层类型（`var Foo: Foo`），不能被自己遮蔽。
     */
    fun lookupSimpleTypeName(
        scopes: Iterable<CfirScope>,
        name: Name,
        exemptDeclarationTypeRef: CfirTypeRef?,
    ): SimpleTypeNameLookup {
        for (scope in scopes) {
            val candidates = candidatesFromScope(scope, name)
            if (candidates.isNotEmpty()) return SimpleTypeNameLookup.Classifiers(candidates)
            if (bindsOwnNonTypeDeclaration(scope, name, exemptDeclarationTypeRef)) {
                return SimpleTypeNameLookup.ShadowedByNonTypeDeclaration
            }
        }
        return SimpleTypeNameLookup.NotDeclared
    }

    /**
     * 判断 [scope] 自身是否把 [name] 绑定到会遮蔽类型名的非类型声明。
     */
    private fun bindsOwnNonTypeDeclaration(
        scope: CfirScope,
        name: Name,
        exemptDeclarationTypeRef: CfirTypeRef?,
    ): Boolean {
        var bound = false
        scope.processOwnNonTypeBindingsByName(name) { symbol ->
            if (!bound && !symbol.isExemptFromShadowing(exemptDeclarationTypeRef)) {
                bound = true
            }
        }
        return bound
    }

    /**
     * 该绑定是否就是当前正在解析的声明自身（官方 `IsNodeInVarDecl` 的等价判定）。
     *
     * 声明自身的类型注解允许引用同名外层类型，因此必须从遮蔽判定中排除。identity 命中
     * 覆盖 `var Foo: Foo` 这类直接形态；偏移包含覆盖声明 source 已经物化的其余形态。
     */
    private fun CfirCallableSymbol<*>.isExemptFromShadowing(exemptDeclarationTypeRef: CfirTypeRef?): Boolean {
        if (exemptDeclarationTypeRef == null) return false
        val declaration = cfir
        if (declaration.returnTypeRef === exemptDeclarationTypeRef) return true
        val declarationSource = declaration.source ?: return false
        val referenceSource = exemptDeclarationTypeRef.source ?: return false
        return declarationSource.startOffset <= referenceSource.startOffset &&
            referenceSource.endOffset <= declarationSource.endOffset
    }

    /** 返回第一个可访问 classifier 候选。 */
    fun firstVisibleScopeCandidate(
        scopes: Iterable<CfirScope>,
        name: Name,
    ): TypeCandidate? = firstVisibleScopeCandidates(scopes, name).firstOrNull()

    /** 从一个确定 scope 中收集可访问 classifier，并保留 use-site substitutor。 */
    fun candidatesFromScope(
        scope: CfirScope,
        name: Name,
    ): List<TypeCandidate> {
        val lookupOrigin = scope.lookupOriginForAccessibility()
        val result = mutableListOf<TypeCandidate>()
        scope.processClassifiersByNameWithSubstitution(name) { symbol, substitutor ->
            if (isAccessible(symbol, lookupOrigin)) {
                result += TypeCandidate(symbol, substitutor, lookupOrigin)
            }
        }
        return result.distinctBy(TypeCandidate::symbol)
    }

    /**
     * 聚合已确定 [classId] 的完整重声明候选。
     *
     * [preferredCandidate] 保留首次 scope lookup 的真实 origin/substitutor；provider 冲突
     * 候选使用 [lookupOrigin]，但仍逐个经过同一 checker，不能因已知 ClassId 绕过访问控制。
     */
    fun collectClassIdCandidates(
        classId: ClassId,
        preferredCandidate: TypeCandidate?,
        lookupOrigin: CfirLookupOrigin,
    ): List<TypeCandidate> = buildList {
        preferredCandidate?.let(::add)
        session.declarationAvailabilityProvider.classLikeCandidates(classId).forEach { symbol ->
            add(
                TypeCandidate(
                    symbol = symbol,
                    substitutor = null,
                    lookupOrigin = lookupOrigin,
                )
            )
        }
    }.distinctBy(TypeCandidate::symbol).filter { candidate ->
        isAccessible(candidate.symbol, candidate.lookupOrigin)
    }

    /** classifier 以其真实查找来源进入唯一可访问性服务。 */
    private fun isAccessible(
        symbol: CfirClassifierSymbol<*>,
        lookupOrigin: CfirLookupOrigin,
    ): Boolean {
        val classLike = symbol as? CfirClassLikeSymbol<*> ?: return true
        val result = session.accessibilityChecker.checkClassLike(
            classLike,
            context.copy(
                lookupOrigin = lookupOrigin,
                kind = CfirAccessKind.TYPE,
            ),
        )
        return result is CfirAccessibilityResult.Accessible
    }
}
