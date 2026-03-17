package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeclDeserializer
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirTypeDeserializer
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import java.util.concurrent.ConcurrentHashMap

/**
 * 基于 .cjo 反序列化的符号提供者。
 *
 * 从 .cjo 文件按需加载外部包的符号声明。
 * 三级缓存：packageContext → classSymbol → callableSymbols。
 */
class CfirDeserializedSymbolProvider(
    session: CfirSession,
    private val cjoManager: CjoManager,
    private val libraryModuleData: CfirModuleData,
) : CfirSymbolProvider(session) {

    override val symbolNamesProvider: CfirSymbolNamesProvider =
        CfirDeserializedSymbolNamesProvider(cjoManager)

    /** 包名 → 反序列化上下文缓存 */
    private val contextCache = ConcurrentHashMap<String, PackageDeserializers>()

    /** ClassId → CfirClassSymbol 缓存 */
    private val classCache = ConcurrentHashMap<ClassId, CfirClassSymbol?>()

    /** (packageFqName, name) → List<CfirCallableSymbol> 缓存 */
    private val callableCache = ConcurrentHashMap<Pair<FqName, Name>, List<CfirCallableSymbol<*>>>()

    override fun hasPackage(fqName: FqName): Boolean {
        return cjoManager.hasPackage(fqName)
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassSymbol? {
        return classCache.getOrPut(classId) {
            val pkgName = classId.packageFqName.asString()
            val deserializers = getOrCreateDeserializers(pkgName) ?: return@getOrPut null
            val header = deserializers.header
            val shortName = classId.shortClassName.asString()

            // 在 topLevelNameToIndices 中查找匹配的类声明
            val indices = header.topLevelNameToIndices[shortName] ?: return@getOrPut null
            for (declIndex in indices) {
                val decl = deserializers.declDeserializer.deserializeDecl(declIndex)
                if (decl is CfirClass && decl.name.asString() == shortName) {
                    return@getOrPut decl.symbol as CfirClassSymbol
                }
            }
            null
        }
    }

    override fun getTopLevelCallableSymbols(
        packageFqName: FqName,
        name: Name,
    ): List<CfirCallableSymbol<*>> {
        val key = packageFqName to name
        return callableCache.getOrPut(key) {
            val pkgName = packageFqName.asString()
            val deserializers = getOrCreateDeserializers(pkgName) ?: return@getOrPut emptyList()
            val header = deserializers.header
            val nameStr = name.asString()

            val indices = header.topLevelNameToIndices[nameStr] ?: return@getOrPut emptyList()
            indices.mapNotNull { declIndex ->
                val decl = deserializers.declDeserializer.deserializeDecl(declIndex)
                (decl as? CfirCallableDeclaration)?.symbol as? CfirCallableSymbol<*>
            }
        }
    }

    /** 获取或创建指定包的反序列化器组 */
    private fun getOrCreateDeserializers(fullPkgName: String): PackageDeserializers? {
        return contextCache.getOrPut(fullPkgName) {
            val header = cjoManager.loadPackageHeader(fullPkgName) ?: return null
            val pkg = cjoManager.loadPackage(fullPkgName) ?: return null
            val context = CfirDeserializationContext(
                pkg = pkg,
                header = header,
                moduleData = libraryModuleData,
            )
            val typeDeserializer = CfirTypeDeserializer(context)
            val declDeserializer = CfirDeclDeserializer(context, typeDeserializer)
            PackageDeserializers(header, typeDeserializer, declDeserializer)
        }
    }

    /** 包级反序列化器组 */
    private class PackageDeserializers(
        val header: CjoPackageHeader,
        @Suppress("unused") val typeDeserializer: CfirTypeDeserializer,
        val declDeserializer: CfirDeclDeserializer,
    )
}
