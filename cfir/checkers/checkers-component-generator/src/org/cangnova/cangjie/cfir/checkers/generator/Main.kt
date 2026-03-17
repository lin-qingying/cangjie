package org.cangnova.cangjie.cfir.checkers.generator

import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.DIAGNOSTICS_LIST
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.ErrorListDiagnosticListRenderer
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.generateDiagnostics
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInvalidDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirComparisonExpression
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirJumpExpression
import org.cangnova.cangjie.cfir.expressions.CfirLambdaExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirMacroExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirPropertyAccess
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccess
import org.cangnova.cangjie.cfir.expressions.CfirQuoteExpression
import org.cangnova.cangjie.cfir.expressions.CfirRangeExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirSpawnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirStringInterpolation
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.expressions.CfirSynchronizedExpression
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperator
import org.cangnova.cangjie.cfir.expressions.CfirUnsafeExpression
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTupleTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.CfirVArrayTypeRef
import java.io.File

fun main(args: Array<String>) {
    val basePackage = "org.cangnova.cangjie.cfir.analysis"
    val packageName = "$basePackage.diagnostics"
    val generationPath = args.getOrNull(1)?.let(::File)
    val task = when {
        args.isEmpty() -> "all"
        args[0] == "checkers" || args[0] == "diagnostics" || args[0] == "all" -> args[0]
        else -> "diagnostics"
    }

    if (task == "checkers" || task == "diagnostics" || task == "all") {
        val checkersPath = generationPath ?: File("cfir/checkers/gen")
        val diagnosticsPath = File("cfir/checkers/gen")
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
                    visitAlso<CfirBlock>(it)
                    visitAlso<CfirLazyBlock>(it)
                    visitAlso<CfirLazyExpression>(it)
                    visitAlso<CfirStringInterpolation>(it)
                    visitAlso<CfirMatchBranch>(it)
                    visitAlso<CfirCatch>(it)
                    visitAlso<CfirLoopExpression>(it)
                    visitAlso<CfirForInExpression>(it)
                    visitAlso<CfirLambdaExpression>(it)
                    visitAlso<CfirArrayLiteral>(it)
                    visitAlso<CfirTupleLiteral>(it)
                    visitAlso<CfirSpawnExpression>(it)
                    visitAlso<CfirSynchronizedExpression>(it)
                    visitAlso<CfirUnsafeExpression>(it)
                    visitAlso<CfirQuoteExpression>(it)
                    visitAlso<CfirMacroExpression>(it)
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
                alias<CfirCallableDeclaration>("CallableDeclarationChecker").let {
                    visitAlso<CfirEnumConstructor>(it)
                    visitAlso<CfirMacroDeclaration>(it)
                    visitAlso<CfirFinalizer>(it)
                    visitAlso<CfirConstructor>(it)
                    visitAlso<CfirPatternVariable>(it)
                }
                alias<CfirClassLikeDeclaration>("ClassLikeChecker").let {
                    visitAlso<CfirExtend>(it)
                }
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
            diagnosticsPath,
            packageName,
            DIAGNOSTICS_LIST,
            starImportsToAdd = setOf(
                ErrorListDiagnosticListRenderer.BASE_PACKAGE,
                ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE,
            ),
        )
        generateNonSuppressibleErrorNamesFile(diagnosticsPath, packageName)
    }
}

