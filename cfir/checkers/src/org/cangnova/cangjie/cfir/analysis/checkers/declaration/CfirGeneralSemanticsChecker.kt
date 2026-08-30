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

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.isCatchParameter
import org.cangnova.cangjie.cfir.patterns.visibleBindingVariables
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.cjMappingConfigProvider
import org.cangnova.cangjie.cfir.session.noPrelude
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.impl.ResolvedImplicitTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 通用语义检查器（General 分组）
 *
 * 对齐 C++ TypeCheckDecl.cpp / TypeCheckUtil.cpp 中的通用语义检查：
 * - ACCESSIBILITY_ERROR / ACCESSIBILITY_WITH_MAIN_HINT: 可访问性检查
 * - CONFLICT_WITH_SUB_PACKAGE: 顶层声明与子包名冲突
 * - AMBIGUOUS_USE: 歧义使用
 * - PARAM_COUNT_MISMATCH: 参数个数不匹配
 * - MISMATCHED_TYPES_BECAUSE / MISMATCHED_TYPES_MULTIPLE_ASSIGN: 类型不匹配
 * - UNABLE_TO_INFER_DECL: 无法推断声明类型
 * - INVALID_NODE_AFTER_CHECK: resolve 后节点无效
 * - CORE_OBJECT_NOT_FOUND_WHEN_NO_PRELUDE: 缺少 core.Object
 *
 * 注意：部分诊断（如 ACCESSIBILITY_ERROR）需要调用点上下文，
 * 属于 resolve 管线和表达式检查器的职责，此处只处理声明层面可判断的部分。
 */
object CfirGeneralSemanticsChecker : CfirFileChecker() {
    /**
     * 对单个文件执行通用声明语义检查。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        checkAccessibilityInFile(declaration)
        checkConflictWithSubPackage(declaration)
        checkCoreObjectAvailable(declaration)
        checkMainFunctionAccessibility(declaration)
        checkExportSamePrivateDecl(declaration)
        checkJavaInteropImports(declaration)
        checkJavaImplRedefinition(declaration)
        checkCJMappingConfigValid(declaration)
    }

    /**
     * 检查顶层声明名是否与子包名冲突。
     *
     * 对齐 C++ DiagKind::sema_conflict_with_sub_package。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkConflictWithSubPackage(file: CfirFile) {
        val packageFqName = file.packageDirective.packageFqName
        for (declaration in file.declarations) {
            val declName = declaration.declarationName() ?: continue
            val childPackageFqName = packageFqName.child(declName)
            if (context.session.symbolProvider.hasPackage(childPackageFqName)) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.CONFLICT_WITH_SUB_PACKAGE,
                    a = declName,
                    b = declName,
                )
            }
        }
    }

    /**
     * 检查 core.Object 是否可用。
     *
     * 对齐 C++ DiagKind::sema_core_object_not_found_when_no_prelude:
     * 使用 --no-prelude 选项时，如果 std.core.Object 不存在则报错。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCoreObjectAvailable(file: CfirFile) {
        val noPreludeEnabled = context.session.noPrelude
        if (!noPreludeEnabled) return

        val stdCoreFqName = org.cangnova.cangjie.name.FqName("std.core")
        val hasStdCorePackage = context.session.symbolProvider.hasPackage(stdCoreFqName)
        val reportSource = file.source ?: file.packageDirective.source ?: file.declarations.firstOrNull()?.source
        if (!hasStdCorePackage) {
            reporter.reportOn(
                source = reportSource,
                factory = CfirErrors.CORE_OBJECT_NOT_FOUND_WHEN_NO_PRELUDE,
            )
            return
        }

        val stdCoreObjectId = org.cangnova.cangjie.name.ClassId(
            stdCoreFqName,
            Name.identifier("Object"),
        )
        val objectSymbol = runCatching {
            context.session.symbolProvider.getClassLikeSymbolByClassId(stdCoreObjectId)
        }.getOrNull()
        if (objectSymbol == null) {
            reporter.reportOn(
                source = reportSource,
                factory = CfirErrors.CORE_OBJECT_NOT_FOUND_WHEN_NO_PRELUDE,
            )
        }
    }

    /**
     * main 函数可访问性检查。
     *
     * 对齐 C++ DiagKind::sema_accessibility_with_main_hint:
     * main 函数必须是 public 的，否则带有 main 提示的可访问性错误。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMainFunctionAccessibility(file: CfirFile) {
        for (declaration in file.declarations) {
            if (declaration !is org.cangnova.cangjie.cfir.declarations.CfirMainFunction) continue
            val visibility = declaration.status.visibility
            if (visibility != Visibilities.Public) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.ACCESSIBILITY_WITH_MAIN_HINT,
                    a = "function",
                    b = org.cangnova.cangjie.name.Name.identifier("main"),
                    c = visibility,
                )
            }
        }
    }

    /**
     * 使用 JavaMirror / JavaImpl / CJMapping 互操作入口时必须导入 interoplib.interop。
     *
     * 对齐 C++ CheckJavaInteropLibImport：诊断挂在触发互操作入口的声明上，
     * 普通 @Java 类型约束不在该入口中报 interoplib 导入错误。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkJavaInteropImports(file: CfirFile) {
        val javaInteropEntryAnnotations = setOf(
            Name.identifier("JavaMirror"),
            Name.identifier("JavaImpl"),
            Name.identifier("CJMapping"),
        )
        val javaInteropDeclarations = file.declarations.filter { decl ->
            decl is CfirClassLikeDeclaration && javaInteropEntryAnnotations.any(decl::hasAnnotation)
        }
        if (javaInteropDeclarations.isEmpty()) return

        val interopFq = org.cangnova.cangjie.name.FqName("interoplib.interop")
        val imported = file.imports.any { imp ->
            val fq = imp.importedFqName ?: return@any false
            fq == interopFq || fq.parent() == interopFq
        }
        if (!imported) javaInteropDeclarations.forEach { declaration ->
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.JAVA_MIRROR_INTEROPLIB_MUST_BE_IMPORTED,
            )
        }
    }

    /**
     * @JavaImpl 不允许重复定义同一 Java 类。
     *
     * 对齐 C++ sema_java_impl_redefinition
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkJavaImplRedefinition(file: CfirFile) {
        val javaImplName = Name.identifier("JavaImpl")
        val byName = mutableMapOf<Name, Int>()
        for (decl in file.declarations) {
            if (!decl.hasAnnotation(javaImplName)) continue
            val declName = decl.declarationName() ?: continue
            byName.merge(declName, 1) { a, b -> a + b }
        }
        for (decl in file.declarations) {
            if (!decl.hasAnnotation(javaImplName)) continue
            val declName = decl.declarationName() ?: continue
            if ((byName[declName] ?: 0) > 1) {
                reporter.reportOn(
                    source = decl.source,
                    factory = CfirErrors.JAVA_IMPL_REDEFINITION,
                    a = declName,
                )
            }
        }
    }

    /**
     * CJMapping 配置加载失败时,通过 [CfirCJMappingConfigProvider] 报告。
     *
     * 对齐 C++ CompileStrategy.cpp:51 `sema_cj_mapping_generic_method_not_get_instance_config`。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCJMappingConfigValid(file: CfirFile) {
        val provider = context.session.cjMappingConfigProvider
        val path = provider.configPath ?: return
        if (provider.isValid) return
        reporter.reportOn(
            source = file.source,
            factory = CfirErrors.CJ_MAPPING_GENERIC_METHOD_NOT_GET_INSTANCE_CONFIG,
            a = path,
        )
    }

    /**
     * 同包内不允许导出两个同名的 private 顶层 nominal 声明。
     *
     * 对齐 C++ `AnalyzeFunctionLinkage` 中的 `sema_export_same_private_decl`：
     * 官方只遍历 `IsNominalDecl() && private && linkage != INTERNAL` 的声明。
     * 函数、属性、字段的同名问题由重声明/重载检查器处理，不能在这里重复报导出限制。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExportSamePrivateDecl(file: CfirFile) {
        val byName = mutableMapOf<Name, Int>()
        for (decl in file.declarations) {
            val vis = when (decl) {
                is CfirClass -> decl.status.visibility
                is CfirInterface -> decl.status.visibility
                is CfirStruct -> decl.status.visibility
                is CfirEnum -> decl.status.visibility
                else -> continue
            }
            if (vis != Visibilities.Private) continue
            val name = decl.declarationName() ?: continue
            byName.merge(name, 1) { a, b -> a + b }
        }
        for (decl in file.declarations) {
            val vis = when (decl) {
                is CfirClass -> decl.status.visibility
                is CfirInterface -> decl.status.visibility
                is CfirStruct -> decl.status.visibility
                is CfirEnum -> decl.status.visibility
                else -> continue
            }
            if (vis != Visibilities.Private) continue
            val name = decl.declarationName() ?: continue
            if ((byName[name] ?: 0) > 1) {
                reporter.reportOn(
                    source = decl.source,
                    factory = CfirErrors.EXPORT_SAME_PRIVATE_DECL,
                )
            }
        }
    }

    /**
     * 检查文件中的声明是否存在可访问性问题。
     *
     * 对齐 C++ DiagKind::sema_accessibility_error:
     * 非 private 声明的签名中不能引用访问级别更低的类型。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAccessibilityInFile(file: CfirFile) {
        for (declaration in file.declarations) {
            checkNonPrivateDeclarationAccessLevelValidity(declaration, containingAccessLevel = null)
        }
    }

    /**
     * 声明对外可见的签名不能暴露更低访问级别的类型。
     *
     * 对齐官方 CheckInternalTypeUse.cpp：
     * - 顶层和成员都跳过 private 声明；
     * - 成员有效访问级别不高于外层 nominal 声明；
     * - 函数检查返回类型、参数类型和泛型约束；
     * - nominal/typealias 检查泛型约束，并递归检查成员。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkNonPrivateDeclarationAccessLevelValidity(
        declaration: CfirDeclaration,
        containingAccessLevel: Visibility?,
    ) {
        val ownVisibility = declaration.accessLevelVisibility() ?: return
        if (Visibilities.isPrivate(ownVisibility)) return

        val effectiveVisibility = ownVisibility.effectiveInside(containingAccessLevel)
        if (declaration is CfirPatternVariable) {
            checkPatternVariableBindingsAccessibility(declaration, effectiveVisibility)
        } else {
            val exposure = declaration.findFirstSignatureExposure(effectiveVisibility)
            if (exposure?.inferred == true) {
                reporter.reportOn(
                    source = declaration.accessibilityDiagnosticSource(),
                    factory = CfirErrors.ACCESSIBILITY_WITH_MAIN_HINT,
                    a = if (declaration is CfirNamedFunction) "function" else "variable",
                    b = declaration.declarationName() ?: Name.special("<unknown>"),
                    c = exposure.visibility,
                )
            } else if (exposure != null) {
                reporter.reportOn(
                    source = declaration.accessibilityDiagnosticSource(),
                    factory = CfirErrors.ACCESSIBILITY_ERROR,
                    a = declaration.declarationName()?.asString() ?: "<unknown>",
                    b = exposure.visibility,
                )
            }
        }

        if (declaration is CfirClassLikeDeclaration) {
            for (member in declaration.declarations) {
                checkNonPrivateDeclarationAccessLevelValidity(member, effectiveVisibility)
            }
        }
    }

    /**
     * 模式变量按绑定逐个检查可访问性暴露。
     *
     * 对齐官方 `CheckPatternVarAccessLevelValidity` / `DiagPatternInternalTypesUse`：
     * 对每个绑定只看它自身类型（元组解构中即对应元素类型）是否暴露更低可见性类型，
     * 只上报那些自身类型有暴露的绑定，而不是把整个元组的一次暴露平摊到所有绑定上。
     * 多绑定模式固定使用 with-main-hint 变体；单绑定 + 显式类型仍走普通可访问性错误。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPatternVariableBindingsAccessibility(
        declaration: CfirPatternVariable,
        effectiveVisibility: Visibility,
    ) {
        val bindings = declaration.pattern.visibleBindingVariables()
        val bindingsUseInferredTypes = bindings.size != 1 || declaration.returnTypeRef.isImplicitAccessibilityType()
        for (binding in bindings) {
            val exposure = binding.returnTypeRef.findFirstExposure(effectiveVisibility) ?: continue
            if (bindingsUseInferredTypes) {
                reporter.reportOn(
                    source = binding.source,
                    factory = CfirErrors.ACCESSIBILITY_WITH_MAIN_HINT,
                    a = "variable",
                    b = binding.name,
                    c = exposure,
                )
            } else {
                reporter.reportOn(
                    source = binding.source,
                    factory = CfirErrors.ACCESSIBILITY_ERROR,
                    a = binding.name.asString(),
                    b = exposure,
                )
            }
        }
    }

    /**
     * 查找声明签名中第一个暴露了更低可见性类型的位置。
     *
     * 检查范围包括类型参数上界、函数返回值和参数类型、属性/字段类型以及 typealias
     * 展开类型。
     */
    context(context: CheckerContext)
    private fun CfirDeclaration.findFirstSignatureExposure(
        declarationVisibility: Visibility,
    ): SignatureExposure? {
        (this as? CfirTypeParameterRefsOwner)
            ?.findFirstTypeParameterBoundExposure(declarationVisibility)
            ?.let { return SignatureExposure(it, inferred = false) }

        return when (this) {
            is CfirNamedFunction -> {
                returnTypeRef.findFirstExposure(declarationVisibility)?.let {
                    SignatureExposure(it, returnTypeRef.isImplicitAccessibilityType())
                }
                    ?: valueParameters.asSequence()
                        .mapNotNull { it.returnTypeRef.findFirstExposure(declarationVisibility) }
                        .firstOrNull()
                        ?.let { SignatureExposure(it, inferred = false) }
            }

            is CfirPatternVariable -> returnTypeRef.findFirstExposure(declarationVisibility)
                ?.let { SignatureExposure(it, inferred = true) }
            is CfirProperty -> returnTypeRef.findFirstExposure(declarationVisibility)
                ?.let { SignatureExposure(it, returnTypeRef.isImplicitAccessibilityType()) }
            is CfirFieldVariable -> returnTypeRef.findFirstExposure(declarationVisibility)
                ?.let { SignatureExposure(it, returnTypeRef.isImplicitAccessibilityType()) }
            is CfirTypeAlias -> expandedTypeRef.findFirstExposure(declarationVisibility)
                ?.let { SignatureExposure(it, inferred = false) }
            else -> null
        }
    }

    /** 签名暴露结果，同时保留诊断是否来自推断类型。 */
    private data class SignatureExposure(val visibility: Visibility, val inferred: Boolean)

    /**
     * 查找类型参数上界中第一个可见性暴露问题。
     */
    context(context: CheckerContext)
    private fun CfirTypeParameterRefsOwner.findFirstTypeParameterBoundExposure(
        declarationVisibility: Visibility,
    ): Visibility? {
        for (typeParameter in typeParameters) {
            for (bound in typeParameter.symbol.resolvedBounds) {
                bound.findFirstExposure(declarationVisibility)?.let { return it }
            }
        }
        return null
    }
}

/**
 * 类/结构体/枚举语义检查器（ClassStruct 分组）
 *
 * 对齐 C++ TypeCheckClassLike.cpp / LegalityOfUsage/:
 * - NON_ABSTRACT_CLASS_CANNOT_BE_SEALED
 * - STATIC_VARIABLE_USE_GENERIC_PARAMETER
 * - INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER: finalizer 中不能调用实例函数
 * - CSTRUCT_CANNOT_IMPL_INTERFACES: @C struct 不能实现接口
 * - EXPORT_SAME_PRIVATE_DECL: 同名 private 导出限制
 */
object CfirClassStructSemanticsChecker : CfirClassLikeChecker() {
    /**
     * C 互操作结构体注解名称。
     */
    private val C_ANNOTATION = Name.identifier("C")

    /**
     * 对 class/struct/enum/interface 执行通用类型声明语义检查。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration is CfirClass) {
            checkSealedOnlyOnAbstract(declaration)
            CfirStaticGenericDependencySemantics.check(declaration)
            checkFinalizerConstraints(declaration)
        }
        if (declaration is CfirStruct) {
            CfirStaticGenericDependencySemantics.check(declaration)
            checkCStructCannotImplInterfaces(declaration)
        }
        if (declaration is CfirEnum) {
            CfirStaticGenericDependencySemantics.check(declaration)
        }
        if (declaration is CfirInterface) {
            CfirStaticGenericDependencySemantics.check(declaration)
        }
    }

    /**
     * 检查 sealed class 必须同时是 abstract class。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSealedOnlyOnAbstract(classDecl: CfirClass) {
        if (!classDecl.status.isSealed) return
        if (classDecl.status.isAbstract) return
        reporter.reportOn(
            source = classDecl.source,
            factory = CfirErrors.NON_ABSTRACT_CLASS_CANNOT_BE_SEALED,
        )
    }

    /**
     * @C struct 不能实现接口。
     *
     * 对齐 C++ DiagKind::sema_cstruct_cannot_impl_interfaces
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCStructCannotImplInterfaces(structDecl: CfirStruct) {
        if (!structDecl.hasAnnotation(C_ANNOTATION)) return
        if (structDecl.superTypeRefs.isNotEmpty()) {
            reporter.reportOn(
                source = structDecl.source,
                factory = CfirErrors.CSTRUCT_CANNOT_IMPL_INTERFACES,
            )
        }
    }

    /**
     * finalizer 中不能使用实例函数。
     *
     * 对齐 C++ DiagKind::sema_instance_func_cannot_be_used_in_finalizer
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkFinalizerConstraints(classDecl: CfirClass) {
        for (member in classDecl.declarations) {
            if (member !is org.cangnova.cangjie.cfir.declarations.CfirFinalizer) continue
            val body = member.body ?: continue
            body.acceptChildren(object : org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid() {
                override fun visitElement(element: org.cangnova.cangjie.cfir.CfirElement) {
                    if (element is org.cangnova.cangjie.cfir.expressions.CfirFunctionCall) {
                        val ref = element.calleeReference as? org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
                        val sym = ref?.resolvedSymbol as? org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
                        val targetFunction = sym?.cfir
                        // 这里只拦 class 实例方法；顶层函数/局部函数本身并不违反 finalizer 约束，
                        // 违规点在于把 `this` 当值传出，由表达式检查器单独报告。
                        if (targetFunction != null && targetFunction.dispatchReceiverType != null && !targetFunction.status.isStatic) {
                            reporter.reportOn(
                                source = element.source,
                                factory = CfirErrors.INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER,
                                a = "function",
                            )
                        }
                    }
                    element.acceptChildren(this, null)
                }
            }, null)
        }
    }

}

/** extend static var / prop / init 不能依赖 extend 自身的泛型参数。 */
object CfirExtendStaticGenericDependencyChecker : CfirExtendChecker() {
    /**
     * 检查 extend 成员中的 static 泛型参数依赖。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirExtend) {
        CfirStaticGenericDependencySemantics.check(declaration)
    }
}

/**
 * static var / prop / init 不能依赖所属 generic owner 的类型参数。
 *
 * 官方 Sema 对 class-like 和 extend 使用同一类规则：在 static 成员内部遍历 `RefType` /
 * `RefExpr` 节点，并把诊断报在实际依赖泛型参数的类型或表达式节点上。
 */
private object CfirStaticGenericDependencySemantics {
    /** 检查 class-like static 成员是否依赖所属 class-like 的类型参数。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun check(classLike: CfirClassLikeDeclaration) {
        checkStaticMembers(
            declarations = classLike.declarations,
            ownerTypeParameters = classLike.staticGenericDependencyParameters(),
        )
    }

    /** 检查 extend static 成员是否依赖所属 extend 的类型参数。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun check(extend: CfirExtend) {
        checkStaticMembers(
            declarations = extend.declarations,
            ownerTypeParameters = extend.typeParameters
                .filterIsInstance<CfirTypeParameter>()
                .mapTo(linkedSetOf()) { it.symbol },
        )
    }

    /** 对拥有声明列表的 generic owner 执行统一 static 成员扫描。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkStaticMembers(
        declarations: List<CfirDeclaration>,
        ownerTypeParameters: Set<CfirTypeParameterSymbol>,
    ) {
        if (ownerTypeParameters.isEmpty()) return
        for (member in declarations) {
            if (member is CfirFieldVariable && member.status.isStatic) {
                val reported = mutableSetOf<StaticGenericDependencyReportKey>()
                member.returnTypeRef.reportStaticGenericDependency(ownerTypeParameters, reported)
                member.initializer?.reportStaticGenericDependency(ownerTypeParameters, reported)
            }
            if (member is CfirProperty && member.status.isStatic) {
                val reported = mutableSetOf<StaticGenericDependencyReportKey>()
                member.returnTypeRef.reportStaticGenericDependency(ownerTypeParameters, reported)
                member.getter?.body?.reportStaticGenericDependency(ownerTypeParameters, reported)
                member.setter?.body?.reportStaticGenericDependency(ownerTypeParameters, reported)
            }
            if (member is CfirConstructor && member.status.isStatic) {
                val reported = mutableSetOf<StaticGenericDependencyReportKey>()
                member.body?.reportStaticGenericDependency(ownerTypeParameters, reported)
            }
        }
    }

    /** 检查类型引用本身是否依赖 static 成员所属 generic owner 的类型参数。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirTypeRef.reportStaticGenericDependency(
        ownerTypeParameters: Set<CfirTypeParameterSymbol>,
        reported: MutableSet<StaticGenericDependencyReportKey>,
    ): Boolean {
        return coneTypeOrNull.reportStaticGenericDependency(source, ownerTypeParameters, reported)
    }

    /** 遍历表达式及其嵌套类型引用，查找 static 成员中对 generic owner 类型参数的依赖。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirExpression.reportStaticGenericDependency(
        ownerTypeParameters: Set<CfirTypeParameterSymbol>,
        reported: MutableSet<StaticGenericDependencyReportKey>,
    ) {
        accept(object : CfirDefaultVisitorVoid() {
            private var currentQualifiedAccessSource: CjSourceElement? = null

            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this, null)
            }

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                visitQualifiedAccessWithStaticGenericDependency(functionCall)
            }

            override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
                visitQualifiedAccessWithStaticGenericDependency(qualifiedAccessExpression)
            }

            override fun visitExpression(expression: CfirExpression) {
                if (expression is CfirQualifiedAccessExpression) {
                    visitQualifiedAccessWithStaticGenericDependency(expression)
                    return
                }
                if (expression.coneTypeOrNull.reportStaticGenericDependency(
                        expression.source,
                        ownerTypeParameters,
                        reported,
                    )
                ) {
                    return
                }
                expression.acceptChildren(this, null)
            }

            override fun visitTypeRef(typeRef: CfirTypeRef) {
                val diagnosticSource = currentQualifiedAccessSource ?: typeRef.source
                if (typeRef.coneTypeOrNull.reportStaticGenericDependency(
                        diagnosticSource,
                        ownerTypeParameters,
                        reported,
                    )
                ) {
                    return
                }
                typeRef.acceptChildren(this, null)
            }

            private fun visitQualifiedAccessWithStaticGenericDependency(expression: CfirQualifiedAccessExpression) {
                if (expression.typeArguments.reportStaticGenericDependencyOn(
                        source = expression.source,
                        ownerTypeParameters = ownerTypeParameters,
                        reported = reported,
                    )
                ) {
                    return
                }
                if (expression.explicitReceiver?.coneTypeOrNull.reportStaticGenericDependency(
                        expression.explicitReceiver?.source ?: expression.source,
                        ownerTypeParameters,
                        reported,
                    )
                ) {
                    return
                }
                if (expression.reportStaticGenericOwnerDependency(reported)) {
                    return
                }
                val previousQualifiedAccessSource = currentQualifiedAccessSource
                currentQualifiedAccessSource = expression.source
                expression.acceptChildren(this, null)
                currentQualifiedAccessSource = previousQualifiedAccessSource
            }
        }, null)
    }

    /** 将调用/访问类型实参中的外层泛型依赖提升到整个调用/访问表达式。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun List<CfirTypeRef>.reportStaticGenericDependencyOn(
        source: CjSourceElement?,
        ownerTypeParameters: Set<CfirTypeParameterSymbol>,
        reported: MutableSet<StaticGenericDependencyReportKey>,
    ): Boolean {
        val typeParameter = firstNotNullOfOrNull { typeRef ->
            ownerTypeParameters.firstOrNull { typeRef.coneTypeOrNull?.containsTypeParameter(it) == true }
        } ?: return false
        return typeParameter.name.reportStaticGenericDependency(source, reported)
    }

    /**
     * static 成员引用若解析到泛型 class-like 的 static 函数/属性，官方在该引用节点上报告。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirQualifiedAccessExpression.reportStaticGenericOwnerDependency(
        reported: MutableSet<StaticGenericDependencyReportKey>,
    ): Boolean {
        val callableSymbol = (calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol as? CfirCallableSymbol<*>
            ?: return false
        val callable = callableSymbol.takeIf { it.isBound }?.cfir ?: return false
        if (callable !is CfirNamedFunction && callable !is CfirProperty) return false
        if (!callable.status.isStatic) return false
        if (callable.origin == CfirDeclarationOrigin.SubstitutionOverride.CallSite) return false
        val ownerClassId = callableSymbol.callableId.classId ?: return false
        val ownerDeclaration = context.session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir
            as? CfirClassLikeDeclaration ?: return false
        val typeParameter = ownerDeclaration.staticGenericDependencyParameters().firstOrNull() ?: return false
        return typeParameter.name.reportStaticGenericDependency(source, reported)
    }

    /** 对单个 Cone 类型执行泛型参数依赖判断，并按 source/type-parameter 去重上报。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun ConeCangJieType?.reportStaticGenericDependency(
        source: CjSourceElement?,
        ownerTypeParameters: Set<CfirTypeParameterSymbol>,
        reported: MutableSet<StaticGenericDependencyReportKey>,
    ): Boolean {
        val typeParameter = ownerTypeParameters.firstOrNull { this?.containsTypeParameter(it) == true } ?: return false
        return typeParameter.name.reportStaticGenericDependency(source, reported)
    }

    /** 按 source/type-parameter 去重上报 static 泛型参数依赖。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun Name.reportStaticGenericDependency(
        source: CjSourceElement?,
        reported: MutableSet<StaticGenericDependencyReportKey>,
    ): Boolean {
        val typeParameterName = this
        val key = StaticGenericDependencyReportKey(source, typeParameterName)
        if (!reported.add(key)) return true
        reporter.reportOn(
            source = source,
            factory = CfirErrors.STATIC_VARIABLE_USE_GENERIC_PARAMETER,
            a = typeParameterName,
        )
        source?.let(context::recordStaticGenericDependency)
        return true
    }

    /** static 泛型参数依赖诊断的去重键。 */
    private data class StaticGenericDependencyReportKey(
        /** 已报告诊断对应的 source。 */
        val source: CjSourceElement?,
        /** 已报告诊断对应的类型参数名。 */
        val typeParameterName: Name,
    )

    /** class-like 可作为 static 成员外层泛型依赖的类型参数符号集合。 */
    private fun CfirClassLikeDeclaration.staticGenericDependencyParameters(): Set<CfirTypeParameterSymbol> = when (this) {
        is CfirClass -> typeParameters.filterIsInstance<CfirTypeParameter>().mapTo(linkedSetOf()) { it.symbol }
        is CfirStruct -> typeParameters.filterIsInstance<CfirTypeParameter>().mapTo(linkedSetOf()) { it.symbol }
        is CfirEnum -> typeParameters.filterIsInstance<CfirTypeParameter>().mapTo(linkedSetOf()) { it.symbol }
        is CfirInterface -> typeParameters.filterIsInstance<CfirTypeParameter>().mapTo(linkedSetOf()) { it.symbol }
        else -> emptySet()
    }
}

/**
 * 属性语义检查器（Property 分组）
 *
 * 对齐 C++ 属性相关检查:
 * - PROPERTY_MUST_HAVE_ACCESSORS
 * - IMMUTABLE_PROPERTY_WITH_SETTER
 * - PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT / _IMMUT: 继承中同名属性 mut/immut 不一致
 * - PROPERTY_MUST_IMPLEMENT_BOTH: 接口属性的 getter/setter 都必须实现
 */
object CfirPropertySemanticsChecker : CfirPropertyChecker() {
    /**
     * 对单个属性声明执行属性语义检查。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirProperty) {
        checkPropertyAccessors(declaration)
        checkImmutablePropertySetter(declaration)
        checkPropertyInheritConsistency(declaration)
        checkPropertyMustImplementBoth(declaration)
    }

    /**
     * 检查非接口、非抽象、非构造参数来源属性必须具有访问器。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPropertyAccessors(property: CfirProperty) {
        if (property.isCatchParameter == true) return
        if (property.source?.kind == CjFakeSourceElementKind.PropertyFromParameter) return
        if (context.containingDeclarations.lastOrNull() is CfirInterfaceSymbol) return
        if (property.status.isAbstract || property.symbol.isAbstractLike(context)) return
        if (property.getter == null && property.setter == null) {
            reporter.reportOn(
                source = property.source,
                factory = CfirErrors.PROPERTY_MUST_HAVE_ACCESSORS,
            )
        }
    }

    /**
     * 检查不可变属性不能声明 setter。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkImmutablePropertySetter(property: CfirProperty) {
        if (!property.status.isMut && property.setter != null) {
            reporter.reportOn(
                source = property.source,
                factory = CfirErrors.IMMUTABLE_PROPERTY_WITH_SETTER,
            )
        }
    }

    /**
     * 子属性实现 default/abstract 父属性时，mut 属性必须同时实现 getter 和 setter。
     *
     * 对齐官方 `StructInheritanceChecker::CheckPropertyInheritance` 中
     * `childProp->isVar && parentProp->TestAnyAttr(DEFAULT, ABSTRACT)` 的触发条件。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPropertyMustImplementBoth(property: CfirProperty) {
        if (!property.status.isOverride) return
        if (!property.status.isMut) return

        val subHasGetter = property.getter != null
        val subHasSetter = property.setter != null
        if (subHasGetter && subHasSetter) return

        val ownerClassId = property.symbol.callableId.classId ?: return
        val ownerSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId) ?: return
        val ownerDecl = ownerSymbol.cfir as? CfirClassLikeDeclaration ?: return

        val superDecls = ownerDecl.superTypeRefs.mapNotNull { ref ->
            val type = (ref as? CfirResolvedTypeRef)?.coneType as? org.cangnova.cangjie.cfir.types.ConeClassLikeType
            type?.let { context.session.symbolProvider.getClassLikeSymbolByClassId(it.classId)?.cfir as? CfirClassLikeDeclaration }
        }

        for (superDecl in superDecls) {
            val superProp = superDecl.declarations.firstOrNull {
                it is CfirProperty && it.name == property.name
            } as? CfirProperty ?: continue

            if (superProp.status.isDefault || superProp.symbol.isAbstractLike(context)) {
                reporter.reportOn(
                    source = property.source,
                    factory = CfirErrors.PROPERTY_MUST_IMPLEMENT_BOTH,
                    a = property.name,
                )
                return
            }
        }
    }

    /**
     * 继承的属性 mut/immut 必须一致。
     *
     * 对齐 C++ sema_property_have_same_declaration_in_inherit_mut / _immut
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPropertyInheritConsistency(property: CfirProperty) {
        if (!property.status.isOverride) return

        val ownerClassId = property.symbol.callableId.classId ?: return
        val ownerSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId) ?: return
        val ownerDecl = ownerSymbol.cfir as? CfirClassLikeDeclaration ?: return

        val superDecls = ownerDecl.superTypeRefs.mapNotNull { ref ->
            val type = (ref as? CfirResolvedTypeRef)?.coneType as? org.cangnova.cangjie.cfir.types.ConeClassLikeType
            type?.let { context.session.symbolProvider.getClassLikeSymbolByClassId(it.classId)?.cfir as? CfirClassLikeDeclaration }
        }

        val subIsMutable = property.status.isMut

        for (superDecl in superDecls) {
            val superProp = superDecl.declarations.firstOrNull {
                it is CfirProperty && it.name == property.name
            } as? CfirProperty ?: continue

            val superIsMutable = superProp.status.isMut

            if (superIsMutable && !subIsMutable) {
                // 父声明是可变（有 setter），子声明不是
                reporter.reportOn(
                    source = property.source,
                    factory = CfirErrors.PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT,
                    a = property.name,
                )
                return
            }
            if (!superIsMutable && subIsMutable) {
                // 父声明不可变，子声明是可变
                reporter.reportOn(
                    source = property.source,
                    factory = CfirErrors.PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT,
                    a = property.name,
                )
                return
            }
        }
    }
}

// ──────────── 工具函数 ────────────

/**
 * 取得声明用于访问级别检查的可见性。
 */
private fun CfirDeclaration.accessLevelVisibility(): Visibility? =
    (this as? CfirMemberDeclaration)?.status?.visibility

/**
 * 计算成员在外层声明可见性约束下的有效可见性。
 */
private fun Visibility.effectiveInside(containingAccessLevel: Visibility?): Visibility {
    val ownRank = cangjieAccessLevelRank() ?: return this
    val containingRank = containingAccessLevel?.cangjieAccessLevelRank() ?: return this
    return if (containingRank < ownRank) containingAccessLevel else this
}

/**
 * 判断当前声明可见性是否允许暴露给定类型可见性。
 */
private fun Visibility.canExpose(typeVisibility: Visibility): Boolean {
    val declarationRank = cangjieAccessLevelRank() ?: return true
    val typeRank = typeVisibility.cangjieAccessLevelRank() ?: return true
    return declarationRank <= typeRank
}

/**
 * 将仓颉访问级别映射为可比较的访问等级。
 */
private fun Visibility.cangjieAccessLevelRank(): Int? = when (this) {
    Visibilities.Private, Visibilities.PrivateToThis -> 0
    Visibilities.Internal -> 1
    Visibilities.Protected -> 2
    Visibilities.Public -> 3
    else -> null
}

/**
 * 取得访问级别诊断应使用的声明 source。
 */
private fun CfirDeclaration.accessibilityDiagnosticSource(): AbstractCjSourceElement? = when (this) {
    is CfirClassLikeDeclaration -> classLikeNameDiagnosticSource()
    is CfirNamedFunction -> functionNameDiagnosticSource()
    else -> source
}

/**
 * 在类型引用上查找第一个可见性暴露问题。
 */
context(context: CheckerContext)
private fun CfirTypeRef.findFirstExposure(declarationVisibility: Visibility): Visibility? {
    val resolvedType = when (this) {
        is CfirResolvedTypeRef -> coneType
        is ResolvedImplicitTypeRef -> typeRef.coneType
        else -> return null
    }
    return resolvedType.findFirstExposure(declarationVisibility)
}

/**
 * 判断声明类型是否来自推断。
 *
 * resolve 可能把隐式类型直接写成无 source 的 resolved type ref，也可能保留
 * [ResolvedImplicitTypeRef] 包装；两种形态都必须映射到 with-main-hint 诊断。
 */
private fun CfirTypeRef.isImplicitAccessibilityType(): Boolean =
    this is CfirImplicitTypeRef || source == null

/**
 * 在 cone 类型结构中递归查找第一个可见性暴露问题。
 *
 * 遍历范围覆盖类型实参、函数类型、元组、varray、指针、交叉/联合类型以及类型别名展开，
 * visitedTypes 用于避免递归类型结构导致无限遍历。
 */
context(context: CheckerContext)
private fun ConeCangJieType.findFirstExposure(
    declarationVisibility: Visibility,
    visitedTypes: MutableSet<ConeCangJieType> = linkedSetOf(),
): Visibility? {
    if (this is ConeErrorType) return null
    if (!visitedTypes.add(this)) return null

    for (projection in typeArguments) {
        projection.type.findFirstExposure(declarationVisibility, visitedTypes)?.let { return it }
    }

    when (this) {
        is ConeFunctionType -> {
            for (parameterType in parameterTypes) {
                parameterType.findFirstExposure(declarationVisibility, visitedTypes)?.let { return it }
            }
            returnType.findFirstExposure(declarationVisibility, visitedTypes)?.let { return it }
        }

        is ConeTupleType -> {
            for (elementType in elementTypes) {
                elementType.findFirstExposure(declarationVisibility, visitedTypes)?.let { return it }
            }
        }

        is ConeVArrayType -> elementType.findFirstExposure(declarationVisibility, visitedTypes)?.let { return it }
        is ConePointerType -> pointeeType.findFirstExposure(declarationVisibility, visitedTypes)?.let { return it }
        is ConeIntersectionType -> {
            for (intersectedType in intersectedTypes) {
                intersectedType.findFirstExposure(declarationVisibility, visitedTypes)?.let { return it }
            }
            upperBoundForApproximation?.findFirstExposure(declarationVisibility, visitedTypes)?.let { return it }
        }

        is ConeUnionType -> {
            for (unionType in unionTypes) {
                unionType.findFirstExposure(declarationVisibility, visitedTypes)?.let { return it }
            }
        }

        else -> {}
    }

    classIdOrNull()?.let { classId ->
        val referencedDeclaration = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
        val referencedVisibility = referencedDeclaration?.accessLevelVisibility()
        if (referencedVisibility != null && !declarationVisibility.canExpose(referencedVisibility)) {
            return referencedVisibility
        }
    }

    if (this is ConeTypeAliasType) {
        expandedType?.findFirstExposure(declarationVisibility, visitedTypes)?.let { return it }
    }

    return null
}

/**
 * 取得声明的语义名称。
 */
private fun CfirDeclaration.declarationName(): Name? = when (this) {
    is CfirClass -> name
    is CfirInterface -> name
    is CfirStruct -> name
    is CfirEnum -> name
    is CfirNamedFunction -> name
    is CfirProperty -> name
    is CfirFieldVariable -> name
    is CfirTypeAlias -> name
    else -> null
}

/**
 * 判断 cone 类型结构中是否引用了指定类型参数符号。
 */
private fun ConeCangJieType.containsTypeParameter(symbol: CfirTypeParameterSymbol): Boolean {
    if (this is ConeTypeParameterType && this.lookupTag.typeParameterSymbol == symbol) return true
    for (arg in this.typeArguments) {
        val argType = arg.type ?: continue
        if (argType.containsTypeParameter(symbol)) return true
    }
    return false
}

/**
 * 从 cone 类型中提取 class-like 或 typealias 的 classId。
 */
private fun ConeCangJieType.classIdOrNull(): org.cangnova.cangjie.name.ClassId? {
    return when (this) {
        is ConeClassLikeType -> classId
        is ConeStructType -> classId
        is ConeEnumType -> classId
        is ConeTypeAliasType -> classId
        else -> null
    }
}
