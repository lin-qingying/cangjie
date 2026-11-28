package org.cangnova.cangjie.test

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.deserialization.LibraryPathFilter
import org.cangnova.cangjie.cfir.deserialization.ModuleDataProvider
import org.cangnova.cangjie.cfir.deserialization.MultipleModuleDataProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.serialization.provider.CjoDeserializationBinding
import java.nio.file.Files
import java.nio.file.Path

/**
 * fixture 的初始 owner 装配：磁盘 roots 必须由调用方明确给出。
 * 空 roots 只表达纯内存声明池，不认领任何磁盘路径，也不把虚拟文件名转换成来源。
 */
fun cjoFixtureModuleDataProvider(moduleData: CfirModuleData, ownedRoots: Set<Path>): ModuleDataProvider {
    val roots = ownedRoots.mapTo(linkedSetOf()) { it.toAbsolutePath().normalize() }
    require(roots.all(Files::exists)) { "CJO fixture ownership requires actual files or directories" }
    return MultipleModuleDataProvider(
        mapOf(moduleData to LibraryPathFilter.LibraryList(roots)), moduleData,
    )
}

/** 只在初始装配处创建原 named producer；lookup 之后必须消费这个明确 binding。 */
fun bindCjoFixture(
    moduleData: CfirModuleData,
    manager: CjoManager,
    ownedRoots: Set<Path>,
): CjoDeserializationBinding =
    CfirDeserializedSymbolProvider(
        session = moduleData.session,
        cjoManager = manager,
        cangjieScopeProvider = CfirCangJieScopeProvider(),
        libraryModuleData = moduleData,
        moduleDataProvider = cjoFixtureModuleDataProvider(moduleData, ownedRoots),
    ).deserializationBinding
