package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirErrorFunction
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
/**
 * 所有函数类符号的密封基类，对齐 K2 `FirFunctionSymbol`。
 *
 * 子类包括具名函数、匿名函数、构造器、main 函数、析构函数等。
 * 提供 [valueParameterSymbols] 统一访问值参数符号列表。
 */
sealed class CfirFunctionSymbol<out D : CfirFunction>(override val callableId: CallableId) : CfirCallableSymbol<D>() {
    val valueParameterSymbols: List<CfirValueParameterSymbol>
        get() = cfir.valueParameters.map { it.symbol }
    override val name: Name
        get() = callableId.callableName

    val hasBody: Boolean
        get() = cfir.body != null
}

interface CfirErrorCallableSymbol<F : CfirCallableDeclaration>
sealed class CfirFunctionWithoutNameSymbol<out F : CfirFunction>(stubName: Name) : CfirFunctionSymbol<F>(
    CallableId(
        FqName("special"),
        stubName
    )
)

class CfirErrorFunctionSymbol : CfirFunctionWithoutNameSymbol<CfirErrorFunction>(Name.identifier("error")),
    CfirErrorCallableSymbol<CfirErrorFunction> {

}
