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
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.providers.CfirSuperTypeGraphEdge
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirCompositeTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExtendMemberScope
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.extendRuleQueryServiceOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperators
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.expandedClassIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.expandedExtendTargetKey
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.type.model.TypeConstructorMarker

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
            if (!member.shadowsExistingMember(targetScope, context, extend, targetType)) continue

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
    private fun ConeCangJieType.createTargetShadowScope(
        excludingExtend: CfirExtend,
    ): CfirTypeScope? {
        return createTargetShadowScope(
            excludingExtend = excludingExtend,
            visited = linkedSetOf(),
        )
    }

    /**
     * 递归构造 shadow scope。
     *
     * 当前 extend 声明自己引入的接口成员是实现目标，不是 shadow 来源；但目标类型原本
     * 声明/继承来的接口成员仍然是已有成员，必须保留。这里消费 resolve 阶段记录的
     * supertype graph edge，按 edge.sourceExtend 跳过当前 extend 自己贡献的接口边，
     * 同时用 visited 阻止非法继承环导致递归爆栈。
     */
    context(context: CheckerContext)
    private fun ConeCangJieType.createTargetShadowScope(
        excludingExtend: CfirExtend,
        visited: MutableSet<ConeCangJieType>,
    ): CfirTypeScope? {
        if (!visited.add(this)) return null
        val scopes = buildList {
            classTargetShadowScope()?.let(::add)
            extendTargetShadowScope(excludingExtend)?.let(::add)
            for (supertype in directShadowSuperTypes(excludingExtend)) {
                supertype.createTargetShadowScope(
                    excludingExtend = excludingExtend,
                    visited = visited,
                )?.let(::add)
            }
        }
        return when (scopes.size) {
            0 -> null
            1 -> scopes.single()
            else -> CfirCompositeTypeScope(scopes, context.session)
        }
    }

    /**
     * 取得 shadow 检查应递归进入的直接父类型。
     *
     * resolve 阶段的 supertype graph edge 保留了父类型来源：声明头父类型的
     * `sourceExtend` 为空，extend 注入父类型的 `sourceExtend` 指向来源 extend。
     * 这比按实例化后的类型做相等比较更精确，能区分 `class B <: I<String>` 这类
     * 目标原有接口和 `extend B <: I<Int64>` 当前声明正在实现的接口。
     */
    context(context: CheckerContext)
    private fun ConeCangJieType.directShadowSuperTypes(excludingExtend: CfirExtend): List<ConeCangJieType> {
        val ownerClassId = classIdOrPrimitiveClassId ?: return emptyList()
        val ownerDeclaration = context.session.symbolProvider
            .getClassLikeSymbolByClassId(ownerClassId)
            ?.cfir as? CfirClassLikeDeclaration
        val declarationSubstitutor = ownerDeclaration?.createDeclarationSubstitutor(this)
        val graphEdges = context.session.directSupertypeProviderOrNull
            ?.getDirectSuperTypeEdges(ownerClassId)
            .orEmpty()

        val directSupertypes = graphEdges.mapNotNull { edge ->
            if (edge.sourceExtend === excludingExtend) return@mapNotNull null
            if (!edge.isShadowInterfaceEdgeVisibleFrom(excludingExtend, context)) return@mapNotNull null
            edge.toUseSiteSupertype(
                ownerType = this,
                declarationSubstitutor = declarationSubstitutor,
            )
        }
        return directSupertypes.withImplicitObjectSuperclass(ownerDeclaration)
    }

    /**
     * 判断 sibling extend 注入的接口边是否属于当前 extend 的 shadow 检查序列。
     *
     * 官方 `CollectExtendByInterfaceInherit` 不会把不相关 sibling 接口逐个作为当前成员的
     * shadow parent；这些接口先在 effective member graph 中合并，abstract 成员被删除，
     * 多 default 冲突由继承检查分类。当前 extend 没有接口时不存在该合并目标，因此仍需
     * 看见 sibling 接口；存在父子接口关系时则按同一检查序列保留。
     */
    private fun CfirSuperTypeGraphEdge.isShadowInterfaceEdgeVisibleFrom(
        currentExtend: CfirExtend,
        context: CheckerContext,
    ): Boolean {
        val siblingExtend = sourceExtend ?: return true
        val query = context.session.extendRuleQueryServiceOrNull ?: return false
        if (query.inheritedInterfacesOf(currentExtend).isEmpty()) return true
        return query.doesExtendInheritFrom(currentExtend, siblingExtend)
    }

    /**
     * 将 graph edge 还原为当前 owner type 上的 use-site 父类型。
     */
    context(context: CheckerContext)
    private fun CfirSuperTypeGraphEdge.toUseSiteSupertype(
        ownerType: ConeCangJieType,
        declarationSubstitutor: ConeSubstitutor?,
    ): ConeCangJieType? {
        val supertype = typeRef.coneType
        val extend = sourceExtend
        if (extend != null) {
            val targetPattern = extend.extendedTypeRef.coneTypeOrNull ?: return null
            val substitution = createExtendDeclarationSubstitution(
                session = context.session,
                extend = extend,
                targetPattern = targetPattern,
                concreteReceiverType = ownerType,
            ) ?: return null
            return substitution.substitutor.substituteOrSelf(supertype)
        }
        return declarationSubstitutor?.substituteOrSelf(supertype) ?: supertype
    }

    /**
     * 官方会为没有显式 concrete superclass 的 class 补 `Object`。
     *
     * shadow scope 递归使用 graph edge 时需要显式恢复这一隐式父类，
     * 否则从 `Object` 继承来的既有成员不会进入 extend shadow 检查。
     */
    private fun List<ConeCangJieType>.withImplicitObjectSuperclass(
        declaration: CfirClassLikeDeclaration?,
    ): List<ConeCangJieType> {
        if (declaration !is CfirClass) return this
        if (declaration.symbol.classId == StdlibClassIds.Any) return this
        if (any { it.isConcreteSuperclassCandidate() }) return this

        val implicitSuperclass = if (declaration.symbol.classId == StdlibClassIds.Object) {
            ConeClassLikeType(StdlibClassIds.Any.toLookupTag(), isInterface = true)
        } else {
            ConeClassLikeType(StdlibClassIds.Object.toLookupTag())
        }
        return (this + implicitSuperclass).distinct()
    }

    /**
     * 判断类型是否占用 class 的 concrete superclass 槽位。
     */
    private fun ConeCangJieType.isConcreteSuperclassCandidate(): Boolean = when (this) {
        is ConeClassLikeType -> !isInterface
        is ConeStructType, is ConeEnumType -> true
        else -> false
    }

    /**
     * 根据 class-like 声明类型参数与当前 owner type 实参创建声明替换器。
     */
    private fun CfirClassLikeDeclaration.createDeclarationSubstitutor(type: ConeCangJieType): ConeSubstitutor? {
        if (type !is ConeLookupTagBasedType) return null
        if (typeParameters.isEmpty()) return ConeSubstitutor.Empty
        if (typeParameters.size != type.typeArguments.size) return null

        val replacements: Map<TypeConstructorMarker, ConeCangJieType> =
            typeParameters.zip(type.typeArguments).associate { (typeParameter, argument) ->
                typeParameter.symbol.toLookupTag() to argument.type
            }
        return replacements.takeIf { it.isNotEmpty() }?.let(::CfirTypeSubstitutorByMap) ?: ConeSubstitutor.Empty
    }

    /**
     * 为 class-like/primitive classifier 目标构造声明侧成员 scope。
     *
     * extend 成员及其注入的父接口由 [extendTargetShadowScope] 和
     * [directShadowSuperTypes] 按来源显式加入。这里若使用 use-site scope，会把同一批
     * extend 成员再次展平进 class scope，导致当前实现目标和其它 extend 的来源身份丢失。
     */
    context(context: CheckerContext)
    private fun ConeCangJieType.classTargetShadowScope(): CfirTypeScope? {
        val targetClassId = expandedClassIdOrPrimitiveClassId ?: return null
        val targetSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(targetClassId)
            as? CfirClassLikeSymbol<*> ?: return null
        val rawScope = CfirClassUseSiteMemberScope(
            session = context.session,
            classSymbol = targetSymbol,
            symbolProvider = context.session.symbolProvider,
            ownerType = this,
            dispatchReceiverType = this,
            scopeKind = CfirClassMemberScopeKind.DECLARATION_SITE,
        )
        return CfirClassSubstitutionScope(
            session = context.session,
            useSiteMemberScope = rawScope,
            dispatchReceiverType = this,
            substitutionOwnerType = this,
        )
    }

    /**
     * 为同一 extend 目标上已有的扩展成员构造 shadow 查询 scope。
     */
    context(context: CheckerContext)
    private fun ConeCangJieType.extendTargetShadowScope(excludingExtend: CfirExtend): CfirTypeScope? {
        val targetKey = expandedExtendTargetKey ?: return null
        val rawScope = CfirExtendMemberScope(
            targetKey = targetKey,
            extendProvider = context.session.extendProvider,
            session = context.session,
            receiverType = this,
            excludingExtend = excludingExtend,
        )
        return CfirClassSubstitutionScope(
            session = context.session,
            useSiteMemberScope = rawScope,
            dispatchReceiverType = this,
            substitutionOwnerType = this,
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
        currentExtend: CfirExtend,
        targetType: ConeCangJieType,
    ): Boolean {
        val symbol = when (this) {
            is CfirNamedFunction -> symbol
            is CfirProperty -> symbol
            else -> return false
        }
        val signature = symbol.overrideSignatureKey()
        var found = false
        targetScope.processCallablesByName(symbol.name) { candidate ->
            if (candidate.canShadowThis(this, signature, context, currentExtend, targetType)) {
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
        currentExtend: CfirExtend,
        targetType: ConeCangJieType,
    ): Boolean {
        if (!isBound) return false
        val original = unwrapSubstitutionOverrides()
        if (original.cfir === currentMember) return false

        // 官方 RemoveMembersShouldNotInherit / IsInvisibleMember 会排除 private 成员。
        if (original.cfir.status.visibility == Visibilities.Private) return false
        if (original.isSyntheticPrimitiveBuiltinOperatorExcludedFromShadow(context)) return false
        if (original.isCurrentExtendImplementationTarget(context, currentExtend, targetType)) return false
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
     * 当前 extend 接口闭包中的成员属于实现目标，不构成 shadow 来源；但若目标类型在
     * 排除当前 extend 后已经通过声明继承或其他适用 extend 具备该接口，则该成员已经
     * 是目标的有效成员，仍需参与 shadow 检查。
     */
    private fun org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>.isCurrentExtendImplementationTarget(
        context: CheckerContext,
        currentExtend: CfirExtend,
        targetType: ConeCangJieType,
    ): Boolean {
        val ownerSymbol = context.ownerClassSymbol(this) ?: return false
        if (ownerSymbol.cfir !is CfirInterface) return false
        val currentInterfaceClosure = context.session.extendRuleQueryServiceOrNull
            ?.inheritedInterfaceClosureClassIdsOf(currentExtend)
            .orEmpty()
        if (ownerSymbol.classId !in currentInterfaceClosure) return false
        return !targetType.hasExistingSuperInterface(ownerSymbol.classId, currentExtend, context, linkedSetOf())
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
     * 在排除当前 extend 自己贡献的 graph edge 后，检查目标类型是否已经具备指定接口。
     */
    private fun ConeCangJieType.hasExistingSuperInterface(
        interfaceClassId: ClassId,
        excludingExtend: CfirExtend,
        context: CheckerContext,
        visited: MutableSet<ConeCangJieType>,
    ): Boolean {
        if (!visited.add(this)) return false
        val directSupertypes = with(context) {
            directShadowSuperTypes(excludingExtend)
        }
        for (supertype in directSupertypes) {
            if (supertype.classIdOrPrimitiveClassId == interfaceClassId) return true
            if (supertype.hasExistingSuperInterface(interfaceClassId, excludingExtend, context, visited)) return true
        }
        return false
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
