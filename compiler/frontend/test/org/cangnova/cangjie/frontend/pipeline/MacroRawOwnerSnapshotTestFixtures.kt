package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawOwnerSnapshot
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawTypeParameterScope
import org.cangnova.cangjie.name.FqName

/** 顶层声明及没有 CFIR 声明宿主的 token/mock fixture 显式使用调用包的顶层词法语境。 */
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

/** 函数体 fixture 保存实际宿主符号，并将 return target 绑定到同一函数对象。 */
internal fun functionBodyMacroOwnerSnapshot(
    packageFqName: FqName,
    function: CfirNamedFunction,
): MacroRawOwnerSnapshot = MacroRawOwnerSnapshot(
    packageFqName = packageFqName,
    containerSymbols = listOf(function.symbol),
    dispatchReceiverType = function.dispatchReceiverType,
    inLocalContext = true,
    functionTarget = CfirFunctionTarget(isLambda = false).apply { bind(function) },
    signature = null,
    nominalSyntax = null,
    capturedTypeParameters = listOf(MacroRawTypeParameterScope(true, function.typeParameters.map { it.symbol })),
    constructedClasses = emptyList(),
)
