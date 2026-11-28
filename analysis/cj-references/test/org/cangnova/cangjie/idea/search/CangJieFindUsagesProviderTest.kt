package org.cangnova.cangjie.idea.search

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CangJieReferenceProvidersService
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjConstantExpression
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeParameter
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjTypePattern
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 锁定 `CangJieFindUsagesProvider` 的语言桥接文本契约。
 */
class CangJieFindUsagesProviderTest : AbstractAnalysisApiExecutionTest(
    "analysis/cj-references/testData/findUsagesProvider",
) {
    /**
     * 使用 standalone CFIR 分析 API 配置运行 find usages provider 测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证 Find Usages UI 展示文本、可搜索性和帮助 ID 契约。
     */
    @Test
    fun presentationContracts(mainFile: CjFile) {
        val provider = CangJieFindUsagesProvider()

        val function = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java).single { it.name == "greet" }
        val property = PsiTreeUtil.findChildrenOfType(mainFile, CjProperty::class.java).single { it.name == "state" }
        val extend = PsiTreeUtil.findChildrenOfType(mainFile, CjExtend::class.java).single()
        val type = mainFile.declarations
            .filterIsInstance<CjTypeStatement>()
            .single { declaration -> declaration !is CjExtend && declaration.name == "Document" }
        val parameter = PsiTreeUtil.findChildrenOfType(mainFile, CjParameter::class.java).single { it.name == "value" }
        val typeParameter = PsiTreeUtil.findChildrenOfType(mainFile, CjTypeParameter::class.java).single { it.name == "T" }
        val bindingPattern = PsiTreeUtil.findChildrenOfType(mainFile, CjBindingPattern::class.java).single { it.name == "captured" }
        val importAlias = PsiTreeUtil.findChildrenOfType(mainFile, CjImportAlias::class.java).single { it.name == "welcome" }

        assertEquals("function", provider.getType(function))
        assertEquals("property", provider.getType(property))
        assertEquals("extend", provider.getType(extend))
        assertEquals("type", provider.getType(type))
        assertEquals("parameter", provider.getType(parameter))
        assertEquals("type parameter", provider.getType(typeParameter))
        assertEquals("pattern binding", provider.getType(bindingPattern))
        assertEquals("import alias", provider.getType(importAlias))

        assertEquals("greet", provider.getDescriptiveName(function))
        assertEquals("state", provider.getNodeText(property, useFullName = false))
        assertEquals("Document", provider.getNodeText(type, useFullName = true))
        assertEquals("welcome", provider.getNodeText(importAlias, useFullName = false))

        assertTrue(provider.canFindUsagesFor(function))
        assertTrue(provider.canFindUsagesFor(property))
        assertTrue(provider.canFindUsagesFor(bindingPattern))
        assertTrue(provider.canFindUsagesFor(importAlias))
        assertNull(provider.getHelpId(function))

        val literal = PsiTreeUtil.findChildrenOfType(mainFile, CjConstantExpression::class.java).first()
        assertFalse(provider.canFindUsagesFor(literal))
    }

    /** 可搜索性必须消费正式语义分类：裸构造器和通配类型模式都没有变量声明身份。 */
    @Test
    fun patternSearchabilityUsesCanonicalSemantics(mainFile: CjFile, testServices: TestServices) {
        val provider = CangJieFindUsagesProvider()
        val semanticProvider = CangJieReferenceProvidersService.getInstance(mainFile.project)
        assertEquals(
            "org.cangnova.cangjie.analysis.references.CangJieReferenceProvidersServiceImpl",
            semanticProvider::class.java.name,
            "该测试必须装配正式语义 provider，不能使用仅按 PSI 形状分类的测试替身",
        )
        val markers = testServices.expressionMarkerProvider
        val constructor = markers.getBottommostElementOfTypeAtCaret<CjVarOrEnumPattern>(mainFile, "constructor")
        val wildcard = markers.getBottommostElementOfTypeAtCaret<CjTypePattern>(mainFile, "wildcard")
        val bareBinder = markers.getBottommostElementOfTypeAtCaret<CjVarOrEnumPattern>(mainFile, "bareBinder")
        val typedBinder = markers.getBottommostElementOfTypeAtCaret<CjTypePattern>(mainFile, "typedBinder")

        assertSame(bareBinder, semanticProvider.getNamedDeclarationTarget(bareBinder))
        assertSame(typedBinder, semanticProvider.getNamedDeclarationTarget(typedBinder))
        assertTrue(provider.canFindUsagesFor(bareBinder), "裸模式决议为绑定时必须可搜索")
        assertTrue(provider.canFindUsagesFor(typedBinder), "具名类型模式必须可搜索")
        assertNull(semanticProvider.getNamedDeclarationTarget(constructor), "Ready 是枚举构造器引用")
        assertNull(semanticProvider.getNamedDeclarationTarget(wildcard), "_: Int64 不引入变量")
        assertFalse(provider.canFindUsagesFor(constructor), "构造器模式不能作为 binder 搜索目标")
        assertFalse(provider.canFindUsagesFor(wildcard), "通配类型模式不能作为 binder 搜索目标")
    }
}
