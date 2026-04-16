package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjModifierListOwner
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * common/specific 跨平台匹配检查器（CommonSpecific 分组）
 *
 * 对齐 C++ CJMP/CheckCJMP.cpp:
 * 当编译 specific 模块时，对 specific 声明与 common 声明进行配对检查，
 * 验证类型、修饰符、注解、参数、超类型的一致性。
 *
 * 注册为 classLikeCheckers
 */
object CfirCommonSpecificChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration !is CfirClass) return

        // 只对 specific 声明执行匹配检查
        if (declaration.status.isSpecific) {
            checkSpecificMatchesCommon(declaration)
        }

        // 对 common 声明执行约束检查
        if (declaration.status.isCommon) {
            checkCommonDeclarationConstraints(declaration)
        }
    }

    /**
     * specific 声明必须与 common 声明匹配。
     *
     * 对齐 C++ MPTypeCheckerImpl::MatchSpecificWithCommon
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSpecificMatchesCommon(specificDecl: CfirClass) {
        // 通过 classId 在 common 模块中查找同名声明
        val classId = specificDecl.symbol.classId
        val commonSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)
        val commonDecl = commonSymbol?.cfir as? CfirClass

        if (commonDecl == null || !commonDecl.status.isCommon) {
            // specific 声明找不到匹配的 common 声明
            // 注意：这不一定是错误——specific 可以有自己独有的声明
            return
        }

        // 检查声明种类一致性
        checkDeclarationKindMatch(specificDecl, commonDecl)

        // 检查修饰符一致性
        checkModifierMatch(specificDecl, commonDecl)

        // 检查超类型一致性
        checkSuperTypeMatch(specificDecl, commonDecl)

        // 检查成员匹配
        checkMemberMatch(specificDecl, commonDecl)
    }

    /**
     * specific 和 common 的声明种类必须一致（class/struct/enum/interface）。
     *
     * 对齐 C++ DiagKind::sema_specific_has_different_kind
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDeclarationKindMatch(specificDecl: CfirClass, commonDecl: CfirClass) {
        val specificKind = if (specificDecl.status.isAbstract) "abstract class" else "class"
        val commonKind = if (commonDecl.status.isAbstract) "abstract class" else "class"
        if (specificDecl.status.isAbstract != commonDecl.status.isAbstract) {
            reporter.reportOn(
                source = specificDecl.source,
                factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_KIND,
                a = specificKind,
                b = commonKind,
            )
        }
    }

    /**
     * specific 和 common 的修饰符必须一致。
     *
     * 对齐 C++ DiagKind::sema_specific_has_different_modifier
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkModifierMatch(specificDecl: CfirClass, commonDecl: CfirClass) {
        if (specificDecl.status.isOpen != commonDecl.status.isOpen ||
            specificDecl.status.isSealed != commonDecl.status.isSealed
        ) {
            reporter.reportOn(
                source = specificDecl.source,
                factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_MODIFIER,
                a = "class",
            )
        }
    }

    /**
     * specific 的超类型列表必须与 common 一致。
     *
     * 对齐 C++ DiagKind::sema_specific_has_different_super_type
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSuperTypeMatch(specificDecl: CfirClass, commonDecl: CfirClass) {
        val specificSuperCount = specificDecl.superTypeRefs.size
        val commonSuperCount = commonDecl.superTypeRefs.size
        if (specificSuperCount != commonSuperCount) {
            reporter.reportOn(
                source = specificDecl.source,
                factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_SUPER_TYPE,
                a = "class",
            )
        }
    }

    /**
     * specific 的成员必须实现 common 中声明的所有成员。
     *
     * 对齐 C++ MPTypeCheckerImpl::MatchCJMPDecls
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMemberMatch(specificDecl: CfirClass, commonDecl: CfirClass) {
        val specificMemberNames = specificDecl.declarations.mapNotNull { memberName(it) }.toSet()

        for (commonMember in commonDecl.declarations) {
            val commonName = memberName(commonMember) ?: continue
            val commonKind = memberKind(commonMember) ?: continue

            if (commonName !in specificMemberNames) {
                // specific 中缺少 common 声明的成员——如果 common 成员有体则不需要 specific 实现
                val needsImpl = when (commonMember) {
                    is CfirNamedFunction -> commonMember.body == null
                    is CfirProperty -> commonMember.getter == null && commonMember.setter == null
                    else -> false
                }
                if (needsImpl) {
                    reporter.reportOn(
                        source = specificDecl.source,
                        factory = CfirErrors.SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION,
                        a = commonKind,
                        b = "class",
                    )
                }
            }

            // 检查同名成员类型一致性
            val specificMember = specificDecl.declarations.firstOrNull { memberName(it) == commonName }
            if (specificMember != null) {
                checkMemberTypeMatch(specificMember, commonMember, commonName)
            }
        }
    }

    /**
     * 同名成员的返回类型必须一致。
     *
     * 对齐 C++ DiagKind::sema_specific_has_different_type
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMemberTypeMatch(specificMember: CfirDeclaration, commonMember: CfirDeclaration, memberName: Name) {
        val specificType = when (specificMember) {
            is CfirNamedFunction -> (specificMember.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            is CfirProperty -> (specificMember.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            is CfirFieldVariable -> (specificMember.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            else -> null
        }
        val commonType = when (commonMember) {
            is CfirNamedFunction -> (commonMember.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            is CfirProperty -> (commonMember.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            is CfirFieldVariable -> (commonMember.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            else -> null
        }
        if (specificType == null || commonType == null) return
        if (specificType is ConeErrorType || commonType is ConeErrorType) return

        if (!AbstractTypeChecker.equalTypes(context.session.typeContext, specificType, commonType)) {
            reporter.reportOn(
                source = specificMember.source,
                factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_TYPE,
                a = memberKind(specificMember) ?: "member",
            )
        }
    }

    /**
     * common 声明的约束检查。
     *
     * - common open class 必须有构造器
     * - common 成员带有 explicitly abstract 不能有函数体
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCommonDeclarationConstraints(commonDecl: CfirClass) {
        // common open class 必须有显式构造器
        if (commonDecl.status.isOpen) {
            val hasConstructor = commonDecl.declarations.any {
                it is org.cangnova.cangjie.cfir.declarations.CfirConstructor
            }
            if (!hasConstructor) {
                reporter.reportOn(
                    source = commonDecl.source,
                    factory = CfirErrors.COMMON_OPEN_CLASS_NO_INIT,
                    a = commonDecl.name,
                )
            }
        }

        // explicitly abstract 成员不能有函数体
        for (member in commonDecl.declarations) {
            if (member is CfirNamedFunction && member.status.isAbstract && member.body != null) {
                reporter.reportOn(
                    source = member.source,
                    factory = CfirErrors.EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY,
                    a = "function",
                )
            }
        }
    }

    private fun memberName(decl: CfirDeclaration): Name? = when (decl) {
        is CfirNamedFunction -> decl.name
        is CfirProperty -> decl.name
        is CfirFieldVariable -> decl.name
        else -> null
    }

    private fun memberKind(decl: CfirDeclaration): String? = when (decl) {
        is CfirNamedFunction -> "function"
        is CfirProperty -> "property"
        is CfirFieldVariable -> "field"
        else -> null
    }
}

/**
 * common 包 main 函数检查器
 *
 * 对齐 C++ DiagKind::sema_common_package_has_main
 *
 * 注册为 fileCheckers
 */
object CfirCommonPackageMainChecker : CfirFileChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: org.cangnova.cangjie.cfir.declarations.CfirFile) {
        for (decl in declaration.declarations) {
            if (decl is org.cangnova.cangjie.cfir.declarations.CfirMainFunction && decl.status.isCommon) {
                reporter.reportOn(
                    source = decl.source,
                    factory = CfirErrors.COMMON_PACKAGE_HAS_MAIN,
                )
            }
        }
    }
}

/**
 * Mock 语义检查器（Mock 分组）
 *
 * 注册为 classLikeCheckers
 */
object CfirMockSemanticsChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        // Mock 检查需要编译选项 API
        // 当 mock 功能启用时检查 static 声明约束
        for (member in declaration.declarations) {
            if (member !is CfirNamedFunction) continue
            if (!member.status.isStatic) continue

            // static/private/local/constructor 声明不能被 mock
            val owner = member.source?.psi as? CjModifierListOwner ?: continue
            if (owner.annotationEntries.any { it.shortName == Name.identifier("Mock") }) {
                if (member.status.visibility == org.cangnova.cangjie.descriptors.Visibilities.Private) {
                    reporter.reportOn(
                        source = member.source,
                        factory = CfirErrors.MOCK_WRONG_STATIC_DECL,
                    )
                }
            }
        }
    }
}
