package org.cangnova.cangjie.parsing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** v1.1.3 CheckVariableParams 的投影结果，覆盖 source/raw 共享的参数保留边界。 */
class CjVariadicParameterRulesTest {
    @Test
    fun anOrdinaryFunctionRetainsTheEllipsisAndStopsAtTheFirstError() {
        val result = CjVariadicParameterRules.select(
            kind = CjmpModifierDeclarationKind.FUNCTION,
            parameterListCount = 1,
            effectiveForeign = false,
            ellipsisParameters = listOf(false, true, true),
        )
        assertTrue(result.hasVariableLengthArgument)
        assertEquals(1, result.variableArgumentIndex)
        assertEquals(null, result.erasedParameterIndex)
        assertEquals(listOf(CjVariadicParameterRules.Error(CjVariadicParameterRules.ErrorKind.NON_FOREIGN, 1)), result.errors)
        assertTrue((0..2).all(result::retains))
    }

    @Test
    fun aValidForeignTailIsTheOnlyRemovedParameter() {
        val result = CjVariadicParameterRules.select(
            kind = CjmpModifierDeclarationKind.FUNCTION,
            parameterListCount = 1,
            effectiveForeign = true,
            ellipsisParameters = listOf(false, false, true),
        )
        assertTrue(result.hasVariableLengthArgument)
        assertEquals(2, result.variableArgumentIndex)
        assertEquals(2, result.erasedParameterIndex)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.retains(0) && result.retains(1))
        assertFalse(result.retains(2))
    }

    @Test
    fun foreignFirstAndMiddleErrorsKeepTheirOwnersButDoNotEraseALegalTailDecision() {
        val result = CjVariadicParameterRules.select(
            kind = CjmpModifierDeclarationKind.FUNCTION,
            parameterListCount = 1,
            effectiveForeign = true,
            ellipsisParameters = listOf(true, false, true, true),
        )
        assertTrue(result.hasVariableLengthArgument)
        assertEquals(3, result.variableArgumentIndex)
        assertEquals(3, result.erasedParameterIndex)
        assertEquals(listOf(
            CjVariadicParameterRules.Error(CjVariadicParameterRules.ErrorKind.FIRST_PARAMETER, 0),
            CjVariadicParameterRules.Error(CjVariadicParameterRules.ErrorKind.NOT_LAST_PARAMETER, 2),
        ), result.errors)
        assertTrue(result.retains(0) && result.retains(2))
        assertFalse(result.retains(3))
    }

    @Test
    fun ordinaryParametersHaveNoVariableArgumentIdentity() {
        val result = CjVariadicParameterRules.select(
            kind = CjmpModifierDeclarationKind.FUNCTION,
            parameterListCount = 1,
            effectiveForeign = true,
            ellipsisParameters = listOf(false, false),
        )
        assertFalse(result.hasVariableLengthArgument)
        assertEquals(null, result.variableArgumentIndex)
        assertEquals(null, result.erasedParameterIndex)
        assertTrue(result.errors.isEmpty())
    }

    /** secondary 使用 CheckDeclModifiers 的返回 FOREIGN；不按 init 关键字排除合法尾部删除。 */
    @Test
    fun secondaryConstructorsUseEffectiveForeignForTheSameCompleteParameterList() {
        for (foreign in listOf(false, true)) {
            val result = CjVariadicParameterRules.select(
                kind = CjmpModifierDeclarationKind.SECONDARY_CONSTRUCTOR,
                parameterListCount = 1,
                effectiveForeign = foreign,
                ellipsisParameters = listOf(false, true, true),
            )
            assertTrue(result.hasVariableLengthArgument)
            assertEquals(if (foreign) 2 else 1, result.variableArgumentIndex)
            assertEquals(if (foreign) 2 else null, result.erasedParameterIndex)
            assertEquals(listOf(CjVariadicParameterRules.Error(
                if (foreign) CjVariadicParameterRules.ErrorKind.NOT_LAST_PARAMETER
                else CjVariadicParameterRules.ErrorKind.NON_FOREIGN,
                1,
            )), result.errors)
            assertTrue(result.retains(0) && result.retains(1))
            assertEquals(!foreign, result.retains(2))
        }
    }

    /** PrimaryCtorDecl 的独立重载始终走非 FOREIGN 分支，且只报告第一个省略号。 */
    @Test
    fun primaryConstructorsNeverAcquireForeignTailDeletion() {
        for (foreign in listOf(false, true)) {
            val result = CjVariadicParameterRules.select(
                kind = CjmpModifierDeclarationKind.PRIMARY_CONSTRUCTOR,
                parameterListCount = 1,
                effectiveForeign = foreign,
                ellipsisParameters = listOf(false, true, true),
            )
            assertTrue(result.hasVariableLengthArgument)
            assertEquals(1, result.variableArgumentIndex)
            assertEquals(null, result.erasedParameterIndex)
            assertEquals(listOf(CjVariadicParameterRules.Error(
                CjVariadicParameterRules.ErrorKind.NON_FOREIGN, 1,
            )), result.errors)
            assertTrue((0..2).all(result::retains))
        }
    }

    @Test
    fun foreignFirstOrMiddleEllipsesRetainTheirParametersWithoutALegalTail() {
        for (kind in listOf(CjmpModifierDeclarationKind.FUNCTION, CjmpModifierDeclarationKind.SECONDARY_CONSTRUCTOR)) {
            for ((parameters, errorKind) in listOf(
                listOf(true) to CjVariadicParameterRules.ErrorKind.FIRST_PARAMETER,
                listOf(true, false) to CjVariadicParameterRules.ErrorKind.FIRST_PARAMETER,
                listOf(false, true, false) to CjVariadicParameterRules.ErrorKind.NOT_LAST_PARAMETER,
            )) {
                val index = parameters.indexOf(true)
                val result = CjVariadicParameterRules.select(kind, 1, true, parameters)
                assertTrue(result.hasVariableLengthArgument)
                assertEquals(index, result.variableArgumentIndex)
                assertEquals(null, result.erasedParameterIndex)
                assertEquals(listOf(CjVariadicParameterRules.Error(errorKind, index)), result.errors)
                assertTrue(parameters.indices.all(result::retains))
            }
        }
    }

    /** ParseFuncParameters 完整收集所有列表后才检查；不能只看第一个列表或 flatten 后的尾部。 */
    @Test
    fun absentOrMultipleParameterListsSkipDiagnosticsFlagsAndDeletion() {
        for (kind in listOf(
            CjmpModifierDeclarationKind.FUNCTION,
            CjmpModifierDeclarationKind.PRIMARY_CONSTRUCTOR,
            CjmpModifierDeclarationKind.SECONDARY_CONSTRUCTOR,
            CjmpModifierDeclarationKind.MACRO,
        )) {
            for (foreign in listOf(false, true)) {
                for (count in listOf(0, 2, 3)) {
                    val parameters = if (count == 0) emptyList() else listOf(false, true, true, false, true)
                    val result = CjVariadicParameterRules.select(kind, count, foreign, parameters)
                    assertFalse(result.hasVariableLengthArgument, "$kind/$count/$foreign")
                    assertEquals(null, result.variableArgumentIndex)
                    assertEquals(null, result.erasedParameterIndex)
                    assertTrue(result.errors.isEmpty())
                    assertTrue(parameters.indices.all(result::retains))
                }
            }
        }
    }

    /** ParseMacroDecl 不调用 CheckVariableParams；宏定义的省略号不能成为 foreign 签名标志。 */
    @Test
    fun macroDefinitionsSkipCheckingEvenWithOneParameterList() {
        for (foreign in listOf(false, true)) {
            for (parameters in listOf(listOf(true), listOf(false, true), listOf(true, false, true))) {
                val result = CjVariadicParameterRules.select(CjmpModifierDeclarationKind.MACRO, 1, foreign, parameters)
                assertFalse(result.hasVariableLengthArgument)
                assertEquals(null, result.variableArgumentIndex)
                assertEquals(null, result.erasedParameterIndex)
                assertTrue(result.errors.isEmpty())
                assertTrue(parameters.indices.all(result::retains))
            }
        }
    }
}
