

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.extensions.ProjectExtensionDescriptor

interface LLCfirSessionConfigurator {
    companion object : ProjectExtensionDescriptor<LLCfirSessionConfigurator>(
        "org.cangnova.cangjie.llCfirSessionConfigurator",
        LLCfirSessionConfigurator::class.java
    )

    fun configure(session: LLCfirSession)
}