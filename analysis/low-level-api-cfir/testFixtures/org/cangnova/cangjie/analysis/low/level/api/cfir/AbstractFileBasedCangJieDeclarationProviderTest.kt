@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.api.impl.base.util.callableId
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieFileBasedDeclarationProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjVisitorUnit
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * 对齐 Kotlin `AbstractFileBasedKotlinDeclarationProviderTest` 的 file-based provider 测试。
 *
 * 仓颉当前公开的 `ClassId` 只建模顶层 class-like 声明，
 * 因此这里只校验顶层 class / typealias / function 的 file-based 查询。
 */
abstract class AbstractFileBasedCangJieDeclarationProviderTest : AbstractAnalysisApiBasedTest() {
    /**
     * 使用源码 low-level CFIR 测试配置。
     */
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    /**
     * file-based declaration provider 测试支持的额外指令。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    /**
     * 构建 file-based provider 并分别按指令和 PSI visitor 校验索引结果。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val provider = CangJieFileBasedDeclarationProvider(mainFile)
        assertContains(
            provider.findFilesForFacadeByPackage(mainFile.packageFqName),
            mainFile,
            "Facade package lookup should return `${mainFile.name}`.",
        )

        checkByDirectives(testServices.cjTestModuleStructure.testModuleStructure, provider)
        checkByVisitor(mainFile, provider)
    }

    /**
     * 根据测试指令校验 provider 查询结果。
     */
    private fun checkByDirectives(moduleStructure: TestModuleStructure, provider: CangJieFileBasedDeclarationProvider) {
        for (directive in moduleStructure.allDirectives[Directives.CLASS]) {
            val classId = ClassId.fromString(directive)
            assertTrue(provider.getAllClassesByClassId(classId).isNotEmpty(), "Class $classId not found")
            assertNotNull(provider.getClassLikeDeclarationByClassId(classId), "Class-like declaration $classId not found")
        }

        for (directive in moduleStructure.allDirectives[Directives.TYPE_ALIAS]) {
            val classId = ClassId.fromString(directive)
            assertTrue(provider.getAllTypeAliasesByClassId(classId).isNotEmpty(), "Type alias $classId not found")
            assertNotNull(provider.getClassLikeDeclarationByClassId(classId), "Class-like declaration $classId not found")
        }

        for (directive in moduleStructure.allDirectives[Directives.FUNCTION]) {
            val callableId = parseCallableId(directive)
            assertTrue(provider.getTopLevelFunctions(callableId).isNotEmpty(), "Function $callableId not found")
        }

    }

    /**
     * 遍历 [cjFile] PSI 并校验 provider 能返回对应顶层声明。
     */
    private fun checkByVisitor(cjFile: CjFile, provider: CangJieFileBasedDeclarationProvider) {
        cjFile.accept(object : CjVisitorUnit() {
            override fun visitElement(element: com.intellij.psi.PsiElement) {
                element.acceptChildren(this)
            }

            override fun visitTypeStatement(typeStatement: CjTypeStatement) {
                super.visitTypeStatement(typeStatement)
                processClassLikeDeclaration(typeStatement)
            }

            override fun visitTypeAlias(typeAlias: CjTypeAlias) {
                super.visitTypeAlias(typeAlias)
                processClassLikeDeclaration(typeAlias)
            }

            private fun processClassLikeDeclaration(declaration: CjClassLikeDeclaration) {
                val classId = declaration.getClassId() ?: return
                assertContains(
                    provider.getTopLevelCangJieClassLikeDeclarationNamesInPackage(classId.packageFqName),
                    classId.shortClassName,
                    "Top-level class-like name `${classId.shortClassName}` should be indexed in `${classId.packageFqName}`.",
                )

                when (declaration) {
                    is CjTypeStatement -> assertContains(
                        provider.getAllClassesByClassId(classId),
                        declaration,
                        "Class `${classId.asString()}` should be returned by class lookup.",
                    )
                    is CjTypeAlias -> assertContains(
                        provider.getAllTypeAliasesByClassId(classId),
                        declaration,
                        "Type alias `${classId.asString()}` should be returned by alias lookup.",
                    )
                }
            }

            override fun visitNamedFunction(function: CjNamedFunction) {
                super.visitNamedFunction(function)
                processCallableDeclaration(function)
            }

            private fun processCallableDeclaration(declaration: CjNamedFunction) {
                val callableId = declaration.callableId ?: return

                if (callableId.classId == null) {
                    assertContains(
                        provider.getTopLevelCallableFiles(callableId),
                        cjFile,
                        "Callable `${callableId.asSingleFqName()}` should map back to `${cjFile.name}`.",
                    )
                    assertContains(
                        provider.getTopLevelCallableNamesInPackage(callableId.packageName),
                        callableId.callableName,
                        "Callable name `${callableId.callableName}` should be indexed in `${callableId.packageName}`.",
                    )
                    assertContains(
                        provider.getTopLevelFunctions(callableId),
                        declaration,
                        "Function `${callableId.asSingleFqName()}` should be returned by top-level function lookup.",
                    )
                }
            }
        })
    }

    /**
     * file-based declaration provider 测试指令。
     */
    private object Directives : SimpleDirectivesContainer() {
        /**
         * 需要通过 ClassId 命中的顶层 class。
         */
        val CLASS by stringDirective("需要通过 ClassId 命中的顶层 class。")
        /**
         * 需要通过 ClassId 命中的顶层 typealias。
         */
        val TYPE_ALIAS by stringDirective("需要通过 ClassId 命中的顶层 typealias。")
        /**
         * 需要通过 CallableId 命中的顶层 function。
         */
        val FUNCTION by stringDirective("需要通过 CallableId 命中的顶层 function。")
    }
}

/**
 * 从指令文本解析 [CallableId]。
 */
private fun parseCallableId(rawString: String): CallableId {
    val chunks = rawString.split('#')
    require(chunks.size == 2) { "Invalid CallableId string format: $rawString" }

    val rawQualifier = chunks[0]
    val rawCallableName = chunks[1]
    val callableName = Name.identifier(rawCallableName)

    return when {
        rawQualifier.endsWith('/') -> CallableId(FqName(rawQualifier.dropLast(1).replace('/', '.')), callableName)
        else -> CallableId(ClassId.fromString(rawQualifier), callableName)
    }
}

/**
 * source 配置下的 file-based declaration provider 测试基类。
 */
abstract class AbstractSourceFileBasedCangJieDeclarationProviderTest : AbstractFileBasedCangJieDeclarationProviderTest()

/**
 * 断言 [elements] 包含 [expected]。
 */
private fun <T> assertContains(elements: Collection<T>, expected: T, message: String) {
    assertTrue(elements.contains(expected), "$message Actual elements: $elements")
}
