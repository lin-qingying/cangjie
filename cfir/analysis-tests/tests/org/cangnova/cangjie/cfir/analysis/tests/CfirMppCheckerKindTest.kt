package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.analysis.CheckersComponent
import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirBasicDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.ComposedDeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.DeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirBasicExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.ComposedExpressionCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.expression.ExpressionCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.type.CfirTypeRefChecker
import org.cangnova.cangjie.cfir.analysis.checkers.type.ComposedTypeCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.type.TypeCheckers
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** 验证 CLI 的两类 checker 隔离与 LL 聚合全部 checker 共用同一注册契约。 */
@OptIn(CheckersComponentInternal::class, SessionConfiguration::class)
class CfirMppCheckerKindTest {
    private val commonDeclaration = DeclarationChecker(MppCheckerKind.Common)
    private val platformDeclaration = DeclarationChecker(MppCheckerKind.Platform)
    private val commonExpression = ExpressionChecker(MppCheckerKind.Common)
    private val platformExpression = ExpressionChecker(MppCheckerKind.Platform)
    private val commonType = TypeChecker(MppCheckerKind.Common)
    private val platformType = TypeChecker(MppCheckerKind.Platform)

    private val declarations = object : DeclarationCheckers() {
        override val basicDeclarationCheckers: Set<CfirBasicDeclarationChecker> = setOf(commonDeclaration, platformDeclaration)
    }
    private val expressions = object : ExpressionCheckers() {
        override val basicExpressionCheckers: Set<CfirBasicExpressionChecker> = setOf(commonExpression, platformExpression)
    }
    private val types = object : TypeCheckers() {
        override val typeRefCheckers: Set<CfirTypeRefChecker> = setOf(commonType, platformType)
    }

    @Test
    fun declarationRegistriesSeparateKindsAndKeepBothKindsForLl() {
        val common = ComposedDeclarationCheckers(MppCheckerKind.Common)
        val platform = ComposedDeclarationCheckers(MppCheckerKind.Platform)
        val allKinds = ComposedDeclarationCheckers { true }
        repeat(2) {
            common.register(declarations)
            platform.register(declarations)
            allKinds.register(declarations)
        }
        assertEquals(setOf(commonDeclaration), common.basicDeclarationCheckers)
        assertEquals(setOf(platformDeclaration), platform.basicDeclarationCheckers)
        assertEquals(setOf(commonDeclaration, platformDeclaration), allKinds.basicDeclarationCheckers)
    }

    @Test
    fun expressionRegistriesSeparateKindsAndKeepBothKindsForLl() {
        val common = ComposedExpressionCheckers(MppCheckerKind.Common)
        val platform = ComposedExpressionCheckers(MppCheckerKind.Platform)
        val allKinds = ComposedExpressionCheckers { true }
        repeat(2) {
            common.register(expressions)
            platform.register(expressions)
            allKinds.register(expressions)
        }
        assertEquals(setOf(commonExpression), common.basicExpressionCheckers)
        assertEquals(setOf(platformExpression), platform.basicExpressionCheckers)
        assertEquals(setOf(commonExpression, platformExpression), allKinds.basicExpressionCheckers)
    }

    @Test
    fun typeRegistriesSeparateKindsAndKeepBothKindsForLl() {
        val common = ComposedTypeCheckers(MppCheckerKind.Common)
        val platform = ComposedTypeCheckers(MppCheckerKind.Platform)
        val allKinds = ComposedTypeCheckers { true }
        repeat(2) {
            common.register(types)
            platform.register(types)
            allKinds.register(types)
        }
        assertEquals(setOf(commonType), common.typeRefCheckers)
        assertEquals(setOf(platformType), platform.typeRefCheckers)
        assertEquals(setOf(commonType, platformType), allKinds.typeRefCheckers)
    }

    @Test
    fun componentRegistersEachCategoryInBothKindRegistries() {
        val component = CheckersComponent()
        repeat(2) {
            component.register(declarations)
            component.register(expressions)
            component.register(types)
        }
        assertEquals(setOf(commonDeclaration), component.commonDeclarationCheckers.basicDeclarationCheckers)
        assertEquals(setOf(platformDeclaration), component.platformDeclarationCheckers.basicDeclarationCheckers)
        assertEquals(setOf(commonExpression), component.commonExpressionCheckers.basicExpressionCheckers)
        assertEquals(setOf(platformExpression), component.platformExpressionCheckers.basicExpressionCheckers)
        assertEquals(setOf(commonType), component.commonTypeCheckers.typeRefCheckers)
        assertEquals(setOf(platformType), component.platformTypeCheckers.typeRefCheckers)
    }

    private class DeclarationChecker(kind: MppCheckerKind) : CfirBasicDeclarationChecker(kind) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: CfirDeclaration) = Unit
    }

    private class ExpressionChecker(kind: MppCheckerKind) : CfirBasicExpressionChecker(kind) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(expression: CfirStatement) = Unit
    }

    private class TypeChecker(kind: MppCheckerKind) : CfirTypeRefChecker(kind) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(typeRef: CfirTypeRef) = Unit
    }
}
