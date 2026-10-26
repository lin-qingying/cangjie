package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassifierLookupTagWithFixedSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassifierLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.name.ClassId

/**
 * @see toSymbol
 */

/**
 * 在指定 use-site session 中把 classifier lookup tag 解析为对应的 classifier symbol。
 */
fun ConeClassifierLookupTag.toSymbol(useSiteSession: CfirSession): CfirClassifierSymbol<*>? {
    return when (this) {
        is ConeClassLikeLookupTag -> toSymbol(useSiteSession)
        is ConeClassifierLookupTagWithFixedSymbol -> this.symbol
        else -> error("missing branch for ${javaClass.name}")
    }
}

/**
 * 使用当前上下文 session 把 classifier lookup tag 解析为 classifier symbol。
 */
context(sessionHolder: SessionHolder)
fun ConeClassifierLookupTag.toSymbol(): CfirClassifierSymbol<*>? {
    return toSymbol(useSiteSession = sessionHolder.session)
}

/**
 * 在指定 session 中把 classifier lookup tag 解析为 class-like symbol；非 class-like 结果返回 `null`。
 */
fun ConeClassifierLookupTag.toClassLikeSymbol(useSiteSession: CfirSession): CfirClassLikeSymbol<*>? {
    return toSymbol(useSiteSession) as? CfirClassLikeSymbol<*>
}

/**
 * 使用当前上下文 session 把 classifier lookup tag 解析为 class-like symbol。
 */
context(sessionHolder: SessionHolder)
fun ConeClassifierLookupTag.toClassLikeSymbol(): CfirClassLikeSymbol<*>? {
    return toClassLikeSymbol(useSiteSession = sessionHolder.session)
}

/**
 * 在指定 use-site session 中把 class-like lookup tag 解析为 class-like symbol，并缓存实现 tag 的绑定结果。
 */
fun ConeClassLikeLookupTag.toSymbol(useSiteSession: CfirSession): CfirClassLikeSymbol<*>? {

    (this as? ConeClassLikeLookupTagImpl)?.boundSymbol?.takeIf { it.first === useSiteSession }?.let { return it.second }

    return useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId).also {
        (this as? ConeClassLikeLookupTagImpl)?.bindSymbolToLookupTag(useSiteSession, it)
    }
}

/**
 * 使用当前上下文 session 把 class-like lookup tag 解析为 class-like symbol。
 */
context(sessionHolder: SessionHolder)
fun ConeClassLikeLookupTag.toSymbol(): CfirClassLikeSymbol<*>? {
    return toSymbol(useSiteSession = sessionHolder.session)
}

/**
 * 在指定 session 中把 class-like lookup tag 解析为普通 class symbol。
 */
fun ConeClassLikeLookupTag.toClassSymbol(session: CfirSession): CfirClassSymbol? {
    return toSymbol(session) as? CfirClassSymbol
}

/**
 * 使用当前上下文 session 把 class-like lookup tag 解析为普通 class symbol。
 */
context(sessionHolder: SessionHolder)
fun ConeClassLikeLookupTag.toClassSymbol(): CfirClassSymbol? {
    return toClassSymbol(session = sessionHolder.session)
}

/**
 * 在指定 session 中把 class-like 类型的 lookup tag 解析为 class-like symbol。
 */
fun ConeClassLikeType.toSymbol(session: CfirSession): CfirClassLikeSymbol<*>? {
    return lookupTag.toSymbol(session)
}

/**
 * 使用当前上下文 session 把 class-like 类型解析为 class-like symbol。
 */
context(sessionHolder: SessionHolder)
fun ConeClassLikeType.toSymbol(): CfirClassLikeSymbol<*>? {
    return toSymbol(session = sessionHolder.session)
}

/**
 * 在指定 session 中把 lookup-tag-based 类型解析为 classifier symbol；非 lookup tag 类型返回 `null`。
 */
fun ConeCangJieType.toSymbol(session: CfirSession): CfirClassifierSymbol<*>? {
    return (this as? ConeLookupTagBasedType)?.lookupTag?.toSymbol(session)
}

/**
 * 使用当前上下文 session 把类型解析为 classifier symbol。
 */
context(sessionHolder: SessionHolder)
fun ConeCangJieType.toSymbol(): CfirClassifierSymbol<*>? {
    return toSymbol(session = sessionHolder.session)
}

/**
 * 在指定 session 中把类型解析为 class-like symbol。
 */
fun ConeCangJieType.toClassLikeSymbol(session: CfirSession): CfirClassLikeSymbol<*>? {
    return toSymbol(session) as? CfirClassLikeSymbol<*>
}

/**
 * 使用当前上下文 session 把类型解析为 class-like symbol。
 */
context(sessionHolder: SessionHolder)
fun ConeCangJieType.toClassLikeSymbol(): CfirClassLikeSymbol<*>? {
    return toClassLikeSymbol(session = sessionHolder.session)
}

/**
 * 把类型解析为其“成员表面声明侧 owner”的 class-like symbol。
 *
 * 与 [toClassLikeSymbol] 的差异：本入口覆盖内建目标身份。`ConePrimitiveType`
 * 不是 lookup-tag 类型，但 [classIdOrPrimitiveClassId] 给出的合成 [ClassId]
 * 在符号层对应 `CfirPrimitiveTypeSymbol`——官方编译器把基本类型同时暴露为
 * 合成 classifier 与真实 builtin type（`builtinTyToExtendMap`），两条身份必须
 * 还原到同一份声明，才能让“声明对照视图”与 use-site 成员表面共享 owner。
 *
 * `CPointer` / `CString` 没有 nominal 声明视图（`CfirExtendTargetKey` 专门为
 * 它们设计了非 ClassId 键），本入口对它们返回 `null`；其成员表面必须经
 * `typeToScope` 接缝按内建目标查询。
 */
fun ConeCangJieType.toOwningClassLikeSymbol(session: CfirSession): CfirClassLikeSymbol<*>? {
    toClassLikeSymbol(session)?.let { return it }
    return classIdOrPrimitiveClassId
        ?.let { classId -> session.symbolProvider.getClassLikeSymbolByClassId(classId) }
}

/**
 * 使用当前上下文 session 把类型解析为成员表面声明侧 owner。
 */
context(sessionHolder: SessionHolder)
fun ConeCangJieType.toOwningClassLikeSymbol(): CfirClassLikeSymbol<*>? {
    return toOwningClassLikeSymbol(session = sessionHolder.session)
}

/**
 * 在指定 session 中把类型解析为类型参数 symbol。
 */
fun ConeCangJieType.toTypeParameterSymbol(session: CfirSession): CfirTypeParameterSymbol? {
    return toSymbol(session) as? CfirTypeParameterSymbol
}

/**
 * 使用当前上下文 session 把类型解析为类型参数 symbol。
 */
context(sessionHolder: SessionHolder)
fun ConeCangJieType.toTypeParameterSymbol(): CfirTypeParameterSymbol? {
    return toTypeParameterSymbol(session = sessionHolder.session)
}

/**
 * 在指定 session 中把类型解析为普通 class symbol；typealias 会按 class-like 类型展开规则处理。
 */
fun ConeCangJieType.toClassSymbol(session: CfirSession): CfirClassSymbol? {
    return (this as? ConeClassLikeType)?.toClassSymbol(session)
}

/**
 * 使用当前上下文 session 把类型解析为普通 class symbol。
 */
context(sessionHolder: SessionHolder)
fun ConeCangJieType.toClassSymbol(): CfirClassSymbol? {
    return toClassSymbol(session = sessionHolder.session)
}

/**
 * 在指定 session 中完全展开 class-like 类型并解析最终普通 class symbol。
 */
fun ConeClassLikeType.toClassSymbol(session: CfirSession): CfirClassSymbol? {
    return (fullyExpandedType(session) as? ConeClassLikeType)?.toSymbol(session) as? CfirClassSymbol
}

/**
 * 使用当前上下文 session 完全展开 class-like 类型并解析最终普通 class symbol。
 */
context(sessionHolder: SessionHolder)
fun ConeClassLikeType.toClassSymbol(): CfirClassSymbol? {
    return toClassSymbol(session = sessionHolder.session)
}

/**
 * 在指定 session 中按 [ClassId] 查找 class-like symbol。
 */
fun ClassId.toSymbol(session: CfirSession): CfirClassLikeSymbol<*>? {
    return session.symbolProvider.getClassLikeSymbolByClassId(this)
}

/**
 * 使用当前上下文 session 按 [ClassId] 查找 class-like symbol。
 */
context(sessionHolder: SessionHolder)
fun ClassId.toSymbol(): CfirClassLikeSymbol<*>? {
    return toSymbol(session = sessionHolder.session)
}
