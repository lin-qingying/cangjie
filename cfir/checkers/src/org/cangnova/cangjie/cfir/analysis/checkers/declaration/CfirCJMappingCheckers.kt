package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjModifierListOwner
import org.cangnova.cangjie.source.psi

/**
 * CJMapping（Java）语义检查器
 *
 * 对齐 C++ sema_cjmapping_* 系列:
 * - cangjie mirror struct 的泛型不支持
 * - cangjie mirror struct 继承接口不支持
 *
 * 注册为 classLikeCheckers
 */
object CfirCJMappingChecker : CfirClassLikeChecker() {
    private val CJ_MAPPING = Name.identifier("CJMapping")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        val owner = declaration.source?.psi as? CjModifierListOwner ?: return
        if (!owner.annotationEntries.any { it.shortName == CJ_MAPPING }) return

        if (declaration is CfirStruct) {
            // struct 泛型不支持
            if (declaration.typeParameters.isNotEmpty()) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.CJMAPPING_STRUCT_GENERIC_NOT_SUPPORTED,
                    a = declaration.typeParameters.joinToString { it.name.asString() },
                )
            }
            // struct 继承接口不支持
            if (declaration.superTypeRefs.isNotEmpty()) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.CJMAPPING_STRUCT_INHERITANCE_INTERFACE_NOT_SUPPORTED,
                )
            }
        }
    }
}

/**
 * ObjC CJMapping 语义检查器
 *
 * 对齐 C++ sema_objc_cjmapping_* 系列:
 * - cangjie mirror 声明继承接口不支持
 * - cangjie mirror 声明泛型不支持
 *
 * 注册为 classLikeCheckers
 */
object CfirObjCCJMappingChecker : CfirClassLikeChecker() {
    private val OBJC_CJ_MAPPING = Name.identifier("ObjCCJMapping")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        val owner = declaration.source?.psi as? CjModifierListOwner ?: return
        if (!owner.annotationEntries.any { it.shortName == OBJC_CJ_MAPPING }) return

        if (declaration.superTypeRefs.isNotEmpty()) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.OBJC_CJMAPPING_INHERITANCE_INTERFACE_NOT_SUPPORTED,
            )
        }

        if (declaration is CfirClassLikeDeclaration) {
            val typeParams = when (declaration) {
                is org.cangnova.cangjie.cfir.declarations.CfirClass -> declaration.typeParameters
                is CfirStruct -> declaration.typeParameters
                is org.cangnova.cangjie.cfir.declarations.CfirEnum -> declaration.typeParameters
                is CfirInterface -> declaration.typeParameters
                else -> emptyList()
            }
            if (typeParams.isNotEmpty()) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.OBJC_CJMAPPING_GENERIC_NOT_SUPPORTED,
                    a = typeParams.joinToString { it.name.asString() },
                )
            }
        }
    }
}
