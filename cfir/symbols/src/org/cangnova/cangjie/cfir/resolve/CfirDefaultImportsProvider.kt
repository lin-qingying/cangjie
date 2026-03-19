package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.ImportPath

/**
 * Default imports provider for Cangjie frontend sessions.
 *
 * Language-level defaults are defined in [DefaultImportsProvider].
 * Platform-specific defaults are currently empty for the unified Cangjie pipeline.
 */
object CfirDefaultImportsProvider : DefaultImportsProvider() {
    override val platformSpecificDefaultImports: List<ImportPath> = emptyList()
}

