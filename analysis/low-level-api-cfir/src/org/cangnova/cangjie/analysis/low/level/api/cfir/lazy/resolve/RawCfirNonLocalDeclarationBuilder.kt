/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.CfirElementFinder
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.codeFragment
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement

/**
 * 对齐 Kotlin `RawFirNonLocalDeclarationBuilder` 的职责边界：
 * 给定一个非局部 PSI 根节点，重建其所属文件的 raw CFIR，并从中取回目标声明。
 *
 * 仓颉主干没有 Kotlin FIR 的 anonymous initializer / enum entry / backing field /
 * delegated-constructor 多分支声明形态，因此这里直接复用现有文件级 raw rebuild +
 * declaration finder 主干能力，而不是继续保留上游漂移的专用 builder 路径。
 */
internal object RawCfirNonLocalDeclarationBuilder {
    /**
     * 以 [rootNonLocalDeclaration] 所在文件为单位重建 raw CFIR，并返回 designation 对应的非局部声明。
     *
     * 函数符号复绑由后续替换流程完成；本函数只保证从同一 PSI 根重新构造出结构等价的 CFIR 声明。
     */
    fun buildWithFunctionSymbolRebind(
        session: CfirSession,
        scopeProvider: CfirScopeProvider,
        designation: CfirDesignation,
        rootNonLocalDeclaration: CjElement,
    ): CfirDeclaration {
        check(rootNonLocalDeclaration is CjDeclaration || rootNonLocalDeclaration is CjCodeFragment)

        val rebuiltFile = PsiRawCfirBuilder(
            session,
            scopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL,
        ).buildCfirFile(rootNonLocalDeclaration.containingCjFile)

        return when (rootNonLocalDeclaration) {
            is CjCodeFragment -> rebuiltFile.codeFragment
            is CjDeclaration -> rebuiltDeclaration(rebuiltFile, designation, rootNonLocalDeclaration)
            else -> errorWithCfirSpecificEntries(
                "Unexpected non-local declaration root",
                psi = rootNonLocalDeclaration,
                cfir = designation.target,
            )
        }
    }

    /**
     * 从 [rebuiltFile] 中查找 [declaration] 对应的重建 CFIR 声明。
     *
     * 找不到时附带原 PSI 与旧 CFIR target 抛出错误，便于定位 raw rebuild 与 declaration finder 的结构偏差。
     */
    private fun rebuiltDeclaration(
        rebuiltFile: CfirFile,
        designation: CfirDesignation,
        declaration: CjDeclaration,
    ): CfirDeclaration {
        return CfirElementFinder.findDeclaration(rebuiltFile, declaration)
            ?: errorWithCfirSpecificEntries(
                "No rebuilt CFIR declaration was found for non-local PSI",
                psi = declaration,
                cfir = designation.target,
            )
    }
}
