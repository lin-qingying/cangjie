package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjModifierListOwner
import org.cangnova.cangjie.source.psi

/**
 * CJMapping（Java）语义检查器
 *
 * 对齐 C++ sema_cjmapping_* 系列
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
            if (declaration.typeParameters.isNotEmpty()) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.CJMAPPING_STRUCT_GENERIC_NOT_SUPPORTED,
                    a = declaration.typeParameters.joinToString { it.name.asString() },
                )
            }
            if (declaration.superTypeRefs.isNotEmpty()) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.CJMAPPING_STRUCT_INHERITANCE_INTERFACE_NOT_SUPPORTED,
                )
            }
        }

        // 不支持的声明类型检查（enum 不能作为 CJMapping）
        if (declaration is CfirEnum) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.CJMAPPING_DECL_NOT_SUPPORTED,
                a = "enum",
            )
        }

        // 成员函数参数和返回类型检查
        for (member in declaration.declarations) {
            if (member !is CfirNamedFunction) continue

            // 检查函数参数类型
            for (param in member.valueParameters) {
                val paramType = (param.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
                if (paramType !is ConePrimitiveType && paramType !is ConeClassLikeType) {
                    reporter.reportOn(
                        source = param.source ?: member.source ?: declaration.source,
                        factory = CfirErrors.CJMAPPING_METHOD_ARG_NOT_SUPPORTED,
                    )
                }
            }

            // 检查函数返回类型
            val returnType = (member.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            if (returnType != null && !returnType.isUnit && returnType !is ConePrimitiveType && returnType !is ConeClassLikeType) {
                val containerKind = when (declaration) {
                    is CfirClass -> "class"
                    is CfirStruct -> "struct"
                    is CfirInterface -> "interface"
                    else -> "type"
                }
                reporter.reportOn(
                    source = member.returnTypeRef.source ?: member.source ?: declaration.source,
                    factory = CfirErrors.CJMAPPING_METHOD_RET_UNSUPPORTED,
                    a = returnType,
                    b = containerKind,
                )
            }
        }
    }
}

/**
 * ObjC CJMapping 语义检查器
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

        val typeParams = when (declaration) {
            is CfirClass -> declaration.typeParameters
            is CfirStruct -> declaration.typeParameters
            is CfirEnum -> declaration.typeParameters
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
