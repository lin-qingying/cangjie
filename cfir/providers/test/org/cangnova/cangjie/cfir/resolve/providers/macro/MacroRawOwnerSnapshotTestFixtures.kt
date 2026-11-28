package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.name.FqName

/** 纯 token/路由 fixture 没有 CFIR 声明宿主，显式采用调用包的顶层词法语境。 */
internal fun topLevelMacroOwnerSnapshot(packageFqName: FqName): MacroRawOwnerSnapshot = MacroRawOwnerSnapshot(
    packageFqName = packageFqName,
    containerSymbols = emptyList(),
    dispatchReceiverType = null,
    inLocalContext = false,
    functionTarget = null,
    signature = null,
    nominalSyntax = null,
    capturedTypeParameters = emptyList(),
    constructedClasses = emptyList(),
)
