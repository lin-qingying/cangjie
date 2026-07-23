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
import org.cangnova.cangjie.cfir.resolve.providers.getContainingClass
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.isStaticMemberForOverride
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.ClassId

/**
 * 为 class-like 声明创建 use-site 成员 scope。
 */
internal fun CheckerContext.createUseSiteMemberScope(declaration: CfirClassLikeDeclaration): CfirTypeScope {
    return when (declaration) {
        is CfirClass -> {
            val symbol = declaration.symbol as? CfirClassLikeSymbol<*> ?: return CfirTypeScope.Empty
            val rawScope = CfirClassUseSiteMemberScope(
                session = session,
                classSymbol = symbol,
                symbolProvider = session.symbolProvider,
                extendProvider = session.extendProvider,
                directSupertypeProvider = session.directSupertypeProviderOrNull,
                scopeKind = CfirClassMemberScopeKind.USE_SITE,
            )
            CfirClassSubstitutionScope(session, rawScope, symbol.constructType())
        }

        is CfirStruct -> {
            val symbol = declaration.symbol as CfirClassLikeSymbol<*>
            CfirClassUseSiteMemberScope(
                session = session,
                classSymbol = symbol,
                symbolProvider = session.symbolProvider,
                extendProvider = session.extendProvider,
                directSupertypeProvider = session.directSupertypeProviderOrNull,
                scopeKind = CfirClassMemberScopeKind.USE_SITE,
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
                scopeKind = CfirClassMemberScopeKind.USE_SITE,
            )
        }
    }
}

/**
 * 查找 callable 符号所属的 class-like 符号。
 */
internal fun CheckerContext.ownerClassSymbol(symbol: CfirCallableSymbol<*>): CfirClassLikeSymbol<*>? {
    val ownerClassId = symbol.ownerClassId()
        ?: return null
    return session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)
}

/**
 * 在检查上下文中读取 callable 所属 class id。
 */
internal fun CfirCallableSymbol<*>.ownerClassId(context: CheckerContext): ClassId? =
    ownerClassId()

/**
 * 读取 callable id 或 provider 记录的所属 class id。
 */
private fun CfirCallableSymbol<*>.ownerClassId(): ClassId? {
    return callableId.classId ?: getContainingClass()?.classId
}

/**
 * 收集与函数符号直接覆盖且签名和静态性一致的父函数。
 *
 * static interface requirement 不进入普通 override graph，因此 static 声明还需要从
 * use-site scope 按稳定签名补齐；这与普通 override 边合并后构成完整的实现关系目标集。
 */
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
    if (targetIsStatic) {
        processFunctionsByName(functionSymbol.name) { candidate ->
            if (
                candidate != functionSymbol &&
                candidate.overrideSignatureKey() == targetSignature &&
                candidate.isStaticMemberForOverride()
            ) {
                result += candidate
            }
        }
    }
    return result.toList()
}

/**
 * 收集与函数符号直接覆盖且签名一致的父函数，忽略静态性差异。
 */
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

/**
 * 收集与属性符号直接覆盖且签名和静态性一致的父属性。
 */
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

/**
 * 收集与属性符号直接覆盖且签名一致的父属性，忽略静态性差异。
 */
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

/**
 * 判断 callable 符号从指定 owner 声明视角是否可见。
 */
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
            val currentFile = ownerDeclaration.symbol.getContainingFile() ?: return true
            val declarationFile = getContainingFile() ?: return true
            val currentPackage = currentFile.packageDirective.packageFqName
            val declarationPackage = declarationFile.packageDirective.packageFqName
            return canAccessPackageInternalDeclaration(currentPackage, declarationPackage)
        }
    }

    val ownerClassId = ownerClassId(context) ?: return true
    val currentClassId = (ownerDeclaration.symbol as? CfirClassLikeSymbol<*>)?.classId ?: return true
    return ownerClassId == currentClassId
}

/**
 * override/redef 的目标搜索遵循官方继承检查的 inherited member 语义：
 * 父类 private 成员不会作为子类可覆盖/可重定义目标参与后续检查。
 */
internal fun CfirCallableSymbol<*>.canParticipateInOverrideTargetSearch(
    ownerDeclaration: CfirClassLikeDeclaration,
    context: CheckerContext,
): Boolean {
    if (!isBound) return true
    if (cfir.status.visibility != Visibilities.Private) return true

    val ownerClassId = ownerClassId(context) ?: return false
    val currentClassId = (ownerDeclaration.symbol as? CfirClassLikeSymbol<*>)?.classId ?: return false
    return ownerClassId == currentClassId
}

/**
 * 判断 callable 符号是否应按抽象成员处理。
 */
internal fun CfirCallableSymbol<*>.isAbstractLike(context: CheckerContext): Boolean {
    if (!isBound) return false
    // 是否抽象必须以 STATUS 阶段的 resolved status 为准。
    // cjo / decompiled 的接口默认实现可能没有本地 body 节点，但 status 已经保留其 concrete 语义。
    return cfir.status.isAbstract
}

/**
 * 判断 callable 符号是否能从指定 owner 声明中被 override/redef。
 */
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

/**
 * 判断 callable 声明是否来自源代码。
 */
internal val CfirCallableDeclaration.isSourceDeclaration: Boolean
    get() = origin == org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin.Source
