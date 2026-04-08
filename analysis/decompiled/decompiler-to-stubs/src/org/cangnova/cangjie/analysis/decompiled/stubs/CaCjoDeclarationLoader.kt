package org.cangnova.cangjie.analysis.decompiled.stubs

import org.cangnova.cangjie.analysis.decompiled.filestubs.CaLoadedCjoPackage
import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeclDeserializer
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirTypeDeserializer
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.name.Name
import java.io.File

/**
 * `.cjo` -> CFIR declaration 列表的共享加载器。
 *
 * decompiler-to-stubs 与 decompiler-to-psi 都依赖这条管线，
 * 因此把模块数据、session 与索引解包策略集中在这里，避免两处漂移。
 */
object CaCjoDeclarationLoader {
    fun loadDeclarations(loadedPackage: CaLoadedCjoPackage): List<CfirDeclaration> {
        val cjoManager = CjoManager(
            CjoSearchPath { key ->
                when (key) {
                    "CANGJIE_LIBRARY", "CANGJIE_STDLIB_MODULE" ->
                        loadedPackage.searchRoots.joinToString(File.pathSeparator) { it.absolutePath }
                    else -> null
                }
            },
        )
        val context = CfirDeserializationContext(
            pkg = loadedPackage.pkg,
            header = loadedPackage.header,
            moduleData = DecompiledModuleData,
            cjoManager = cjoManager,
        )
        val typeDeserializer = CfirTypeDeserializer(context)
        val declDeserializer = CfirDeclDeserializer(context, typeDeserializer)
        val declarationIndices = buildList {
            loadedPackage.header.topLevelNameToIndices.values.forEach(::addAll)
            addAll(loadedPackage.header.topLevelExtendIndices)
        }.distinct().sorted()
        return declarationIndices.mapNotNull(declDeserializer::deserializeDecl)
    }

    private object DecompiledSession : CfirSession(Kind.Library) {
        override fun toString(): String = "CaDecompiledDeclarationLoaderSession"
    }

    private object DecompiledModuleData : CfirModuleData() {
        override val name: Name = Name.identifier("analysis-decompiled")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        override val isCommon: Boolean = true
        override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
        override val stableModuleName: String = "analysis-decompiled"
        override val session: CfirSession
            get() = DecompiledSession

        init {
            bindSession(DecompiledSession)
        }
    }
}
