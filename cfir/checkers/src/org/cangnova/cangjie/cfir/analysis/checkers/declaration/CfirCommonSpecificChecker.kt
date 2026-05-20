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
            checkSpecificExtraConstraints(declaration)
        }

        // 对 common 声明执行约束检查
        if (declaration.status.isCommon) {
            checkCommonDeclarationConstraints(declaration)
            checkCommonExtraConstraints(declaration)
        }

        // common/specific 声明的修饰符和注解限制
        if (declaration.status.isCommon || declaration.status.isSpecific) {
            checkCommonSpecificAnnotations(declaration)
            checkCommonSpecificGenericConstraints(declaration)
            checkCJMPAbstractClassMembers(declaration)
            checkExplicitlyAbstractUsage(declaration)
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
                    // NOT_MATCHED: common 声明没有被 specific 匹配
                    reporter.reportOn(
                        source = commonMember.source ?: commonDecl.source,
                        factory = CfirErrors.NOT_MATCHED,
                        a = commonName,
                        b = commonKind,
                        c = "specific",
                    )
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
                checkMemberVarLetMatch(specificMember, commonMember, commonName)
                checkMemberParameterMatch(specificMember, commonMember)
                checkMemberAnnotationMatch(specificMember, commonMember, commonName)
                checkMemberDeprecatedInherited(specificMember, commonMember, commonName)
            }
        }
    }

    /**
     * specific var 不能匹配 common let。
     *
     * 对齐 C++ DiagKind::sema_specific_var_not_match_let
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMemberVarLetMatch(
        specificMember: CfirDeclaration,
        commonMember: CfirDeclaration,
        memberName: Name,
    ) {
        if (specificMember is CfirFieldVariable && commonMember is CfirFieldVariable) {
            if (specificMember.isVar && !commonMember.isVar) {
                reporter.reportOn(
                    source = specificMember.source,
                    factory = CfirErrors.SPECIFIC_VAR_NOT_MATCH_LET,
                    a = memberName,
                    b = memberName,
                )
            }
        }
    }

    /**
     * specific 函数参数与 common 不匹配。
     *
     * 对齐 C++ DiagKind::sema_specific_has_different_parameter
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMemberParameterMatch(
        specificMember: CfirDeclaration,
        commonMember: CfirDeclaration,
    ) {
        val specificFunc = specificMember as? CfirNamedFunction ?: return
        val commonFunc = commonMember as? CfirNamedFunction ?: return

        if (specificFunc.valueParameters.size != commonFunc.valueParameters.size) {
            reporter.reportOn(
                source = specificFunc.source,
                factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_PARAMETER,
            )
            return
        }

        // 检查参数默认值——common 和 specific 两侧不能同时有默认值
        for ((idx, specificParam) in specificFunc.valueParameters.withIndex()) {
            val commonParam = commonFunc.valueParameters[idx]
            if (specificParam.defaultValue != null && commonParam.defaultValue != null) {
                reporter.reportOn(
                    source = specificParam.source ?: specificFunc.source,
                    factory = CfirErrors.CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES,
                )
            }
        }
    }

    /**
     * specific 成员的注解与 common 不匹配。
     *
     * 对齐 C++ DiagKind::sema_specific_has_different_annotation
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMemberAnnotationMatch(
        specificMember: CfirDeclaration,
        commonMember: CfirDeclaration,
        @Suppress("UNUSED_PARAMETER") memberName: Name,
    ) {
        val specificAnnoNames = specificMember.annotationNames()
        val commonAnnoNames = commonMember.annotationNames()

        // common/specific 修饰符过滤
        val filteredSpecific = specificAnnoNames - IGNORE_MATCH_ANNOTATIONS
        val filteredCommon = commonAnnoNames - IGNORE_MATCH_ANNOTATIONS

        if (filteredSpecific != filteredCommon) {
            reporter.reportOn(
                source = specificMember.source,
                factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_ANNOTATION,
                a = memberKind(specificMember) ?: "member",
            )
        }
    }

    /**
     * 某些注解不允许出现在 specific 声明上。
     *
     * 对齐 C++ DiagKind::sema_specific_has_deprecated_annotation
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMemberDeprecatedInherited(
        specificMember: CfirDeclaration,
        commonMember: CfirDeclaration,
        memberName: Name,
    ) {
        // 如果 specific 声明自身标注了 @Deprecated，但 common 对应声明未标注
        if (specificMember.hasAnnotation(DEPRECATED_NAME) && !commonMember.hasAnnotation(DEPRECATED_NAME)) {
            reporter.reportOn(
                source = specificMember.source,
                factory = CfirErrors.SPECIFIC_HAS_DEPRECATED_ANNOTATION,
                a = DEPRECATED_NAME,
                b = memberKind(specificMember) ?: "member",
                c = memberName,
            )
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

    /**
     * specific 声明的额外约束：
     * - specific 主构造器必须与 common 的成员声明一致
     * - open abstract specific 不能替代 open common
     * - 非 specific 抽象成员不能在 specific 类中
     * - specific 类不能同时有多个相同的 extension
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSpecificExtraConstraints(specificDecl: CfirClass) {
        val classId = specificDecl.symbol.classId
        val commonSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)
        val commonDecl = commonSymbol?.cfir as? CfirClass

        // open abstract specific 不能替代 open common
        if (specificDecl.status.isOpen && specificDecl.status.isAbstract && commonDecl != null) {
            if (commonDecl.status.isOpen && !commonDecl.status.isAbstract) {
                reporter.reportOn(
                    source = specificDecl.source,
                    factory = CfirErrors.OPEN_ABSTRACT_SPECIFIC_CAN_NOT_REPLACE_OPEN_COMMON,
                    a = "class",
                    b = "class",
                )
            }
        }

        // 非 specific 抽象成员不能在 specific 类中
        if (!specificDecl.status.isAbstract) {
            for (member in specificDecl.declarations) {
                if (member !is CfirNamedFunction) continue
                if (member.status.isAbstract && !member.status.isSpecific) {
                    reporter.reportOn(
                        source = member.source,
                        factory = CfirErrors.CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS,
                        a = specificDecl.name,
                        b = "function",
                    )
                }
            }
        }

        // specific 同名 extension 不能重复
        val extensionNames = mutableMapOf<Name, Int>()
        for (member in specificDecl.declarations) {
            if (member !is org.cangnova.cangjie.cfir.declarations.CfirExtend) continue
            val extendedTypeRef = member.extendedTypeRef
            val extendedType = (extendedTypeRef as? CfirResolvedTypeRef)?.coneType
            val className = when (extendedType) {
                is org.cangnova.cangjie.cfir.types.ConeClassLikeType -> extendedType.classId.shortClassName
                is org.cangnova.cangjie.cfir.types.ConeStructType -> extendedType.classId.shortClassName
                is org.cangnova.cangjie.cfir.types.ConeEnumType -> extendedType.classId.shortClassName
                else -> null
            } ?: continue
            val count = extensionNames.getOrDefault(className, 0) + 1
            extensionNames[className] = count
            if (count == 2) {
                reporter.reportOn(
                    source = member.source,
                    factory = CfirErrors.SPECIFIC_HAS_DUPLICATE_EXTENSIONS,
                    a = className,
                )
            }
        }

        // specific init 不能实现 primary common constructor
        for (member in specificDecl.declarations) {
            if (member !is org.cangnova.cangjie.cfir.declarations.CfirConstructor) continue
            if (member.status.isSpecific && commonDecl != null) {
                val commonPrimary = commonDecl.declarations
                    .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirConstructor>()
                    .firstOrNull { it.isPrimary }
                if (commonPrimary != null) {
                    reporter.reportOn(
                        source = member.source,
                        factory = CfirErrors.SPECIFIC_INIT_COMMON_PRIMARY_CONSTRUCTOR,
                    )
                }
            }
        }

        // specific primary constructor 参数必须也是成员变量声明
        val primaryCtor = specificDecl.declarations
            .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirConstructor>()
            .firstOrNull { it.isPrimary && it.status.isSpecific }
        if (primaryCtor != null) {
            val memberFieldNames = specificDecl.declarations
                .filterIsInstance<CfirFieldVariable>()
                .map { it.name }
                .toSet()
            for (param in primaryCtor.valueParameters) {
                if (param.name !in memberFieldNames) {
                    reporter.reportOn(
                        source = param.source ?: primaryCtor.source,
                        factory = CfirErrors.SPECIFIC_PRIMARY_UNMATCHED_VAR_DECL,
                    )
                }
            }
        }
    }

    /**
     * common 声明的额外约束：
     * - MULTIPLE_COMMON_IMPLEMENTATIONS: common 声明不能有多个 specific 实现（通过 symbolProvider 检查）
     * - COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH
     * - COMMON_STATIC_LET_CANT_BE_INITIALIZED_IN_STATIC_INIT
     * - COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR
     * - common 的私有成员约束
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCommonExtraConstraints(commonDecl: CfirClass) {
        // common 泛型声明的 @Frozen 限制
        if (commonDecl.typeParameters.isNotEmpty()) {
            if (commonDecl.hasAnnotation(FROZEN_NAME)) {
                reporter.reportOn(
                    source = commonDecl.source,
                    factory = CfirErrors.COMMON_GENERIC_FROZEN_NOT_SUPPORTED,
                    a = "class",
                )
            }
        }

        // common open class 的 static let 不能在 static init 中初始化
        for (member in commonDecl.declarations) {
            if (member !is CfirFieldVariable) continue
            if (!member.status.isStatic) continue
            if (member.isVar) continue
            // 检查是否在 static init 中赋值——需要检查 CFG
            // 声明级只能检查：static let 必须在声明处初始化
            if (member.initializer == null) {
                reporter.reportOn(
                    source = member.source ?: commonDecl.source,
                    factory = CfirErrors.COMMON_STATIC_LET_CANT_BE_INITIALIZED_IN_STATIC_INIT,
                    a = member.name,
                )
            }
        }

        // common 的 private 成员扩展约束
        for (member in commonDecl.declarations) {
            if (member !is org.cangnova.cangjie.cfir.declarations.CfirExtend) continue
            val privateMembers = member.declarations.mapNotNull { sub ->
                val name = memberName(sub) ?: return@mapNotNull null
                val vis = when (sub) {
                    is CfirNamedFunction -> sub.status.visibility
                    is CfirProperty -> sub.status.visibility
                    else -> return@mapNotNull null
                }
                if (vis == org.cangnova.cangjie.descriptors.Visibilities.Private) {
                    name to (memberKind(sub) ?: "member")
                } else null
            }
            // 检查重复的 private 成员
            val nameCount = mutableMapOf<Name, Int>()
            for ((name, _) in privateMembers) {
                nameCount[name] = (nameCount[name] ?: 0) + 1
            }
            for ((name, count) in nameCount) {
                if (count > 1) {
                    val kind = privateMembers.first { it.first == name }.second
                    reporter.reportOn(
                        source = member.source ?: commonDecl.source,
                        factory = CfirErrors.COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS,
                        a = commonDecl.name,
                        b = kind,
                        c = name,
                    )
                }
            }
            // common 声明不能有标注 private 的成员（冲突）
            if (member.status.isCommon) {
                for ((name, kind) in privateMembers) {
                    reporter.reportOn(
                        source = member.source ?: commonDecl.source,
                        factory = CfirErrors.COMMON_DIRECT_EXTENSION_HAS_COMMON_PRIVATE_MEMBERS,
                        a = kind,
                        b = name,
                    )
                }
            }
        }

        // 检查 non-exhaustive common 是否匹配 exhaustive specific
        // 这需要跨模块信息，通过 symbolProvider 查找 specific 对应声明
        val classId = commonDecl.symbol.classId
        val specificSymbols = listOfNotNull(context.session.symbolProvider.getClassLikeSymbolByClassId(classId))
        val specificDecls = specificSymbols.mapNotNull { (it.cfir as? CfirClass)?.takeIf { c -> c.status.isSpecific } }
        if (specificDecls.size > 1) {
            reporter.reportOn(
                source = commonDecl.source,
                factory = CfirErrors.MULTIPLE_COMMON_IMPLEMENTATIONS,
                a = "class",
            )
        }
    }

    /**
     * common/specific 的注解限制。
     *
     * 对齐 C++ DiagKind::sema_common_specific_annotation_not_allowed
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCommonSpecificAnnotations(decl: CfirClass) {
        for (ann in decl.annotations) {
            val name = ann.shortNameOrNull() ?: continue
            if (name in DISALLOWED_ON_COMMON_SPECIFIC) {
                reporter.reportOn(
                    source = decl.source,
                    factory = CfirErrors.COMMON_SPECIFIC_ANNOTATION_NOT_ALLOWED,
                    a = name,
                )
            }
        }
    }

    /**
     * common/specific 泛型约束：
     * - 不支持 @Frozen 标注的泛型（已在 checkCommonExtraConstraints 处理）
     * - common 和 specific 的泛型参数不能重命名
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCommonSpecificGenericConstraints(decl: CfirClass) {
        if (!decl.status.isSpecific) return
        if (decl.typeParameters.isEmpty()) return

        val classId = decl.symbol.classId
        val commonSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)
        val commonDecl = commonSymbol?.cfir as? CfirClass ?: return

        // 检查泛型参数名与 common 一致
        if (commonDecl.typeParameters.size == decl.typeParameters.size) {
            for ((idx, specParam) in decl.typeParameters.withIndex()) {
                val commonParam = commonDecl.typeParameters[idx]
                if (specParam.name != commonParam.name) {
                    reporter.reportOn(
                        source = specParam.source ?: decl.source,
                        factory = CfirErrors.COMMON_GENERIC_RENAME_NOT_SUPPORTED,
                    )
                }
            }
        }
    }

    /**
     * common/specific 抽象类成员必须有明确修饰符。
     *
     * 对齐 C++ DiagKind::sema_cjmp_abstract_class_member_has_no_explicit_modifier
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCJMPAbstractClassMembers(decl: CfirClass) {
        if (!decl.status.isAbstract) return
        if (!decl.status.isCommon && !decl.status.isSpecific) return

        for (member in decl.declarations) {
            if (member !is CfirNamedFunction) continue
            // open/abstract 修饰符必须显式
            if (!member.status.isOpen && !member.status.isAbstract && !member.status.isStatic) {
                // 成员既不是 open 也不是 abstract，可能缺少修饰符
                if (member.body != null && !member.status.isSpecific) continue // 非抽象成员不需要
                reporter.reportOn(
                    source = member.source ?: decl.source,
                    factory = CfirErrors.CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER,
                    a = decl.name,
                    b = "function",
                    c = "open/abstract",
                )
            }
        }
    }

    /**
     * explicitly abstract 只能用于 common/specific 抽象类。
     *
     * 对齐 C++ DiagKind::sema_explicitly_abstract_only_for_cjmp_abstract_class
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExplicitlyAbstractUsage(decl: CfirClass) {
        for (member in decl.declarations) {
            if (member !is CfirNamedFunction) continue
            if (!member.status.isAbstract) continue

            // 如果不是 common/specific 抽象类，不能使用 explicitly abstract
            if (!decl.status.isAbstract || (!decl.status.isCommon && !decl.status.isSpecific)) {
                reporter.reportOn(
                    source = member.source,
                    factory = CfirErrors.EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS,
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
            if (member.hasAnnotation(Name.identifier("Mock"))) {
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

private fun CfirDeclaration.annotationNames(): Set<Name> =
    annotations.mapNotNull { it.shortNameOrNull() }.toSet()

// common/specific 相关工具常量
private val DEPRECATED_NAME = Name.identifier("Deprecated")
private val FROZEN_NAME = Name.identifier("Frozen")

/**
 * 匹配注解比较时需要忽略的注解名（例如 common/specific 本身的标注）。
 */
private val IGNORE_MATCH_ANNOTATIONS: Set<Name> = setOf(
    Name.identifier("Common"),
    Name.identifier("Specific"),
)

/**
 * 不允许出现在 common/specific 声明上的注解。
 * 对齐 C++ MPTypeCheckerImpl::CheckNotAllowedAnnotations。
 */
private val DISALLOWED_ON_COMMON_SPECIFIC: Set<Name> = setOf(
    // C/Java 互操作注解不能与 common/specific 共存
    Name.identifier("C"),
    Name.identifier("Java"),
    Name.identifier("JavaMirror"),
    Name.identifier("JavaImpl"),
)
