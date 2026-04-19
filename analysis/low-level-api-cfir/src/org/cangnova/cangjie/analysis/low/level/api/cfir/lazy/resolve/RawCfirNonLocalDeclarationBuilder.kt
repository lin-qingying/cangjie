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
                fir = designation.target,
            )
        }
    }

    private fun rebuiltDeclaration(
        rebuiltFile: CfirFile,
        designation: CfirDesignation,
        declaration: CjDeclaration,
    ): CfirDeclaration {
        return CfirElementFinder.findDeclaration(rebuiltFile, declaration)
            ?: errorWithCfirSpecificEntries(
                "No rebuilt CFIR declaration was found for non-local PSI",
                psi = declaration,
                fir = designation.target,
            )
    }
}
