/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.providers.canAccessPackageInternalDeclaration
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.isStaticMemberForOverride
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.ClassId

internal fun CheckerContext.createUseSiteMemberScope(declaration: CfirClassLikeDeclaration): CfirTypeScope {
    return when (declaration) {
        is CfirClass -> session.cangjieScopeProvider.getDeclarationSiteMemberScope(
            declaration,
            session,
            scopeSession,
        )

        is CfirStruct -> {
            val symbol = declaration.symbol as CfirClassLikeSymbol<*>
            CfirClassUseSiteMemberScope(
                session = session,
                classSymbol = symbol,
                symbolProvider = session.symbolProvider,
                extendProvider = session.extendProvider,
                directSupertypeProvider = session.directSupertypeProviderOrNull,
                scopeKind = CfirClassMemberScopeKind.DECLARATION_SITE,
            )
        }

        else -> {
            val symbol = declaration.symbol as? CfirClassLikeSymbol<*> ?: return CfirTypeScope.Empty
            CfirClassUseSiteMemberScope(
                session = session,
                classSymbol = symbol,
                symbolProvider = session.symbolProvider,
                extendProvider = session.extendProvider,
                directSupertypeProvider = session.directSupertypeProviderOrNull,
                scopeKind = CfirClassMemberScopeKind.DECLARATION_SITE,
            )
        }
    }
}

internal fun CheckerContext.ownerClassSymbol(symbol: CfirCallableSymbol<*>): CfirClassLikeSymbol<*>? {
    val ownerClassId = symbol.ownerClassId(session = session)
        ?: return null
    return session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)
}

internal fun CfirCallableSymbol<*>.ownerClassId(context: CheckerContext): ClassId? =
    ownerClassId(session = context.session)

private fun CfirCallableSymbol<*>.ownerClassId(session: org.cangnova.cangjie.cfir.session.CfirSession): ClassId? {
    return callableId.classId ?: session.cfirProvider.getContainingClass(this)?.classId
}

internal fun CfirTypeScope.collectDirectOverriddenFunctions(functionSymbol: CfirNamedFunctionSymbol): List<CfirFunctionSymbol<*>> {
    val targetSignature = functionSymbol.overrideSignatureKey()
    val targetIsStatic = functionSymbol.isStaticMemberForOverride()
    val result = linkedSetOf<CfirFunctionSymbol<*>>()
    processDirectOverriddenFunctionsWithBaseScope(functionSymbol) { candidate, _ ->
        if (
            candidate != functionSymbol &&
            candidate.overrideSignatureKey() == targetSignature &&
            candidate.isStaticMemberForOverride() == targetIsStatic
        ) {
            result += candidate
        }
        ProcessorAction.NEXT
    }
    return result.toList()
}

internal fun CfirTypeScope.collectDirectOverriddenFunctionsIgnoringStatic(
    functionSymbol: CfirNamedFunctionSymbol,
): List<CfirFunctionSymbol<*>> {
    val targetSignature = functionSymbol.overrideSignatureKey()
    val result = linkedSetOf<CfirFunctionSymbol<*>>()
    processDirectOverriddenFunctionsWithBaseScope(functionSymbol) { candidate, _ ->
        if (candidate != functionSymbol && candidate.overrideSignatureKey() == targetSignature) {
            result += candidate
        }
        ProcessorAction.NEXT
    }
    return result.toList()
}

internal fun CfirTypeScope.collectDirectOverriddenProperties(propertySymbol: CfirPropertySymbol): List<CfirPropertySymbol> {
    val targetSignature = propertySymbol.overrideSignatureKey()
    val targetIsStatic = propertySymbol.isStaticMemberForOverride()
    val result = linkedSetOf<CfirPropertySymbol>()
    processDirectOverriddenPropertiesWithBaseScope(propertySymbol) { candidate, _ ->
        if (
            candidate != propertySymbol &&
            candidate.overrideSignatureKey() == targetSignature &&
            candidate.isStaticMemberForOverride() == targetIsStatic
        ) {
            result += candidate
        }
        ProcessorAction.NEXT
    }
    return result.toList()
}

internal fun CfirTypeScope.collectDirectOverriddenPropertiesIgnoringStatic(
    propertySymbol: CfirPropertySymbol,
): List<CfirPropertySymbol> {
    val targetSignature = propertySymbol.overrideSignatureKey()
    val result = linkedSetOf<CfirPropertySymbol>()
    processDirectOverriddenPropertiesWithBaseScope(propertySymbol) { candidate, _ ->
        if (candidate != propertySymbol && candidate.overrideSignatureKey() == targetSignature) {
            result += candidate
        }
        ProcessorAction.NEXT
    }
    return result.toList()
}

internal fun CfirCallableSymbol<*>.isVisibleIn(
    ownerDeclaration: CfirClassLikeDeclaration,
    context: CheckerContext,
): Boolean {
    if (!isBound) return true
    // override / abstract-member 语义使用“派生类视角”的可见性：
    // private-like 成员只能在声明它的同一类体内参与 override / implementation 计算；
    // internal 按“当前类所在包 + 子包”判断，protected 在仓颉里按模块可见处理。
    when (cfir.status.visibility) {
        Visibilities.Public, Visibilities.Protected -> return true
        Visibilities.Internal -> {
            val currentFile = context.session.cfirProvider.getContainingFile(ownerDeclaration.symbol) ?: return true
            val declarationFile = context.session.cfirProvider.getContainingFile(this) ?: return true
            val currentPackage = currentFile.packageDirective.packageFqName
            val declarationPackage = declarationFile.packageDirective.packageFqName
            return canAccessPackageInternalDeclaration(currentPackage, declarationPackage)
        }
    }

    val ownerClassId = ownerClassId(context) ?: return true
    val currentClassId = (ownerDeclaration.symbol as? CfirClassLikeSymbol<*>)?.classId ?: return true
    return ownerClassId == currentClassId
}

internal fun CfirCallableSymbol<*>.isAbstractLike(context: CheckerContext): Boolean {
    if (!isBound) return false
    if (cfir.status.isAbstract) return true

    val ownerClass = context.ownerClassSymbol(this)?.cfir
    if (ownerClass !is CfirInterface) return false

    val declaration = cfir
    return when (declaration) {
        is CfirFunction -> (this as? CfirFunctionSymbol<*>)?.hasBody == false
        is CfirProperty -> declaration.getter == null && declaration.setter == null
        else -> false
    }
}

internal fun CfirCallableSymbol<*>.isOverridableFrom(
    ownerDeclaration: CfirClassLikeDeclaration,
    context: CheckerContext,
): Boolean {
    if (!isBound) return false

    val ownerClassId = ownerClassId(context) ?: return false
    val currentClassId = (ownerDeclaration.symbol as? CfirClassLikeSymbol<*>)?.classId ?: return false
    if (ownerClassId == currentClassId) return false
    if (!isVisibleIn(ownerDeclaration, context)) return false

    if (isAbstractLike(context)) return true
    if (cfir.status.isOpen || cfir.status.isOverride) return true

    val ownerClassDeclaration = context.ownerClassSymbol(this)?.cfir
    return ownerClassDeclaration is CfirInterface
}

internal val CfirCallableDeclaration.isSourceDeclaration: Boolean
    get() = origin == org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin.Source
