package org.cangjie.cfir.checkers.generator

import org.cangjie.cfir.checkers.generator.diagnostics.DIAGNOSTICS_LIST
import org.cangjie.cfir.checkers.generator.diagnostics.model.ErrorListDiagnosticListRenderer
import org.cangjie.cfir.checkers.generator.diagnostics.model.generateDiagnostics
import org.cangjie.cfir.declarations.CfirAnnotation
import org.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangjie.cfir.declarations.CfirClass
import org.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangjie.cfir.declarations.CfirDeclaration
import org.cangjie.cfir.declarations.CfirFile
import org.cangjie.cfir.declarations.CfirFunction
import org.cangjie.cfir.declarations.CfirInvalidDeclaration
import org.cangjie.cfir.declarations.CfirMainFunction
import org.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangjie.cfir.declarations.CfirProperty
import org.cangjie.cfir.declarations.CfirTypeAlias
import org.cangjie.cfir.declarations.CfirTypeParameter
import org.cangjie.cfir.declarations.CfirValueParameter
import org.cangjie.cfir.declarations.CfirVariable
import org.cangjie.cfir.expressions.CfirAssignment
import org.cangjie.cfir.expressions.CfirBinaryOp
import org.cangjie.cfir.expressions.CfirComparisonExpression
import org.cangjie.cfir.expressions.CfirErrorExpression
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.CfirFunctionCall
import org.cangjie.cfir.expressions.CfirIfExpression
import org.cangjie.cfir.expressions.CfirJumpExpression
import org.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangjie.cfir.expressions.CfirMatchExpression
import org.cangjie.cfir.expressions.CfirPropertyAccess
import org.cangjie.cfir.expressions.CfirQualifiedAccess
import org.cangjie.cfir.expressions.CfirRangeExpression
import org.cangjie.cfir.expressions.CfirReturnExpression
import org.cangjie.cfir.expressions.CfirStatement
import org.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangjie.cfir.expressions.CfirThrowExpression
import org.cangjie.cfir.expressions.CfirTryExpression
import org.cangjie.cfir.expressions.CfirTypeOperator
import org.cangjie.cfir.types.CfirBasicTypeRef
import org.cangjie.cfir.types.CfirErrorTypeRef
import org.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangjie.cfir.types.CfirTupleTypeRef
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.types.CfirUserTypeRef
import org.cangjie.cfir.types.CfirVArrayTypeRef
import java.io.File

fun main(args: Array<String>) {
    val basePackage = "org.cangjie.cfir.analysis"
    val packageName = "$basePackage.diagnostics"
    val generationPath = args.getOrNull(1)?.let(::File)
    val task = when {
        args.isEmpty() -> "all"
        args[0] == "checkers" || args[0] == "diagnostics" || args[0] == "all" -> args[0]
        else -> "diagnostics"
    }

    if (task == "checkers" || task == "diagnostics" || task == "all") {
        val checkersPath = generationPath ?: File("cfir/checkers/gen")
        generateCheckersComponents(
            checkersPath,
            "$basePackage.checkers.type",
            "CfirTypeChecker",
            CfirTypeRef::class,
            CfirTypeRef::class,
        ) {
            alias<CfirTypeRef>("TypeRefChecker").let {
                visitAlso<CfirImplicitTypeRef>(it)
                visitAlso<CfirUserTypeRef>(it)
                visitAlso<CfirBasicTypeRef>(it)
                visitAlso<CfirFunctionTypeRef>(it)
                visitAlso<CfirTupleTypeRef>(it)
                visitAlso<CfirVArrayTypeRef>(it)
            }
            alias<CfirResolvedTypeRef>("ResolvedTypeRefChecker").let {
                // CFIR: CfirErrorTypeRef is not a subtype of CfirResolvedTypeRef in current tree model.
            }
        }
        if (task == "checkers" || task == "all") {
            generateCheckersComponents(
                checkersPath,
                "$basePackage.checkers.expression",
                "CfirExpressionChecker",
                CfirStatement::class,
                CfirExpression::class,
            ) {
                alias<CfirStatement>("BasicExpressionChecker", false).let {
                    visitAlso<CfirExpression>(it)
                }
                alias<CfirLiteralExpression>("LiteralExpressionChecker")
                alias<CfirFunctionCall>("FunctionCallChecker")
                alias<CfirPropertyAccess>("PropertyAccessChecker")
                alias<CfirQualifiedAccess>("QualifiedAccessChecker")
                alias<CfirAssignment>("AssignmentChecker")
                alias<CfirBinaryOp>("BinaryOpChecker")
                alias<CfirComparisonExpression>("ComparisonExpressionChecker")
                alias<CfirTypeOperator>("TypeOperatorChecker")
                alias<CfirIfExpression>("IfExpressionChecker")
                alias<CfirMatchExpression>("MatchExpressionChecker")
                alias<CfirTryExpression>("TryExpressionChecker")
                alias<CfirThrowExpression>("ThrowExpressionChecker")
                alias<CfirReturnExpression>("ReturnExpressionChecker")
                alias<CfirJumpExpression>("JumpExpressionChecker")
                alias<CfirRangeExpression>("RangeExpressionChecker")
                alias<CfirSubscriptExpression>("SubscriptExpressionChecker")
                alias<CfirErrorExpression>("ErrorExpressionChecker")
            }

            generateCheckersComponents(
                checkersPath,
                "$basePackage.checkers.declaration",
                "CfirDeclarationChecker",
                CfirDeclaration::class,
                CfirDeclaration::class,
            ) {
                alias<CfirDeclaration>("BasicDeclarationChecker")
                alias<CfirMemberDeclaration>("MemberDeclarationChecker")
                alias<CfirCallableDeclaration>("CallableDeclarationChecker")
                alias<CfirClassLikeDeclaration>("ClassLikeChecker")
                alias<CfirClass>("ClassChecker")
                alias<CfirFile>("FileChecker")
                alias<CfirFunction>("FunctionChecker")
                alias<CfirMainFunction>("MainFunctionChecker")
                alias<CfirProperty>("PropertyChecker")
                alias<CfirVariable>("VariableChecker")
                alias<CfirTypeAlias>("TypeAliasChecker")
                alias<CfirTypeParameter>("TypeParameterChecker")
                alias<CfirValueParameter>("ValueParameterChecker")
                alias<CfirInvalidDeclaration>("InvalidDeclarationChecker")
            }
        }

        generateDiagnostics(
            checkersPath,
            packageName,
            DIAGNOSTICS_LIST,
            starImportsToAdd = setOf(
                ErrorListDiagnosticListRenderer.BASE_PACKAGE,
                ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE,
            ),
        )
        generateNonSuppressibleErrorNamesFile(checkersPath, packageName)
    }
}
