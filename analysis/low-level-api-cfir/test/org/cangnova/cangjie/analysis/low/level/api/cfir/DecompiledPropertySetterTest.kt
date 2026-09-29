package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolvableSessionForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjProperty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 真实 CJO 反编译 setter 的无类型参数由属性签名提供类型，不跳过访问器。 */
class DecompiledPropertySetterTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/decompiledLibraries",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun decompiledSetterParameter(mainFile: CjFile) {
        val session = mainFile.getResolvableSessionForTest()
        val thread = session.symbolProvider.getClassLikeSymbolByClassId(ClassId(FqName("std.core"), Name.identifier("Thread")))!!.cfir as CfirClass
        val property = thread.declarations.filterIsInstance<CfirProperty>().single { it.name.asString() == "name" }
        assertTrue((property.psi as CjProperty).containingCjFile.isCompiled)
        val setter = requireNotNull(property.setter)
        val parameter = setter.valueParameters.single()
        assertEquals("value", parameter.name.asString())
        assertNull((parameter.psi as CjParameter).typeReference)
        assertEquals(property.returnTypeRef.coneType, parameter.returnTypeRef.coneType)
        assertSame(setter.symbol, parameter.containingDeclarationSymbol)
        assertTrue(setter.symbol.isBound)
        assertSame(property.symbol, setter.propertySymbol)
    }
}
