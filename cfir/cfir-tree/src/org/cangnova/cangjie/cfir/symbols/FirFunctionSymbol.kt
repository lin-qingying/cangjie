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
 *
 * @property callableId 函数符号的 callable id；匿名/错误函数使用 special 包下的 stub id。
 */
sealed class CfirFunctionSymbol<out D : CfirFunction>(override val callableId: CallableId) : CfirCallableSymbol<D>() {
    /**
     * 函数值参数对应的符号列表。
     */
    val valueParameterSymbols: List<CfirValueParameterSymbol>
        get() = cfir.valueParameters.map { it.symbol }

    /**
     * 函数名称。
     */
    override val name: Name
        get() = callableId.callableName

    /**
     * 函数是否拥有函数体。
     */
    val hasBody: Boolean
        get() = cfir.body != null
}

/**
 * 错误 callable 符号的标记接口。
 */
interface CfirErrorCallableSymbol<F : CfirCallableDeclaration>

/**
 * 没有真实用户名称的函数符号基类。
 *
 * @param stubName 用于构造 special callable id 的内部名称。
 */
sealed class CfirFunctionWithoutNameSymbol<out F : CfirFunction>(stubName: Name) : CfirFunctionSymbol<F>(
    CallableId(
        FqName("special"),
        stubName
    )
)

/**
 * 错误函数符号。
 */
class CfirErrorFunctionSymbol : CfirFunctionWithoutNameSymbol<CfirErrorFunction>(Name.identifier("error")),
    CfirErrorCallableSymbol<CfirErrorFunction> {

}
