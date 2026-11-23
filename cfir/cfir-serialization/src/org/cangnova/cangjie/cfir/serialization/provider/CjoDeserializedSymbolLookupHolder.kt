package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeclarationMaterialization
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/** 快照缓存归 manager；metadata 服务及其声明缓存按不可变 binding 隔离。 */
class CjoDeserializedSymbolLookupHolder(private val cjoManager: CjoManager) {
    private data class LookupKey(val binding: CjoDeserializationBinding, val module: CfirModuleData, val mode: CjoDeserializationMode)
    private data class ContextKey(
        val binding: CjoDeserializationBinding,
        val module: CfirModuleData,
        val snapshot: Any,
        val sourcePath: Path?,
        val mode: CjoDeserializationMode,
    )
    private val bindings = ConcurrentHashMap.newKeySet<CjoDeserializationBinding>()
    private val lookups = ConcurrentHashMap<LookupKey, CfirDeserializedSymbolProvider>()
    private val contexts = ConcurrentHashMap<ContextKey, CfirDeserializationContext>()

    /** 仅完整构造的原 named producer 发布装配；派生 provider 不递归注册 computeIfAbsent。 */
    internal fun register(binding: CjoDeserializationBinding, mode: CjoDeserializationMode, provider: CfirDeserializedSymbolProvider) {
        check(binding.dependencyManager === cjoManager)
        val previous = lookups.putIfAbsent(LookupKey(binding, binding.libraryModuleData, mode), provider)
        check(previous == null || previous === provider) { "Duplicate named CJO producer for binding" }
        if (binding.ownManager !== cjoManager) binding.ownManager.deserializedSymbolLookups.attach(binding)
        bindings.add(binding)
    }

    private fun attach(binding: CjoDeserializationBinding) {
        check(binding.ownManager === cjoManager)
        bindings.add(binding)
    }

    /** 旧入口只查既有的唯一装配；缺失与歧义均拒绝，不构造 Single/default owner。 */
    fun bindingForModule(moduleData: CfirModuleData): CjoDeserializationBinding {
        val matches = bindings.filter { moduleData in it.moduleDataProvider.allModuleData }
        check(matches.size == 1) { "Expected one registered CJO binding for $moduleData, found " + matches.size }
        return matches.single()
    }

    fun forModuleData(moduleData: CfirModuleData, mode: CjoDeserializationMode = CjoDeserializationMode.FULL): CfirDeserializedSymbolProvider =
        forBinding(bindingForModule(moduleData), moduleData, mode)

    fun forBinding(binding: CjoDeserializationBinding, moduleData: CfirModuleData, mode: CjoDeserializationMode): CfirDeserializedSymbolProvider {
        if (binding.dependencyManager !== cjoManager) {
            check(binding.ownManager === cjoManager)
            return binding.dependencyManager.deserializedSymbolLookups.forBinding(binding, moduleData, mode)
        }
        check(moduleData in binding.moduleDataProvider.allModuleData)
        return lookups.computeIfAbsent(LookupKey(binding, moduleData, mode)) {
            CfirDeserializedSymbolProvider(
                session = binding.session,
                cjoManager = binding.ownManager,
                cangjieScopeProvider = binding.scopeProvider,
                libraryModuleData = binding.libraryModuleData,
                moduleDataProvider = binding.moduleDataProvider,
                dependencyCjoManager = binding.dependencyManager,
                deserializationMode = mode,
                binding = binding,
            )
        }
    }

    internal fun canonical(context: CfirDeserializationContext): CfirDeserializationContext {
        check(context.binding.dependencyManager === cjoManager && context.binding in bindings) {
            "CJO canonical context requires its registered original producer"
        }
        context.requireAuthority()
        val key = ContextKey(context.binding, context.moduleData, context.snapshotIdentity,
            context.sourcePath?.toAbsolutePath()?.normalize(), context.mode)
        return checkNotNull(contexts.compute(key) { _, previous ->
            if (previous == null) context else {
                previous.requireAuthority()
                require(previous.header === context.header && previous.packageContextProvider === context.packageContextProvider &&
                    previous.implicitSystemAnnotations == context.implicitSystemAnnotations) {
                    "CJO canonical contexts have different original services or headers"
                }
                previous
            }
        })
    }

    internal fun materialization(): CjoDeclarationMaterialization = cjoManager.declarationMaterialization
}
