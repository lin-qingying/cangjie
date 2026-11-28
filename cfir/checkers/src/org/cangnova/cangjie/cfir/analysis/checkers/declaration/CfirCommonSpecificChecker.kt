package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangnova.cangjie.CjmpLanguageFeature
import org.cangnova.cangjie.supportsCjmpFeature
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.cfir.analysis.checkers.CjmpGate
import org.cangnova.cangjie.cfir.analysis.checkers.isFromDeclarationFile
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.cjmpMatchResultForSpecific
import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.declarations.cjmpValueParameters
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpDiagnosticArgument
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpDiagnosticLabel
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpImplementationKind
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpSide
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.isFromCommonPart
import org.cangnova.cangjie.cfir.declarations.hasAbstractAttribute
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.diagnostics.withRelatedInformation
import org.cangnova.cangjie.cfir.expressions.isSupportedBuiltinAnnotation
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.declarations.cjmpHasCommonDefault
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.session.CfirCjmpCommonSideFacts
import org.cangnova.cangjie.cfir.session.defaultCfirCjmpCommonSideFacts
import org.cangnova.cangjie.cfir.session.CfirCjmpCandidateDiagnostic
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.cjmpFamilySnapshotFor
import org.cangnova.cangjie.cfir.session.cjmpCommonCounterpartOf
import org.cangnova.cangjie.cfir.session.cjmpCommonCounterpartsOf
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMismatchKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpCompatibilityIssue
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.typeConstraintDiagnosticData
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatchingContext
import org.cangnova.cangjie.resolve.calls.mpp.CjmpAnnotationMatchingPolicy
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationSourceElement
import org.cangnova.cangjie.cfir.declarations.utils.ownTypeParameters
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.patterns.primaryBindingNameOrNull
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.resolve.providers.classifyDeclaredSupertype
import org.cangnova.cangjie.cfir.resolve.providers.ordinarySupertypeTypeOrNull
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.descriptors.Visibilities
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
object CfirCommonSpecificChecker : CfirClassLikeChecker(MppCheckerKind.Common) {
    /**
     * 分发 common/specific class 声明的匹配、约束和注解检查。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        // 覆盖面（G21）：class/struct/interface/enum 全量参与（官方 MatchNominativeDecl 面）；
        // typealias 不参与 CJMP 配对（修饰符谓词已拒绝其携带 cjmp）。
        if (declaration is CfirTypeAlias) return

        // 版本门（D16 单入口）。
        if (!CjmpGate.isEnabled(context)) return

        // 配对结论（NOT_MATCHED / 种类 / 修饰符 / 超类型 / 穷尽性）统一由 CfirCjmpMatchingChecker
        // 与 CfirCjmpCommonSideChecker 消费配对存储报告；成员重定义由通用冲突 collector
        // 在 CJMP 合并成员视图上统一处理。
        if (declaration.status.isSpecific) {
            checkSpecificExtraConstraints(declaration)
        }

        // common/specific 声明的修饰符和注解限制
        if (declaration.status.isCommon || declaration.status.isSpecific) {
            checkCJMPAbstractClassMembers(declaration)
        }
    }

    /**
     * specific 声明的额外约束：
     * - 非 specific 抽象成员不能在 specific 类中
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSpecificExtraConstraints(specificDecl: CfirClassLikeDeclaration) {
        // 官方判据（CheckCJMP.cpp:1310-1338 CheckAbstractClassMembers）：
        // specific **abstract class**（仅 CLASS_DECL）的成员不得为非 specific 的抽象成员；
        // 成员限 function/property，且跳过来自 common 部分的成员。
        if (specificDecl is CfirClass && specificDecl.status.isAbstract && specificDecl.status.isSpecific) {
            for (member in specificDecl.declarations) {
                val memberKind = when (member) {
                    is CfirNamedFunction -> CfirCjmpDiagnosticLabel.FUNCTION
                    is CfirProperty -> CfirCjmpDiagnosticLabel.PROPERTY
                    else -> continue
                }
                if (member.status.isAbstract && !member.status.isSpecific && !member.status.isCommon) {
                    reporter.reportOn(
                        source = member.source,
                        factory = CfirErrors.CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS,
                        a = specificDecl.name,
                        b = CfirCjmpDiagnosticArgument.Label(memberKind),
                    )
                }
            }
        }

        // SPECIFIC_INIT_COMMON_PRIMARY_CONSTRUCTOR / SPECIFIC_PRIMARY_UNMATCHED_VAR_DECL 不在此报告：
        // 官方只在 MatchCJMPFunction 把 specific 构造器与 common 标记的主构造器配对时触发（CheckCJMP.cpp:967-980），
        // 而主构造器不能带 common/specific（WRONG_MODIFIER_TARGET），cjc 1.1.3 无可达触发点。
        // 此前按"common 侧存在任意主构造器 / specific 标记主构造器"的启发式报告，会在合法程序上误报。
    }

    /**
     * common/specific 抽象类成员必须有明确修饰符。
     *
     * 对齐 C++ DiagKind::sema_cjmp_abstract_class_member_has_no_explicit_modifier
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCJMPAbstractClassMembers(decl: CfirClassLikeDeclaration) {
        val abstractClass = decl as? CfirClass ?: return
        if (!abstractClass.status.isAbstract) return
        if (!abstractClass.status.isCommon && !abstractClass.status.isSpecific) return
        if (isFromDeclarationFile(context)) return

        val side = if (abstractClass.status.isCommon) CfirCjmpSide.COMMON else CfirCjmpSide.SPECIFIC
        for (member in abstractClass.declarations) {
            val memberKind = when (member) {
                is CfirNamedFunction -> CfirCjmpDiagnosticLabel.FUNCTION
                is CfirProperty -> CfirCjmpDiagnosticLabel.PROPERTY
                else -> continue
            }
            if (member.origin.generated || member.isFromCommonPart) continue
            if (member.status.isCommon || member.status.isSpecific || member.status.hasAbstractAttribute) continue
            if (member.hasOfficialCjmpBody()) continue

            reporter.reportOn(
                source = member.source ?: abstractClass.source,
                factory = CfirErrors.CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER,
                a = CfirCjmpDiagnosticArgument.Side(side),
                b = CfirCjmpDiagnosticArgument.Label(memberKind),
                c = CfirCjmpDiagnosticArgument.Side(side),
            )
        }
    }

    /** 官方判定函数体与属性 body 不同；属性只把首个非合成 getter 视为实现。 */
    private fun CfirMemberDeclaration.hasOfficialCjmpBody(): Boolean = when (this) {
        is CfirNamedFunction -> body != null
        is CfirProperty -> getter?.origin?.generated == false
        else -> false
    }

}

/** common/specific 声明通用限制；覆盖 class-like、顶层 callable 与 extend。 */
object CfirCommonSpecificDeclarationChecker : CfirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        val member = declaration as? CfirMemberDeclaration ?: return
        if (!member.status.isCommon && !member.status.isSpecific) return
        if (member.isFromCommonPart) return
        if (!CjmpGate.isEnabled(context)) return

        if (context.languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.GenericDeclarations)) {
            checkGenericFrozen(member)
        }
        checkCommonSpecificAnnotations(member)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkGenericFrozen(declaration: CfirMemberDeclaration) {
        // 泛型声明才受 @Frozen 限制；构造器借用的所属类型参数不使其成为泛型声明。
        if (declaration.typeParameters.ownTypeParameters.isEmpty()) return
        val frozen = declaration.annotations
            .filterIsInstance<org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall>()
            .firstOrNull { annotation ->
                annotation.annotationKind == BuiltInAnnotationKind.FROZEN &&
                        annotation.isSupportedBuiltinAnnotation(
                            BuiltInAnnotationKind.FROZEN,
                            context.languageVersionSettings,
                        )
            } ?: return
        reporter.reportOn(
            source = frozen.source ?: declaration.source,
            factory = CfirErrors.COMMON_GENERIC_FROZEN_NOT_SUPPORTED,
            a = CfirCjmpDiagnosticArgument.DeclarationName(declaration),
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCommonSpecificAnnotations(declaration: CfirDeclaration) {
        for (annotation in declaration.annotations) {
            val call = annotation as? org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall ?: continue
            if (!CjmpAnnotationMatchingPolicy.isDisallowedOnCommonSpecific(
                    call.annotationIdentity,
                    context.languageVersionSettings.languageVersion,
                )
            ) continue

            val name = when (val identity = call.annotationIdentity) {
                is CangjieAnnotationIdentity.LanguageBuiltIn -> identity.sourceName
                is CangjieAnnotationIdentity.PlatformDerived -> identity.sourceName
                is CangjieAnnotationIdentity.PackageDirective -> identity.sourceName
                CangjieAnnotationIdentity.Unknown -> checkNotNull(call.annotationSourceName) {
                    "An unknown annotation must preserve its source spelling"
                }
                is CangjieAnnotationIdentity.Custom,
                is CangjieAnnotationIdentity.SystemMacro,
                is CangjieAnnotationIdentity.SpecialExpression,
                null,
                -> error("A permitted annotation was classified as unsupported on common/specific")
            }
            reporter.reportOn(
                source = call.source ?: declaration.source,
                factory = CfirErrors.COMMON_SPECIFIC_ANNOTATION_NOT_ALLOWED,
                a = Name.identifier(name),
            )
        }
    }
}

/** Common 编译中的顶层 common extend 检查，对齐官方 `CheckCommonExtensions`。 */
object CfirCommonSpecificExtendChecker : CfirExtendChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirExtend) {
        if (!declaration.status.isCommon) return
        if (!CjmpGate.isEnabled(context) || context.session.cjmpSettings.mode != CfirCjmpMode.COMMON) return

        val session = context.session
        val targetType = declaration.extendedTypeRef.coneTypeOrNull ?: return
        val packageName = checkNotNull(context.containingFileSymbol).cfir.packageDirective.packageFqName
        val privateSignatures = mutableSetOf<String>()

        // provider 的声明顺序对应 CollectCJMPDecls；common-private 分支不进入已见签名集合。
        for (extend in session.extendProvider.getExtendsInPackage(packageName)) {
            if (!extend.status.isCommon || extend.moduleData !== declaration.moduleData) continue
            if (extend.superTypeRefs.any { typeRef ->
                    typeRef.classifyDeclaredSupertype(session).ordinarySupertypeTypeOrNull()
                        ?.toSymbol(session)?.cfir is CfirInterface
                }) continue
            val extendedType = extend.extendedTypeRef.coneTypeOrNull ?: continue
            if (!AbstractTypeChecker.equalTypes(session.typeContext, targetType, extendedType)) continue

            for (member in extend.declarations) {
                val kind = when (member) {
                    is CfirNamedFunction -> CfirCjmpDiagnosticLabel.FUNCTION
                    is CfirProperty -> CfirCjmpDiagnosticLabel.PROPERTY
                    else -> continue
                }
                if (member.status.visibility != Visibilities.Private) continue
                val signature = CfirRedeclarationPresenter.representCommonExtensionMember(member, extend) ?: continue
                when {
                    signature.rawName in privateSignatures -> if (extend === declaration) {
                        reporter.reportOn(
                            source = member.source,
                            factory = CfirErrors.COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS,
                            a = CfirCjmpDiagnosticArgument.ExtendedType(declaration),
                            b = CfirCjmpDiagnosticArgument.Label(kind),
                            c = signature.name,
                        )
                    }
                    member.status.isCommon -> if (extend === declaration) {
                        reporter.reportOn(
                            source = member.source,
                            factory = CfirErrors.COMMON_DIRECT_EXTENSION_HAS_COMMON_PRIVATE_MEMBERS,
                            a = CfirCjmpDiagnosticArgument.Label(kind),
                            b = signature.name,
                        )
                    }
                    else -> privateSignatures.add(signature.rawName)
                }
            }
            if (extend === declaration) break
        }
    }
}

/**
 * common 包 main 函数检查器
 *
 * 对齐官方 `TypeCheckerImpl::CheckEntryFunc` 的 `sema_common_package_has_main`（cjc 1.1.3）：判据是**文件级**
 * `File::isCommon`——common 编译（CHIR 输出）中，文件内任一声明带 `common` 修饰符即成为 common package part
 * 文件（`MPParserImpl::CheckCJMPModifiers` 置位），该文件中的 main 报错（与 main 在文件中的先后无关）；
 * 同包其它不含 common 声明的文件中的 main 不报。`common main()` 本身由修饰符规则拒绝，不使文件成为 common。
 *
 * 注册为 fileCheckers
 */
object CfirCommonPackageMainChecker : CfirFileChecker(MppCheckerKind.Common) {
    override val requiresImplementation: Boolean get() = true

    /**
     * 检查 common package part 文件中是否声明了 main 函数。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        // 版本门 + 模式门（G12 逃逸修复）：非 1.1+ 语义或非 CJMP 编译下整族静默；common part 只存在于 common 编译。
        if (!CjmpGate.isEnabled(context)) return
        if (context.session.cjmpSettings.mode != CfirCjmpMode.COMMON) return
        if (declaration.declarations.none { it.declaresCjmpCommon() }) return
        for (decl in declaration.declarations) {
            if (decl is org.cangnova.cangjie.cfir.declarations.CfirMainFunction) {
                reporter.reportOn(
                    source = decl.source,
                    factory = CfirErrors.COMMON_PACKAGE_HAS_MAIN,
                )
            }
        }
    }

    /**
     * 声明自身或其成员带 `common` 修饰符（官方对每个经 `ParseDecl` 解析的声明调用 `CheckCJMPModifiers`，成员亦然）。
     */
    private fun CfirDeclaration.declaresCjmpCommon(): Boolean {
        if (this is org.cangnova.cangjie.cfir.declarations.CfirMainFunction) return false
        if ((this as? CfirMemberDeclaration)?.status?.isCommon == true) return true
        val members = when (this) {
            is CfirClassLikeDeclaration -> declarations
            is CfirExtend -> declarations
            else -> return false
        }
        return members.any { it.declaresCjmpCommon() }
    }
}

/**
 * Mock 语义检查器（Mock 分组）
 *
 * 注册为 classLikeCheckers
 */
object CfirMockSemanticsChecker : CfirClassLikeChecker(MppCheckerKind.Common) {
    /**
     * 检查 class-like 声明中 mock 相关静态成员限制。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        // Mock 检查需要编译选项 API
        // 当 mock 功能启用时检查 static 声明约束
        for (member in declaration.declarations) {
            if (member !is CfirNamedFunction) continue
            if (!member.status.isStatic) continue

            // static/private/local/constructor 声明不能被 mock
            // `Mock` is a user/system macro identity, not an official built-in
            // annotation kind. Without a resolved macro declaration identity
            // this checker must not infer it from a short name.
        }
    }
}

/**
 * CJMP 配对结论检查器（specific 方向；逐条对位 cjc 1.1.3 `CheckCJMP.cpp`）。
 *
 * 只在 specific 编译（mode=SPECIFIC）且版本门开启时工作，消费 CJMP_MATCHING 阶段发布的
 * 声明事实及其规范组快照：
 * - 未配对：`DiagNotMatchedDecl(decl, "specific", …, "common")`；参数级失败先在参数上报告
 *  （`sema_specific_has_different_parameter` / `sema_cjmp_parameter_default_value_both_sides`）；
 *   nominal 种类不同只报 `specific_has_different_kind`、common 带默认实现而 specific 为 abstract 只报
 *   `sema_specific_member_must_have_implementation`（官方同位置后报诊断被诊断引擎去重，1.1.3 实测）；
 * - 已配对：修饰符（`MatchCJMPDeclAttrs`）、变量/属性类型（`MatchCJMPVar`/`MatchCJMPProp`）、
 *   var/let、注解多重集（`MatchCJMPDeclAnnotations`）、第二绑定（`TrySetSpecificImpl`）、
 *   nominal 超类型与枚举穷尽性（`MatchCommonNominalDeclWithSpecific`/`MatchNominativeDecl`）。
 */
object CfirCjmpMatchingChecker : CfirBasicDeclarationChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        if (declaration is CfirEnumConstructor) {
            checkEnumConstructor(declaration)
            return
        }
        val member = declaration as? CfirMemberDeclaration ?: return
        if (!member.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)) return
        if (declaration is CfirTypeAlias) return
        if (!CjmpGate.isEnabled(context)) return
        if (context.session.cjmpSettings.mode != CfirCjmpMode.SPECIFIC) return

        val family = context.session.cjmpFamilySnapshotFor(declaration) ?: return
        if (declaration is CfirExtend && family.isDuplicateSpecificExtend(declaration)) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.SPECIFIC_HAS_DUPLICATE_EXTENSIONS,
                    a = CfirCjmpDiagnosticArgument.ExtendedType(declaration),
            )
            return
        }

        if (declaration is CfirPatternVariable) {
            checkPatternVariable(declaration, family)
            return
        }

        val commonCounterparts = context.session.cjmpCommonCounterpartsOf(declaration)
        reportCandidateDiagnostics(declaration, family)
        if (commonCounterparts.isNotEmpty()) {
            commonCounterparts.forEach { common ->
                checkMatched(declaration, common as CfirMemberDeclaration)
            }
        } else {
            reportUnmatched(declaration, family)
        }
    }

    /**
     * specific enum 的构造器（官方构造器随外层携带 SPECIFIC）：无 common 对应构造器即 NOT_MATCHED
     *（common enum 非穷尽时的多出构造器由配对阶段静默）。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkEnumConstructor(declaration: CfirEnumConstructor) {
        val enum = context.findClosestDeclaration<CfirEnum>() ?: return
        if (!enum.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)) return
        if (!CjmpGate.isEnabled(context)) return
        if (context.session.cjmpSettings.mode != CfirCjmpMode.SPECIFIC) return
        val family = context.session.cjmpFamilySnapshotFor(declaration) ?: return
        reportCandidateDiagnostics(declaration, family)
        val counterparts = context.session.cjmpCommonCounterpartsOf(declaration)
        if (counterparts.isNotEmpty()) {
            counterparts.forEach { common -> checkMatched(declaration, common as CfirMemberDeclaration) }
        } else {
            reportUnmatched(declaration, family)
        }
    }

    /** 模式外壳与 VAR 诊断各用自己的真实身份；停止项之后的 binding 不参与 CJMP 检查。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPatternVariable(declaration: CfirPatternVariable, family: CfirCjmpFamilySnapshot) {
        for (binding in declaration.pattern.bindingVariables()) {
            if (!family.hasResolutionResult(binding)) continue
            reportCandidateDiagnostics(binding, family)
            val counterparts = binding.symbol.cjmpMatchResultForSpecific()?.counterparts.orEmpty()
                .map { it.commonSymbol.cfir }
            if (counterparts.isEmpty()) {
                reportUnmatched(binding, family)
            } else {
                counterparts.forEach { common -> checkMatched(binding, common as CfirMemberDeclaration) }
            }
        }
        val pattern = declaration.pattern
        if (pattern !is CfirBindingPattern || pattern.nestedPattern != null) {
            reportUnmatched(declaration, family)
        }
    }

    /** 重放 first-fit 失败候选已触发的诊断；后续候选成功只决定映射，不撤销这些诊断。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportCandidateDiagnostics(
        declaration: CfirDeclaration,
        family: CfirCjmpFamilySnapshot,
    ) {
        for (diagnostic in family.candidateDiagnosticsFor(declaration)) {
            when (diagnostic) {
                is CfirCjmpCandidateDiagnostic.MissingBody -> {
                    val callable = checkNotNull(declaration as? CfirCallableDeclaration) {
                        "A missing-body CJMP candidate must be callable"
                    }
            val commonContainerName = diagnostic.commonCandidate?.let(::cjmpCandidateContainerName)
                ?: callable?.symbol?.callableId?.classId?.shortClassName
                    reporter.reportOn(
                        source = declaration.source,
                        factory = CfirErrors.SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION,
                a = CfirCjmpDiagnosticArgument.Name(callable.symbol.name),
                b = CfirCjmpDiagnosticArgument.ContainerName(commonContainerName),
                    )
                }
                is CfirCjmpCandidateDiagnostic.ParameterMismatch -> {
                    val mismatch = diagnostic.mismatch
                    val parameter = (declaration as CfirCallableDeclaration).cjmpValueParameters().getOrNull(mismatch.parameterIndex)
                    reporter.reportOn(
                        source = parameter?.source ?: declaration.source,
                        factory = if (mismatch.kind == CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES) {
                            CfirErrors.CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES
                        } else {
                            CfirErrors.SPECIFIC_HAS_DIFFERENT_PARAMETER
                        },
                    )
                }
                // 官方把占用诊断报告在 common；specific 侧只保留未匹配结论。
                is CfirCjmpCandidateDiagnostic.Occupied -> Unit
            }
        }
    }

    /** 对齐官方 `commonDecl.outerDecl.identifier`，同时覆盖 class 成员与 extend 成员。 */
    private fun cjmpCandidateContainerName(candidate: CfirDeclaration): Name? {
        val callable = candidate as? CfirCallableDeclaration ?: return null
        val containingExtend = callable.moduleData.session.extendProvider.getContainingExtend(callable.symbol)
        return containingExtend?.let(::cjmpDeclName)
            ?: callable.symbol.callableId.classId?.shortClassName
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportUnmatched(declaration: CfirDeclaration, family: CfirCjmpFamilySnapshot) {
        val kinds = family.mismatchKindsFor(declaration)
        if (!family.isUnmatched(declaration) && kinds.isEmpty()) return

        // 官方对非 nominal 声明的真实外层 enum 作非穷尽豁免；普通成员也适用。
        // 候选参数诊断已先重放，此处只决定是否追加 NOT_MATCHED。
        if (declaration is CfirCallableDeclaration) {
            val enum = context.findClosestDeclaration<CfirClassLikeDeclaration>() as? CfirEnum
            val hasNonExhaustiveCommonEnum = enum?.let { specificEnum ->
                specificEnum.symbol.cjmpMatchResultForSpecific()?.counterparts.orEmpty()
                    .any { (it.commonSymbol.cfir as? CfirEnum)?.isNonExhaustive == true }
            } == true
            if (hasNonExhaustiveCommonEnum) return
        }

        // 官方 NeedToReportMissingBody 只报告成员缺实现诊断，不再追加 NOT_MATCHED。
        if (CjmpMismatchKind.MISSING_BODY in kinds) return

        if (declaration is CfirClassLikeDeclaration && CjmpMismatchKind.CLASS_KIND in kinds) {
            val common = family.mismatchCandidatesFor(declaration, CjmpMismatchKind.CLASS_KIND).first()
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_KIND,
                a = CfirCjmpDiagnosticArgument.DeclarationKind(cjmpDeclarationKind(declaration)),
                b = CfirCjmpDiagnosticArgument.DeclarationKind(cjmpDeclarationKind(common)),
            )
            return
        }
        reporter.reportOn(
            source = declaration.cjmpVariableDiagnosticOwner().source,
            factory = CfirErrors.NOT_MATCHED,
            a = CfirCjmpDiagnosticArgument.Side(CfirCjmpSide.SPECIFIC),
            b = CfirCjmpDiagnosticArgument.NotMatchedDeclaration(declaration, null),
            c = CfirCjmpDiagnosticArgument.Side(CfirCjmpSide.COMMON),
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMatched(specific: CfirDeclaration, common: CfirMemberDeclaration) {
        val specificMember = specific as CfirMemberDeclaration
        val specificSource = specific.cjmpVariableDiagnosticOwner().source
        val counterpart = checkNotNull(specific.symbol.cjmpMatchResultForSpecific())
            .counterparts.single { it.commonSymbol === common.symbol }
        val compatibilityContext = CfirCjmpMatchingContext(context.session)
        val issues = compatibilityContext.checkCompatibility(
            specific.symbol,
            common.symbol,
            counterpart.typeParameterMapping,
        )
        for (issue in issues) {
            when (issue) {
                is CjmpCompatibilityIssue.GenericConstraint -> {
                    val index = issue.typeParameterIndex
                    // 下标来自兼容性检查的自有参数投影，这里必须按同一投影取参数。
                    val typeParameter = specificMember.typeParameters.ownTypeParameters[index]
                    val constraintSource = specificMember.attributes.typeConstraintDiagnosticData
                        ?.typeConstraints
                        ?.firstOrNull { it.parameterName == typeParameter.symbol.name }
                        ?.constraintSource
                    reporter.reportOn(
                        source = constraintSource ?: specific.source,
                        factory = CfirErrors.GENERIC_CONSTRAINT_NOT_LOOSER,
                    )
                }

                CjmpCompatibilityIssue.GenericParameterCount -> reporter.reportOn(
                    source = specificMember.source,
                    factory = CfirErrors.GENERIC_MEMBER_TYPE_ARGUMENT_DIFFERENT,
                    a = cjmpDeclName(specific),
                )

                CjmpCompatibilityIssue.OpenAbstractMember -> {
                    val kind = CfirCjmpDiagnosticArgument.DeclarationKind(cjmpDeclarationKind(common))
                    reporter.reportOn(
                        source = specific.source,
                        factory = CfirErrors.OPEN_ABSTRACT_SPECIFIC_CAN_NOT_REPLACE_OPEN_COMMON,
                        a = kind,
                        b = kind,
                    )
                }

                CjmpCompatibilityIssue.Modifiers -> reportModifier(specific)

                CjmpCompatibilityIssue.Supertypes -> reporter.reportOn(
                    source = specific.source,
                    factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_SUPER_TYPE,
                    a = CfirCjmpDiagnosticArgument.DeclarationKind(cjmpDeclarationKind(specific)),
                )

                CjmpCompatibilityIssue.EnumExhaustiveness -> reporter.reportOn(
                    source = specific.source,
                    factory = CfirErrors.COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH,
                    a = CfirCjmpDiagnosticArgument.DeclarationKind(cjmpDeclarationKind(common)),
                    b = CfirCjmpDiagnosticArgument.DeclarationKind(cjmpDeclarationKind(specific)),
                )

                CjmpCompatibilityIssue.ReturnType -> reporter.reportOn(
                    source = specific.source,
                    factory = CfirErrors.RETURN_TYPE_INCOMPATIBLE,
                    a = (specific as CfirCallableDeclaration).symbol.name,
                )

                CjmpCompatibilityIssue.DeclarationType -> {
                    val kind = when (specific) {
                        is CfirProperty -> CfirCjmpDiagnosticArgument.DeclarationKind(cjmpDeclarationKind(specific))
                        is CfirVariable -> CfirCjmpDiagnosticArgument.VariableKind(specific.isVar)
                        else -> error("Declaration type incompatibility requires a property or variable")
                    }
                    reporter.reportOn(source = specificSource, factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_TYPE, a = kind)
                }

                CjmpCompatibilityIssue.Mutability -> {
                    val specificVariable = specific as CfirVariable
                    val commonVariable = common as CfirVariable
                    reporter.reportOn(
                        source = specificSource,
                        factory = CfirErrors.SPECIFIC_VAR_NOT_MATCH_LET,
                        a = CfirCjmpDiagnosticArgument.VariableKind(specificVariable.isVar),
                        b = CfirCjmpDiagnosticArgument.VariableKind(commonVariable.isVar),
                    )
                }

                is CjmpCompatibilityIssue.PropertyMutability -> {
                    val property = if (issue.specificIsMutable) common as CfirProperty else specific as CfirProperty
                    reporter.reportOn(
                        source = property.source,
                        factory = if (issue.specificIsMutable) {
                            CfirErrors.PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT
                        } else {
                            CfirErrors.PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT
                        },
                        a = property.name,
                        context = context.forDiagnosticSourceOwner(property.symbol, property.source),
                    )
                }

            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportModifier(specific: CfirDeclaration): Boolean {
        reporter.reportOn(
            source = specific.cjmpVariableDiagnosticOwner().source,
            factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_MODIFIER,
            a = CfirCjmpDiagnosticArgument.DeclarationKind(cjmpDeclarationKind(specific)),
        )
        return false
    }

}

/** 源码 common 由 Platform visitor 在真实声明上下文中检查，不自行枚举依赖文件。 */
object CfirCjmpCommonSideChecker : CfirBasicDeclarationChecker(MppCheckerKind.Platform) {
    override val requiresImplementation: Boolean get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        if (!CjmpGate.isEnabled(context) || !context.session.cjmpSettings.compileSpecific) return
        if (!declaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.COMMON)) return
        // 对齐 CollectCJMPDecls：只报告包级 common 及其 nominal/extend 的直接 common 成员。
        val parents = context.containingDeclarations.map { it.cfir }.filterNot { it is CfirFile }
        if (parents.size > 1) return
        val outer = parents.singleOrNull()
        if (outer != null && (outer !is CfirClassLikeDeclaration && outer !is CfirExtend ||
                    !outer.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.COMMON))) return
        val source = declaration.source ?: return
        val packageName = context.containingFileSymbol?.cfir?.packageDirective?.packageFqName ?: return
        val family = context.session.cjmpFamilyAggregate.ensureFamilyComplete(packageName)
        reportCjmpCommonSideDeclaration(
            declaration, outer, source, context, family, reporter,
            reportMissingImplementation = !context.session.cjmpSettings.compileCommon,
        )
    }
}

/** 源码和二进制 common 共享语义判定；调用方提供真实的诊断来源上下文。 */
internal fun reportCjmpCommonSideDeclaration(
    common: CfirDeclaration,
    outer: CfirDeclaration?,
    source: org.cangnova.cangjie.source.AbstractCjSourceElement,
    diagnosticContext: DiagnosticContext,
    family: CfirCjmpFamilySnapshot,
    reporter: DiagnosticReporter,
    reportMissingImplementation: Boolean,
) {
    val commonSideFacts = defaultCfirCjmpCommonSideFacts
    val occupiedSpecifics = commonSideFacts.occupiedSpecificDeclarations(common, family)
    val implementationKinds = occupiedSpecifics.map { cjmpImplementationKind(it, common) }.distinct()
    if (implementationKinds.isNotEmpty()) {
        for (kind in implementationKinds) reporter.reportOn(
            source = source,
            factory = CfirErrors.MULTIPLE_COMMON_IMPLEMENTATIONS,
            a = CfirCjmpDiagnosticArgument.ImplementationKind(kind),
            context = diagnosticContext,
        )
    } else if (reportMissingImplementation && commonSideFacts.mustReportNotMatched(common, outer, family)) {
        reporter.reportOn(
            source = source,
            factory = CfirErrors.NOT_MATCHED,
            a = CfirCjmpDiagnosticArgument.Side(CfirCjmpSide.COMMON),
            b = CfirCjmpDiagnosticArgument.NotMatchedDeclaration(common, outer),
            c = CfirCjmpDiagnosticArgument.Side(CfirCjmpSide.SPECIFIC),
            context = diagnosticContext,
        )
    }
}

/** 在 checker 层选择官方实现种类分组；具体文案由诊断 renderer 输出。 */
private fun cjmpImplementationKind(
    specific: CfirDeclaration,
    common: CfirDeclaration,
): CfirCjmpImplementationKind = when {
    specific is CfirEnumConstructor && common is CfirEnumConstructor ->
        CfirCjmpImplementationKind.EnumConstructor(
            requireNotNull(specific.symbol.callableId.classId).shortClassName,
        )
    common is CfirProperty -> CfirCjmpImplementationKind.Property(cjmpDeclName(common))
    common is CfirFunction || common is CfirEnumConstructor && common.cjmpValueParameters().isNotEmpty() ->
        CfirCjmpImplementationKind.Declaration(CfirCjmpDiagnosticLabel.FUNCTION)
    else -> CfirCjmpImplementationKind.Declaration(cjmpDeclarationKind(common))
}

internal fun cjmpDeclarationKind(declaration: CfirDeclaration): CfirCjmpDiagnosticLabel = when (declaration) {
    is CfirClass -> CfirCjmpDiagnosticLabel.CLASS
    is CfirStruct -> CfirCjmpDiagnosticLabel.STRUCT
    is CfirInterface -> CfirCjmpDiagnosticLabel.INTERFACE
    is CfirEnum -> CfirCjmpDiagnosticLabel.ENUM
    is CfirExtend -> CfirCjmpDiagnosticLabel.EXTEND
    is CfirProperty -> CfirCjmpDiagnosticLabel.PROPERTY
    is CfirFunction -> CfirCjmpDiagnosticLabel.FUNCTION
    is CfirEnumConstructor ->
        if (declaration.cjmpValueParameters().isEmpty()) CfirCjmpDiagnosticLabel.VARIABLE else CfirCjmpDiagnosticLabel.FUNCTION
    is CfirVariable -> CfirCjmpDiagnosticLabel.VARIABLE
    is CfirTypeAlias -> CfirCjmpDiagnosticLabel.TYPE_ALIAS
    else -> error("No CJMP diagnostic kind is defined for ${declaration::class.qualifiedName}")
}

internal fun cjmpDeclName(declaration: CfirDeclaration): Name = when (declaration) {
    is CfirClassLikeDeclaration -> declaration.name
    is CfirEnumConstructor -> declaration.name
    is CfirExtend -> (declaration.extendedTypeRef as? CfirResolvedTypeRef)
        ?.coneType?.let { type ->
            (type as? org.cangnova.cangjie.cfir.types.ConeClassLikeType)?.classId?.shortClassName
                ?: Name.identifier(type.toString())
        } ?: Name.special("<extend>")
    is CfirPatternVariable -> declaration.pattern.primaryBindingNameOrNull() ?: declaration.symbol.name
    is CfirCallableDeclaration -> declaration.symbol.name
    else -> Name.special("<no name>")
}
