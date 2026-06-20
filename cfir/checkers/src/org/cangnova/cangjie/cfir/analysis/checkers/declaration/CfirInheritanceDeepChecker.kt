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
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 继承深层检查器（InheritanceDeep 分组）
 *
 * 对齐 C++ InheritanceChecker/ 目录：
 * - CANNOT_INHERIT_SEALED: sealed 类只能在同包中被继承
 * - INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC: 抽象类 static 成员未实现
 * - INVALID_MEMBER_VISIBILITY_IN_CLASS: abstract/open 成员必须是 public 或 protected
 *
 * 注册为 classLikeCheckers
 */
object CfirInheritanceDeepChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration is CfirClass) {
            checkSealedInheritanceScope(declaration)
            checkAbstractClassStaticUnimplemented(declaration)
            checkMemberVisibilityNotWiderThanClass(declaration)
        }
        checkInheritedMemberKindConsistency(declaration.memberInheritanceSubject())
        checkSuperMembersKindConsistency(declaration)
        checkInheritedMemberTypeConsistency(declaration)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    internal fun checkExtend(declaration: CfirExtend) {
        checkInheritedMemberKindConsistency(declaration.memberInheritanceSubject())
        checkExtendTargetMutCompatibility(declaration)
    }

    /**
     * `extend T <: I` 需要让目标类型 `T` 的既有成员满足接口 `I` 的 mut 签名。
     *
     * 该路径不同于 extend 块内声明成员覆盖接口成员：冲突成员可能已经定义在目标类型上，
     * 因此诊断落在 extend 声明本身。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExtendTargetMutCompatibility(extend: CfirExtend) {
        val targetDecl = extend.extendedTypeRef.resolvedClassLikeDeclaration() ?: return
        val targetScope = context.createUseSiteMemberScope(targetDecl)
        val reported = mutableSetOf<Name>()

        for (superTypeRef in extend.superTypeRefs) {
            val superDecl = superTypeRef.resolvedClassLikeDeclaration() ?: continue
            for (superMember in superDecl.declarations) {
                val superInfo = (superMember as? CfirNamedFunction)
                    ?.symbol
                    ?.inheritedMemberInfoOrNull(context)
                    ?: continue

                val targetInfos = buildList {
                    targetScope.processFunctionsByName(superInfo.name) { symbol ->
                        symbol.inheritedMemberInfoOrNull(context)?.let(::add)
                    }
                }

                if (targetInfos.any { it.hasMutFunctionConflict(superInfo) } && reported.add(superInfo.name)) {
                    reporter.reportOn(
                        source = extend.source?.firstCharacterDiagnosticSource() ?: superTypeRef.source,
                        factory = CfirErrors.INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE,
                    )
                }
            }
        }
    }

    /**
     * 多个父类型中同名成员的声明类型（function/property）不一致。
     *
     * 对齐 C++ sema_inherit_super_member_kind_inconsistent
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSuperMembersKindConsistency(classDecl: CfirClassLikeDeclaration) {
        val kindsByName = mutableMapOf<Name, MutableSet<String>>()
        for (superTypeRef in classDecl.superTypeRefs) {
            val type = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (type is ConeErrorType) continue
            val classId = (type as? ConeClassLikeType)?.classId ?: continue
            val superDecl = context.session.symbolProvider
                .getClassLikeSymbolByClassId(classId)?.cfir as? CfirClassLikeDeclaration ?: continue
            val superScope = context.createUseSiteMemberScope(superDecl)
            for (name in superScope.getCallableNames()) {
                superScope.processCallablesByName(name) { symbol ->
                    val info = symbol.inheritedMemberInfoOrNull(context) ?: return@processCallablesByName
                    kindsByName.getOrPut(info.name) { mutableSetOf() }.add(info.kind)
                }
            }
        }
        for ((name, kinds) in kindsByName) {
            if (kinds.size > 1) {
                reporter.reportOn(
                    source = classDecl.source,
                    factory = CfirErrors.INHERIT_SUPER_MEMBER_KIND_INCONSISTENT,
                    a = name,
                )
            }
        }
    }

    /**
     * 多个父类型中同名函数成员的返回类型不一致（且非子类型关系）。
     *
     * 对齐 C++ sema_inherit_member_type_inconsistent
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInheritedMemberTypeConsistency(classDecl: CfirClassLikeDeclaration) {
        val ownerClassId = (classDecl.symbol as? CfirClassLikeSymbol<*>)?.classId
        val classScope = context.createUseSiteMemberScope(classDecl)
        for (name in classScope.getCallableNames()) {
            val inheritedFunctions = mutableListOf<CfirFunctionSymbol<*>>()
            classScope.processFunctionsByName(name) { symbol ->
                if (symbol.ownerClassId(context) != ownerClassId) {
                    inheritedFunctions += symbol
                }
            }
            val bySignature = inheritedFunctions
                .filter { it.isBound }
                .groupBy { it.overrideSignatureKey() }
            for ((_, symbols) in bySignature) {
                val returnTypes = symbols.mapNotNull { it.resolvedReturnTypeOrNull(context) }
                    .filterNot { it is ConeErrorType }
                if (returnTypes.size < 2 || !returnTypes.hasInconsistentInheritedTypes(context)) continue
                reporter.reportOn(
                    source = classDecl.source,
                    factory = CfirErrors.INHERIT_MEMBER_TYPE_INCONSISTENT,
                    a = "return types",
                    b = "function",
                    c = name,
                )
                return
            }
        }
    }

    private fun CfirFunctionSymbol<*>.resolvedReturnTypeOrNull(
        context: CheckerContext,
    ): ConeCangJieType? {
        if (!isBound) return null
        (cfir.returnTypeRef as? CfirResolvedTypeRef)?.coneType?.let { return it }
        return context.returnTypeCalculator.tryCalculateReturnType(cfir).coneType
    }

    private fun List<ConeCangJieType>.hasInconsistentInheritedTypes(context: CheckerContext): Boolean {
        val typeCheckerState = context.session.typeContext
        for (i in indices) {
            for (j in i + 1 until size) {
                val first = this[i]
                val second = this[j]
                if (AbstractTypeChecker.equalTypes(typeCheckerState, first, second)) continue
                val related = AbstractTypeChecker.isSubtypeOf(typeCheckerState, first, second) ||
                        AbstractTypeChecker.isSubtypeOf(typeCheckerState, second, first)
                if (!related) return true
            }
        }
        return false
    }

    /**
     * sealed 类只能在同一个包中被继承。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSealedInheritanceScope(classDecl: CfirClass) {
        for (superTypeRef in classDecl.superTypeRefs) {
            val resolvedType = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (resolvedType is ConeErrorType) continue
            val superClassId = (resolvedType as? ConeClassLikeType)?.classId ?: continue
            val superSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(superClassId) ?: continue
            val superDecl = superSymbol.cfir as? CfirClass ?: continue

            if (superDecl.status.isSealed) {
                val superPackage = superClassId.packageFqName
                val currentPackage = classDecl.symbol.classId.packageFqName
                if (superPackage != currentPackage) {
                    reporter.reportOn(
                        source = superTypeRef.source ?: classDecl.source,
                        factory = CfirErrors.CANNOT_INHERIT_SEALED,
                        a = "class",
                        b = classDecl.name.asString(),
                        c = "sealed class",
                        d = superDecl.name,
                    )
                }
            }
        }
    }

    /**
     * 继承抽象类时，父类的 static 抽象函数必须被实现。
     *
     * 对齐 C++ DiagKind::sema_inherit_abstract_class_static_unimplement_func
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAbstractClassStaticUnimplemented(classDecl: CfirClass) {
        if (classDecl.status.isAbstract) return // 抽象类本身不需要实现

        for (superTypeRef in classDecl.superTypeRefs) {
            val resolvedType = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (resolvedType is ConeErrorType) continue
            val superClassId = (resolvedType as? ConeClassLikeType)?.classId ?: continue
            val superSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(superClassId) ?: continue
            val superDecl = superSymbol.cfir as? CfirClass ?: continue
            if (!superDecl.status.isAbstract) continue

            // 查找父类中的 static abstract 函数
            for (superMember in superDecl.declarations) {
                if (superMember !is CfirNamedFunction) continue
                if (!superMember.status.isStatic || !superMember.status.isAbstract) continue

                // 检查子类是否实现了该 static 函数
                val implemented = classDecl.declarations.any { member ->
                    member is CfirNamedFunction &&
                        member.status.isStatic &&
                        member.name == superMember.name &&
                        member.body != null
                }
                if (!implemented) {
                    reporter.reportOn(
                        source = classDecl.source,
                        factory = CfirErrors.INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC,
                        a = classDecl.name,
                        b = "static function",
                        c = superMember.name,
                    )
                }
            }
        }
    }

    /**
     * abstract/open 成员必须是 public 或 protected。
     *
     * 对齐 C++ DeclAttributeChecker.cpp:
     * - 抽象类中的 abstract 成员不能是 private/internal；
     * - 可继承类中的 open 成员不能是 private/internal。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMemberVisibilityNotWiderThanClass(classDecl: CfirClass) {
        val classIsAbstract = classDecl.status.isAbstract
        val classIsInheritable = classDecl.status.isOpen || classDecl.status.isAbstract
        for (member in classDecl.declarations) {
            val memberInfo = when (member) {
                is CfirNamedFunction -> InvalidVisibilityMemberInfo(
                    visibility = member.status.visibility,
                    modifier = member.invalidVisibilityModifier(classIsAbstract, classIsInheritable),
                    kind = "function",
                    source = member.functionNameDiagnosticSource(),
                )

                is CfirProperty -> InvalidVisibilityMemberInfo(
                    visibility = member.status.visibility,
                    modifier = member.invalidVisibilityModifier(classIsAbstract, classIsInheritable),
                    kind = "property",
                    source = member.propertyNameDiagnosticSource(),
                )
                else -> continue
            }
            if (memberInfo.modifier != null &&
                memberInfo.visibility != Visibilities.Public &&
                memberInfo.visibility != Visibilities.Protected
            ) {
                reporter.reportOn(
                    source = memberInfo.source ?: member.source ?: classDecl.source,
                    factory = CfirErrors.INVALID_MEMBER_VISIBILITY_IN_CLASS,
                    a = memberInfo.modifier,
                    b = memberInfo.kind,
                )
            }
        }
    }

    private data class InvalidVisibilityMemberInfo(
        val visibility: Visibility,
        val modifier: String?,
        val kind: String,
        val source: AbstractCjSourceElement?,
    )

    private fun CfirNamedFunction.invalidVisibilityModifier(
        classIsAbstract: Boolean,
        classIsInheritable: Boolean,
    ): String? {
        if (status.isStatic) return null
        if (status.isAbstract && classIsAbstract) return "abstract"
        if (status.isOpen && classIsInheritable) return "open"
        return null
    }

    private fun CfirProperty.invalidVisibilityModifier(
        classIsAbstract: Boolean,
        classIsInheritable: Boolean,
    ): String? {
        if (status.isStatic) return null
        if (status.isAbstract && classIsAbstract) return "abstract"
        if (status.isOpen && classIsInheritable) return "open"
        return null
    }

    /**
     * 检查继承的同名成员之间的声明类型（函数/属性）一致性。
     *
     * 对齐 C++ InheritanceChecker:
     * - INHERIT_MEMBER_KIND_INCONSISTENT: 子类成员类型与父类同名成员不一致
     * - INHERIT_SUPER_MEMBER_KIND_INCONSISTENT: 多个父类型的同名成员类型不一致
     * - INHERIT_MEMBER_TYPE_INCONSISTENT: 多个父类型的同名成员返回类型不一致
     * - INHERIT_NOT_RETURN_THIS: open 函数返回 This 类型时 override 必须保持
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInheritedMemberKindConsistency(subject: MemberInheritanceSubject) {
        val ownMembers = subject.declarations.mapNotNull { member ->
            when (member) {
                is CfirNamedFunction -> InheritedMemberInfo(
                    name = member.name,
                    kind = "function",
                    isStatic = member.status.isStatic,
                    isConst = member.status.isConst,
                    isMut = member.status.isMut,
                    source = member.source,
                    nameSource = member.functionNameDiagnosticSource(),
                    ownerName = null,
                    symbol = member.symbol,
                )

                is CfirProperty -> InheritedMemberInfo(
                    name = member.name,
                    kind = member.inheritanceMemberKind(),
                    isStatic = member.status.isStatic,
                    isConst = member.status.isConst,
                    isMut = member.status.isMut,
                    source = member.source,
                    nameSource = member.propertyNameDiagnosticSource(),
                    ownerName = null,
                    symbol = member.symbol,
                )

                is CfirFieldVariable -> InheritedMemberInfo(
                    name = member.name,
                    kind = "variable",
                    isStatic = member.status.isStatic,
                    isConst = false,
                    isMut = false,
                    source = member.source,
                    nameSource = member.fieldVariableNameDiagnosticSource(),
                    ownerName = null,
                    symbol = member.symbol,
                )

                else -> null
            }
        }.groupBy { it.name }

        val reportedStaticConflicts = mutableSetOf<Name>()
        val reportedKindConflicts = mutableSetOf<Name>()
        val reportedConstConflicts = mutableSetOf<String>()
        val reportedMutConflicts = mutableSetOf<String>()
        val reportedVariableShadows = mutableSetOf<Name>()
        val reportedCannotOverrides = mutableSetOf<String>()
        val reportedExtendOverrides = mutableSetOf<String>()
        for (inheritedSource in subject.inheritedSources) {
            val superType = (inheritedSource.typeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (superType is ConeErrorType) continue
            val superClassId = (superType as? ConeClassLikeType)?.classId ?: continue
            val superSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(superClassId) ?: continue
            val superDecl = superSymbol.cfir as? CfirClassLikeDeclaration ?: continue
            val superScope = context.createUseSiteMemberScope(superDecl)

            for (name in ownMembers.keys) {
                val superInfos = buildList {
                    superScope.processCallablesByName(name) { symbol ->
                        symbol.inheritedMemberInfoOrNull(context)?.let(::add)
                    }
                    if (inheritedSource.includeDirectExtends) {
                        addAll(collectDirectExtendMemberInfos(superClassId, name, context))
                    }
                }
                for (superInfo in superInfos) {
                    val classDecl = subject.classLikeDeclaration
                    if (classDecl != null && superInfo.symbol?.isVisibleIn(classDecl, context) == false) {
                        continue
                    }

                    val ownSameNameMembers = ownMembers[superInfo.name].orEmpty()

                    for (ownInfo in ownSameNameMembers) {
                        val hasStaticConflict = ownInfo.isStatic != superInfo.isStatic
                        if (hasStaticConflict) {
                            if (reportedStaticConflicts.add(ownInfo.name)) {
                                reporter.reportOn(
                                    source = ownInfo.source?.firstCharacterDiagnosticSource()
                                        ?: ownInfo.nameSource
                                        ?: subject.source,
                                    factory = CfirErrors.INHERIT_MEMBER_KIND_INCONSISTENT,
                                    a = ownInfo.staticKind,
                                    b = ownInfo.name,
                                    c = superInfo.staticKind,
                                    d = superInfo.ownerName ?: superClassId.shortClassName,
                                )
                            }
                        }

                        if (ownInfo.kind != superInfo.kind) {
                            if (!hasStaticConflict && reportedKindConflicts.add(ownInfo.name)) {
                                reporter.reportOn(
                                    source = ownInfo.nameSource ?: subject.source,
                                    factory = CfirErrors.INHERIT_MEMBER_KIND_INCONSISTENT,
                                    a = ownInfo.kind,
                                    b = ownInfo.name,
                                    c = superInfo.kind,
                                    d = superInfo.ownerName ?: superClassId.shortClassName,
                                )
                            }
                            continue
                        }

                        if (!hasStaticConflict && ownInfo.hasConstFunctionConflict(superInfo)) {
                            val key = ownInfo.overrideDiagnosticKey(superInfo)
                            if (reportedConstConflicts.add(key)) {
                                reporter.reportOn(
                                    source = ownInfo.nameSource ?: ownInfo.source ?: subject.source,
                                    factory = CfirErrors.INHERIT_MEMBER_KIND_INCONSISTENT,
                                    a = "non-constant function",
                                    b = ownInfo.name,
                                    c = "'const' function",
                                    d = superInfo.ownerName ?: superClassId.shortClassName,
                                )
                            }
                        }

                        if (!hasStaticConflict && ownInfo.hasMutFunctionConflict(superInfo)) {
                            val key = ownInfo.overrideDiagnosticKey(superInfo)
                            if (reportedMutConflicts.add(key)) {
                                reporter.reportOn(
                                    source = ownInfo.source?.firstCharacterDiagnosticSource()
                                        ?: ownInfo.nameSource
                                        ?: subject.source,
                                    factory = CfirErrors.INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE,
                                )
                            }
                        }

                        if (!hasStaticConflict && ownInfo.kind == "variable" && reportedVariableShadows.add(ownInfo.name)) {
                            reporter.reportOn(
                                source = ownInfo.source?.firstCharacterDiagnosticSource() ?: subject.source,
                                factory = CfirErrors.MEMBER_VARIABLE_CAN_NOT_SHADOW,
                                a = ownInfo.name,
                            )
                        }

                        if (subject.classLikeDeclaration != null && ownInfo.overridesExtendMember(superInfo, context)) {
                            val key = ownInfo.overrideDiagnosticKey(superInfo)
                            if (reportedExtendOverrides.add(key)) {
                                reporter.reportOn(
                                    source = ownInfo.nameSource ?: ownInfo.source ?: subject.source,
                                    factory = CfirErrors.EXTEND_FUNCTION_CANNOT_OVERRIDDEN,
                                    a = ownInfo.kind,
                                    b = ownInfo.name,
                                )
                            }
                            continue
                        }

                        if (classDecl != null && ownInfo.canNotOverride(superInfo, classDecl, context)) {
                            val key = ownInfo.overrideDiagnosticKey(superInfo)
                            if (reportedCannotOverrides.add(key)) {
                                reporter.reportOn(
                                    source = ownInfo.nameSource ?: ownInfo.source ?: subject.source,
                                    factory = CfirErrors.CANNOT_OVERRIDE,
                                    a = ownInfo.kind,
                                    b = ownInfo.name,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun CfirCallableSymbol<*>.inheritedMemberInfoOrNull(context: CheckerContext): InheritedMemberInfo? {
        if (!isBound) return null
        val declaration = cfir
        if (!canBeInheritedMember()) return null
        val ownerName = ownerClassId(context)?.shortClassName
        return when (declaration) {
            is CfirNamedFunction -> InheritedMemberInfo(
                name = declaration.name,
                kind = "function",
                isStatic = declaration.status.isStatic,
                isConst = declaration.status.isConst,
                isMut = declaration.status.isMut,
                source = declaration.source,
                nameSource = declaration.functionNameDiagnosticSource(),
                ownerName = ownerName,
                symbol = this,
            )

            is CfirProperty -> InheritedMemberInfo(
                name = declaration.name,
                kind = declaration.inheritanceMemberKind(),
                isStatic = declaration.status.isStatic,
                isConst = declaration.status.isConst,
                isMut = declaration.status.isMut,
                source = declaration.source,
                nameSource = declaration.propertyNameDiagnosticSource(),
                ownerName = ownerName,
                symbol = this,
            )

            is CfirFieldVariable -> InheritedMemberInfo(
                name = declaration.name,
                kind = "variable",
                isStatic = declaration.status.isStatic,
                isConst = false,
                isMut = false,
                source = declaration.source,
                nameSource = declaration.fieldVariableNameDiagnosticSource(),
                ownerName = ownerName,
                symbol = this,
            )

            else -> null
        }
    }

    /**
     * 官方 GetInheritedSuperMembers 会把父类型可见 extend 的成员并入继承成员表。
     * 本项目 declaration-site scope 不承担这件事，因此继承诊断在这里显式读取 extendProvider。
     */
    private fun collectDirectExtendMemberInfos(
        superClassId: ClassId,
        name: Name,
        context: CheckerContext,
    ): List<InheritedMemberInfo> {
        val provider = context.session.extendProvider
        return buildList {
            for (extend in provider.getExtendsForClass(superClassId)) {
                if (!provider.isExtendAccessible(extend)) continue
                for (member in extend.declarations) {
                    when (member) {
                        is CfirNamedFunction -> {
                            if (member.name != name) continue
                            member.symbol?.inheritedMemberInfoOrNull(context)?.let(::add)
                        }

                        is CfirProperty -> {
                            if (member.name != name) continue
                            member.symbol.inheritedMemberInfoOrNull(context)?.let(::add)
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    /**
     * 官方继承检查在收集 inherited member 后会执行 RemoveMembersShouldNotInherit，
     * private 成员不会进入后续同名成员、类型一致性和 shadow 检查。
     * 继承诊断还需要在 use-site 处按派生类视角过滤 internal/package 可见性，
     * 该过滤在 [checkInheritedMemberKindConsistency] 拿到 subject 后完成。
     */
    private fun CfirCallableSymbol<*>.canBeInheritedMember(): Boolean =
        cfir.status.visibility != Visibilities.Private

    /**
     * 官方 CheckInheritanceAttributes 在同 kind 的函数/属性形成覆盖关系后检查父成员开放性。
     * 非 abstract、非 static、非 interface 且没有 open 语义的父成员不能被子成员覆盖。
     */
    private fun InheritedMemberInfo.canNotOverride(
        superInfo: InheritedMemberInfo,
        classDecl: CfirClassLikeDeclaration,
        context: CheckerContext,
    ): Boolean {
        if (kind != superInfo.kind) return false
        if (kind != "function" && kind != "property") return false
        if (!hasSameOverrideSignature(superInfo)) return false

        val superSymbol = superInfo.symbol ?: return false
        if (!superSymbol.isBound) return false
        if (superSymbol.cfir.status.isStatic) return false
        if (superSymbol.isAbstractLike(context)) return false
        if (superSymbol.isOverridableFrom(classDecl, context)) return false

        return true
    }

    /**
     * 官方 CheckExtendMemberValid 会先处理“类成员覆盖父类型 extend 成员”的情况，
     * 报 sema_extend_function_cannot_overridden 后不再进入普通 cannot-override 检查。
     */
    private fun InheritedMemberInfo.overridesExtendMember(
        superInfo: InheritedMemberInfo,
        context: CheckerContext,
    ): Boolean {
        if (kind != superInfo.kind) return false
        if (kind != "function" && kind != "property") return false
        if (!hasSameOverrideSignature(superInfo)) return false
        val superSymbol = superInfo.symbol ?: return false
        val originalSuperSymbol = superSymbol.unwrapSubstitutionOverrides()
        return context.session.extendProvider.getContainingExtend(originalSuperSymbol) != null
    }

    private fun InheritedMemberInfo.hasSameOverrideSignature(superInfo: InheritedMemberInfo): Boolean {
        if (kind == "property") return true
        val ownSymbol = symbol ?: return false
        val superSymbol = superInfo.symbol ?: return false
        return ownSymbol.overrideSignatureKey() == superSymbol.overrideSignatureKey()
    }

    private fun InheritedMemberInfo.hasConstFunctionConflict(superInfo: InheritedMemberInfo): Boolean {
        if (kind != "function" || superInfo.kind != "function") return false
        if (isConst || !superInfo.isConst) return false
        return hasSameOverrideSignature(superInfo)
    }

    private fun InheritedMemberInfo.hasMutFunctionConflict(superInfo: InheritedMemberInfo): Boolean {
        if (kind != "function" || superInfo.kind != "function") return false
        if (isMut == superInfo.isMut) return false
        return hasSameOverrideSignature(superInfo)
    }

    private fun InheritedMemberInfo.overrideDiagnosticKey(superInfo: InheritedMemberInfo): String =
        buildString {
            append(kind)
            append(':')
            append(name.asString())
            append(':')
            append(symbol?.overrideSignatureKey().orEmpty())
            append(':')
            append(superInfo.symbol?.overrideSignatureKey().orEmpty())
        }

    private data class InheritedMemberInfo(
        val name: Name,
        val kind: String,
        val isStatic: Boolean,
        val isConst: Boolean,
        val isMut: Boolean,
        val source: CjSourceElement?,
        val nameSource: AbstractCjSourceElement?,
        val ownerName: Name?,
        val symbol: CfirCallableSymbol<*>?,
    ) {
        val staticKind: String get() = if (isStatic) "static" else "non-static"
    }

    /**
     * 主构造 `let/var` 参数在 CFIR 中复用 property 结构承载 getter/setter，
     * 但官方继承检查把它们作为 VAR_DECL 参与同名成员 shadow 规则。
     */
    private fun CfirProperty.inheritanceMemberKind(): String =
        if (source?.kind == CjFakeSourceElementKind.PropertyFromParameter) "variable" else "property"

    private data class MemberInheritanceSubject(
        val declarations: List<CfirDeclaration>,
        val inheritedSources: List<InheritedMemberSource>,
        val source: CjSourceElement?,
        val classLikeDeclaration: CfirClassLikeDeclaration?,
    )

    private data class InheritedMemberSource(
        val typeRef: CfirTypeRef,
        val includeDirectExtends: Boolean,
    )

    private fun CfirClassLikeDeclaration.memberInheritanceSubject(): MemberInheritanceSubject =
        MemberInheritanceSubject(
            declarations = declarations,
            inheritedSources = superTypeRefs.map { InheritedMemberSource(it, includeDirectExtends = true) },
            source = source,
            classLikeDeclaration = this,
        )

    private fun CfirExtend.memberInheritanceSubject(): MemberInheritanceSubject =
        MemberInheritanceSubject(
            declarations = declarations,
            inheritedSources = buildList {
                add(InheritedMemberSource(extendedTypeRef, includeDirectExtends = false))
                addAll(superTypeRefs.map { InheritedMemberSource(it, includeDirectExtends = false) })
            },
            source = source,
            classLikeDeclaration = null,
        )

    context(context: CheckerContext)
    private fun CfirTypeRef.resolvedClassLikeDeclaration(): CfirClassLikeDeclaration? {
        val classId = ((this as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType)?.classId ?: return null
        val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        return symbol.cfir as? CfirClassLikeDeclaration
    }

}

/**
 * extend 声明也属于官方 `InheritableDecl`，需要进入同一组继承成员一致性检查。
 */
object CfirExtendInheritanceDeepChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirExtend) {
        CfirInheritanceDeepChecker.checkExtend(declaration)
    }
}
