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
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.AbstractCjSourceElement
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
        checkInheritedMemberKindConsistency(declaration)
        checkSuperMembersKindConsistency(declaration)
        checkInheritedMemberTypeConsistency(declaration)
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
            for (m in superDecl.declarations) {
                val (n, k) = when (m) {
                    is CfirNamedFunction -> m.name to "function"
                    is CfirProperty -> m.name to "property"
                    else -> continue
                }
                kindsByName.getOrPut(n) { mutableSetOf() }.add(k)
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
            val (memberVisibility, modifier, memberKind) = when (member) {
                is CfirNamedFunction -> Triple(
                    member.status.visibility,
                    member.invalidVisibilityModifier(classIsAbstract, classIsInheritable),
                    "function",
                )
                is CfirProperty -> Triple(
                    member.status.visibility,
                    member.invalidVisibilityModifier(classIsAbstract, classIsInheritable),
                    "property",
                )
                else -> continue
            }
            if (modifier != null &&
                memberVisibility != Visibilities.Public &&
                memberVisibility != Visibilities.Protected
            ) {
                reporter.reportOn(
                    source = member.source ?: classDecl.source,
                    factory = CfirErrors.INVALID_MEMBER_VISIBILITY_IN_CLASS,
                    a = modifier,
                    b = memberKind,
                )
            }
        }
    }

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
    private fun checkInheritedMemberKindConsistency(classDecl: CfirClassLikeDeclaration) {
        val ownMembers = classDecl.declarations.mapNotNull { member ->
            when (member) {
                is CfirNamedFunction -> InheritedMemberInfo(
                    name = member.name,
                    kind = "function",
                    isStatic = member.status.isStatic,
                    source = member.functionNameDiagnosticSource(),
                )

                is CfirProperty -> InheritedMemberInfo(
                    name = member.name,
                    kind = "property",
                    isStatic = member.status.isStatic,
                    source = member.source,
                )

                else -> null
            }
        }.groupBy { it.name }

        val reportedStaticConflicts = mutableSetOf<Name>()
        val reportedKindConflicts = mutableSetOf<Name>()
        for (superTypeRef in classDecl.superTypeRefs) {
            val superType = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (superType is ConeErrorType) continue
            val superClassId = (superType as? ConeClassLikeType)?.classId ?: continue
            val superSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(superClassId) ?: continue
            val superDecl = superSymbol.cfir as? CfirClassLikeDeclaration ?: continue

            for (superMember in superDecl.declarations) {
                val superInfo = when (superMember) {
                    is CfirNamedFunction -> InheritedMemberInfo(
                        name = superMember.name,
                        kind = "function",
                        isStatic = superMember.status.isStatic,
                        source = superMember.source,
                    )

                    is CfirProperty -> InheritedMemberInfo(
                        name = superMember.name,
                        kind = "property",
                        isStatic = superMember.status.isStatic,
                        source = superMember.source,
                    )

                    else -> continue
                }
                val ownSameNameMembers = ownMembers[superInfo.name].orEmpty()

                for (ownInfo in ownSameNameMembers) {
                    if (ownInfo.isStatic != superInfo.isStatic) {
                        if (reportedStaticConflicts.add(ownInfo.name)) {
                            reporter.reportOn(
                                source = ownInfo.source ?: classDecl.source,
                                factory = CfirErrors.INHERIT_MEMBER_KIND_INCONSISTENT,
                                a = ownInfo.staticKind,
                                b = ownInfo.name,
                                c = superInfo.staticKind,
                                d = superClassId.shortClassName,
                            )
                        }
                        continue
                    }

                    if (ownInfo.kind != superInfo.kind) {
                        if (reportedKindConflicts.add(ownInfo.name)) {
                            reporter.reportOn(
                                source = ownInfo.source ?: classDecl.source,
                                factory = CfirErrors.INHERIT_MEMBER_KIND_INCONSISTENT,
                                a = ownInfo.kind,
                                b = ownInfo.name,
                                c = superInfo.kind,
                                d = superClassId.shortClassName,
                            )
                        }
                        continue
                    }
                }
            }
        }
    }

    private data class InheritedMemberInfo(
        val name: Name,
        val kind: String,
        val isStatic: Boolean,
        val source: AbstractCjSourceElement?,
    ) {
        val staticKind: String get() = if (isStatic) "static" else "non-static"
    }

}
