package org.cangnova.cangjie.cfir.checkers.generator

import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.DIAGNOSTICS_LIST
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.ErrorListDiagnosticListRenderer
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.generateDiagnostics
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
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
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirComparisonExpression
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirHandleClause
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirBreakExpression
import org.cangnova.cangjie.cfir.expressions.CfirContinueExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLoopJump
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirPerformExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirQuoteExpression
import org.cangnova.cangjie.cfir.expressions.CfirRangeExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirResumeExpression
import org.cangnova.cangjie.cfir.expressions.CfirSpawnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirStringInterpolation
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.expressions.CfirSuperReceiverExpression
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
                // CFIR: CfirErrorTypeRef is a subtype of CfirResolvedTypeRef in current tree model,
                // so it should share the same checker entrypoint instead of falling back to visitElement.
                visitAlso<CfirErrorTypeRef>(it)
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
                    visitAlso<CfirPerformExpression>(it)
                    visitAlso<CfirResumeExpression>(it)
                    visitAlso<CfirHandleClause>(it)
                    visitAlso<CfirStringInterpolation>(it)
                    visitAlso<CfirMatchBranch>(it)
                    visitAlso<CfirCatch>(it)
                    visitAlso<CfirLoopExpression>(it)
                    visitAlso<CfirForInExpression>(it)
                    visitAlso<CfirAnonymousFunctionExpression>(it)
                    visitAlso<CfirArrayLiteral>(it)
                    visitAlso<CfirTupleLiteral>(it)
                    visitAlso<CfirSpawnExpression>(it)
                    visitAlso<CfirSynchronizedExpression>(it)
                    visitAlso<CfirUnsafeExpression>(it)
                    visitAlso<CfirQuoteExpression>(it)
                }
                alias<CfirLiteralExpression>("LiteralExpressionChecker")
                alias<CfirFunctionCall>("FunctionCallChecker")
                alias<CfirNamedAccessExpression>("NamedAccessChecker")
                alias<CfirQualifiedAccessExpression>("QualifiedAccessChecker")
                alias<CfirSuperReceiverExpression>("SuperReceiverExpressionChecker")
                alias<CfirAssignment>("AssignmentChecker")
                alias<CfirBinaryOp>("BinaryOpChecker")
                alias<CfirComparisonExpression>("ComparisonExpressionChecker")
                alias<CfirTypeOperator>("TypeOperatorChecker")
                alias<CfirIfExpression>("IfExpressionChecker")
                alias<CfirMatchExpression>("MatchExpressionChecker")
                alias<CfirTryExpression>("TryExpressionChecker")
                alias<CfirThrowExpression>("ThrowExpressionChecker")
                alias<CfirReturnExpression>("ReturnExpressionChecker")
                alias<CfirLoopJump>("LoopJumpChecker", false).let {
                    visitAlso<CfirBreakExpression>(it)
                    visitAlso<CfirContinueExpression>(it)
                }
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
                alias<CfirMemberDeclaration>("MemberDeclarationChecker", false)
                alias<CfirCallableDeclaration>("CallableDeclarationChecker", false).let {
                    visitAlso<CfirPatternBindingVariable>(it)
                }
                alias<CfirFunction>("FunctionChecker", false).let {
                    visitAlso<CfirMacroDeclaration>(it)
                    visitAlso<CfirFinalizer>(it)
                }
                alias<CfirEnumConstructor>("EnumConstructorChecker")
                alias<CfirNamedFunction>("SimpleFunctionChecker")
                alias<CfirProperty>("PropertyChecker")
                alias<CfirClassLikeDeclaration>("ClassLikeChecker", false).let {
                    visitAlso<CfirClass>(it)
                    visitAlso<CfirInterface>(it)

                    visitAlso<CfirStruct>(it)

                    visitAlso<CfirEnum>(it)


                }
                alias<CfirAnonymousFunction>("AnonymousFunctionChecker")
                alias<CfirPropertyAccessor>("PropertyAccessorChecker")

                alias<CfirConstructor>("ConstructorChecker")
                alias<CfirFile>("FileChecker")
                alias<CfirTypeParameter>("CfirTypeParameterChecker")
                alias<CfirExtend>("ExtendChecker")
                alias<CfirMainFunction>("MainFunctionChecker")
                alias<CfirPatternVariable>("PatternVariableChecker")
                alias<CfirFieldVariable>("FieldVariableChecker")
                alias<CfirTypeAlias>("TypeAliasChecker")
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
