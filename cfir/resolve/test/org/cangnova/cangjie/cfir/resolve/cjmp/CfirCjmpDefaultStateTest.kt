package org.cangnova.cangjie.cfir.resolve.cjmp

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirParameterDefaultValueMetadata
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunctionCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameterCopy
import org.cangnova.cangjie.cfir.declarations.defaultValueMetadata
import org.cangnova.cangjie.cfir.declarations.hasInitialDefaultValue
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildLazyExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** 检查真实参数的初始化位与 helper 独立提取，不以库占位表达式或参数符号代替来源。 */
class CfirCjmpDefaultStateTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    private val session = CallResolutionTestFixtures.newTestSession()
    private val context = CfirCjmpMatchingContext(session)

    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun metadataInitialFlagAndRealHelperProduceFourIndependentCombinations() {
        for (hasInitial in listOf(false, true)) {
            for (hasHelper in listOf(false, true)) {
                val function = function(defaultValue = null, origin = CfirDeclarationOrigin.Library)
                val parameter = function.valueParameters.single()
                val helper = if (hasHelper) {
                    CallResolutionTestFixtures.buildFunctionSymbol(session, "defaultHelper", returnType = ConePrimitiveType.INT64).cfir
                } else {
                    null
                }
                val metadata = CfirParameterDefaultValueMetadata(hasInitial, helper)
                parameter.defaultValueMetadata = metadata

                val state = cfirCjmpDefaultStates(function).single()
                assertEquals(hasInitial, state.hasInitial)
                assertEquals(hasInitial, parameter.hasInitialDefaultValue)
                assertEquals(hasInitial, context.valueParameters(function.symbol).single().hasDefaultValue)
                assertSame(helper?.symbol, state.desugaredDefaultSource)
                assertNotSame(parameter.symbol, state.desugaredDefaultSource)
                assertSame(metadata, parameter.defaultValueMetadata)
                assertNull(parameter.defaultValue)
            }
        }
    }

    @Test
    fun explicitFalseMetadataIsNotOverriddenByALibraryUnitMarker() {
        // 反序列化的 marker 使用参数类型，UNIT/null 只占位，不是源码中的 Unit 默认值。
        val marker = unitLiteral(ConePrimitiveType.INT64)
        val function = function(marker, origin = CfirDeclarationOrigin.Library)
        val parameter = function.valueParameters.single()
        parameter.defaultValueMetadata = CfirParameterDefaultValueMetadata(hasInitial = false, function = null)

        val state = cfirCjmpDefaultStates(function).single()
        assertFalse(state.hasInitial)
        assertFalse(parameter.hasInitialDefaultValue)
        assertFalse(context.valueParameters(function.symbol).single().hasDefaultValue)
        assertNull(state.desugaredDefaultSource)
        assertSame(marker, parameter.defaultValue)
    }

    @Test
    fun sourceDefaultExpressionCarriesInitialWithoutInventingAHelper() {
        val expression = buildLiteralExpression {
            coneTypeOrNull = ConePrimitiveType.INT64
            kind = CfirLiteralKind.INT
            value = 1L
        }
        val function = function(expression)
        assertInitialWithoutHelper(function, expression)
    }

    @Test
    fun lazySourceDefaultIsRecognizedWithoutResolvingTheExpression() {
        val expression = buildLazyExpression()
        val function = function(expression)
        // CfirLazyExpression 的类型与内容 getter 会抛异常；提取事实只需判定节点存在。
        assertInitialWithoutHelper(function, expression)
    }

    @Test
    fun actualUnitSourceDefaultRemainsAnInitialWithoutDesugaredSource() {
        val expression = unitLiteral(ConePrimitiveType.UNIT)
        val function = function(expression, parameterType = ConePrimitiveType.UNIT)
        assertInitialWithoutHelper(function, expression)
    }

    @Test
    fun sourceParameterWithoutDefaultCarriesNeitherInitialNorHelper() {
        val function = function(defaultValue = null)
        val state = cfirCjmpDefaultStates(function).single()
        assertFalse(state.hasInitial)
        assertFalse(function.valueParameters.single().hasInitialDefaultValue)
        assertFalse(context.valueParameters(function.symbol).single().hasDefaultValue)
        assertNull(state.desugaredDefaultSource)
        assertNull(function.valueParameters.single().defaultValueMetadata)
        assertEquals(CjRealSourceElementKind, function.source!!.kind)
        assertEquals(CjRealSourceElementKind, function.valueParameters.single().source!!.kind)
    }

    private fun assertInitialWithoutHelper(function: CfirNamedFunction, expression: CfirExpression) {
        val parameter = function.valueParameters.single()
        val state = cfirCjmpDefaultStates(function).single()
        assertTrue(state.hasInitial)
        assertTrue(parameter.hasInitialDefaultValue)
        assertTrue(context.valueParameters(function.symbol).single().hasDefaultValue)
        assertNull(state.desugaredDefaultSource)
        assertNull(parameter.defaultValueMetadata)
        assertSame(expression, parameter.defaultValue)
        assertEquals(CfirDeclarationOrigin.Source, function.origin)
        assertEquals(CfirDeclarationOrigin.Source, parameter.origin)
        assertEquals(CjRealSourceElementKind, function.source!!.kind)
        assertEquals(CjRealSourceElementKind, parameter.source!!.kind)
    }

    private fun unitLiteral(type: ConeCangJieType): CfirExpression = buildLiteralExpression {
        coneTypeOrNull = type
        kind = CfirLiteralKind.UNIT
        value = null
    }

    /** 替换为真实 source/library 声明，并为新函数和参数建立各自的符号身份。 */
    private fun function(
        defaultValue: CfirExpression?,
        origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
        parameterType: ConeCangJieType = ConePrimitiveType.INT64,
    ): CfirNamedFunction {
        val original = CallResolutionTestFixtures.buildFunctionSymbol(session, "value", parameterTypes = listOf(parameterType)).cfir
        val functionSymbol = CfirNamedFunctionSymbol(original.symbol.callableId)
        // Source 声明沿现有 parsing fixture 建立真实 PSI 锚点，函数和参数各自使用对应节点。
        val sourceFunction = if (origin == CfirDeclarationOrigin.Source) {
            val type = when (parameterType) {
                ConePrimitiveType.UNIT -> "Unit"
                ConePrimitiveType.INT64 -> "Int64"
                else -> error("This default-state fixture requires an Int64 or Unit parameter")
            }
            val initial = if (defaultValue == null) "" else if (parameterType == ConePrimitiveType.UNIT) " = ()" else " = 1"
            val file = createPsiFile("defaultState", "func value(p0!: $type$initial): Unit {}") as CjFile
            assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).isEmpty())
            file.declarations.single() as CjNamedFunction
        } else {
            null
        }
        val parameter = buildValueParameterCopy(original.valueParameters.single()) {
            source = sourceFunction?.valueParameters?.single()?.toCjPsiSourceElement()
            symbol = CfirValueParameterSymbol(original.valueParameters.single().symbol.callableId)
            containingDeclarationSymbol = functionSymbol
            isNamed = true
            this.origin = origin
            this.defaultValue = defaultValue
        }
        return buildNamedFunctionCopy(original) {
            source = sourceFunction?.toCjPsiSourceElement()
            symbol = functionSymbol
            this.origin = origin
            valueParameters.clear()
            valueParameters += parameter
        }
    }
}
