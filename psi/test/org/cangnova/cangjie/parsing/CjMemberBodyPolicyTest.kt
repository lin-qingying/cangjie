package org.cangnova.cangjie.parsing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `CjMemberBodyPolicy` 的纯表驱动测试：不依赖 parser / PSI / builder / 配置。
 *
 * 期望值来自官方 `ParserImpl::CheckFuncBody`（`ParseDecl.cpp:1533-1562`）与
 * `ParserImpl::ParsePropDecl`（`Parser.cpp:340-394`），并由 cjc 1.0.5 探针交叉验证。
 */
class CjMemberBodyPolicyTest {

    private fun function(
        scopeKind: CjScopeKind,
        hasBody: Boolean = false,
        hasReturnType: Boolean = true,
        isForeign: Boolean = false,
        isIntrinsic: Boolean = false,
        isDeclarationFile: Boolean = false,
        isSignatureBroken: Boolean = false,
        cjmp: CjmpOverlayFacts? = null,
    ) = CjMemberBodyFacts(
        scopeKind = scopeKind,
        hasBody = hasBody,
        hasAccessors = false,
        hasReturnType = hasReturnType,
        isForeign = isForeign,
        isIntrinsic = isIntrinsic,
        isDeclarationFile = isDeclarationFile,
        isSignatureBroken = isSignatureBroken,
        cjmp = cjmp,
    )

    private fun property(
        scopeKind: CjScopeKind,
        hasBody: Boolean = false,
        hasAccessors: Boolean = false,
        isForeign: Boolean = false,
        isIntrinsic: Boolean = false,
        isDeclarationFile: Boolean = false,
        isSignatureBroken: Boolean = false,
        cjmp: CjmpOverlayFacts? = null,
    ) = CjMemberBodyFacts(
        scopeKind = scopeKind,
        hasBody = hasBody,
        hasAccessors = hasAccessors,
        hasReturnType = false,
        isForeign = isForeign,
        isIntrinsic = isIntrinsic,
        isDeclarationFile = isDeclarationFile,
        isSignatureBroken = isSignatureBroken,
        cjmp = cjmp,
    )

    // ==================== 函数过程：CheckFuncBody ====================

    @Test
    fun classBodyWithoutBodyIsImplicitAbstract() {
        val decision = CjMemberBodyPolicy.checkFunctionBody(function(CjScopeKind.CLASS_BODY))
        assertTrue(decision.markImplicitAbstract)
        assertNull(decision.diagnostic)
    }

    @Test
    fun interfaceBodyWithoutBodyIsImplicitAbstract() {
        val decision = CjMemberBodyPolicy.checkFunctionBody(function(CjScopeKind.INTERFACE_BODY))
        assertTrue(decision.markImplicitAbstract)
        assertNull(decision.diagnostic)
    }

    @Test
    fun classBodyWithoutReturnTypeReportsAbstractReturnType() {
        val decision = CjMemberBodyPolicy.checkFunctionBody(
            function(CjScopeKind.CLASS_BODY, hasReturnType = false)
        )
        assertTrue(decision.markImplicitAbstract)
        assertEquals(CjMemberBodyDiagnostic.ABSTRACT_FUNCTION_MUST_HAVE_RETURN_TYPE, decision.diagnostic)
    }

    @Test
    fun classBodyWithReturnTypeHasNoDiagnostic() {
        val decision = CjMemberBodyPolicy.checkFunctionBody(
            function(CjScopeKind.CLASS_BODY, hasReturnType = true)
        )
        assertTrue(decision.markImplicitAbstract)
        assertNull(decision.diagnostic)
    }

    @Test
    fun structBodyWithoutBodyReportsMissingFunctionBody() {
        val decision = CjMemberBodyPolicy.checkFunctionBody(function(CjScopeKind.STRUCT_BODY))
        assertFalse(decision.markImplicitAbstract)
        assertEquals(CjMemberBodyDiagnostic.MISSING_FUNCTION_BODY, decision.diagnostic)
    }

    @Test
    fun enumExtendAndTopLevelReportMissingFunctionBody() {
        for (scopeKind in listOf(
            CjScopeKind.ENUM_BODY,
            CjScopeKind.EXTEND_BODY,
            CjScopeKind.TOPLEVEL,
        )) {
            val decision = CjMemberBodyPolicy.checkFunctionBody(function(scopeKind))
            assertFalse(decision.markImplicitAbstract, scopeKind.name)
            assertEquals(CjMemberBodyDiagnostic.MISSING_FUNCTION_BODY, decision.diagnostic, scopeKind.name)
        }
    }

    @Test
    fun unknownScopeFunctionIsSilent() {
        val decision = CjMemberBodyPolicy.checkFunctionBody(function(CjScopeKind.UNKNOWN_SCOPE))
        assertFalse(decision.markImplicitAbstract)
        assertNull(decision.diagnostic)
    }

    @Test
    fun foreignFunctionIsSilent() {
        val decision = CjMemberBodyPolicy.checkFunctionBody(
            function(CjScopeKind.TOPLEVEL, isForeign = true)
        )
        assertFalse(decision.markImplicitAbstract)
        assertNull(decision.diagnostic)
    }

    @Test
    fun intrinsicFunctionIsSilent() {
        val decision = CjMemberBodyPolicy.checkFunctionBody(
            function(CjScopeKind.CLASS_BODY, hasReturnType = false, isIntrinsic = true)
        )
        assertFalse(decision.markImplicitAbstract)
        assertNull(decision.diagnostic)
    }

    @Test
    fun functionWithBodyIsSilent() {
        val decision = CjMemberBodyPolicy.checkFunctionBody(
            function(CjScopeKind.CLASS_BODY, hasBody = true)
        )
        assertFalse(decision.markImplicitAbstract)
        assertNull(decision.diagnostic)
    }

    @Test
    fun declarationFileFunctionSuppressesMarkingAndDiagnostics() {
        val classDecision = CjMemberBodyPolicy.checkFunctionBody(
            function(CjScopeKind.CLASS_BODY, hasReturnType = false, isDeclarationFile = true)
        )
        assertFalse(classDecision.markImplicitAbstract)
        assertNull(classDecision.diagnostic)

        val structDecision = CjMemberBodyPolicy.checkFunctionBody(
            function(CjScopeKind.STRUCT_BODY, isDeclarationFile = true)
        )
        assertFalse(structDecision.markImplicitAbstract)
        assertNull(structDecision.diagnostic)
    }

    @Test
    fun brokenFunctionSignatureKeepsMarkingButSuppressesDiagnostics() {
        val classDecision = CjMemberBodyPolicy.checkFunctionBody(
            function(CjScopeKind.CLASS_BODY, hasReturnType = false, isSignatureBroken = true)
        )
        assertTrue(classDecision.markImplicitAbstract)
        assertNull(classDecision.diagnostic)

        val topLevelDecision = CjMemberBodyPolicy.checkFunctionBody(
            function(CjScopeKind.TOPLEVEL, hasReturnType = false, isSignatureBroken = true)
        )
        assertFalse(topLevelDecision.markImplicitAbstract)
        assertNull(topLevelDecision.diagnostic)
    }

    // ==================== 属性过程：ParsePropDecl ====================

    @Test
    fun classAndInterfaceBodylessPropertyIsImplicitAbstract() {
        for (scopeKind in listOf(CjScopeKind.CLASS_BODY, CjScopeKind.INTERFACE_BODY)) {
            val decision = CjMemberBodyPolicy.checkPropertyBody(property(scopeKind))
            assertTrue(decision.markImplicitAbstract, scopeKind.name)
            assertNull(decision.diagnostic, scopeKind.name)
        }
    }

    @Test
    fun unknownScopeBodylessPropertyIsMarkedNotSilent() {
        val decision = CjMemberBodyPolicy.checkPropertyBody(property(CjScopeKind.UNKNOWN_SCOPE))
        assertTrue(decision.markImplicitAbstract)
        assertNull(decision.diagnostic)
    }

    @Test
    fun structEnumExtendAndTopLevelBodylessPropertyReportMissingPropertyBody() {
        for (scopeKind in listOf(
            CjScopeKind.STRUCT_BODY,
            CjScopeKind.ENUM_BODY,
            CjScopeKind.EXTEND_BODY,
            CjScopeKind.TOPLEVEL,
        )) {
            val decision = CjMemberBodyPolicy.checkPropertyBody(property(scopeKind))
            assertFalse(decision.markImplicitAbstract, scopeKind.name)
            assertEquals(CjMemberBodyDiagnostic.MISSING_PROPERTY_BODY, decision.diagnostic, scopeKind.name)
        }
    }

    @Test
    fun declarationFileOnlySuppressesPropertyDiagnostic() {
        val classDecision = CjMemberBodyPolicy.checkPropertyBody(
            property(CjScopeKind.CLASS_BODY, isDeclarationFile = true)
        )
        assertTrue(classDecision.markImplicitAbstract)
        assertNull(classDecision.diagnostic)

        val structDecision = CjMemberBodyPolicy.checkPropertyBody(
            property(CjScopeKind.STRUCT_BODY, isDeclarationFile = true)
        )
        assertFalse(structDecision.markImplicitAbstract)
        assertNull(structDecision.diagnostic)
    }

    @Test
    fun propertyWithBodyOrAccessorsIsSilent() {
        for (facts in listOf(
            property(CjScopeKind.CLASS_BODY, hasBody = true),
            property(CjScopeKind.CLASS_BODY, hasAccessors = true),
            property(CjScopeKind.STRUCT_BODY, hasBody = true),
        )) {
            val decision = CjMemberBodyPolicy.checkPropertyBody(facts)
            assertFalse(decision.markImplicitAbstract)
            assertNull(decision.diagnostic)
        }
    }

    @Test
    fun propertyProcessHasNoForeignOrIntrinsicFactor() {
        // 官方 ParsePropDecl 的 abstract 分支不测试 FOREIGN / INTRINSIC；探针显示
        // foreign prop 在 class/interface 体内本身是非法修饰符，标记逻辑仍走 abstract 分支。
        val foreign = CjMemberBodyPolicy.checkPropertyBody(
            property(CjScopeKind.CLASS_BODY, isForeign = true)
        )
        assertTrue(foreign.markImplicitAbstract)
        assertNull(foreign.diagnostic)

        val intrinsic = CjMemberBodyPolicy.checkPropertyBody(
            property(CjScopeKind.CLASS_BODY, isIntrinsic = true)
        )
        assertTrue(intrinsic.markImplicitAbstract)
        assertNull(intrinsic.diagnostic)
    }

    @Test
    fun brokenPropertySignatureSuppressesMissingBody() {
        val decision = CjMemberBodyPolicy.checkPropertyBody(
            property(CjScopeKind.STRUCT_BODY, isSignatureBroken = true)
        )
        assertFalse(decision.markImplicitAbstract)
        assertNull(decision.diagnostic)
    }

    // ==================== CJMP overlay ====================

    private val cjmpAbstractClass = CjmpOverlayFacts(
        memberIsCommon = false,
        memberIsExplicitAbstract = false,
        enclosingClassIsAbstract = true,
        enclosingClassIsCommonOrSpecific = true,
    )

    @Test
    fun cjmpAbstractClassClearsImplicitAbstractForFunctionAndProperty() {
        val functionFacts = function(CjScopeKind.CLASS_BODY, cjmp = cjmpAbstractClass)
        val functionDecision = CjMemberBodyPolicy.applyCjmpOverlay(
            CjMemberBodyPolicy.checkFunctionBody(functionFacts),
            functionFacts,
        )
        assertFalse(functionDecision.markImplicitAbstract)

        val propertyFacts = property(CjScopeKind.CLASS_BODY, cjmp = cjmpAbstractClass)
        val propertyDecision = CjMemberBodyPolicy.applyCjmpOverlay(
            CjMemberBodyPolicy.checkPropertyBody(propertyFacts),
            propertyFacts,
        )
        assertFalse(propertyDecision.markImplicitAbstract)
    }

    @Test
    fun explicitAbstractMemberSurvivesCjmpOverlay() {
        val facts = function(
            CjScopeKind.CLASS_BODY,
            cjmp = cjmpAbstractClass.copy(memberIsExplicitAbstract = true),
        )
        val decision = CjMemberBodyPolicy.applyCjmpOverlay(
            CjMemberBodyPolicy.checkFunctionBody(facts),
            facts,
        )
        assertTrue(decision.markImplicitAbstract)
    }

    @Test
    fun commonMemberNeverImplicitAbstractInClassBody() {
        val facts = function(
            CjScopeKind.CLASS_BODY,
            cjmp = cjmpAbstractClass.copy(memberIsCommon = true),
        )
        val decision = CjMemberBodyPolicy.applyCjmpOverlay(
            CjMemberBodyPolicy.checkFunctionBody(facts),
            facts,
        )
        assertFalse(decision.markImplicitAbstract)
    }

    @Test
    fun cjmpOverlayWithoutClassFactsIsIdentity() {
        val facts = property(CjScopeKind.INTERFACE_BODY, cjmp = null)
        val decision = CjMemberBodyPolicy.applyCjmpOverlay(
            CjMemberBodyPolicy.checkPropertyBody(facts),
            facts,
        )
        assertTrue(decision.markImplicitAbstract)
    }
}
