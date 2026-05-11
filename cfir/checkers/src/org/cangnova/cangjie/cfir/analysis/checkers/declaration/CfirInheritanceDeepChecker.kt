package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.Name

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
        if (declaration !is CfirClass) return
        checkSealedInheritanceScope(declaration)
        checkAbstractClassStaticUnimplemented(declaration)
        checkMemberVisibilityNotWiderThanClass(declaration)
        checkInheritedMemberKindConsistency(declaration)
        checkSuperMembersKindConsistency(declaration)
        checkInheritedMemberTypeConsistency(declaration)
        checkOverrideReturnThis(declaration)
    }

    /**
     * 多个父类型中同名成员的声明类型（function/property）不一致。
     *
     * 对齐 C++ sema_inherit_super_member_kind_inconsistent
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSuperMembersKindConsistency(classDecl: CfirClass) {
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
    private fun checkInheritedMemberTypeConsistency(classDecl: CfirClass) {
        val fnTypesByName = mutableMapOf<Name, MutableList<org.cangnova.cangjie.cfir.types.ConeCangJieType>>()
        for (superTypeRef in classDecl.superTypeRefs) {
            val type = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (type is ConeErrorType) continue
            val classId = (type as? ConeClassLikeType)?.classId ?: continue
            val superDecl = context.session.symbolProvider
                .getClassLikeSymbolByClassId(classId)?.cfir as? CfirClassLikeDeclaration ?: continue
            for (m in superDecl.declarations) {
                if (m !is CfirNamedFunction) continue
                val rt = (m.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
                fnTypesByName.getOrPut(m.name) { mutableListOf() }.add(rt)
            }
        }
        for ((name, types) in fnTypesByName) {
            if (types.size < 2) continue
            val first = types[0]
            if (types.any { it != first }) {
                reporter.reportOn(
                    source = classDecl.source,
                    factory = CfirErrors.INHERIT_MEMBER_TYPE_INCONSISTENT,
                    a = "return types",
                    b = "function",
                    c = name,
                )
            }
        }
    }

    /**
     * 父类 open 函数返回 This 时，override 子函数必须保持 This。
     *
     * 对齐 C++ sema_inherit_not_return_this
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkOverrideReturnThis(classDecl: CfirClass) {
        for (member in classDecl.declarations) {
            if (member !is CfirNamedFunction) continue
            if (!member.status.isOverride) continue
            val myRet = (member.returnTypeRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType
            if (myRet?.isThisType == true) continue

            // 在父类型中查找同名 open 函数
            var superReturnsThis = false
            for (superTypeRef in classDecl.superTypeRefs) {
                val t = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
                val cid = (t as? ConeClassLikeType)?.classId ?: continue
                val sd = context.session.symbolProvider
                    .getClassLikeSymbolByClassId(cid)?.cfir as? CfirClassLikeDeclaration ?: continue
                val sm = sd.declarations.firstOrNull {
                    it is CfirNamedFunction && it.name == member.name
                } as? CfirNamedFunction ?: continue
                val sr = (sm.returnTypeRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType
                if (sr?.isThisType == true) { superReturnsThis = true; break }
            }
            if (superReturnsThis) {
                reporter.reportOn(
                    source = member.returnTypeRef.source ?: member.source ?: classDecl.source,
                    factory = CfirErrors.INHERIT_NOT_RETURN_THIS,
                )
            }
        }
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
    private fun checkInheritedMemberKindConsistency(classDecl: CfirClass) {
        // 收集子类自身的成员
        val ownMembers = classDecl.declarations.mapNotNull { member ->
            when (member) {
                is CfirNamedFunction -> member.name to "function"
                is CfirProperty -> member.name to "property"
                else -> null
            }
        }.toMap()

        // 对每个父类型，检查同名成员的声明类型是否一致
        for (superTypeRef in classDecl.superTypeRefs) {
            val superType = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (superType is ConeErrorType) continue
            val superClassId = (superType as? ConeClassLikeType)?.classId ?: continue
            val superSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(superClassId) ?: continue
            val superDecl = superSymbol.cfir as? CfirClassLikeDeclaration ?: continue

            for (superMember in superDecl.declarations) {
                val (superName, superKind) = when (superMember) {
                    is CfirNamedFunction -> superMember.name to "function"
                    is CfirProperty -> superMember.name to "property"
                    else -> continue
                }
                val ownKind = ownMembers[superName] ?: continue

                if (ownKind != superKind) {
                    reporter.reportOn(
                        source = classDecl.source,
                        factory = CfirErrors.INHERIT_MEMBER_KIND_INCONSISTENT,
                        a = ownKind,
                        b = superName,
                        c = superKind,
                        d = superClassId.shortClassName,
                    )
                }
            }
        }
    }
}
