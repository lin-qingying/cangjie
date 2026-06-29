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
 * Builtins session 使用的 CFIR provider。
 *
 * Builtins 声明来自 [symbolProvider]，不具备源码容器文件查询能力，因此容器相关 API 要么返回空结果，
 * 要么在不应调用的路径上直接报错。
 */
internal class LLCfirBuiltinsSessionProvider(
    /**
     * 提供 builtins class-like symbol 的底层 symbol provider。
     */
    override val symbolProvider: CfirSymbolProvider
) : CfirProvider() {
    /**
     * 通过 class id 从 builtins symbol provider 取得 class-like CFIR 声明。
     */
    override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? =
        symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir

    /**
     * Builtins session 不支持按 class id 查询容器文件。
     */
    override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile = shouldNotBeCalled()

    /**
     * Builtins session 中 classifier 没有源码容器文件。
     */
    override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? = null

    /**
     * Builtins session 中 callable 没有源码容器文件。
     */
    override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? = null

    /**
     * Builtins session 不按包暴露源码文件。
     */
    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> = emptyList()

    /**
     * Builtins session 不支持按包枚举 class 名称。
     */
    override fun getClassNamesInPackage(fqName: FqName): Set<Name> = shouldNotBeCalled()

    /**
     * 标记 builtins provider 中不应被调用的文件级查询路径。
     */
    private fun shouldNotBeCalled(): Nothing = error("Should not be called for LLCfirBuiltinsSession")
}
