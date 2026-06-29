package org.cangnova.cangjie.analysis.api.impl.base.test.cases.annotations

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 按 `ClassId` 精确读取声明注解的抽象测试。
 *
 * 测试验证 `CaAnnotationList.contains`、索引访问和按 `annotation.classId` 查找三条路径是否指向同一个注解。
 */
abstract class AbstractAnalysisApiSpecificAnnotationOnDeclarationTest : AbstractAnalysisApiComponentTest() {
    /**
     * 当前测试额外注册的 ClassId 指令集合。
     *
     * 父类提供目标定位指令，本测试补充待命中的注解 `ClassId`。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    /**
     * 执行指定注解访问测试。
     *
     * 方法读取 testData 中的目标 `ClassId`，恢复 caret 所在声明的 symbol 注解列表，并比较直接索引访问与解析后查找结果。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val classIdString = directives.singleValue(Directives.CLASS_ID)

        val actual = copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjDeclaration>(contextFile)
            val declarationSymbol = declaration.symbol as? org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
                ?: error("Declaration `${declaration.text}` does not resolve to a declaration symbol.")
            val annotationList = declarationSymbol.annotations
            val classId = ClassId.fromString(classIdString)

            fun renderAnnotation(annotation: CaAnnotation): String = buildString {
                appendLine("${CjDeclaration::class.simpleName}: ${declaration::class.simpleName} ${declaration.name}")
                append(TestAnnotationRenderer.renderSingleAnnotation(annotation))
            }

            testServices.assertions.assertTrue(classId in annotationList) {
                "ClassId $classId is not found in the annotation list."
            }

            val directAccess = renderAnnotation(annotationList[classId].single())
            val resolvedAccess = renderAnnotation(annotationList.single { annotation -> annotation.classId == classId })
            testServices.assertions.assertEquals(resolvedAccess, directAccess) {
                "Result before and after resolving the target annotation differs."
            }

            resolvedAccess
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    /**
     * 指定注解访问测试使用的专用指令集合。
     *
     * 该容器只声明目标注解 `ClassId`，其它目标声明定位指令复用组件测试公共协议。
     */
    private object Directives : SimpleDirectivesContainer() {
        /**
         * 当前测试期望在声明注解列表中命中的注解 `ClassId`。
         *
         * 测试会把该字符串解析为 `ClassId`，并用它验证注解列表的 contains、get 和迭代查找行为。
         */
        val CLASS_ID by stringDirective("当前测试期望命中的注解 ClassId。", applicability = DirectiveApplicability.File)
    }
}
