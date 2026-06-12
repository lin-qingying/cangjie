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
import org.cangnova.cangjie.cfir.isCatchParameter
import org.cangnova.cangjie.cfir.session.cjMappingConfigProvider
import org.cangnova.cangjie.cfir.session.noPrelude
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind

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
        val exposure = declaration.findFirstSignatureExposure(effectiveVisibility)
        if (exposure != null) {
            reporter.reportOn(
                source = declaration.accessibilityDiagnosticSource(),
                factory = CfirErrors.ACCESSIBILITY_ERROR,
                a = declaration.declarationName()?.asString() ?: "<unknown>",
                b = exposure,
            )
        }

        if (declaration is CfirClassLikeDeclaration) {
            for (member in declaration.declarations) {
                checkNonPrivateDeclarationAccessLevelValidity(member, effectiveVisibility)
            }
        }
    }

    context(context: CheckerContext)
    private fun CfirDeclaration.findFirstSignatureExposure(
        declarationVisibility: Visibility,
    ): Visibility? {
        (this as? CfirTypeParameterRefsOwner)
            ?.findFirstTypeParameterBoundExposure(declarationVisibility)
            ?.let { return it }

        return when (this) {
            is CfirNamedFunction -> {
                returnTypeRef.findFirstExposure(declarationVisibility)
                    ?: valueParameters.asSequence()
                        .mapNotNull { it.returnTypeRef.findFirstExposure(declarationVisibility) }
                        .firstOrNull()
            }

            is CfirProperty -> returnTypeRef.findFirstExposure(declarationVisibility)
            is CfirFieldVariable -> returnTypeRef.findFirstExposure(declarationVisibility)
            is CfirTypeAlias -> expandedTypeRef.findFirstExposure(declarationVisibility)
            else -> null
        }
    }

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
 * - TYPE_UNINITIALIZED_STATIC_FIELD: static 成员未初始化
 * - INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER: finalizer 中不能调用实例函数
 * - CSTRUCT_CANNOT_IMPL_INTERFACES: @C struct 不能实现接口
 * - EXPORT_SAME_PRIVATE_DECL: 同名 private 导出限制
 */
object CfirClassStructSemanticsChecker : CfirClassLikeChecker() {
    private val C_ANNOTATION = Name.identifier("C")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration is CfirClass) {
            checkSealedOnlyOnAbstract(declaration)
            checkStaticVariableGenericParameterDependency(declaration)
            checkUninitializedStaticFields(declaration)
            checkFinalizerConstraints(declaration)
        }
        if (declaration is CfirStruct) {
            checkStaticVariableGenericParameterDependency(declaration)
            checkCStructCannotImplInterfaces(declaration)
            checkUninitializedStaticFields(declaration)
        }
        if (declaration is CfirEnum) {
            checkStaticVariableGenericParameterDependency(declaration)
            checkUninitializedStaticFields(declaration)
        }
        if (declaration is CfirInterface) {
            checkStaticVariableGenericParameterDependency(declaration)
        }
    }

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
     * static let 字段必须有初始化值。
     *
     * 对齐 C++ DiagKind::sema_type_uninitialized_static_field
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkUninitializedStaticFields(classLike: CfirClassLikeDeclaration) {
        for (member in classLike.declarations) {
            val fieldVariable = member as? CfirFieldVariable ?: continue
            if (!fieldVariable.status.isStatic) continue
            if (fieldVariable.isVar) continue // var 可以后续赋值
            if (fieldVariable.initializer != null) continue
            reporter.reportOn(
                source = fieldVariable.source,
                factory = CfirErrors.TYPE_UNINITIALIZED_STATIC_FIELD,
                a = fieldVariable.name,
            )
        }
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

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkStaticVariableGenericParameterDependency(classLike: CfirClassLikeDeclaration) {
        val typeParameterNames = when (classLike) {
            is CfirClass -> classLike.typeParameters.map { it.name }.toSet()
            is CfirStruct -> classLike.typeParameters.map { it.name }.toSet()
            is CfirEnum -> classLike.typeParameters.map { it.name }.toSet()
            is CfirInterface -> classLike.typeParameters.map { it.name }.toSet()
            else -> return
        }
        if (typeParameterNames.isEmpty()) return
        for (member in classLike.declarations) {
            val fieldVariable = member as? CfirFieldVariable ?: continue
            if (!fieldVariable.status.isStatic) continue
            val resolvedType = (fieldVariable.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            for (typeParamName in typeParameterNames) {
                if (resolvedType.containsTypeParameter(typeParamName)) {
                    reporter.reportOn(
                        source = fieldVariable.source,
                        factory = CfirErrors.STATIC_VARIABLE_USE_GENERIC_PARAMETER,
                        a = typeParamName,
                    )
                    break
                }
            }
        }
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
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirProperty) {
        checkPropertyAccessors(declaration)
        checkImmutablePropertySetter(declaration)
        checkPropertyInheritConsistency(declaration)
        checkPropertyMustImplementBoth(declaration)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPropertyAccessors(property: CfirProperty) {
        if (property.isCatchParameter == true) return
        if (property.source?.kind == CjFakeSourceElementKind.PropertyFromParameter) return
        if (context.containingDeclarations.lastOrNull() is CfirInterface) return
        if (property.status.isAbstract) return
        if (property.getter == null && property.setter == null) {
            reporter.reportOn(
                source = property.source,
                factory = CfirErrors.PROPERTY_MUST_HAVE_ACCESSORS,
            )
        }
    }

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
     * 实现接口属性时必须同时实现 getter 和 setter（如果接口声明了两者）。
     *
     * 对齐 C++ DiagKind::sema_property_must_implement_both
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPropertyMustImplementBoth(property: CfirProperty) {
        // override 的属性如果父声明同时有 getter 和 setter，子属性也必须同时实现
        if (!property.status.isOverride) return

        // 通过 symbolProvider 查找父类/接口对应属性
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

            val superHasGetter = superProp.getter != null
            val superHasSetter = superProp.setter != null
            val subHasGetter = property.getter != null
            val subHasSetter = property.setter != null

            if (superHasGetter && superHasSetter && (!subHasGetter || !subHasSetter)) {
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

        val subIsMutable = property.setter != null

        for (superDecl in superDecls) {
            val superProp = superDecl.declarations.firstOrNull {
                it is CfirProperty && it.name == property.name
            } as? CfirProperty ?: continue

            val superIsMutable = superProp.setter != null

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

private fun CfirDeclaration.accessLevelVisibility(): Visibility? =
    (this as? CfirMemberDeclaration)?.status?.visibility

private fun Visibility.effectiveInside(containingAccessLevel: Visibility?): Visibility {
    val ownRank = cangjieAccessLevelRank() ?: return this
    val containingRank = containingAccessLevel?.cangjieAccessLevelRank() ?: return this
    return if (containingRank < ownRank) containingAccessLevel else this
}

private fun Visibility.canExpose(typeVisibility: Visibility): Boolean {
    val declarationRank = cangjieAccessLevelRank() ?: return true
    val typeRank = typeVisibility.cangjieAccessLevelRank() ?: return true
    return declarationRank <= typeRank
}

private fun Visibility.cangjieAccessLevelRank(): Int? = when (this) {
    Visibilities.Private, Visibilities.PrivateToThis -> 0
    Visibilities.Internal -> 1
    Visibilities.Protected -> 2
    Visibilities.Public -> 3
    else -> null
}

private fun CfirDeclaration.accessibilityDiagnosticSource(): AbstractCjSourceElement? = when (this) {
    is CfirClassLikeDeclaration -> classLikeNameDiagnosticSource()
    is CfirNamedFunction -> functionNameDiagnosticSource()
    else -> source
}

context(context: CheckerContext)
private fun CfirTypeRef.findFirstExposure(declarationVisibility: Visibility): Visibility? {
    val resolvedType = (this as? CfirResolvedTypeRef)?.coneType ?: return null
    return resolvedType.findFirstExposure(declarationVisibility)
}

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

private fun ConeCangJieType.containsTypeParameter(name: Name): Boolean {
    if (this is ConeTypeParameterType && this.lookupTag.name == name) return true
    for (arg in this.typeArguments) {
        val argType = arg.type ?: continue
        if (argType.containsTypeParameter(name)) return true
    }
    return false
}

private fun ConeCangJieType.classIdOrNull(): org.cangnova.cangjie.name.ClassId? {
    return when (this) {
        is ConeClassLikeType -> classId
        is ConeStructType -> classId
        is ConeEnumType -> classId
        is ConeTypeAliasType -> classId
        else -> null
    }
}
