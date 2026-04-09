package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeCreator

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiTypeCreatorTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.containerClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedQualifiedTypeRender
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedShortTypeRender
import org.cangnova.cangjie.analysis.api.impl.base.test.secondTargetClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.targetClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.typeCreationKind
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * public type creator generated 测试。
 *
 * 这组测试固定覆盖仓颉公开 Analysis API 里真实存在的全部类型构造入口：
 * - class-like type
 * - generic class-like type
 * - tuple type
 * - intersection type
 * - union type
 * - function / c-function / closure function
 *
 * class-like 场景同时要求 `buildClassLikeType(classId, ...)` 与
 * `buildClassLikeType(symbol, ...)` 结果一致，避免两个公开入口语义漂移。
 */
abstract class AbstractTypeCreatorTest : AbstractAnalysisApiComponentTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiTypeCreatorTestDirectives

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)

        analyzeForTest(mainFile) {
            val primaryClass = resolveClassSymbol(mainModule, directives.targetClassName)
            val secondaryClass = directives.secondTargetClassName?.let { name -> resolveClassSymbol(mainModule, name) }
            val containerClass = directives.containerClassName?.let { name -> resolveClassSymbol(mainModule, name) }

            val createdType = when (directives.typeCreationKind) {
                "CLASS" -> assertClassLikeConstruction(primaryClass)
                "GENERIC_CLASS" -> {
                    val container = requireNotNull(containerClass) { "GENERIC_CLASS 用例必须声明 CONTAINER_CLASS。" }
                    assertClassLikeConstruction(container, listOf(primaryClass.defaultType))
                }
                "TUPLE" -> buildTupleType(
                    listOf(
                        primaryClass.defaultType,
                        requireNotNull(secondaryClass) { "TUPLE 用例必须声明 SECOND_TARGET_CLASS。" }.defaultType,
                    ),
                )
                "INTERSECTION" -> buildIntersectionType(
                    listOf(
                        primaryClass.defaultType,
                        requireNotNull(secondaryClass) { "INTERSECTION 用例必须声明 SECOND_TARGET_CLASS。" }.defaultType,
                    ),
                )
                "UNION" -> buildUnionType(
                    listOf(
                        primaryClass.defaultType,
                        requireNotNull(secondaryClass) { "UNION 用例必须声明 SECOND_TARGET_CLASS。" }.defaultType,
                    ),
                )
                "FUNCTION" -> buildFunctionType(
                    parameterTypes = listOf(primaryClass.defaultType),
                    returnType = requireNotNull(secondaryClass) { "FUNCTION 用例必须声明 SECOND_TARGET_CLASS。" }.defaultType,
                )
                "C_FUNCTION" -> buildFunctionType(
                    parameterTypes = listOf(primaryClass.defaultType),
                    returnType = requireNotNull(secondaryClass) { "C_FUNCTION 用例必须声明 SECOND_TARGET_CLASS。" }.defaultType,
                    isCFunction = true,
                )
                "CLOSURE_FUNCTION" -> buildFunctionType(
                    parameterTypes = listOf(primaryClass.defaultType),
                    returnType = requireNotNull(secondaryClass) { "CLOSURE_FUNCTION 用例必须声明 SECOND_TARGET_CLASS。" }.defaultType,
                    isClosureType = true,
                    hasVariableLengthArgument = true,
                )
                else -> error("Unsupported type creation kind: ${directives.typeCreationKind}")
            }

            assertEquals(
                directives.expectedQualifiedTypeRender,
                normalizeTypeRendering(createdType.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES)),
                "qualified renderer 输出不符合预期。",
            )
            assertEquals(
                directives.expectedShortTypeRender,
                normalizeTypeRendering(createdType.render(CaTypeRendererForSource.WITH_SHORT_NAMES)),
                "short renderer 输出不符合预期。",
            )
        }
    }

    private fun CaSession.assertClassLikeConstruction(
        symbol: CaClassLikeSymbol,
        typeArguments: List<CaType> = emptyList(),
    ): CaClassLikeType {
        val classId = requireNotNull(symbol.classId) {
            "type creator 公开 class-like 构造要求稳定 ClassId：${symbol::class.simpleName}"
        }

        val byClassId = buildClassLikeType(classId, typeArguments)
        val bySymbol = buildClassLikeType(symbol, typeArguments)

        assertEquals(
            normalizeTypeRendering(byClassId.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES)),
            normalizeTypeRendering(bySymbol.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES)),
            "buildClassLikeType(classId) 与 buildClassLikeType(symbol) 结果不一致。",
        )
        assertNotNull(byClassId.classLikeSymbol, "构造后的 class-like type 应可恢复 classLikeSymbol。")
        return byClassId
    }

    private fun CaSession.resolveClassSymbol(mainModule: CjTestModule, className: String): CaClassLikeSymbol {
        val declaration = mainModule.cjFiles.asSequence()
            .flatMap { file -> PsiTreeUtil.findChildrenOfType(file, CjTypeStatement::class.java).asSequence() }
            .singleOrNull { typeStatement -> typeStatement.name == className }
            ?: error("Cannot uniquely locate class declaration `$className` in module `${mainModule.name}`.")

        val classId = requireNotNull(declaration.getClassId()) {
            "type creator generated 测试只接受具备稳定 ClassId 的 class-like 声明：$className"
        }
        return getClassLikeSymbol(classId)
            ?: error("Analysis API 无法恢复 class-like symbol: `${classId.asString()}`")
    }
}
