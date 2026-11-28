package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.resolve.calls.mpp.AbstractCjmpMatcher
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchResult
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMismatchKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpTypeCompatibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** 验证 CJMP 返回类型比较使用真实生产路径产生的已解析类型引用与 Quest 状态。 */
class CfirCjmpReturnTypeStateTest {
    private val session = CallResolutionTestFixtures.newTestSession()
    private val context = CfirCjmpMatchingContext(session)

    @Test
    fun resolvedReturnTypeMustCompareItsConcreteType() {
        val integer = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
        val boolean = ConePrimitiveType.BOOLEAN.toCfirResolvedTypeRef(null)
        assertEquals(CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE), match(boolean, integer))
        assertEquals(CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE), match(integer, boolean))
        assertEquals(CjmpMatchResult.Matched, match(integer, integer))
    }

    @Test
    fun emptyBodyUnitIsConcreteEvenWithoutSourceAnnotation() {
        assertEquals(
            CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE),
            match(ConePrimitiveType.UNIT.toCfirResolvedTypeRef(null), ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)),
        )
    }

    @Test
    fun actualQuestAcceptsConcreteReturnTypeInEitherDirection() {
        val integer = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
        assertEquals(CjmpMatchResult.Matched, match(buildImplicitTypeRef {}, integer))
        assertEquals(CjmpMatchResult.Matched, match(integer, buildImplicitTypeRef {}))
    }

    @Test
    fun resolvedErrorTypeTakesPrecedenceOverQuest() {
        val error = ConeErrorType(ConeSimpleDiagnostic("unresolved return type")).toCfirResolvedTypeRef(null)
        assertEquals(CjmpMatchResult.TypeNotResolved, match(error, buildImplicitTypeRef {}))
        assertEquals(CjmpMatchResult.TypeNotResolved, match(buildImplicitTypeRef {}, error))
    }

    @Test
    fun postMatchReturnTypeCheckStartsAtAlpha68WithoutChangingSignatureComparison() {
        val alpha66 = CallResolutionTestFixtures.newTestSession(
            languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_66),
        )
        val alpha68 = CallResolutionTestFixtures.newTestSession(
            languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_68),
        )
        val alpha66Specific = CallResolutionTestFixtures.buildFunctionSymbol(alpha66, "specific")
        val alpha66Common = CallResolutionTestFixtures.buildFunctionSymbol(alpha66, "common")
        alpha66Specific.cfir.replaceReturnTypeRef(ConePrimitiveType.BOOLEAN.toCfirResolvedTypeRef(null))
        alpha66Common.cfir.replaceReturnTypeRef(ConePrimitiveType.INT64.toCfirResolvedTypeRef(null))

        assertEquals(
            CjmpTypeCompatibility.COMPATIBLE,
            CfirCjmpMatchingContext(alpha66).postMatchReturnTypeCompatibility(
                alpha66Specific,
                alpha66Common,
                emptyMap(),
            ),
        )
        assertEquals(
            CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE),
            AbstractCjmpMatcher.match(alpha66Specific, alpha66Common, CfirCjmpMatchingContext(alpha66)),
        )

        val alpha68Specific = CallResolutionTestFixtures.buildFunctionSymbol(alpha68, "specific")
        val alpha68Common = CallResolutionTestFixtures.buildFunctionSymbol(alpha68, "common")
        alpha68Specific.cfir.replaceReturnTypeRef(ConePrimitiveType.BOOLEAN.toCfirResolvedTypeRef(null))
        alpha68Common.cfir.replaceReturnTypeRef(ConePrimitiveType.INT64.toCfirResolvedTypeRef(null))
        assertEquals(
            CjmpTypeCompatibility.INCOMPATIBLE,
            CfirCjmpMatchingContext(alpha68).postMatchReturnTypeCompatibility(
                alpha68Specific,
                alpha68Common,
                emptyMap(),
            ),
        )
    }

    private fun match(specificType: CfirTypeRef, commonType: CfirTypeRef): CjmpMatchResult {
        val specific = CallResolutionTestFixtures.buildFunctionSymbol(session, "specific")
        val common = CallResolutionTestFixtures.buildFunctionSymbol(session, "common")
        specific.cfir.replaceReturnTypeRef(specificType)
        common.cfir.replaceReturnTypeRef(commonType)
        return AbstractCjmpMatcher.match(specific, common, context)
    }
}
