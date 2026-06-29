/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Library session 使用的 CFIR provider。
 *
 * 库声明通过 [symbolProvider] 查询，库 session 不维护源码文件集合，因此文件容器相关 API 只返回空结果
 * 或在不允许调用的路径上报错。
 */
internal class LLCfirLibrarySessionProvider(
    /**
     * 提供库 class-like symbol 的底层 symbol provider。
     */
    override val symbolProvider: CfirSymbolProvider
) : CfirProvider() {
    /**
     * 通过 class id 从库 symbol provider 取得 class-like CFIR 声明。
     */
    override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? {
        return symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
    }

    /**
     * Library session 不支持按 class id 查询容器文件。
     */
    override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile = shouldNotBeCalled()

    /**
     * Library session 中 classifier 没有源码容器文件。
     */
    override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? = null

    /**
     * Library session 中 callable 没有源码容器文件。
     */
    override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? = null

    /**
     * Library session 不按包暴露源码文件。
     */
    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> = emptyList()

    /**
     * Library session 不支持按包枚举 class 名称。
     */
    override fun getClassNamesInPackage(fqName: FqName): Set<Name> = shouldNotBeCalled()

    /**
     * 标记 library provider 中不应被调用的文件级查询路径。
     */
    private fun shouldNotBeCalled(): Nothing = error("Should not be called for LLCfirLibrarySessionProvider")
}
