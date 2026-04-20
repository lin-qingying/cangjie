package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

interface CaDefaultImportProvider : CaLifetimeOwner {
    val defaultImports: CaDefaultImports
}
