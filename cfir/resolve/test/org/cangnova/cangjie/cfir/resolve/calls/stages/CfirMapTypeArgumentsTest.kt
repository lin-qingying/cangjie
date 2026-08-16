@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.TestSession
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCandidate
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildFunctionSymbol
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildTypedExpression
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.completeCallForTest
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.newResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.newTestSession
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.registerHierarchyProviders
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.runStagesForTest
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [CfirMapTypeArguments] 及后续类型参数求解阶段测试。
 *
 * 每个测试构造独立的 session；需要 `Child <: Parent` 继承关系的用例
 * 通过 [registerHierarchyProviders] 注册内存符号提供器。
 */
class CfirMapTypeArgumentsTest {

    private val boxId = ClassId(FqName.ROOT, Name.identifier("Box"))
    private val parentId = ClassId(FqName.ROOT, Name.identifier("Parent"))
    private val childId = ClassId(FqName.ROOT, Name.identifier("Child"))

    private fun boxOf(type: ConeCangJieType): ConeClassLikeType = ConeClassLikeType(
        lookupTag = ConeClassLikeLookupTagImpl(boxId),
        typeArguments = listOf(type),
    )

    private fun classLikeOf(classId: ClassId): ConeClassLikeType = ConeClassLikeType(
        lookupTag = ConeClassLikeLookupTagImpl(classId),
    )

    @Test
    fun `infer from invariant class type argument`() {
        val session = newTestSession()
        val context = newResolutionContext(session)

        val t = ExtendTestFixtures.newTypeParameter(session.moduleData, "T")
        val tType = ExtendTestFixtures.typeParameterType(t)
        val symbol = buildFunctionSymbol(
            session, "box",
            returnType = boxOf(tType),
            parameterTypes = listOf(boxOf(tType)),
            typeParameters = listOf(t),
        )
        val boxOfInt = boxOf(ConePrimitiveType.INT32)
        val callInfo = buildCallInfo(session, "box", arguments = listOf(buildTypedExpression(boxOfInt)))
        val candidate = buildCandidate(session, symbol, callInfo)
        runStagesForTest(
            candidate, context,
            CfirMapTypeArguments, CfirMapArguments, CfirCreateFreshTypeVariableSubstitutorStage, CfirCheckArguments,
        )

        assertEquals(CandidateApplicability.RESOLVED, candidate.applicability)
        assertEquals(boxOfInt, completeCallForTest(candidate, context))
    }

    @Test
    fun `declaration upper bound should participate in solving`() {
        val session = newTestSession()
        val parent = ExtendTestFixtures.newClass(session.moduleData, "Parent", parentId)
        val child = ExtendTestFixtures.newClass(
            session.moduleData, "Child", childId,
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(parentId)),
        )
        session.registerHierarchyProviders(listOf(parent, child))
        val context = newResolutionContext(session)

        val t = ExtendTestFixtures.newTypeParameter(
            session.moduleData, "T",
            bounds = listOf(ExtendTestFixtures.classTypeRef(parentId)),
        )
        val tType = ExtendTestFixtures.typeParameterType(t)
        val symbol = buildFunctionSymbol(
            session, "pick",
            returnType = tType,
            parameterTypes = listOf(tType),
            typeParameters = listOf(t),
        )

val childType = classLikeOf(childId)
        val callInfo = buildCallInfo(session, "pick", arguments = listOf(buildTypedExpression(childType)))
        val candidate = buildCandidate(session, symbol, callInfo)
        runStagesForTest(
            candidate, context,
            CfirMapTypeArguments, CfirMapArguments, CfirCreateFreshTypeVariableSubstitutorStage, CfirCheckArguments,
        )

        assertEquals(CandidateApplicability.RESOLVED, candidate.applicability)
        assertEquals(childType, completeCallForTest(candidate, context))
    }

    @Test
    fun `conflicting declaration bound`() {
        val session = newTestSession()
        val context = newResolutionContext(session)

        val t = ExtendTestFixtures.newTypeParameter(
            session.moduleData, "T",
            bounds = listOf(ExtendTestFixtures.classTypeRef(parentId)),
        )
        val tType = ExtendTestFixtures.typeParameterType(t)
        val symbol = buildFunctionSymbol(
            session, "bad",
            returnType = tType,
            parameterTypes = listOf(tType),
            typeParameters = listOf(t),
        )

        val callInfo = buildCallInfo(session, "bad", arguments = listOf(buildTypedExpression(ConePrimitiveType.BOOLEAN)))
        val candidate = buildCandidate(session, symbol, callInfo)
        runStagesForTest(
            candidate, context,
            CfirMapTypeArguments, CfirMapArguments, CfirCreateFreshTypeVariableSubstitutorStage, CfirCheckArguments,
        )

        assertEquals(CandidateApplicability.INAPPLICABLE, candidate.applicability)
        assertTrue(candidate.diagnostics.single() is ArgumentTypeMismatch)
    }

    @Test
    fun `identity should infer same type`() {
        val session = newTestSession()
        val context = newResolutionContext(session)

        val t = ExtendTestFixtures.newTypeParameter(session.moduleData, "T")
        val tType = ExtendTestFixtures.typeParameterType(t)
        val symbol = buildFunctionSymbol(
            session, "id",
            returnType = tType,
            parameterTypes = listOf(tType),
            typeParameters = listOf(t),
        )

        val callInfo = buildCallInfo(session, "id", arguments = listOf(buildTypedExpression(ConePrimitiveType.INT64)))
        val candidate = buildCandidate(session, symbol, callInfo)
        runStagesForTest(
            candidate, context,
            CfirMapTypeArguments, CfirMapArguments, CfirCreateFreshTypeVariableSubstitutorStage, CfirCheckArguments,
        )

        assertEquals(CandidateApplicability.RESOLVED, candidate.applicability)
        assertEquals(ConePrimitiveType.INT64, completeCallForTest(candidate, context))
    }
}
