package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.psi.CjModifierListOwner

/**
 * 泛型 Java 互操作检查器（GenericDeep Java 子集）
 *
 * 对齐 C++ TypeCheckGeneric.cpp 中 Java 相关泛型约束:
 * - @Java 类型中 static 成员的类型不能依赖泛型参数
 * - 基本类型不能作为 @Java 泛型的类型参数
 * - @Java 泛型上界必须是 @Java 类型
 *
 * 注册为 classLikeCheckers
 */
object CfirGenericJavaInteropChecker : CfirClassLikeChecker() {
    private val JAVA = Name.identifier("Java")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        val owner = declaration.source?.psi as? CjModifierListOwner ?: return
        if (!owner.hasAnnotationEntry(JAVA)) return

        checkStaticMembersNotDependOnGenericParams(declaration)
        checkGenericUpperBoundsAreJava(declaration)
    }

    private fun CfirClassLikeDeclaration.typeParametersList(): List<CfirTypeParameter> = when (this) {
        is CfirClass -> typeParameters
        is CfirStruct -> typeParameters
        is CfirEnum -> typeParameters
        is CfirInterface -> typeParameters
        else -> emptyList()
    }

    /**
     * @Java 类型中 static 成员的类型不能引用所在类的泛型参数。
     *
     * 对齐 C++ DiagKind::sema_generic_static_access
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkStaticMembersNotDependOnGenericParams(declaration: CfirClassLikeDeclaration) {
        val typeParamNames = declaration.typeParametersList().map { it.name }.toSet()
        if (typeParamNames.isEmpty()) return

        for (member in declaration.declarations) {
            val isStatic = when (member) {
                is CfirNamedFunction -> member.status.isStatic
                is CfirProperty -> member.status.isStatic
                is CfirFieldVariable -> member.status.isStatic
                else -> false
            }
            if (!isStatic) continue

            val memberType = when (member) {
                is CfirNamedFunction -> (member.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                is CfirProperty -> (member.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                is CfirFieldVariable -> (member.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                else -> null
            } ?: continue

            if (typeContainsAnyParam(memberType, typeParamNames)) {
                reporter.reportOn(
                    source = member.source,
                    factory = CfirErrors.GENERIC_STATIC_ACCESS,
                )
            }
        }
    }

    /**
     * @Java 类型的泛型参数上界必须也是 @Java 类型。
     *
     * 对齐 C++ DiagKind::sema_generic_upper_bounds_must_be_java_in_java
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkGenericUpperBoundsAreJava(declaration: CfirClassLikeDeclaration) {
        for (typeParam in declaration.typeParametersList()) {
            for (bound in typeParam.symbol.resolvedBounds) {
                val boundType = bound.coneType
                if (boundType is ConeClassLikeType) {
                    val boundClassId = boundType.classId
                    val boundSymbol = context.session.symbolProvider
                        .getClassLikeSymbolByClassId(boundClassId) ?: continue
                    val boundDecl = boundSymbol.cfir
                    val boundOwner = boundDecl.source?.psi as? CjModifierListOwner
                    if (boundOwner != null && !boundOwner.hasAnnotationEntry(JAVA)) {
                        reporter.reportOn(
                            source = bound.source ?: typeParam.source,
                            factory = CfirErrors.GENERIC_UPPER_BOUNDS_MUST_BE_JAVA_IN_JAVA,
                        )
                    }
                }
                // 基本类型不能作为 @Java 泛型参数
                if (boundType is ConePrimitiveType) {
                    reporter.reportOn(
                        source = bound.source ?: typeParam.source,
                        factory = CfirErrors.PRIMITIVE_TYPE_AS_GENERICS_ARG,
                    )
                }
            }
        }
    }

    private fun typeContainsAnyParam(type: ConeCangJieType, paramNames: Set<Name>): Boolean {
        if (type is ConeTypeParameterType && type.lookupTag.name in paramNames) return true
        for (arg in type.typeArguments) {
            val argType = arg.type ?: continue
            if (typeContainsAnyParam(argType, paramNames)) return true
        }
        return false
    }

    private fun CjModifierListOwner.hasAnnotationEntry(name: Name): Boolean =
        annotationEntries.any { it.shortName == name }
}
