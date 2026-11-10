@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.StubBasedAnnotationDeserializer
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.StubBasedCfirDeserializationContext
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.StubBasedCfirExtendBuilder
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.StubBasedCfirTypeDeserializer
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.extensionSignatureKey
import org.cangnova.cangjie.cfir.declarations.normalizeCfirExtendTypeText
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjExtend
import java.util.concurrent.ConcurrentHashMap

/**
 * 为 `.cjo` 反序列化 extend 物化带 PSI 的 CFIR extend。
 *
 * `.cjo` 声明的 `source` 恒为 null，快速文档/转到声明没有目标；这里在包内 decompiled PSI
 * 中按「接收者 + 父类型」结构签名找到对应 `CjExtend`，再交给
 * [StubBasedCfirExtendBuilder] 生成带 PSI 的声明（类型引用与 `T` 复用二进制结果）。
 */
internal class LLExtendPsiMaterializer(
    private val project: Project,
    private val packageOf: (CfirExtend) -> FqName?,
) {
    /** 按 module 缓存的 declaration provider。 */
    private val declarationProviders = ConcurrentHashMap<org.cangnova.cangjie.analysis.api.projectStructure.CaModule, CangJieDeclarationProvider>()

    /** 按 (module, 包) 缓存的「结构签名 -> CjExtend」。 */
    private val psiExtendsByPackage =
        ConcurrentHashMap<Pair<org.cangnova.cangjie.analysis.api.projectStructure.CaModule, FqName>, Map<String, java.util.ArrayDeque<CjExtend>>>()

    /** 按二进制 extend 缓存的物化结果。 */
    private val materialized = ConcurrentHashMap<CfirExtend, CfirExtend>()
    private val failedExtends = java.util.concurrent.ConcurrentHashMap.newKeySet<CfirExtend>()

    /**
     * 物化 [binaryExtend]；找不到对应 PSI 时返回 null（调用方保留二进制声明）。
     */
    @Synchronized
    fun materialize(binaryExtend: CfirExtend): CfirExtend? {
        if (binaryExtend.source != null) return binaryExtend
        if (binaryExtend in failedExtends) return null
        materialized[binaryExtend]?.let { return it }
        val packageFqName = packageOf(binaryExtend) ?: return null
        val ownerSession = binaryExtend.moduleData.session as? LLCfirSession ?: return null
        val ownerModule = ownerSession.moduleData.caModule
        val psi = psiExtends(ownerModule, packageFqName)[LLExtendStructuralKeys.of(binaryExtend)]
            ?.pollFirst() ?: return null
        return try {
            val context = createContext(ownerSession, packageFqName)
            val built = StubBasedCfirExtendBuilder(context).build(psi, binaryExtend)
            materialized[binaryExtend] = built
            built
        } catch (exception: RuntimeException) {
            // decompiled PSI 与 `.cjo` 存在差异时，单条 extend 物化失败不能炸整个 session 查询；
            // 失败项回填 PSI 并记录，避免重复消费/重试抖动。
            psiExtends(ownerModule, packageFqName)[LLExtendStructuralKeys.of(binaryExtend)]?.addFirst(psi)
            failedExtends += binaryExtend
            log.warn("Cannot materialize PSI for extend ${LLExtendStructuralKeys.of(binaryExtend)}", exception)
            null
        }
    }

    /**
     * 返回 [ownerModule] 内 [packageFqName] 包中 `CjExtend` 的完整结构键索引。
     *
     * 同一 key 可能有多个 PSI 声明（例如同类型上不同成员集的 extend），以队列保存并逐个消费。
     */
    private fun psiExtends(
        ownerModule: org.cangnova.cangjie.analysis.api.projectStructure.CaModule,
        packageFqName: FqName,
    ): Map<String, java.util.ArrayDeque<CjExtend>> =
        psiExtendsByPackage.computeIfAbsent(ownerModule to packageFqName) { (module, packageName) ->
            val declarationProvider = declarationProviders.computeIfAbsent(module) { targetModule ->
                CangJieDeclarationProviderFactory.getInstance(project)
                    .createDeclarationProvider(targetModule.contentScope, targetModule)
            }
            val result = LinkedHashMap<String, java.util.ArrayDeque<CjExtend>>()
            declarationProvider.getPackageFiles(packageName).forEach { file ->
                file.declarations.filterIsInstance<CjExtend>().forEach { extend ->
                    result.getOrPut(LLExtendStructuralKeys.of(extend)) { java.util.ArrayDeque() }.add(extend)
                }
            }
            result
        }

    /**
     * 为 [ownerSession] 创建 stub 反序列化上下文。
     */
    private fun createContext(
        ownerSession: LLCfirSession,
        packageFqName: FqName,
    ): StubBasedCfirDeserializationContext {
        val moduleData = ownerSession.moduleData
        val annotationDeserializer = StubBasedAnnotationDeserializer(ownerSession)
        return StubBasedCfirDeserializationContext(
            moduleData = moduleData,
            packageFqName = packageFqName,
            relativeClassName = null,
            typeDeserializer = StubBasedCfirTypeDeserializer(
                moduleData,
                annotationDeserializer,
                parent = null,
                containingSymbol = null,
                owner = null,
                initialOrigin = CfirDeclarationOrigin.Library,
            ),
            annotationDeserializer = annotationDeserializer,
            containerSource = null,
            outerClassSymbol = null,
            outerTypeParameters = emptyList(),
            initialOrigin = CfirDeclarationOrigin.Library,
        )
    }

    private val log = com.intellij.openapi.diagnostic.logger<LLExtendPsiMaterializer>()
}
