package org.cangnova.cangjie.cfir.resolve.transformers.body.resolve

import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.diagnostic.ConePostponedInferenceDiagnostic
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedReferenceError
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 以真实 CFIR 节点检查 pending/source-error 优先级和独立函数正文边界。 */
class CfirSourceErrorPriorityTest {
    private val session = CallResolutionTestFixtures.newTestSession()
    private val diagnostic = ConeUnresolvedReferenceError(Name.identifier("missingValue"))
    private val error = ConeErrorType(diagnostic)
    private val pending = ConeErrorType(ConePostponedInferenceDiagnostic)
    private val wrappedPending = ConeErrorType(
        ConeUnreportedDuplicateDiagnostic(ConeUnreportedDuplicateDiagnostic(ConePostponedInferenceDiagnostic)),
    )

    private fun block(type: ConeCangJieType, vararg children: CfirExpression) = buildBlock {
        coneTypeOrNull = type
        statements += children
    }

    @Test
    fun ownPendingDoesNotHideActualChildError() {
        assertSame(error, block(pending, block(error)).firstErrorTypeOrNull(session))
    }

    @Test
    fun earlierWrappedPendingDoesNotHideLaterActualError() {
        assertSame(error, block(ConePrimitiveType.UNIT, block(wrappedPending), block(error)).firstErrorTypeOrNull(session))
        assertSame(error, block(ConePrimitiveType.UNIT, block(error), block(pending)).firstErrorTypeOrNull(session))
    }

    @Test
    fun diagnosticHolderErrorAndChildrenRemainVisibleBehindPendingType() {
        val realHolder = buildErrorExpression {
            this.diagnostic = this@CfirSourceErrorPriorityTest.diagnostic
            expression = block(pending)
        }
        val result = checkNotNull(realHolder.firstErrorTypeOrNull(session))
        assertSame(diagnostic, (result.diagnostic as ConeUnreportedDuplicateDiagnostic).original)
        val pendingHolder = buildErrorExpression {
            diagnostic = ConePostponedInferenceDiagnostic
            nonExpressionElement = block(error)
        }
        assertSame(error, pendingHolder.firstErrorTypeOrNull(session))
    }

    @Test
    fun pendingOnlyRemainsUnfinishedAndCleanBlockHasNoError() {
        assertSame(pending, block(pending, block(wrappedPending)).firstErrorTypeOrNull(session))
        assertSame(wrappedPending, block(ConePrimitiveType.UNIT, block(wrappedPending)).firstErrorTypeOrNull(session))
        assertNull(block(ConePrimitiveType.UNIT).firstErrorTypeOrNull(session))
    }

    @Test
    fun namedFunctionBodyIsIndependentButItsHeaderIsVisible() {
        val session = CallResolutionTestFixtures.newTestSession()
        val function = CallResolutionTestFixtures.buildFunctionSymbol(session, "independent").cfir
        function.replaceBody(block(error))
        val root = buildBlock {
            coneTypeOrNull = pending
            statements += function
        }
        assertSame(pending, root.firstErrorTypeOrNull(session))
        function.replaceReturnTypeRef(buildErrorTypeRef { diagnostic = this@CfirSourceErrorPriorityTest.diagnostic })
        val result = checkNotNull(root.firstErrorTypeOrNull(session))
        assertSame(diagnostic, (result.diagnostic as ConeUnreportedDuplicateDiagnostic).original)
    }

    @Test
    fun lambdaOwnBodyErrorRemainsVisibleInOuterPendingBlock() {
        val session = CallResolutionTestFixtures.newTestSession()
        val fixtureSource = checkNotNull(CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32).source)
        val lambda = buildAnonymousFunctionExpression {
            source = fixtureSource
            isTrailingLambda = false
            anonymousFunction = buildAnonymousFunction {
                source = fixtureSource
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.RAW_CFIR
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                status = CfirDeclarationStatusImpl()
                symbol = CfirAnonymousFunctionSymbol()
                returnTypeRef = ConePrimitiveType.UNIT.toCfirResolvedTypeRef()
                typeRef = ConeFunctionType(emptyList(), ConePrimitiveType.UNIT).toCfirResolvedTypeRef()
                body = block(error)
                hasExplicitParameterList = true
                isLambda = true
            }
        }
        assertSame(error, block(pending, lambda).firstErrorTypeOrNull(session))
    }

    @Test
    fun sourceErrorSequencePreservesActualErrorAndPendingIdentity() {
        assertSame(error, sequenceOf(null, pending, wrappedPending, error).firstSourceErrorOrPending())
        assertSame(error, sequenceOf(error, wrappedPending).firstSourceErrorOrPending())
        assertSame(pending, sequenceOf(null, pending, wrappedPending).firstSourceErrorOrPending())
        assertTrue(wrappedPending.isPostponedInference)
        val sameText = ConeErrorType(ConeSimpleDiagnostic("Postponed inference"))
        assertFalse(sameText.isPostponedInference)
        assertSame(sameText, sequenceOf(wrappedPending, sameText).firstSourceErrorOrPending())
    }
}
