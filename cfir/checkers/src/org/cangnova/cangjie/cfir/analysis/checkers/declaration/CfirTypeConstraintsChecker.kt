package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirTypeConstraintDiagnosticData
import org.cangnova.cangjie.cfir.declarations.CfirTypeConstraintReference
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.declarations.typeConstraintDiagnosticData
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedTypeQualifierError
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * 泛型 where 约束声明检查器。
 *
 * 该检查器处理 raw/build 阶段记录的 type constraint 诊断数据，确保约束中的名字必须来自当前
 * 声明的类型参数列表。
 */
object CfirTypeConstraintsChecker : CfirBasicDeclarationChecker() {
    /**
     * 检查声明是否带有悬空 type constraint。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        val owner = declaration as? CfirTypeParameterRefsOwner ?: return
        val diagnosticData = declaration.attributes.typeConstraintDiagnosticData ?: return

        reportDanglingTypeConstraints(owner, diagnosticData)
    }

    /**
     * 报告约束中引用了非类型参数名字的错误。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportDanglingTypeConstraints(
        owner: CfirTypeParameterRefsOwner,
        diagnosticData: CfirTypeConstraintDiagnosticData,
    ) {
        val declaredTypeParameters = owner.typeParameters
            .map { it.symbol.name }
            .toSet()

        diagnosticData.typeConstraints.forEach { constraint ->
            if (constraint.parameterName in declaredTypeParameters) return@forEach

            if (constraint.parameterName.resolvesToVisibleClassifier()) {
                reporter.reportOn(
                    source = constraint.source,
                    factory = CfirErrors.NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER,
                    a = constraint.parameterName,
                )
            } else {
                reporter.reportOn(
                    source = constraint.source,
                    factory = CfirErrors.UNDECLARED_TYPE_NAME,
                    a = constraint.parameterName.asString(),
                )
            }
            constraint.reportDanglingBoundTypeErrors(declaredTypeParameters)
        }
    }

    /**
     * 左侧不是当前 owner 的类型参数时，RHS 不会挂入任何类型参数 bounds。
     * 官方仍会继续检查 RHS type-name，因此这里补上 dangling constraint 的 RHS 未解析类型诊断。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirTypeConstraintReference.reportDanglingBoundTypeErrors(declaredTypeParameters: Set<Name>) {
        boundTypeRefs.forEach { boundTypeRef ->
            boundTypeRef.reportDanglingBoundTypeErrors(declaredTypeParameters)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirTypeRef.reportDanglingBoundTypeErrors(declaredTypeParameters: Set<Name>) {
        when (this) {
            is CfirErrorTypeRef -> reportUnresolvedTypeQualifier()
            is CfirUserTypeRef -> reportUnresolvedRawUserType(declaredTypeParameters)
            else -> Unit
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirUserTypeRef.reportUnresolvedRawUserType(declaredTypeParameters: Set<Name>) {
        val lastQualifier = qualifier.lastOrNull() ?: return
        if (lastQualifier.name !in declaredTypeParameters && !lastQualifier.name.resolvesToVisibleClassifier()) {
            reporter.reportOn(
                source = lastQualifier.source ?: source,
                factory = CfirErrors.UNDECLARED_TYPE_NAME,
                a = lastQualifier.name.asString(),
            )
        }
        qualifier.forEach { qualifierPart ->
            qualifierPart.typeArguments.forEach { typeArgument ->
                typeArgument.reportDanglingBoundTypeErrors(declaredTypeParameters)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirErrorTypeRef.reportUnresolvedTypeQualifier() {
        val diagnostic = diagnostic.unwrapUnreportedDuplicateDiagnostic() as? ConeUnresolvedTypeQualifierError ?: return
        val qualifier = diagnostic.qualifiers.lastOrNull()
        reporter.reportOn(
            source = qualifier?.source ?: source,
            factory = CfirErrors.UNDECLARED_TYPE_NAME,
            a = (qualifier?.name ?: Name.identifier(diagnostic.qualifier)).asString(),
        )
    }

    private tailrec fun ConeDiagnostic.unwrapUnreportedDuplicateDiagnostic(): ConeDiagnostic =
        if (this is ConeUnreportedDuplicateDiagnostic) original.unwrapUnreportedDuplicateDiagnostic() else this

    /**
     * 官方先按类型名解析 where 左侧；只有名字可解析但不是当前 owner 的类型参数时，
     * 才进入“约束名不是类型参数”的声明诊断。
     */
    context(context: CheckerContext)
    private fun Name.resolvesToVisibleClassifier(): Boolean {
        val file = context.containingFileSymbol?.takeIf { it.isBound }?.cfir
        if (file != null && file.hasTopLevelClassifier(this)) return true

        val packageFqName = file?.packageDirective?.packageFqName
        val provider = context.session.symbolProvider
        return listOfNotNull(packageFqName, StandardNames.BASIC_PACKAGE_FQ_NAME)
            .distinct()
            .any { candidatePackage ->
                provider.getClassLikeSymbolByClassId(ClassId(candidatePackage, this)) != null
            }
    }

    private fun CfirFile.hasTopLevelClassifier(name: Name): Boolean =
        declarations.any { declaration ->
            declaration is CfirClassLikeDeclaration && declaration.name == name
        }
}
