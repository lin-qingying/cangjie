package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.cjMappingConfigProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjModifierListOwner
import org.cangnova.cangjie.source.psi

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
        val stdCoreObjectId = org.cangnova.cangjie.name.ClassId(
            org.cangnova.cangjie.name.FqName("std.core"),
            Name.identifier("Object"),
        )
        val objectSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(stdCoreObjectId)
        if (objectSymbol == null) {
            // 只在 std.core 包本身不存在时报告（即 --no-prelude 场景）
            val stdCoreFqName = org.cangnova.cangjie.name.FqName("std.core")
            if (!context.session.symbolProvider.hasPackage(stdCoreFqName)) {
                reporter.reportOn(
                    source = file.source,
                    factory = CfirErrors.CORE_OBJECT_NOT_FOUND_WHEN_NO_PRELUDE,
                )
            }
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
     * 使用 Java 互操作注解时必须导入 interoplib.interop。
     *
     * 对齐 C++ sema_java_mirror_interoplib_must_be_imported
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkJavaInteropImports(file: CfirFile) {
        val javaAnnNames = setOf(
            Name.identifier("Java"),
            Name.identifier("JavaMirror"),
            Name.identifier("JavaImpl"),
        )
        val usesJavaInterop = file.declarations.any { decl ->
            val owner = decl.source?.psi as? CjModifierListOwner ?: return@any false
            owner.annotationEntries.any { it.shortName in javaAnnNames }
        }
        if (!usesJavaInterop) return
        val interopFq = org.cangnova.cangjie.name.FqName("interoplib.interop")
        val imported = file.imports.any { imp ->
            val fq = imp.importedFqName ?: return@any false
            fq == interopFq || fq.parent() == interopFq
        }
        if (!imported) {
            reporter.reportOn(
                source = file.source,
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
            val owner = decl.source?.psi as? CjModifierListOwner ?: continue
            if (owner.annotationEntries.none { it.shortName == javaImplName }) continue
            val declName = decl.declarationName() ?: continue
            byName.merge(declName, 1) { a, b -> a + b }
        }
        for (decl in file.declarations) {
            val owner = decl.source?.psi as? CjModifierListOwner ?: continue
            if (owner.annotationEntries.none { it.shortName == javaImplName }) continue
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
     * 同包内不允许导出两个同名的 private 顶层声明。
     *
     * 对齐 C++ sema_export_same_private_decl
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExportSamePrivateDecl(file: CfirFile) {
        val byName = mutableMapOf<Name, Int>()
        for (decl in file.declarations) {
            val vis = when (decl) {
                is CfirNamedFunction -> decl.status.visibility
                is CfirProperty -> decl.status.visibility
                is CfirFieldVariable -> decl.status.visibility
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
                is CfirNamedFunction -> decl.status.visibility
                is CfirProperty -> decl.status.visibility
                is CfirFieldVariable -> decl.status.visibility
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
     * 公开声明的签名中不能引用非公开类型。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAccessibilityInFile(file: CfirFile) {
        for (declaration in file.declarations) {
            checkPublicDeclarationExposesPrivateType(declaration)
        }
    }

    /**
     * 公开声明不能暴露非公开类型。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPublicDeclarationExposesPrivateType(declaration: CfirDeclaration) {
        val visibility = when (declaration) {
            is CfirNamedFunction -> declaration.status.visibility
            is CfirProperty -> declaration.status.visibility
            is CfirFieldVariable -> declaration.status.visibility
            is CfirClass -> declaration.status.visibility
            is CfirInterface -> declaration.status.visibility
            is CfirStruct -> declaration.status.visibility
            is CfirEnum -> declaration.status.visibility
            else -> return
        }
        if (visibility != Visibilities.Public) return

        // 检查返回类型或属性类型的可访问性
        val returnTypeRef = when (declaration) {
            is CfirNamedFunction -> declaration.returnTypeRef
            is CfirProperty -> declaration.returnTypeRef
            is CfirFieldVariable -> declaration.returnTypeRef
            else -> return
        }
        val resolvedType = (returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        if (resolvedType is ConeErrorType) return

        val typeClassId = resolvedType.classIdOrNull() ?: return
        val typeSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(typeClassId) ?: return
        val typeDecl = typeSymbol.cfir
        val typeVisibility = when (typeDecl) {
            is CfirClass -> typeDecl.status.visibility
            is CfirInterface -> typeDecl.status.visibility
            is CfirStruct -> typeDecl.status.visibility
            is CfirEnum -> typeDecl.status.visibility
            else -> return
        }
        if (typeVisibility == Visibilities.Private || typeVisibility == Visibilities.Internal) {
            reporter.reportOn(
                source = returnTypeRef.source ?: declaration.source,
                factory = CfirErrors.ACCESSIBILITY_ERROR,
                a = declaration.declarationName()?.asString() ?: "<unknown>",
                b = typeVisibility,
            )
        }
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
        val owner = structDecl.source?.psi as? CjModifierListOwner ?: return
        if (!owner.annotationEntries.any { it.shortName == C_ANNOTATION }) return
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
                        if (sym != null && !sym.cfir.status.isStatic) {
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
        if (property.getter == null && property.setter == null) {
            reporter.reportOn(
                source = property.source,
                factory = CfirErrors.PROPERTY_MUST_HAVE_ACCESSORS,
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkImmutablePropertySetter(property: CfirProperty) {
        if (  property.setter != null) {
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
        is org.cangnova.cangjie.cfir.types.ConeClassLikeType -> classId
        is org.cangnova.cangjie.cfir.types.ConeStructType -> classId
        is org.cangnova.cangjie.cfir.types.ConeEnumType -> classId
        else -> null
    }
}
