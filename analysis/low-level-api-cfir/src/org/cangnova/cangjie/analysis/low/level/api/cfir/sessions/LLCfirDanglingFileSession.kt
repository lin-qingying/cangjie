

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.cfir.BuiltinTypes
import org.cangnova.cangjie.cfir.PrivateSessionConstructor

internal class LLCfirDanglingFileSession @PrivateSessionConstructor constructor(
    ktModule: CaDanglingFileModule,
    override val moduleComponents: LLCfirModuleResolveComponents,
    builtinTypes: BuiltinTypes
) : LLCfirResolvableModuleSession(ktModule, builtinTypes) {
    private val cachedModificationStamp: Long = ktModule.modificationStamp

    val hasFileModifications: Boolean
        get() {
            val ktModule = this.ktModule as CaDanglingFileModule
            return cachedModificationStamp != ktModule.modificationStamp
        }
}

private val CaDanglingFileModule.modificationStamp: Long
    get() = files.sumOf { it.modificationStamp }