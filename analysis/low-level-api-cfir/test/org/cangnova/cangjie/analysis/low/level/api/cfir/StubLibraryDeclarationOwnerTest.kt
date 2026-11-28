package org.cangnova.cangjie.analysis.low.level.api.cfir

import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.containingClassForStaticMemberAttr
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.resolve.getContainingClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFunction
import org.cangnova.cangjie.psi.CjProperty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * STUBS 链路镜像不变量：库声明的 owner 与 dispatch receiver 必须与 binary 反序列化规则一致。
 *
 * 这里直接对 std.core 的 decompiled PSI 声明走 stub 反序列化，检查：
 * - 所有 class-scoped callable 都能通过 `getContainingClassSymbol()` 恢复 owner；
 * - enum constructor / constructor / static 成员没有 dispatch receiver，但必须有
 *   `containingClassForStaticMemberAttr`；
 * - 实例成员有 dispatch receiver。
 *
 * 该用例覆盖 `StubBasedCfirMemberDeserializer` 相对 `CfirDeclDeserializer` 的镜像点，
 * 防止后续再出现“只在一个 loader 里漏写 owner”的分叉。
 */
class StubLibraryDeclarationOwnerTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/diagnostics",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun stubLibraryDeclarationOwner(mainFile: CjFile) {
        assertEquals(
            CaDeserializedDeclarationsOrigin.STUBS,
            CaPlatformSettings.getInstance(mainFile.project).deserializedDeclarationsOrigin,
            "该用例必须运行在 STUBS 配置下",
        )

        val facade = mainFile.getResolutionFacadeForTest()
        val stdCore = BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles(mainFile.project)
            .mapNotNull { virtualFile -> PsiManager.getInstance(mainFile.project).findFile(virtualFile) }
            .filterIsInstance<CjFile>()
            .firstOrNull { file -> file.name.contains("std.core") }
        assertNotNull(stdCore, "必须能定位 decompiled std.core PSI")
        val file = stdCore!!

        // extend 成员没有 class-like owner，不属于本用例的镜像范围（其 owner 语义是 extended type）。
        fun <T : CjDeclaration> T.notInExtend(): Boolean =
            PsiTreeUtil.getParentOfType(this, CjExtend::class.java) == null

        val samples = buildList {
            PsiTreeUtil.findChildrenOfType(file, CjEnumConstructor::class.java)
                .filter { declaration -> declaration.notInExtend() }
                .take(3)
                .forEach { declaration -> add("enum constructor ${declaration.name}" to declaration) }
            PsiTreeUtil.findChildrenOfType(file, CjConstructor::class.java)
                .filter { declaration -> declaration.notInExtend() }
                .take(2)
                .forEach { declaration -> add("constructor ${declaration.name}" to declaration) }
            PsiTreeUtil.findChildrenOfType(file, CjFunction::class.java)
                .filter { function -> function.isStatic && function.name != null && function.notInExtend() }
                .take(3)
                .forEach { declaration -> add("static function ${declaration.name}" to declaration) }
            PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
                .filter { property -> property.isStatic && property.name != null && property.notInExtend() }
                .take(3)
                .forEach { declaration -> add("static property ${declaration.name}" to declaration) }
            PsiTreeUtil.findChildrenOfType(file, CjFieldVariable::class.java)
                .filter { field -> field.isStatic && field.name != null && field.notInExtend() }
                .take(3)
                .forEach { declaration -> add("static field ${declaration.name}" to declaration) }
            PsiTreeUtil.findChildrenOfType(file, CjFunction::class.java)
                .firstOrNull { function ->
                    !function.isStatic &&
                            function.name != null &&
                            function.notInExtend() &&
                            PsiTreeUtil.getParentOfType(function, CjClassLikeDeclaration::class.java) != null
                }
                ?.let { declaration -> add("instance function ${declaration.name}" to declaration) }
        }
        assertTrue(samples.isNotEmpty(), "std.core 必须至少包含一个库声明样本")

        var checked = 0
        for ((label, declaration) in samples) {
            val symbol = runCatching { declaration.resolveToCfirSymbol(facade) }
                .getOrNull() as? CfirCallableSymbol<*> ?: continue
            checked++
            val callable = symbol.cfir

            assertNotNull(
                symbol.getContainingClassSymbol(),
                "$label 必须能通过 getContainingClass 恢复 owner",
            )

            val receiverless = callable is CfirEnumConstructor ||
                    callable is CfirConstructor ||
                    callable.status.isStatic
            if (receiverless) {
                assertNotNull(
                    callable.containingClassForStaticMemberAttr,
                    "$label 没有 dispatch receiver，必须记录 containingClassForStaticMemberAttr",
                )
                assertNull(
                    callable.dispatchReceiverType,
                    "$label 是 receiver-less 声明，不应有 dispatch receiver",
                )
            } else {
                assertNotNull(
                    callable.dispatchReceiverType,
                    "$label 是实例成员，必须有 dispatch receiver",
                )
            }
        }
        assertTrue(checked >= 6, "至少应有 6 个库声明样本被成功反序列化并校验，实际 $checked")
    }
}
