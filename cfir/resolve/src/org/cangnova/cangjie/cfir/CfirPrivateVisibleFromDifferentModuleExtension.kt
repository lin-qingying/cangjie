package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

/**
 * 对位 Kotlin `FirPrivateVisibleFromDifferentModuleExtension`。
 *
 * 该扩展点用于让 IDE/low-level 在特殊分析场景下放宽 private 可见性边界，
 * 例如 dangling file 需要看见其上下文文件中的 private 顶层声明。
 */
abstract class CfirPrivateVisibleFromDifferentModuleExtension : CfirSessionComponent {
    /** 判断当前 session 是否可以读取 [otherModuleData] 中的 private 声明。 */
    abstract fun canSeePrivateDeclarationsOfModule(otherModuleData: CfirModuleData): Boolean

    /** 判断 [useSiteFile] 是否可以读取 [targetFile] 中的 private 顶层声明。 */
    abstract fun canSeePrivateTopLevelDeclarationsFromFile(useSiteFile: CfirFile, targetFile: CfirFile): Boolean
}

/** 当前 session 注册的跨模块 private 可见性扩展；未注册时为 null。 */
val CfirSession.privateVisibleFromDifferentModulesExtension: CfirPrivateVisibleFromDifferentModuleExtension?
    by CfirSession.nullableSessionComponentAccessor()
