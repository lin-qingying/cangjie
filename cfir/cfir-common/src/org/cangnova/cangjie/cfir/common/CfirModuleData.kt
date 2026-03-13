package org.cangnova.cangjie.cfir.common

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.name.Name

/**
 * 模块元数据，标识 CFIR 节点所属的编译模块。
 */
class CfirModuleData(
    val name: Name,
    val platform: CfirPlatform = CfirPlatform.DEFAULT,
    val dependencies: List<CfirModuleData> = emptyList(),
) : CfirSessionComponent {
    override fun toString(): String = "CfirModuleData($name)"
}

/**
 * 编译目标平台。
 */
enum class CfirPlatform {
    DEFAULT,
    OHOS,       // HarmonyOS/OpenHarmony
    LINUX,
    WINDOWS,
    MACOS,
}



val CfirSession.nullableModuleData: CfirModuleData? by CfirSession.nullableSessionComponentAccessor()

val CfirSession.moduleData: CfirModuleData
    get() = nullableModuleData ?: error("Module data is not registered in $this")
