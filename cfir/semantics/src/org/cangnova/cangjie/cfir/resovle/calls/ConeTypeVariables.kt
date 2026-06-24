package org.cangnova.cangjie.cfir.resovle.calls

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeTypeVariable
import org.cangnova.cangjie.name.SpecialNames

/**
 * 延迟解析原子对应的临时类型变量。
 *
 * 该变量用于把 postponed atom 放入约束系统，等待 lambda、callable reference 等结构完成后再固定。
 */
class ConeTypeVariableForPostponedAtom(name: String) : ConeTypeVariable(name)

/**
 * lambda 形参类型对应的临时类型变量。
 */
class ConeTypeVariableForLambdaParameterType(name: String) : ConeTypeVariable(name)

/**
 * lambda 返回类型对应的临时类型变量。
 *
 * @property argument 产生该返回类型变量的 CFIR 声明。
 */
class ConeTypeVariableForLambdaReturnType(val argument: CfirDeclaration, name: String) : ConeTypeVariable(name)

/**
 * 声明侧类型参数对应的约束系统类型变量。
 *
 * @property typeParameterSymbol 被转换为类型变量的类型参数符号。
 */
class ConeTypeParameterBasedTypeVariable(
    val typeParameterSymbol: CfirTypeParameterSymbol
) : ConeTypeVariable(SpecialNames.safeIdentifier(typeParameterSymbol.name).identifier, typeParameterSymbol.toLookupTag())
