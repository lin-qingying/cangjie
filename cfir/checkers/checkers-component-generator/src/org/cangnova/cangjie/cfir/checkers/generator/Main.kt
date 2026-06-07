package org.cangnova.cangjie.cfir.checkers.generator

import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.DIAGNOSTICS_LIST
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.ErrorListDiagnosticListRenderer
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.generateDiagnostics
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.types.*
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
                visitAlso<CfirUnresolvedTypeRef>(it)
                visitAlso<CfirUserTypeRef>(it)
                visitAlso<CfirBasicTypeRef>(it)
                visitAlso<CfirFunctionTypeRef>(it)
                visitAlso<CfirOptionTypeRef>(it)
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
                    visitAlso<CfirWrappedExpression>(it)
                    visitAlso<CfirOptionalExpression>(it)
                    visitAlso<CfirOptionalChainExpression>(it)
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
                    visitAlso<CfirInoutArgumentExpression>(it)
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
                alias<CfirDeclaration>("BasicDeclarationChecker").let {
                    visitAlso<CfirCodeFragment>(it)
                }
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
