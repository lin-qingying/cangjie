

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.cfir.PrivateSessionConstructor
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes

internal class LLCfirDanglingFileSession @PrivateSessionConstructor constructor(
    caModule: CaDanglingFileModule,
    override val moduleComponents: LLCfirModuleResolveComponents,
    builtinTypes: CfirBuiltinTypes
) : LLCfirResolvableModuleSession(caModule, builtinTypes) {
    private val cachedModificationStamp: Long = caModule.modificationStamp

    val hasFileModifications: Boolean
        get() {
            val danglingFileModule = this.caModule as CaDanglingFileModule
            return cachedModificationStamp != danglingFileModule.modificationStamp
        }
}

private val CaDanglingFileModule.modificationStamp: Long
    get() = files.sumOf { it.modificationStamp }
