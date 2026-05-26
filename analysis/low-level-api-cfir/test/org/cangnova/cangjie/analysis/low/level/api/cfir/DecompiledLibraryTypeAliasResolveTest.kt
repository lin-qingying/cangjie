package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 回归真实 `LibraryBinaryDecompiled` 场景下的 source type resolve。
 *
 * 这里显式要求：
 * 1. source 模块直接依赖 `LibraryBinaryDecompiled` 测试模块；
 * 2. source 模块在类型位置引用库里的 `public type`；
 * 3. `getOrBuildCfir()` 不再因为反序列化 type alias 缺少 `scopeProvider` 崩溃。
 *
 * Kotlin 对位的 low-level binary/decompiled 测试不在测试体里检查 binary root 细节，
 * 而是把 binary/decompiled 装配责任交给共享模块工厂和 configurator。
 * 这里保持同样的职责边界，只验证真正需要的依赖关系与解析结果。
 *
 * 注意：low-level `getOrBuildCfir(typeRef)` 的合同是“返回可追溯到同一 source typeRef 的最终 CFIR 节点”，
 * 而不是“保证 typeRef 外壳一定不是 error wrapper”。
 * `CfirElementsRecorder` 明确允许 `ErroneousTypealiasExpansion` / `visitErrorTypeRef(...delegatedTypeRef...)`
 * 这类映射，以便 IDE/Analysis API 仍能从错误外壳里回看到声明侧 alias 名称。
 * 因此这里锁定的真实回归点是：不能崩，并且必须保留 `delegatedTypeRef -> RemoteAlias`。
 */
class DecompiledLibraryTypeAliasResolveTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/decompiledLibraries",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun decompiledTypeAliasReference(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val libraryModule = testServices.cjTestModuleStructure.getModule("lib")
        assertTrue(
            libraryModule.caModule in mainModule.caModule.directRegularDependencies,
            "主模块必须直接依赖 decompiled library 模块，才能覆盖真实 stub-based library provider 路径。",
        )

        val typeReference = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjTypeReference>(mainFile)
        val resolvedTypeRef = typeReference.getOrBuildCfir(mainFile.getResolutionFacadeForTest()) as? CfirResolvedTypeRef

        assertNotNull(
            resolvedTypeRef,
            "source 函数头中的 library type alias 应该被正常解析成 CfirResolvedTypeRef。",
        )
        val resolvedType = requireNotNull(resolvedTypeRef)
        if (resolvedType is CfirErrorTypeRef || resolvedType.coneType is ConeErrorType) {
            assertTrue(
                resolvedType.delegatedTypeRef != null,
                "即使 low-level typeRef 外壳仍是 error wrapper，也必须保留 delegatedTypeRef 让上层恢复 alias 语义。",
            )
        }
        val declaredTypeRef = resolvedType.delegatedTypeRef
        assertNotNull(
            declaredTypeRef,
            "resolved type ref 必须保留声明侧 delegatedTypeRef，避免测试只验证“解析不崩”而看不到 typealias 真相。",
        )
        val declaredUserTypeRef = declaredTypeRef as? CfirUserTypeRef
        assertNotNull(
            declaredUserTypeRef,
            "真实 library typealias 的声明侧类型应继续保留为 CfirUserTypeRef。",
        )
        assertEquals(
            "RemoteAlias",
            declaredUserTypeRef?.qualifier?.lastOrNull()?.name?.asString(),
            "delegatedTypeRef 必须继续指向被声明的 typealias 名称，而不是被提前抹平成展开类型。",
        )
    }
}
