package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbolOfType
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.TargetSourceTestSupport
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.LLContainingClassCalculator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.correspondingField
import org.cangnova.cangjie.cfir.correspondingValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.isMemberParameter
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 主构造 let/var 参数生成真实字段，同时保留独立参数身份。
 *
 * 官方 DesugarInTypeCheck.cpp 的成员参数反糖生成 VarDecl；这里通过真实 LL 与公开
 * Analysis API 验证字段归属、延迟解析、源码映射和跨 analyze 指针恢复。
 */
@OptIn(CaExperimentalApi::class)
class CfirPrimaryConstructorFieldTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/primaryConstructorFields",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun classFields(mainFile: CjFile) = assertPrimaryConstructorFields(mainFile, CfirClass::class.java)

    @Test
    fun structFields(mainFile: CjFile) = assertPrimaryConstructorFields(mainFile, CfirStruct::class.java)

    /** Deprecated 只允许带默认值的命名参数；反糖后参数和字段均保留各自的注解节点。 */
    @Test
    fun annotatedFields(mainFile: CjFile) = assertPrimaryConstructorFields(mainFile, CfirClass::class.java)

    private fun assertPrimaryConstructorFields(mainFile: CjFile, ownerKind: Class<out CfirClassLikeDeclaration>) {
        val facade = mainFile.getResolutionFacadeForTest()
        val file = mainFile.getOrBuildCfirFile(facade)
        val sources = TargetSourceTestSupport(file, mainFile)
        val owner = file.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
        val ownerPsi = mainFile.declarations.filterIsInstance<CjTypeStatement>().single()
        val targetOwnerPsi = sources.targetElement(ownerPsi) as CjTypeStatement
        assertTrue(ownerKind.isInstance(owner))
        sources.assertTargetElement(ownerPsi, owner.psi)

        val constructor = owner.declarations.filterIsInstance<CfirConstructor>().single()
        assertTrue(constructor.isPrimary)
        val fields = owner.declarations.filterIsInstance<CfirFieldVariable>().associateBy { it.name }
        val expectedMutability = mapOf(Name.identifier("fixed") to false, Name.identifier("mutable") to true)
        assertEquals(expectedMutability.keys, fields.keys)
        assertEquals(3, owner.declarations.size, "Only the constructor and its two fields should be generated")
        assertFalse(owner.declarations.any { it is CfirProperty || it is CfirPropertyAccessor })

        val parameters = constructor.valueParameters.associateBy { it.name }
        assertEquals(expectedMutability.keys, parameters.keys)
        assertEquals(ownerPsi.primaryConstructorParameters.size, targetOwnerPsi.primaryConstructorParameters.size)
        for ((index, parameterPsi) in ownerPsi.primaryConstructorParameters.withIndex()) {
            val targetParameterPsi = sources.targetElement(parameterPsi) as CjParameter
            assertSame(targetOwnerPsi.primaryConstructorParameters[index], targetParameterPsi)
            val name = Name.identifier(requireNotNull(parameterPsi.name))
            val parameter = parameters.getValue(name)
            val field = fields.getValue(name)
            // 首个语义请求从字段发起，不能先用参数 PSI 或 owner 查询预热构造器。
            field.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
            assertSame(parameter, parameterPsi.getOrBuildCfir(facade))
            assertSame(parameter, targetParameterPsi.getOrBuildCfir(facade))
            assertSame(parameter.symbol, parameterPsi.resolveToCfirSymbolOfType<CfirValueParameterSymbol>(facade))
            assertSame(parameter.symbol, targetParameterPsi.resolveToCfirSymbolOfType<CfirValueParameterSymbol>(facade))
            assertSame(field, parameter.correspondingField)
            assertSame(parameter, field.correspondingValueParameter)
            assertTrue(parameter.isMemberParameter)
            assertTrue(field.isMemberParameter)
            sources.assertTargetElement(parameterPsi, parameter.psi)
            sources.assertTargetElement(parameterPsi, field.psi)
            assertEquals(CjFakeSourceElementKind.FieldFromParameter, field.source?.kind)
            assertSame(owner.symbol, LLContainingClassCalculator.getContainingClassSymbol(field.symbol))
            assertEquals(expectedMutability.getValue(name), field.isVar)

            assertEquals(ConePrimitiveType.INT64, field.returnTypeRef.coneTypeOrNull)
            assertEquals(ConePrimitiveType.INT64, parameter.returnTypeRef.coneTypeOrNull)
            assertEquals(parameterPsi.annotationEntries.size, parameter.annotations.size)
            assertEquals(parameterPsi.annotationEntries.size, targetParameterPsi.annotationEntries.size)
            assertEquals(parameter.annotations.size, field.annotations.size)
            for ((annotationIndex, annotations) in parameter.annotations.zip(field.annotations).withIndex()) {
                val (parameterAnnotation, fieldAnnotation) = annotations
                assertNotSame(parameterAnnotation, fieldAnnotation)
                assertSame(parameterAnnotation.psi, fieldAnnotation.psi)
                sources.assertTargetElement(parameterPsi.annotationEntries[annotationIndex], parameterAnnotation.psi)
                sources.assertTargetElement(parameterPsi.annotationEntries[annotationIndex], fieldAnnotation.psi)
                assertSame(field.symbol, (fieldAnnotation as CfirAnnotationCall).containingDeclarationSymbol)
            }
        }

        val diagnostics = mainFile.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertEquals(emptyList<String>(), diagnostics.map { it.factoryName }.sorted())
        for (parameterPsi in ownerPsi.primaryConstructorParameters) {
            val targetParameterPsi = sources.targetElement(parameterPsi) as CjParameter
            val name = Name.identifier(requireNotNull(parameterPsi.name))
            val parameter = parameters.getValue(name)
            assertSame(parameter, parameterPsi.getOrBuildCfir(facade), "File recording must keep parameter PSI canonical")
            assertSame(parameter, targetParameterPsi.getOrBuildCfir(facade), "Target recording must keep the same parameter canonical")
            assertSame(parameter.symbol, parameterPsi.resolveToCfirSymbolOfType<CfirValueParameterSymbol>(facade))
            assertSame(parameter.symbol, targetParameterPsi.resolveToCfirSymbolOfType<CfirValueParameterSymbol>(facade))
            assertSame(fields.getValue(name), parameter.correspondingField)
            assertSame(parameter, fields.getValue(name).correspondingValueParameter)
        }
        assertSame(file, mainFile.getOrBuildCfirFile(facade))
        assertSame(file, sources.targetFile.getOrBuildCfirFile(facade))

        assertPublicFields(ownerPsi, expectedMutability, facade, sources)
    }

    /** 字段作用域与参数 PSI 走各自的公开符号入口，指针恢复必须仍然得到 FIELD。 */
    private fun assertPublicFields(
        ownerPsi: CjTypeStatement,
        expectedMutability: Map<Name, Boolean>,
        facade: LLResolutionFacade,
        sources: TargetSourceTestSupport,
    ) {
        val pointers = mutableMapOf<Name, CaSymbolPointer<CaSymbol>>()
        analyzeForTest(ownerPsi) {
            val ownerSymbol = ownerPsi.symbol as CaClassLikeSymbol
            assertSame(ownerPsi, ownerSymbol.psi)
            val callables = ownerSymbol.declaredMemberScope.callables.toList()
            assertEquals(expectedMutability.size, callables.size)
            assertTrue(callables.all { it is CaFieldSymbol })
            val fields = callables.filterIsInstance<CaFieldSymbol>().associateBy { it.name }
            assertEquals(expectedMutability.keys, fields.keys)
            // 原树与本代 Parsed 目标树的公开 owner 必须共享真实原 PSI 及 equals/hashCode 身份。
            val targetOwnerPsi = sources.targetElement(ownerPsi) as CjTypeStatement
            val targetOwnerSymbol = targetOwnerPsi.symbol as CaClassLikeSymbol
            assertSame(ownerPsi, targetOwnerSymbol.psi)
            assertEquals(ownerSymbol, targetOwnerSymbol)
            assertEquals(targetOwnerSymbol, ownerSymbol)
            assertEquals(ownerSymbol.hashCode(), targetOwnerSymbol.hashCode())
            val targetCallables = targetOwnerSymbol.declaredMemberScope.callables.toList()
            assertEquals(expectedMutability.size, targetCallables.size)
            assertTrue(targetCallables.all { it is CaFieldSymbol })
            val targetFields = targetCallables.filterIsInstance<CaFieldSymbol>().associateBy { it.name }
            assertEquals(expectedMutability.keys, targetFields.keys)
            for (parameterPsi in ownerPsi.primaryConstructorParameters) {
                val name = Name.identifier(requireNotNull(parameterPsi.name))
                val field = fields.getValue(name)
                val parameter = parameterPsi.symbol
                assertTrue(parameter is CaValueParameterSymbol)
                assertNotSame(parameter, field)
                assertSame(parameterPsi, parameter.psi)
                assertSame(parameterPsi, field.psi)
                // 公开入口仍从原 owner 发起；目标参数查询也必须导航到同一个原参数。
                val targetParameterPsi = sources.targetElement(parameterPsi) as CjParameter
                val targetParameter = targetParameterPsi.symbol
                assertTrue(targetParameter is CaValueParameterSymbol)
                assertSame(parameterPsi, targetParameter.psi)
                assertEquals(parameter, targetParameter)
                assertEquals(targetParameter, parameter)
                assertEquals(parameter.hashCode(), targetParameter.hashCode())
                assertSame(parameterPsi.getOrBuildCfir(facade), targetParameterPsi.getOrBuildCfir(facade))
                assertSame(
                    parameterPsi.resolveToCfirSymbolOfType<CfirValueParameterSymbol>(facade),
                    targetParameterPsi.resolveToCfirSymbolOfType<CfirValueParameterSymbol>(facade),
                )
                // 官方成员参数反糖保留独立字段和参数；共用原参数 PSI 不能合并两种符号身份。
                val targetField = targetFields.getValue(name)
                assertSame(parameterPsi, targetField.psi)
                assertEquals(field, targetField)
                assertEquals(targetField, field)
                assertEquals(field.hashCode(), targetField.hashCode())
                assertNotSame(targetParameter, targetField)
                assertNotEquals(parameter, field)
                assertNotEquals(field, parameter)
                assertNotEquals(targetParameter, targetField)
                assertNotEquals(targetField, targetParameter)
                assertEquals(ownerSymbol, field.containingDeclaration)
                val containingDeclaration = requireNotNull(field.containingDeclaration)
                assertEquals(containingDeclaration, ownerSymbol)
                assertEquals(ownerSymbol.hashCode(), containingDeclaration.hashCode())
                assertSame(ownerPsi, containingDeclaration.psi)
                assertEquals(targetOwnerSymbol, targetField.containingDeclaration)
                val targetContainingDeclaration = requireNotNull(targetField.containingDeclaration)
                assertEquals(targetContainingDeclaration, targetOwnerSymbol)
                assertEquals(targetOwnerSymbol.hashCode(), targetContainingDeclaration.hashCode())
                assertSame(ownerPsi, targetContainingDeclaration.psi)
                assertEquals(CaSymbolLocation.CLASS, field.location)
                assertEquals(!expectedMutability.getValue(name), field.isLet)
                assertEquals(parameterPsi.annotationEntries.size, field.annotations.size)
                pointers[name] = field.createPointer()
            }
        }

        analyzeForTest(ownerPsi) {
            val ownerSymbol = ownerPsi.symbol as CaClassLikeSymbol
            assertSame(ownerPsi, ownerSymbol.psi)
            val targetOwnerPsi = sources.targetElement(ownerPsi) as CjTypeStatement
            val targetOwnerSymbol = targetOwnerPsi.symbol as CaClassLikeSymbol
            assertSame(ownerPsi, targetOwnerSymbol.psi)
            assertEquals(ownerSymbol, targetOwnerSymbol)
            assertEquals(targetOwnerSymbol, ownerSymbol)
            assertEquals(ownerSymbol.hashCode(), targetOwnerSymbol.hashCode())
            for ((name, pointer) in pointers) {
                val restored = pointer.restoreSymbol(this)
                assertTrue(restored is CaFieldSymbol, "A member-parameter field pointer must restore a field, not its parameter")
                val field = restored as CaFieldSymbol
                assertEquals(name, field.name)
                assertEquals(!expectedMutability.getValue(name), field.isLet)
                assertEquals(ownerSymbol, field.containingDeclaration)
                assertEquals(ownerSymbol.declaredMemberScope.callables(name).single(), field)
                // 新 analyze 中从目标 owner 查询，仍须得到与恢复字段相等的 FIELD。
                val targetField = targetOwnerSymbol.declaredMemberScope.callables(name).single()
                assertTrue(targetField is CaFieldSymbol)
                assertEquals(field, targetField)
                assertEquals(targetField, field)
                assertEquals(field.hashCode(), targetField.hashCode())
                val parameterPsi = ownerPsi.primaryConstructorParameters.single { it.name == name.asString() }
                assertSame(parameterPsi, field.psi)
                assertSame(parameterPsi, targetField.psi)
                assertTrue(parameterPsi.symbol is CaValueParameterSymbol)
            }
        }
    }
}
