package org.cangnova.cangjie.analysis.decompiler.stub

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeclDeserializer
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirTypeDeserializer
import java.io.File

/**
 * `.cjo` -> CFIR declaration 列表的共享加载器。
 *
 * 这里只保留 `.cjo` 解包与反序列化本身。
 * `moduleData/session` 的 owner 必须来自外部既有框架：
 * - Analysis/IDE 路径走 LL session factory / session cache；
 * - 反编译文本路径只通过 compiled stub -> decompiled PSI -> text builder。
 */
object CjoDeclarationLoader {
    fun loadDeclarations(
        loadedPackage: LoadedCjoPackage,
        moduleData: CfirModuleData,
    ): List<CfirDeclaration> {
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
            moduleData = moduleData,
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
}
