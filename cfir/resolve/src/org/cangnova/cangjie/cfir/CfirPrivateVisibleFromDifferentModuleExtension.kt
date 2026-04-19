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
    abstract fun canSeePrivateDeclarationsOfModule(otherModuleData: CfirModuleData): Boolean

    abstract fun canSeePrivateTopLevelDeclarationsFromFile(useSiteFile: CfirFile, targetFile: CfirFile): Boolean
}

val CfirSession.privateVisibleFromDifferentModulesExtension: CfirPrivateVisibleFromDifferentModuleExtension?
    by CfirSession.nullableSessionComponentAccessor()
