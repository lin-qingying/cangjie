

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.cfir.PrivateSessionConstructor
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes

/**
 * dangling file 使用的可解析 low-level CFIR session。
 *
 * 该 session 绑定临时 PSI 文件集合，并通过文件修改戳检测创建后是否发生过文件内容变更。
 */
internal class LLCfirDanglingFileSession @PrivateSessionConstructor constructor(
    caModule: CaDanglingFileModule,

    /**
     * dangling session 的模块解析组件。
     */
    override val moduleComponents: LLCfirModuleResolveComponents,
    builtinTypes: CfirBuiltinTypes
) : LLCfirResolvableModuleSession(caModule, builtinTypes) {
    /**
     * session 创建时 dangling 文件集合的修改戳快照。
     */
    private val cachedModificationStamp: Long = caModule.modificationStamp

    /**
     * dangling 文件集合自 session 创建后是否发生过内容变更。
     */
    val hasFileModifications: Boolean
        get() {
            val danglingFileModule = this.caModule as CaDanglingFileModule
            return cachedModificationStamp != danglingFileModule.modificationStamp
    }
}

/**
 * dangling module 中所有 PSI 文件修改戳的聚合值。
 */
private val CaDanglingFileModule.modificationStamp: Long
    get() = files.sumOf { it.modificationStamp }
