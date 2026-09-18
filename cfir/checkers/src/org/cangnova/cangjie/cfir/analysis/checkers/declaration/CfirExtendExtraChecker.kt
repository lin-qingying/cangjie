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
import org.cangnova.cangjie.cfir.analysis.checkers.context.accessContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.scopes.processCallablesByNameWithLookupProvenance
import org.cangnova.cangjie.cfir.session.accessibilityChecker
import org.cangnova.cangjie.cfir.session.extendRuleQueryService
import org.cangnova.cangjie.cfir.session.extendRuleQueryServiceOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperators
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.annotations.CangjiePlatformAnnotationKind

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
     * Java 实现类型注解名称。
     */
    private val JAVA_IMPL = CangjiePlatformAnnotationKind.JAVA_IMPL

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
        if (targetDecl.hasSupportedBuiltinAnnotation(
                context.languageVersionSettings,
                org.cangnova.cangjie.annotations.BuiltInAnnotationKind.JAVA,
            )
        ) {
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
        if (targetDecl.hasSupportedPlatformAnnotation(
                context.languageVersionSettings,
                CangjiePlatformAnnotationKind.JAVA_IMPL,
            )) {
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
    internal fun checkMemberShadowing(
        extend: CfirExtend,
        importedContext: CfirImportedExtendCheckContext? = null,
    ) {
        val targetTypeRef = extend.extendedTypeRef
        val targetType = (targetTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        val targetScope = targetType.createTargetShadowScope(extend) ?: return
        val reportContext = importedContext ?: context

        for (member in extend.declarations) {
            val memberName = member.shadowableName() ?: continue
            if (!member.shadowsExistingMember(targetScope, context, targetType, importedContext)) continue

            val typeName = targetType.classIdOrPrimitiveClassId?.shortClassName ?: continue
            val source = when (member) {
                is CfirNamedFunction -> member.functionNameDiagnosticSource()
                is CfirProperty -> member.propertyNameDiagnosticSource()
                else -> member.source
            }
            reporter.reportOn(
                source = if (importedContext != null) {
                    importedContext.sourceForDeclaration(source ?: member.source ?: extend.source)
                } else source ?: member.source ?: extend.source,
                factory = CfirErrors.EXTEND_MEMBER_CANNOT_SHADOW,
                a = memberName,
                b = typeName,
                context = reportContext,
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
        importedContext: CfirImportedExtendCheckContext?,
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
        /*
         * shadow parent 是「extend 目标类型类层次的有效成员」，对齐官方
         * `StructInheritanceChecker::CheckExtendMemberValid`：protected 成员即使对
         * extend 所在包不可见也构成同名冲突，但 private 成员不参与。
         *
         * `checkCallable` 的 use-site 可见性会按 extend 所在包把跨包 protected 成员排除，
         * 因此不能只消费 accessible 集合：需要把「非 extend 来源的 protected 类层次成员」
         * 放回候选集，同时保留 accessibility 的 extend-export 过滤（非导出 sibling extend
         * 成员仍是 shadow parent 之外）并排除 private。
         *
         * cjc 1.0.5 探针：protected（跨包）-> 报告 EXTEND_MEMBER_CANNOT_SHADOW；
         * private -> 不报告。
         */
        targetScope.processCallablesByNameWithLookupProvenance(symbol.name) { candidate ->
            if (importedContext?.accepts(candidate.provenance.sourceExtend) == false) {
                return@processCallablesByNameWithLookupProvenance
            }
            val accessible = context.session.accessibilityChecker.checkCallable(
                symbol = candidate.symbol,
                context = accessContext,
                provenance = candidate.provenance,
            ) is CfirAccessibilityResult.Accessible
            if (!accessible && !candidate.symbol.isShadowRelevantProtectedClassMember()) {
                return@processCallablesByNameWithLookupProvenance
            }
            if (candidate.symbol.canShadowThis(this, signature, candidate.provenance, context, importedContext != null)) {
                found = true
            }
        }
        return found
    }

    /**
     * 判断候选是否为不会因 use-site 可见性被排除的 protected 类层次成员。
     *
     * 非导出 sibling extend 成员的可见性/导出过滤仍由 accessibility checker 负责，
     * 不能靠此特例绕过；因此只有「非 extend 来源、显式 protected」的声明会被放行。
     */
    private fun org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>.isShadowRelevantProtectedClassMember(): Boolean {
        if (getContainingExtend() != null) return false
        val declaration = unwrapSubstitutionOverrides().cfir as? CfirMemberDeclaration ?: return false
        return declaration.status.visibility == Visibilities.Protected
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
        provenance: CfirCallableLookupProvenance,
        context: CheckerContext,
        ignoreInheritedExtendRelations: Boolean,
    ): Boolean {
        if (!isBound) return false
        val original = unwrapSubstitutionOverrides()
        if (original.cfir === currentMember) return false

        // 同一目标上的父子 extend 关系已经由接口继承图归并；子 extend 的成员不应
        // 反向把父 extend 的具体实现判为 shadow。无关 sibling extend 仍必须继续
        // 进入 import6 这类消费包冲突检查。
        val currentExtend = when (currentMember) {
            is CfirNamedFunction -> currentMember.symbol
            is CfirProperty -> currentMember.symbol
            else -> null
        }?.getContainingExtend()
        val sourceExtend = provenance.sourceExtend
        if (
            ignoreInheritedExtendRelations &&
            currentExtend != null &&
            sourceExtend != null &&
            context.session.extendRuleQueryServiceOrNull?.areExtendsInInheritRelation(
                currentExtend,
                sourceExtend,
            ) == true
        ) {
            return false
        }

        if (original.isSyntheticPrimitiveBuiltinOperatorExcludedFromShadow(currentMember, context)) return false
        if (original.isInterfaceRequirementMember(context)) return false
        if (original.isIndependentInterfaceDefault(currentMember, provenance, context)) return false
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
     *
     * 同一内建签名可能以两种声明形式出现在 use-site scope 中，排除判定必须按形式区分：
     * - builtin provider 构造的 synthetic FakeFunction 成员：语言内建运算符
     *   （`+`/`-`/`<<` 等算术、位移运算符），在 std.core 中没有对应的 extend 声明，
     *   官方 f5/g2 探针确认返回类型正确时也不报告 shadow；
     * - std 库元数据反序列化的 Library 成员（PSI/light-tree 均可能）：比较运算符
     *   （`==`/`!=`/`<`/`>` 等）在 std.core 中经 `extend <类型> <: Equatable/Comparable`
     *   显式声明，是真正的 extend 成员，参与 shadow 检查。
     *
     * 命中内建签名后对 Library 成员按返回类型分派（对齐官方 `IsBuiltInOperatorFuncInExtend`）：
     * - 返回类型与内建一致：该路径合成内建实现（无诊断），extend 成员仍作为普通成员参与
     *   shadow 检查（官方 p1/f5 Bool `==`、g5 Int64 `==` 探针：返回类型正确时报告 shadow）；
     * - 返回类型与内建不一致：该路径报告 `RETURN_TYPE_INCOMPATIBLE` 取代 shadow
     *   （官方 p2/f5 Int64 `==`、g6 探针：返回类型错误时不报告 shadow）。
     */
    private fun org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>.isSyntheticPrimitiveBuiltinOperatorExcludedFromShadow(
        currentMember: CfirDeclaration,
        context: CheckerContext,
    ): Boolean {
        val function = cfir as? CfirNamedFunction ?: return false
        if (!function.status.isOperator) return false
        // primitive receiver 取自当前 extend 的目标类型：针对同一个内建签名，std.core
        // 以 `extend <类型> <: Equatable/Comparable` 引入的对象成员其宿主是接口而非 primitive，
        // 因此不能从候选符号自身的 owner 取；receiver 永远是被扩展的 primitive 目标。
        val currentFunction = currentMember as? CfirNamedFunction ?: return false
        val receiverType = currentFunction.symbol.getContainingExtend()
            ?.extendedTypeRef
            ?.coneTypeOrNull
            ?: return false
        val argumentTypes = function.valueParameters.map { parameter ->
            parameter.returnTypeRef.coneTypeOrNull ?: return false
        }
        val match = BuiltinPrimitiveOperators.resolve(
            name = function.name,
            receiverType = receiverType,
            argumentTypes = argumentTypes,
        ) ?: return false
        // 语言内建（synthetic）成员不构成 shadow parent：官方 f5/g2 探针中
        // extend Int64 的 `+`/`-`/`<<`（返回类型正确）均无 shadow 诊断。
        if (function.origin == CfirDeclarationOrigin.Synthetic.FakeFunction) return true
        // std.core extend 成员按返回类型与内建是否一致分派（见上方 KDoc）。
        val currentReturnType = currentFunction.resolvedReturnTypeOrNull(context) ?: return false
        return !AbstractTypeChecker.equalTypes(context.session.typeContext, match.returnType, currentReturnType)
    }

    /**
     * 读取函数声明的语义返回类型：优先使用已解析 type ref，否则经 checker context
     * 的 return type calculator 计算，与其他 CFIR 检查器保持一致。
     */
    private fun CfirNamedFunction.resolvedReturnTypeOrNull(context: CheckerContext): ConeCangJieType? {
        (returnTypeRef as? CfirResolvedTypeRef)?.coneType?.let { return it }
        return context.returnTypeCalculator.tryCalculateReturnType(this).coneType
    }

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
     * 识别由互不继承的 sibling extend 接口共同形成的待实现默认成员。
     *
     * 官方 MergeInheritedMemberHelper 使用 IsExtendInheritRelation 区分默认实现冲突
     * 与既有父实现：前者由当前 extend 的成员实现，后者仍参与 shadow 检查。
     * 来源必须取自 use-site 成员图；目标类型自身继承的接口和相关 extend 引入的默认
     * 成员已经是有效实现，不能仅因当前 extend 声明了接口便将其排除。
     */
    private fun org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>.isIndependentInterfaceDefault(
        currentMember: CfirDeclaration,
        provenance: CfirCallableLookupProvenance,
        context: CheckerContext,
    ): Boolean {
        val owner = context.ownerClassSymbol(this)?.cfir
        if (owner !is CfirInterface) return false
        val sourceExtend = provenance.sourceExtend ?: return false
        if (provenance.requirementInterfaceType == null) return false
        val currentExtend = when (currentMember) {
            is CfirNamedFunction -> currentMember.symbol
            is CfirProperty -> currentMember.symbol
            else -> return false
        }.getContainingExtend() ?: return false
        val implementsInterface = currentExtend.superTypeRefs.any { superTypeRef ->
            val classId = (superTypeRef as? CfirResolvedTypeRef)
                ?.coneType
                ?.classIdOrPrimitiveClassId
                ?: return@any false
            context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir is CfirInterface
        }
        return implementsInterface &&
                !context.session.extendRuleQueryService.areExtendsInInheritRelation(currentExtend, sourceExtend)
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
