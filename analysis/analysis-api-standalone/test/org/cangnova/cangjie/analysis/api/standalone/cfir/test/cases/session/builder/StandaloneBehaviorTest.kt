@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.cfir.test.cases.session.builder

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationValue
import org.cangnova.cangjie.analysis.api.annotations.CaConstantValue
import org.cangnova.cangjie.analysis.api.components.analysisScope
import org.cangnova.cangjie.analysis.api.platform.packages.createPackageProvider
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.session.CaStandaloneSessionBuilder
import org.cangnova.cangjie.analysis.api.symbols.symbol
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.psiUtil.findDescendantOfType
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Standalone 行为层测试。
 *
 * 这里对位 Kotlin standalone 的专用行为测试，但只保留仓颉真实存在的语义边界：
 * 1. package provider 只暴露 standalone 可见源码闭包；
 * 2. 嵌套 class-like 别名必须能稳定索引并展开到最终目标类型。
 * 3. source 注解必须能通过公开 Analysis API 恢复 tuple 与 const 等真实注解值。
 */
class StandaloneBehaviorTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-standalone/testData/behavior",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(StandaloneBuilderPlatformTestServiceRegistrar)

    @Test
    fun sourcePackageProviderSeesMergedSourcePackages(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        val context = CaStandaloneSessionBuilder(mainFile.project).build(mainModule.caModule)

        context.analyze(mainModule.caModule) {
            val packageProvider = mainFile.project.createPackageProvider(analysisScope)

            assertTrue(packageProvider.doesPackageExist(FqName("sample")))
            assertTrue(packageProvider.doesPackageExist(FqName("sample.standalone.packages")))
            assertTrue(packageProvider.doesPackageExist(FqName("sample.standalone.packages.app")))
            assertTrue(packageProvider.doesPackageExist(FqName("sample.standalone.packages.helper")))
            assertFalse(packageProvider.doesPackageExist(FqName("std")))

            assertEquals(
                setOf(Name.identifier("app"), Name.identifier("helper")),
                packageProvider.getSubpackageNames(FqName("sample.standalone.packages")),
            )
        }
    }

    @Test
    fun transitiveTypeAliasExpandedType(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        val context = CaStandaloneSessionBuilder(mainFile.project).build(mainModule.caModule)
        val typeAlias = mainFile.declarations.filterIsInstance<CjTypeAlias>().single { declaration ->
            declaration.name == "FinalAlias"
        }

        context.analyze(typeAlias) {
            val expandedType = typeAlias.symbol.expandedType
            assertTrue(expandedType is CaClassLikeType)
            expandedType as CaClassLikeType

            assertEquals(
                ClassId.fromString("sample/standalone/behavior/Target"),
                expandedType.classId,
            )
        }
    }

    @Test
    fun stubbedAnnotationArguments(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        val context = CaStandaloneSessionBuilder(mainFile.project).build(mainModule.caModule)
        val annotatedClass = mainFile.findDescendantOfType<CjTypeStatement> { declaration ->
            declaration.name == "Main"
        } ?: error("Test file `${mainFile.name}` must declare annotated class `Main`.")

        context.analyze(annotatedClass) {
            val annotation = annotatedClass.symbol.annotations.single()
            assertEquals(3, annotation.arguments.size)

            val shapeArgument = annotation.arguments[0]
            val shapeValue = shapeArgument.expression
            assertTrue(shapeValue is CaAnnotationValue.TupleValue)
            shapeValue as CaAnnotationValue.TupleValue
            assertEquals(2, shapeValue.values.size)

            val arityValue = shapeValue.values[0]
            assertTrue(arityValue is CaAnnotationValue.ConstantValue)
            arityValue as CaAnnotationValue.ConstantValue
            val arityConstant = arityValue.value
            assertTrue(arityConstant is CaConstantValue.Int64Value, arityConstant::class.qualifiedName)
            assertEquals(1L, (arityConstant as CaConstantValue.Int64Value).value)

            val kindValue = shapeValue.values[1]
            assertTrue(kindValue is CaAnnotationValue.ConstantValue)
            kindValue as CaAnnotationValue.ConstantValue
            val kindConstant = kindValue.value
            assertTrue(kindConstant is CaConstantValue.StringValue)
            assertEquals("hello", (kindConstant as CaConstantValue.StringValue).value)

            val labelArgument = annotation.arguments[1]
            val labelValue = labelArgument.expression
            assertTrue(labelValue is CaAnnotationValue.ConstantValue)
            labelValue as CaAnnotationValue.ConstantValue
            val labelConstant = labelValue.value
            assertTrue(labelConstant is CaConstantValue.StringValue)
            assertEquals("demo", (labelConstant as CaConstantValue.StringValue).value)

            val levelArgument = annotation.arguments[2]
            val levelValue = levelArgument.expression
            assertTrue(levelValue is CaAnnotationValue.ConstantValue)
            levelValue as CaAnnotationValue.ConstantValue
            val levelConstant = levelValue.value
            assertTrue(levelConstant is CaConstantValue.Int64Value, levelConstant::class.qualifiedName)
            assertEquals(2L, (levelConstant as CaConstantValue.Int64Value).value)
        }
    }
}
