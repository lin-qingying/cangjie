package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.withCfirSymbolIdEntry
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry
import kotlin.reflect.KClass

internal fun CfirSymbol<*>.errorInLazyResolve(name: String, actualClass: KClass<*>, expected: KClass<*>): Nothing {
    errorWithAttachment("Unexpected $name. Expected is ${expected.simpleName}, but was ${actualClass.simpleName}") {
        withCfirEntry("cfirElement", cfir)
        withCfirSymbolIdEntry("cfirSymbol", this@errorInLazyResolve)
    }
}

