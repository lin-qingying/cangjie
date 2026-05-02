package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForDebug
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForDebug
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 锁定 renderer preset 的细粒度行为。
 *
 * 这组测试重点覆盖：
 * 1. qualified / short names 两套 declaration preset
 * 2. body / 默认值 / placeholder 等细粒度 detail preset
 * 3. debug renderer 与 source renderer 的共享结构
 * 4. tuple / intersection / union / function type 的公开 type renderer 输出
 */
class AnalysisApiRendererPresetTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/rendererPresets",
) {
    override val configurator: AnalysisApiTestConfigurator =
        CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
            AnalysisApiTestConfiguratorFactoryData(
                frontend = FrontendKind.Cfir,
                moduleKind = TestModuleKind.Source,
                analysisSessionMode = AnalysisSessionMode.Normal,
                analysisApiMode = AnalysisApiMode.Standalone,
            ),
        )

    @Test
    fun presetRendering(mainFile: CjFile, mainModule: CjTestModule) {
        val cachedValueReference = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .last { reference -> reference.referencedName == "cachedValue" }
        val cachedTransformDeclaration = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { declaration -> declaration.name == "cachedTransform" }

        analyzeForTest(mainFile) {
            val fileScope = mainFile.getFileScope()
            val userSymbol = fileScope.classifierSymbol("User")
            val baseSymbol = fileScope.classifierSymbol("Base")
            val boxSymbol = fileScope.classifierSymbol("Box")
            val holderSymbol = fileScope.classifierSymbol("Holder")
            val resultSymbol = fileScope.classifierSymbol("Result")
            val greetSymbol = fileScope.callableSymbol("greet")
            val sideEffectSymbol = fileScope.callableSymbol("sideEffect")
            val intValueSymbol = fileScope.callableSymbol("intValue")
            val floatValueSymbol = fileScope.callableSymbol("floatValue")
            val cachedLocalSymbol = cachedValueReference.resolveToSymbol() as CaLocalVariableSymbol
            val cachedTransformSymbol = cachedTransformDeclaration.symbol
            val stateProperty = holderSymbol.declaredMemberScope.propertySymbol("state")

            assertEquals(
                "class sample.renderer.presets.User <: sample.renderer.presets.Base",
                userSymbol.render(CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES),
            )
            assertEquals(
                "class User <: Base",
                userSymbol.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES),
            )
            assertEquals(
                "class User",
                userSymbol.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES_WITHOUT_SUPER_TYPES),
            )
            assertEquals(
                "class Box",
                boxSymbol.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES_WITHOUT_TYPE_PARAMETERS),
            )

            val enumWithMembers = resultSymbol.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES)
            assertTrue(enumWithMembers.startsWith("enum Result {"))
            assertTrue(enumWithMembers.contains("Error(String)"))
            assertTrue(enumWithMembers.contains("Ok(Int64)"))
            assertTrue(enumWithMembers.contains("func code(): Int64"))
            assertTrue(
                enumWithMembers.indexOf("Error(String)") < enumWithMembers.indexOf("func code(): Int64"),
                "枚举构造器应排在普通成员之前。actual=$enumWithMembers",
            )
            assertTrue(
                resultSymbol.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES_WITH_MEMBERS_AND_BODY).contains("return 0"),
                "带 body 的 members preset 应保留枚举成员函数体。",
            )

            assertEquals(
                "internal final class sample.renderer.presets.User <: sample.renderer.presets.Base",
                userSymbol.render(CaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES),
            )

            assertEquals(
                "func greet<T> where T <: sample.renderer.presets.Base(value: T, fallback!: Int64): T",
                greetSymbol.render(CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES),
            )
            assertEquals(
                "func greet<T> where T <: Base(value: T, fallback!: Int64 = 0): T",
                greetSymbol.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES_WITH_DEFAULT_PARAMETER_VALUES),
            )
            assertEquals(
                "internal final func greet<T> where T <: Base(value: T, fallback!: Int64): T",
                greetSymbol.render(CaDeclarationRendererForDebug.WITH_SHORT_NAMES),
            )
            assertEquals(
                "greet<T> where T <: sample.renderer.presets.Base(value: T, fallback!: Int64): T",
                greetSymbol.render(CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_RAW_SIGNATURES),
            )
            assertTrue(
                greetSymbol.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES_WITH_BODY).contains("return value"),
                "function body preset 应保留源码函数体文本。",
            )
            assertTrue(
                greetSymbol.render(CaDeclarationRendererForDebug.WITH_SHORT_NAMES_WITH_PLACEHOLDER_BODIES).contains("{ ... }"),
                "placeholder body preset 应输出函数体占位文本。",
            )
            assertTrue(
                greetSymbol.render(CaDeclarationRendererForDebug.WITH_SHORT_NAMES_WITH_MEMBERS_AND_BODY).contains("return value"),
                "debug body preset 也应保留源码函数体文本。",
            )
            assertTrue(
                greetSymbol.render(CaDeclarationRendererForDebug.WITH_SHORT_NAMES_WITH_PLACEHOLDER_DETAILS).contains("fallback!: Int64 = ..."),
                "placeholder detail preset 应输出参数默认值占位文本。",
            )
            assertEquals(
                "func sideEffect(flag: Bool)",
                sideEffectSymbol.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES),
            )

            val renderedProperty = stateProperty.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES_WITH_BODY)
            assertTrue(renderedProperty.startsWith("prop state: Int64"))
            assertTrue(renderedProperty.contains("get()"))
            assertTrue(renderedProperty.contains("set(value)"))
            assertEquals(
                "state: Int64",
                stateProperty.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES_RAW_SIGNATURES),
            )

            assertEquals(
                "let cachedValue: Int64",
                cachedLocalSymbol.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES),
            )
            assertEquals(
                "let cachedValue: Int64 = 42",
                cachedLocalSymbol.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES_WITH_INITIALIZERS),
            )
            assertEquals(
                "func cachedTransform(value: Int64): Int64",
                cachedTransformSymbol.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES),
            )
            assertTrue(
                cachedTransformSymbol.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES_WITH_BODY).contains("return value + cachedValue"),
                "局部函数 renderer 应能恢复函数体而不是在 symbol 恢复阶段抛异常。",
            )

            assertTrue(
                stateProperty.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES_WITH_PLACEHOLDER_BODIES).contains("get() { ... }"),
                "property accessor placeholder preset 应输出 getter 占位体。",
            )
            assertTrue(
                stateProperty.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES_WITH_PLACEHOLDER_BODIES).contains("set(...) { ... }"),
                "property accessor placeholder preset 应输出 setter 占位体。",
            )

            val userType = buildClassType(userSymbol)
            val baseType = buildClassType(baseSymbol)
            val boxOfUserType = buildClassType(boxSymbol) {
                argument(userType)
            }
            val tupleType = buildTupleType(listOf(userType, baseType))
            val intersectionType = buildIntersectionType(listOf(userType, baseType))
            val unionType = buildUnionType(listOf(userType, baseType))
            val functionType = buildFunctionType(listOf(userType), baseType)
            val cFunctionType = buildFunctionType(listOf(userType), baseType, isCFunction = true)
            val closureFunctionType = buildFunctionType(
                parameterTypes = listOf(userType),
                returnType = baseType,
                isClosureType = true,
                hasVariableLengthArgument = true,
            )

            assertEquals(
                "(sample.renderer.presets.User, sample.renderer.presets.Base)",
                normalizeTypeRendering(tupleType.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES)),
            )
            assertEquals(
                "User & Base",
                normalizeTypeRendering(intersectionType.render(CaTypeRendererForSource.WITH_SHORT_NAMES)),
            )
            assertEquals(
                "sample.renderer.presets.User | sample.renderer.presets.Base",
                normalizeTypeRendering(unionType.render(CaTypeRendererForDebug.WITH_QUALIFIED_NAMES)),
            )
            assertEquals(
                "Box",
                normalizeTypeRendering(boxOfUserType.render(CaTypeRendererForSource.WITH_SHORT_NAMES_WITHOUT_TYPE_ARGUMENTS)),
            )
            assertEquals(
                "(User) -> Base",
                normalizeTypeRendering(functionType.render(CaTypeRendererForDebug.WITH_SHORT_NAMES)),
            )
            assertEquals(
                "cfunc (User) -> Base",
                normalizeTypeRendering(cFunctionType.render(CaTypeRendererForSource.WITH_SHORT_NAMES)),
            )
            assertEquals(
                "(User) -> Base",
                normalizeTypeRendering(cFunctionType.render(CaTypeRendererForSource.WITH_SHORT_NAMES_WITHOUT_FUNCTION_KIND_KEYWORDS)),
            )
            assertEquals(
                "closure (sample.renderer.presets.User, ...) -> sample.renderer.presets.Base",
                normalizeTypeRendering(closureFunctionType.render(CaTypeRendererForDebug.WITH_QUALIFIED_NAMES)),
            )

            val unitType = sideEffectSymbol.returnType
            val boolType = (sideEffectSymbol as CaFunctionSymbol).valueParameters.single().returnType
            val intType = intValueSymbol.returnType
            val floatType = floatValueSymbol.returnType

            assertTrue(unitType is CaPrimitiveType)
            assertTrue(unitType !is CaUsualClassType)
            assertEquals("Unit", normalizeTypeRendering(unitType.render(CaTypeRendererForSource.WITH_SHORT_NAMES)))
            assertEquals("Bool", normalizeTypeRendering(boolType.render(CaTypeRendererForSource.WITH_SHORT_NAMES)))
            assertEquals("Int32", normalizeTypeRendering(intType.render(CaTypeRendererForSource.WITH_SHORT_NAMES)))
            assertEquals("Float64", normalizeTypeRendering(floatType.render(CaTypeRendererForSource.WITH_SHORT_NAMES)))
            assertEquals(PrimitiveTypeKind.UNIT, (unitType as CaPrimitiveType).kind)
            assertEquals(null, unitType.classLikeSymbol)

            assertEquals(mainModule.caModule, userSymbol.containingModule)
        }
    }

    private fun normalizeTypeRendering(rendered: String): String {
        return rendered.replace('/', '.')
    }

    private fun org.cangnova.cangjie.analysis.api.scopes.CaScope.classifierSymbol(name: String): CaClassLikeSymbol {
        return classifiers(Name.identifier(name))
            .filterIsInstance<CaClassLikeSymbol>()
            .single()
    }

    private fun org.cangnova.cangjie.analysis.api.scopes.CaScope.callableSymbol(name: String): CaCallableSymbol {
        return callables(Name.identifier(name)).single()
    }

    private fun org.cangnova.cangjie.analysis.api.scopes.CaScope.propertySymbol(name: String): CaPropertySymbol {
        return callables(Name.identifier(name))
            .filterIsInstance<CaPropertySymbol>()
            .single()
    }
}
