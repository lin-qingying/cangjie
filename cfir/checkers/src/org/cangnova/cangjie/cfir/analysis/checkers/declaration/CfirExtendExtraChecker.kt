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

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.Name

/**
 * Extend 补充检查器（ExtendExtra 分组）
 *
 * 对齐 C++ TypeCheckExtend.cpp 中未被已有 9 个 Extend checker 覆盖的诊断：
 * - EXTEND_FUNCTION_CANNOT_OVERRIDDEN: extend 中的函数不能被 override
 * - EXTEND_MEMBER_CANNOT_SHADOW: extend 成员不能遮蔽原有成员
 * - EXTEND_ILLEGAL_MEMBER: extend 中不允许的成员类型（如构造器、字段）
 * - EXTEND_A_JAVA_TYPE: 不能 extend @Java 标注的类型
 * - EXTEND_REF_TARGET_CANNOT_BE_JAVA_IMPL: extend 不能指向 @JavaImpl 声明
 * - TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE: 不能 extend 导入的接口
 */
object CfirExtendExtraChecker : CfirExtendChecker() {
    private val JAVA = Name.identifier("Java")
    private val JAVA_IMPL = Name.identifier("JavaImpl")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirExtend) {
        checkIllegalMembers(declaration)
        checkExtendJavaType(declaration)
        checkExtendJavaImplTarget(declaration)
        checkOverrideInExtend(declaration)
        checkMemberShadowing(declaration)
        checkExtendImportedInterface(declaration)
    }

    /**
     * extend 中不允许构造器、字段变量等成员。
     *
     * 对齐 C++ DiagKind::sema_extend_illegal_member
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkIllegalMembers(extend: CfirExtend) {
        for (member in extend.declarations) {
            when (member) {
                is CfirConstructor -> {
                    reporter.reportOn(
                        source = member.source ?: extend.source,
                        factory = CfirErrors.EXTEND_ILLEGAL_MEMBER,
                    )
                }
                is CfirFieldVariable -> {
                    reporter.reportOn(
                        source = member.source ?: extend.source,
                        factory = CfirErrors.EXTEND_ILLEGAL_MEMBER,
                    )
                }
                else -> Unit
            }
        }
    }

    /**
     * 不能 extend @Java 标注的类型。
     *
     * 对齐 C++ DiagKind::sema_extend_a_java_type
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExtendJavaType(extend: CfirExtend) {
        val targetTypeRef = extend.extendedTypeRef
        val targetType = (targetTypeRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: return
        val targetDecl = context.session.symbolProvider
            .getClassLikeSymbolByClassId(targetType.classId)?.cfir ?: return
        if (CfirExtendSemantics.hasAnnotation(targetDecl, JAVA)) {
            reporter.reportOn(
                source = targetTypeRef.source ?: extend.source,
                factory = CfirErrors.EXTEND_A_JAVA_TYPE,
            )
        }
    }

    /**
     * extend 不能指向 @JavaImpl 声明。
     *
     * 对齐 C++ DiagKind::sema_extend_ref_target_cannot_be_java_impl
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExtendJavaImplTarget(extend: CfirExtend) {
        val targetTypeRef = extend.extendedTypeRef
        val targetType = (targetTypeRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: return
        val targetDecl = context.session.symbolProvider
            .getClassLikeSymbolByClassId(targetType.classId)?.cfir ?: return
        if (CfirExtendSemantics.hasAnnotation(targetDecl, JAVA_IMPL)) {
            reporter.reportOn(
                source = targetTypeRef.source ?: extend.source,
                factory = CfirErrors.EXTEND_REF_TARGET_CANNOT_BE_JAVA_IMPL,
            )
        }
    }

    /**
     * extend 中的函数如果标记了 override，则报错。
     *
     * 对齐 C++ DiagKind::sema_extend_function_cannot_overridden
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkOverrideInExtend(extend: CfirExtend) {
        for (member in extend.declarations) {
            if (member is CfirNamedFunction && member.status.isOverride) {
                reporter.reportOn(
                    source = member.source ?: extend.source,
                    factory = CfirErrors.EXTEND_FUNCTION_CANNOT_OVERRIDDEN,
                    a = "function",
                    b = member.name,
                )
            }
        }
    }

    /**
     * extend 成员不能遮蔽被扩展类型的已有成员。
     *
     * 对齐 C++ DiagKind::sema_extend_member_cannot_shadow
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMemberShadowing(extend: CfirExtend) {
        val targetTypeRef = extend.extendedTypeRef
        val targetType = (targetTypeRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: return
        val targetDecl = context.session.symbolProvider
            .getClassLikeSymbolByClassId(targetType.classId)?.cfir as? org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
            ?: return
        val targetScope = createTargetShadowScope(targetDecl, targetType, extend)

        for (member in extend.declarations) {
            val memberName = member.shadowableName() ?: continue
            if (!member.shadowsExistingMember(targetScope, context)) continue

            val typeName = targetType.classId.shortClassName
            val source = when (member) {
                is CfirNamedFunction -> member.functionNameDiagnosticSource()
                is CfirProperty -> member.propertyNameDiagnosticSource()
                else -> member.source
            }
            reporter.reportOn(
                source = source ?: member.source ?: extend.source,
                factory = CfirErrors.EXTEND_MEMBER_CANNOT_SHADOW,
                a = memberName,
                b = typeName,
            )
        }
    }

    /**
     * extend shadow 需要按被扩展类型的实例化 use-site 成员集比较。
     *
     * Kotlin FIR 在 `FirClassSubstitutionScope` 中创建 use-site substitution override；
     * 仓颉官方 `CheckMembersWithInheritedDecls` 则把被扩展类型本体成员、同目标可见
     * extend 成员和 extend 引入的接口成员合并后，再与当前 extend 成员比较。
     *
     * 因此这里使用目标类型的 use-site scope，并在外层套 substitution scope；不能
     * 直接使用 declaration-site 裸 scope，否则 `C<A>.f(A)` 与 `extend<B> C<B>.f(B)`
     * 会被误判为不同签名，也看不到同一目标上的其它 extend 成员。
     */
    context(context: CheckerContext)
    private fun createTargetShadowScope(
        targetDecl: org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration,
        targetType: ConeClassLikeType,
        excludingExtend: CfirExtend,
    ): CfirTypeScope {
        val targetSymbol = targetDecl.symbol as? CfirClassLikeSymbol<*> ?: return CfirTypeScope.Empty
        val rawScope = CfirClassUseSiteMemberScope(
            session = context.session,
            classSymbol = targetSymbol,
            symbolProvider = context.session.symbolProvider,
            extendProvider = context.session.extendProvider,
            directSupertypeProvider = context.session.directSupertypeProviderOrNull,
            ownerType = targetType,
            dispatchReceiverType = targetType,
            scopeKind = CfirClassMemberScopeKind.USE_SITE,
            excludingExtend = excludingExtend,
        )
        return CfirClassSubstitutionScope(
            session = context.session,
            useSiteMemberScope = rawScope,
            dispatchReceiverType = targetType,
            substitutionOwnerType = targetType,
        )
    }

    /**
     * extend shadow 必须按成员签名判断，不能只按名称判断。
     *
     * Kotlin FIR 的 extension shadow checker 会比较参数个数、泛型参数个数和 overloadability；
     * 本项目已有 `overrideSignatureKey()` 作为 override/继承共用签名入口，这里复用同一入口，
     * 与官方编译器 `StructInheritanceChecker::CheckExtendMemberValid` 的 member-signature 语义对齐。
     */
    private fun CfirDeclaration.shadowsExistingMember(
        targetScope: org.cangnova.cangjie.cfir.scopes.CfirTypeScope,
        context: CheckerContext,
    ): Boolean {
        return when (this) {
            is CfirNamedFunction -> {
                val signature = symbol.overrideSignatureKey()
                var found = false
                targetScope.processFunctionsByName(name) { candidate ->
                    if (candidate.canShadowThis(this, signature, context)) {
                        found = true
                    }
                }
                found
            }

            is CfirProperty -> {
                val signature = symbol.overrideSignatureKey()
                var found = false
                targetScope.processPropertiesByName(name) { candidate ->
                    if (candidate.canShadowThis(this, signature, context)) {
                        found = true
                    }
                }
                found
            }

            else -> false
        }
    }

    private fun org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>.canShadowThis(
        currentMember: CfirDeclaration,
        currentSignature: String,
        context: CheckerContext,
    ): Boolean {
        if (!isBound) return false
        if (unwrapSubstitutionOverrides().cfir === currentMember) return false

        // 官方 RemoveMembersShouldNotInherit / IsInvisibleMember 会排除 private 成员。
        if (cfir.status.visibility == Visibilities.Private) return false
        if (isInterfaceRequirementMember(context)) return false
        return overrideSignatureKey() == currentSignature
    }

    private fun org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>.isInterfaceRequirementMember(
        context: CheckerContext,
    ): Boolean {
        val owner = context.ownerClassSymbol(this)?.cfir
        if (owner !is org.cangnova.cangjie.cfir.declarations.CfirInterface) return false
        return when (val declaration = cfir) {
            is CfirFunction -> declaration.body == null || declaration.status.isAbstract
            is CfirProperty -> declaration.status.isAbstract ||
                (declaration.getter?.body == null && declaration.setter?.body == null)
            else -> declaration.status.isAbstract
        }
    }

    private fun CfirDeclaration.shadowableName(): Name? = when (this) {
        is CfirNamedFunction -> name
        is CfirProperty -> name
        else -> null
    }

    /**
     * 不能 extend 导入的接口（只能在定义包中 extend）。
     *
     * 对齐 C++ DiagKind::sema_type_cannot_extend_imported_interface
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExtendImportedInterface(extend: CfirExtend) {
        for (superTypeRef in extend.superTypeRefs) {
            val superType = (superTypeRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: continue
            if (CfirExtendSemantics.isProtectedInterface(superType.classId)) continue
            val superDecl = context.session.symbolProvider
                .getClassLikeSymbolByClassId(superType.classId)?.cfir
                as? org.cangnova.cangjie.cfir.declarations.CfirInterface ?: continue

            // 检查接口是否在当前模块中定义
            val interfaceModuleData = superDecl.moduleData
            val extendModuleData = extend.moduleData
            if (interfaceModuleData != extendModuleData) {
                if (extend.duplicatesInheritedTargetInterface(superTypeRef)) continue
                reporter.reportOn(
                    source = superTypeRef.source ?: extend.source,
                    factory = CfirErrors.TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE,
                    a = "extend",
                    b = superType.classId.shortClassName,
                )
            }
        }
    }
}
