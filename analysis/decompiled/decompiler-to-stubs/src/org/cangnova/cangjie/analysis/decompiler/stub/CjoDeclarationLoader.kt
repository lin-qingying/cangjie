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
    /**
     * 将已加载的 `.cjo` package 反序列化为顶层 CFIR 声明列表。
     *
     * 该方法按 package header 中记录的顶层声明索引和 extend 索引读取声明，并使用外部传入的
     * [moduleData] 作为 owner，确保反编译 stub 与真实项目模块结构保持一致。
     */
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
