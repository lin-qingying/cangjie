package org.cangnova.cangjie.analysis.api.impl.base.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjTypeStatement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * type 相关 generated 测试的公共辅助。
 *
 * 这一层只服务公开 Analysis API 契约：
 * 1. 通过测试模块里的源码声明恢复稳定 `CaClassLikeSymbol`
 * 2. 基于公开 `CaTypeCreator` 构造各种 `CaType`
 * 3. 在需要时校验 `buildClassType(classId)` 与 `buildClassType(symbol)` 一致
 */
internal object AnalysisApiTypeTestSupport {
    /**
     * 从测试模块源码中按名称恢复稳定的 class-like symbol。
     *
     * 该函数先在 PSI 中定位唯一 `CjTypeStatement`，再通过声明的 `ClassId` 进入公开 Analysis API
     * 查询，确保后续类型构造不直接依赖 PSI 实现细节。
     */
    context(session: CaSession)
    fun resolveClassSymbol(mainModule: CjTestModule, className: String): CaClassLikeSymbol {
        val declaration = mainModule.cjFiles.asSequence()
            .flatMap { file -> PsiTreeUtil.findChildrenOfType(file, CjTypeStatement::class.java).asSequence() }
            .singleOrNull { typeStatement -> typeStatement.name == className }
            ?: error("Cannot uniquely locate class declaration `$className` in module `${mainModule.name}`.")

        val classId = requireNotNull(declaration.getClassId()) {
            "type generated 测试只接受具备稳定 ClassId 的 class-like 声明：$className"
        }
        return with(session) { getClassLikeSymbol(classId) }
            ?: error("Analysis API 无法恢复 class-like symbol: `${classId.asString()}`")
    }

    /**
     * 根据测试指令描述构造公开 `CaType` 实例。
     *
     * 该函数覆盖 type creator 和 type relation 测试共同需要的类型形态，包括 class-like、
     * 泛型类、tuple、交并类型、函数类型、变长数组和类型参数类型。
     */
    context(session: CaSession)
    fun buildType(
        kind: String,
        primaryClass: CaClassLikeSymbol,
        secondaryClass: CaClassLikeSymbol? = null,
        containerClass: CaClassLikeSymbol? = null,
        typeParameterOwnerClass: CaClassLikeSymbol? = null,
        targetTypeParameterName: String? = null,
    ): CaType {
        return when (kind) {
            "CLASS" -> assertClassLikeConstruction(primaryClass)
            "GENERIC_CLASS" -> {
                val container = requireNotNull(containerClass) { "GENERIC_CLASS 用例必须声明容器类。" }
                assertClassLikeConstruction(container, listOf(with(session) { primaryClass.defaultType }))
            }
            "TUPLE" -> with(session) {
                buildTupleType(
                    listOf(
                        primaryClass.defaultType,
                        requireNotNull(secondaryClass) { "TUPLE 用例必须声明第二个类。" }.defaultType,
                    ),
                )
            }
            "INTERSECTION" -> with(session) {
                buildIntersectionType(
                    listOf(
                        primaryClass.defaultType,
                        requireNotNull(secondaryClass) { "INTERSECTION 用例必须声明第二个类。" }.defaultType,
                    ),
                )
            }
            "UNION" -> with(session) {
                buildUnionType(
                    listOf(
                        primaryClass.defaultType,
                        requireNotNull(secondaryClass) { "UNION 用例必须声明第二个类。" }.defaultType,
                    ),
                )
            }
            "FUNCTION" -> with(session) {
                buildFunctionType(
                    parameterTypes = listOf(primaryClass.defaultType),
                    returnType = requireNotNull(secondaryClass) { "FUNCTION 用例必须声明返回类型类。" }.defaultType,
                )
            }
            "C_FUNCTION" -> with(session) {
                buildFunctionType(
                    parameterTypes = listOf(primaryClass.defaultType),
                    returnType = requireNotNull(secondaryClass) { "C_FUNCTION 用例必须声明返回类型类。" }.defaultType,
                    isCFunction = true,
                )
            }
            "CLOSURE_FUNCTION" -> with(session) {
                buildFunctionType(
                    parameterTypes = listOf(primaryClass.defaultType),
                    returnType = requireNotNull(secondaryClass) { "CLOSURE_FUNCTION 用例必须声明返回类型类。" }.defaultType,
                    isClosureType = true,
                    hasVariableLengthArgument = true,
                )
            }
            "VARARG_ARRAY" -> with(session) {
                buildVarargArrayType(primaryClass.defaultType)
            }
            "TYPE_PARAMETER" -> with(session) {
                val owner = requireNotNull(typeParameterOwnerClass) { "TYPE_PARAMETER 用例必须声明类型参数 owner 类。" }
                val typeParameterName = requireNotNull(targetTypeParameterName) { "TYPE_PARAMETER 用例必须声明目标类型参数名。" }
                val typeParameterSymbol = owner.typeParameters.singleOrNull { typeParameter ->
                    typeParameter.name.asString() == typeParameterName
                } ?: error("Owner `${owner.classId?.asString()}` does not have type parameter `$typeParameterName`.")

                buildTypeParameterType(typeParameterSymbol)
            }
            else -> error("Unsupported test type kind: $kind")
        }
    }

    /**
     * 构造 class-like 类型并校验 ClassId 与 symbol 两条公开入口的一致性。
     *
     * 类型构造测试不仅需要得到一个类型，还要保证 `buildClassType(classId)` 与
     * `buildClassType(symbol)` 在 qualified 渲染和 symbol 恢复上保持同一语义结果。
     */
    context(session: CaSession)
    private fun assertClassLikeConstruction(
        symbol: CaClassLikeSymbol,
        typeArguments: List<CaType> = emptyList(),
    ): CaClassLikeType {
        val classId = requireNotNull(symbol.classId) {
            "class-like type 构造要求稳定 ClassId：${symbol::class.simpleName}"
        }

        val byClassId = with(session) {
            buildClassType(classId) {
                typeArguments.forEach { argument(it) }
            }
        } as CaClassLikeType
        val bySymbol = with(session) {
            buildClassType(symbol) {
                typeArguments.forEach { argument(it) }
            }
        } as CaClassLikeType

        assertEquals(
            normalizeTypeRender(with(session) { byClassId.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES) }),
            normalizeTypeRender(with(session) { bySymbol.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES) }),
            "buildClassType(classId) 与 buildClassType(symbol) 结果不一致。",
        )
        assertNotNull(byClassId.symbol, "构造后的 class-like type 应可恢复 symbol。")
        return byClassId
    }

    /**
     * 归一化类型渲染中的路径分隔符。
     *
     * 部分类型渲染会从内部名字携带 `/` 分隔符，测试期望使用点语义上的点号形式，
     * 因此在比较前统一转换为 `.`。
     */
    private fun normalizeTypeRender(rendered: String): String {
        return rendered.replace('/', '.')
    }
}
