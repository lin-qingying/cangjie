package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.containingClassLookupTag
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol

/**
 * 基于声明站点 session 解析可调用声明所属名义类型。
 *
 * Kotlin 的 RegularClass 在仓颉中分别由 class、struct、interface、enum 表示，
 * 此处使用共同的 class-like 抽象，不丢弃值类型或枚举的真实声明归属。
 */
fun CfirCallableDeclaration.getContainingClass(): CfirClassLikeDeclaration? =
    containingClassLookupTag()?.toSymbol(moduleData.session)?.cfir

/**
 * 返回声明所属的 class-like 符号；若不存在则返回 null。
 *
 * 解析始终基于声明站点 session，以保证 expect/actual、库符号与源码符号的宿主查询一致。
 */
fun CfirBasedSymbol<*>.getContainingClassSymbol(): CfirClassLikeSymbol<*>? =
    cfir.moduleData.session.cfirProvider.getContainingClass(this)

/** 返回 callable 的所属类型；顶层 callable 则返回所属文件。 */
fun CfirCallableSymbol<*>.getContainingSymbol(session: CfirSession): CfirBasedSymbol<*>? =
    getContainingClassSymbol() ?: session.cfirProvider.getCfirCallableContainerFile(this)?.symbol

/**
 * 基于声明本身返回其所属 class-like 符号。
 */
fun CfirDeclaration.getContainingClassSymbol(): CfirClassLikeSymbol<*>? = symbol.getContainingClassSymbol()
