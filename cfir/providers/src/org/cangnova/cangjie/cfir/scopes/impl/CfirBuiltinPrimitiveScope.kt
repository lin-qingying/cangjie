package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOriginScope
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.isExposedBuiltinClassifier
import org.cangnova.cangjie.name.Name

/**
 * 仓颉 primitive classifier 的全局名称 scope。
 *
 * 官方 parser 将 `UInt32.foo()` 这类语法直接识别为 primitive type expression；primitive
 * 不是用户 import 得到的 `std.core` 声明，不能依赖默认 `std.core.*` scope 才能解析。这里
 * 以独立 scope 暴露 primitive 的合成 class-like symbol，使类型限定符、构造调用和普通
 * primitive 成员访问都通过同一个名称解析入口工作。
 */
class CfirBuiltinPrimitiveScope(
    private val session: CfirSession,
) : CfirScope(), CfirLookupOriginScope {

    private val symbolsByName: Map<Name, org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol<*>> by lazy {
        PrimitiveTypeKind.entries
            .filter(PrimitiveTypeKind::isExposedBuiltinClassifier)
            .mapNotNull { kind ->
                session.symbolProvider
                    .getClassLikeSymbolByClassId(kind.classId)
                    ?.let { Name.identifier(kind.typeName) to it }
            }
            .toMap()
    }

    override val lookupOrigin: CfirLookupOrigin = CfirLookupOrigin.DEFAULT_IMPORT

    override val scopeOwnerLookupNames: List<String>
        get() = symbolsByName.keys.map(Name::asString)

    override fun mayContainName(name: Name): Boolean = name in symbolsByName

    override fun processClassifiersByName(
        name: Name,
        processor: (org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol<*>) -> Unit,
    ) {
        symbolsByName[name]?.let(processor)
    }

    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: ScopeSession,
    ): CfirScope = CfirBuiltinPrimitiveScope(newSession)
}
