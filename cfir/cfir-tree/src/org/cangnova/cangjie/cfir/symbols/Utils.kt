package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.withCfirSymbolIdEntry
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry
import kotlin.reflect.KClass

/**
 * 抛出 lazy resolve 阶段中的类型不变量错误。
 *
 * 错误会附带当前 CFIR 元素和符号 id，便于定位哪个 symbol 在目标阶段后仍未形成期望节点类型。
 */
internal fun CfirBasedSymbol<*>.errorInLazyResolve(name: String, actualClass: KClass<*>, expected: KClass<*>): Nothing {
    errorWithAttachment("Unexpected $name. Expected is ${expected.simpleName}, but was ${actualClass.simpleName}") {
        withCfirEntry("cfirElement", cfir)
        withCfirSymbolIdEntry("cfirSymbol", this@errorInLazyResolve)
        withEntry("resolvePhase", cfir.resolvePhase.toString())
        withEntry("origin", cfir.origin.toString())
        withEntry("sessionClass", cfir.moduleData.session::class.java.name)
        if (this@errorInLazyResolve is CfirValueParameterSymbol) {
            val owner = this@errorInLazyResolve.containingDeclarationSymbol
            withCfirEntry("parameterOwner", owner.cfir)
            withCfirSymbolIdEntry("parameterOwnerSymbol", owner)
            withEntry("parameterOwnerPhase", owner.cfir.resolvePhase.toString())
            withEntry("parameterOwnerSessionClass", owner.cfir.moduleData.session::class.java.name)
            withEntry("sameOwnerModuleData", (owner.cfir.moduleData === cfir.moduleData).toString())
        }
    }
}
