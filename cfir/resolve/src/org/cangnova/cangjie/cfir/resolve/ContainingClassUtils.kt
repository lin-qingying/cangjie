/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.containingClassLookupTag
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol

/**
 * 基于声明站点 session 解析可调用声明所属外层类。
 */
fun CfirCallableDeclaration.getContainingClass(): CfirClass? =
    containingClassLookupTag()?.toClassSymbol(moduleData.session)?.cfir

/**
 * 返回声明所属的 class-like 符号；若不存在则返回 null。
 *
 * 解析始终基于声明站点 session，以保证 expect/actual、库符号与源码符号的宿主查询一致。
 */
fun CfirBasedSymbol<*>.getContainingClassSymbol(): CfirClassLikeSymbol<*>? {
    val session = cfir.moduleData.session
    return when (this) {
        is CfirCallableSymbol<*> -> session.cfirProvider.getContainingClass(this)
        is CfirClassLikeSymbol<*> -> session.cfirProvider.getContainingClass(this)
        else -> session.cfirProvider.getContainingClass(this)
    }
}

/**
 * 基于声明本身返回其所属 class-like 符号。
 */
fun CfirDeclaration.getContainingClassSymbol(): CfirClassLikeSymbol<*>? = when (this) {
    is CfirCallableDeclaration -> symbol.getContainingClassSymbol()
    is CfirClassLikeDeclaration -> symbol.getContainingClassSymbol()
    else -> null
}
