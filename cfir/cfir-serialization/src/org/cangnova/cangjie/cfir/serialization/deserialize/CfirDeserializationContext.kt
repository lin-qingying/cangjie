package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.Package
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import java.util.concurrent.ConcurrentHashMap

/**
 * 反序列化上下文。
 *
 * 持有 FlatBuffers Package 对象（零拷贝引用 ByteBuffer），
 * 以及类型和声明的缓存，避免重复反序列化。
 */
class CfirDeserializationContext(
    /** FlatBuffers Package 对象 */
    val pkg: Package,
    /** 包头信息 */
    val header: CjoPackageHeader,
    /** 库模块元数据 */
    val moduleData: CfirModuleData,
) {
    /** allTypes 索引 → ConeCangjieType 缓存 */
    val typeCache = ConcurrentHashMap<Int, ConeCangjieType>()

    /** allDecls 索引 → CfirDeclaration 缓存 */
    val declCache = ConcurrentHashMap<Int, CfirDeclaration>()

    /** 导入包的 Package 对象缓存（pkgId-1 → Package） */
    val importedPackages = ConcurrentHashMap<Int, Package>()
}
