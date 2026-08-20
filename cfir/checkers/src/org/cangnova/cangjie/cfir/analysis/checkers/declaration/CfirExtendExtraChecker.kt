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
import org.cangnova.cangjie.cfir.analysis.checkers.context.accessContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.accessibilityChecker
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperators
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

/**
 * Extend 补充检查器（ExtendExtra 分组）
 *
 * 对齐 C++ TypeCheckExtend.cpp 中未被已有 9 个 Extend checker 覆盖的诊断：
 * - EXTEND_FUNCTION_CANNOT_OVERRIDDEN: extend 中的函数不能被 override
 * - EXTEND_MEMBER_CANNOT_SHADOW: extend 成员不能遮蔽原有成员
 * - EXTEND_ILLEGAL_MEMBER: extend 中不允许的成员类型（如构造器、字段）
 * - EXTEND_A_JAVA_TYPE: 不能 extend @Java 标注的类型
 * - EXTEND_REF_TARGET_CANNOT_BE_JAVA_IMPL: extend 不能指向 @JavaImpl 声明
 */
object CfirExtendExtraChecker : CfirExtendChecker() {
    /**
     * Java 互操作基础注解名称。
     */
    private val JAVA = Name.identifier("Java")

    /**
     * Java 实现类型注解名称。
     */
    private val JAVA_IMPL = Name.identifier("JavaImpl")

    /**
     * 官方 primitive equality 特例：Bool/Unit 的 `==`/`!=` 同时保留 shadow 诊断。
     */
    private val primitiveEqualityShadowKinds = setOf(PrimitiveTypeKind.BOOLEAN, PrimitiveTypeKind.UNIT)

    /**
     * 会参与 primitive equality shadow 特例的 operator 名称。
     */
    private val primitiveEqualityShadowNames = setOf(
        OperatorNameConventions.EQUALS,
        OperatorNameConventions.NOT_EQUALS,
    )

    /**
     * 对单个 extend 声明执行额外语义检查。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirExtend) {
        checkIllegalMembers(declaration)
        checkExtendJavaType(declaration)
        checkExtendJavaImplTarget(declaration)
        checkOverrideInExtend(declaration)
        checkMemberShadowing(declaration)
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
        val targetType = (targetTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        val targetScope = targetType.createTargetShadowScope(extend) ?: return

        for (member in extend.declarations) {
            val memberName = member.shadowableName() ?: continue
            if (!member.shadowsExistingMember(targetScope, context, targetType)) continue

            val typeName = targetType.classIdOrPrimitiveClassId?.shortClassName ?: continue
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
     * extend shadow 统一消费 providers 构造的实例化 use-site 成员图。
     *
     * 当前 extend 的直接成员和接口父边由 [excludingExtend] 在结构层排除；其它 sibling
     * extend、声明父边、泛型替换和隐式 Object 均由同一个 scope owner 维护。checker 不再
     * 重走 supertype graph，只负责在显式 use-site 可见性过滤后判断 shadow relation。
     */
    context(context: CheckerContext)
    private fun ConeCangJieType.createTargetShadowScope(
        excludingExtend: CfirExtend,
    ): CfirTypeScope? = CfirClassUseSiteMemberScope.createForUseSiteType(
        session = context.session,
        ownerType = this,
        excludingExtend = excludingExtend,
    )

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
        targetType: ConeCangJieType,
    ): Boolean {
        val symbol = when (this) {
            is CfirNamedFunction -> symbol
            is CfirProperty -> symbol
            else -> return false
        }
        val signature = symbol.overrideSignatureKey()
        val accessContext = context.accessContext(CfirAccessKind.CALLABLE).copy(
            receiverType = targetType,
            lookupOrigin = CfirLookupOrigin.MEMBER,
        )
        var found = false
        context.session.accessibilityChecker.processAccessibleCallablesByName(
            scope = targetScope,
            name = symbol.name,
            context = accessContext,
        ) { candidate ->
            if (candidate.symbol.canShadowThis(this, signature, context)) {
                found = true
            }
        }
        return found
    }

    /**
     * 判断目标 callable 符号是否会被当前 extend 成员遮蔽。
     *
     * 过滤未绑定符号、当前成员自身的 substitution override、private 成员以及接口抽象需求，
     * 剩余成员通过 override 签名 key 判断是否真正同签名。
     */
    private fun org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>.canShadowThis(
        currentMember: CfirDeclaration,
        currentSignature: String,
        context: CheckerContext,
    ): Boolean {
        if (!isBound) return false
        val original = unwrapSubstitutionOverrides()
        if (original.cfir === currentMember) return false

        if (original.isSyntheticPrimitiveBuiltinOperatorExcludedFromShadow(context)) return false
        if (original.isInterfaceRequirementMember(context)) return false
        if (!original.cfir.hasSameShadowMemberKind(currentMember)) {
            // 官方 CheckSameNameInheritanceInfo 对 inherited-interface 的 cross-kind 成员
            // 只报告 kind inconsistency；只有目标类型自身的不同种类成员才继续进入 shadow 分类。
            if (context.ownerClassSymbol(original)?.cfir is CfirInterface) return false
            return original.cfir.shadowMemberStaticStatus() == currentMember.shadowMemberStaticStatus()
        }
        return overrideSignatureKey() == currentSignature
    }

    /**
     * 官方同名继承检查在签名比较前先区分声明种类；函数与属性同名时即构成 shadow，
     * 不要求二者能够形成普通 override 签名。
     */
    private fun CfirDeclaration.hasSameShadowMemberKind(other: CfirDeclaration): Boolean =
        (this is CfirNamedFunction && other is CfirNamedFunction) ||
            (this is CfirProperty && other is CfirProperty)

    /** 返回参与 shadow 分类的成员 static 状态。 */
    private fun CfirDeclaration.shadowMemberStaticStatus(): Boolean? = when (this) {
        is CfirNamedFunction -> status.isStatic
        is CfirProperty -> status.isStatic
        is CfirFieldVariable -> status.isStatic
        else -> null
    }

    /**
     * primitive 的语言内建 operator 由专门的 built-in overload 规则处理。
     *
     * 官方 `IsBuiltInOperatorFuncInExtend` 会把这些内建签名作为特殊实现/诊断入口，
     * 它们不是普通目标成员，不能再次参与 `extend member cannot shadow`。
     */
    private fun org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>.isSyntheticPrimitiveBuiltinOperatorExcludedFromShadow(
        context: CheckerContext,
    ): Boolean {
        val function = cfir as? CfirNamedFunction ?: return false
        if (function.origin != CfirDeclarationOrigin.Synthetic.FakeFunction) return false
        if (!function.status.isOperator) return false
        val owner = context.ownerClassSymbol(this)?.cfir as? CfirPrimitiveTypeDeclaration ?: return false
        val argumentTypes = function.valueParameters.map { parameter ->
            parameter.returnTypeRef.coneTypeOrNull ?: return false
        }
        BuiltinPrimitiveOperators.resolve(
            name = function.name,
            receiverType = ConePrimitiveType(owner.kind),
            argumentTypes = argumentTypes,
        ) ?: return false
        if (function.isPrimitiveEqualityShadowException(owner.kind)) return false
        return true
    }

    /**
     * `Bool`/`Unit` 的 equality/inequality 在官方实现中仍作为 `extend Bool/Unit`
     * 既有成员参与 shadow 检查，不能按普通 synthetic builtin operator 过滤掉。
     */
    private fun CfirNamedFunction.isPrimitiveEqualityShadowException(kind: PrimitiveTypeKind): Boolean =
        kind in primitiveEqualityShadowKinds && name in primitiveEqualityShadowNames

    /**
     * 判断候选是否只是接口的抽象实现需求。
     *
     * 官方 `GetVisibleExtendMembersForExtend` 在合并 sibling extend 的接口成员后删除 abstract
     * 项；它们用于实现义务检查，不是 extend shadow parent。
     */
    private fun org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>.isInterfaceRequirementMember(
        context: CheckerContext,
    ): Boolean {
        val owner = context.ownerClassSymbol(this)?.cfir
        if (owner !is CfirInterface) return false
        return when (val declaration = cfir) {
            is CfirFunction -> declaration.body == null || declaration.status.isAbstract
            is CfirProperty -> declaration.status.isAbstract ||
                (declaration.getter?.body == null && declaration.setter?.body == null)
            else -> declaration.status.isAbstract
        }
    }

    /**
     * 取得可参与 extend shadow 检查的声明名称。
     */
    private fun CfirDeclaration.shadowableName(): Name? = when (this) {
        is CfirNamedFunction -> name
        is CfirProperty -> name
        else -> null
    }
}
