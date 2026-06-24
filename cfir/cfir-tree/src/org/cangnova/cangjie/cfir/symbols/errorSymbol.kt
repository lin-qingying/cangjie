package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.declarations.CfirErrorNamedValue
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.impl.CfirErrorTypeRefImpl
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name

/**
 * 错误具名值符号。
 *
 * 该符号用于变量、属性或其他 named value 解析失败后的占位，使后续阶段仍能沿 callable symbol
 * 管线继续运行并保留诊断信息。
 *
 * @property callableId 错误值符号的 callable id。
 * @property diagnostic 产生该错误符号的诊断。
 */
open class CfirErrorNamedValueSymbol(
    override val callableId: CallableId,
    val diagnostic: ConeDiagnostic
) : CfirNamedValueSymbol<CfirErrorNamedValue>(callableId), CfirErrorCallableSymbol<CfirErrorNamedValue> {
    /**
     * 错误符号名称，来自 callable id。
     */
    override val name: Name
        get() = callableId.callableName

    /**
     * 返回调试用符号文本。
     */
    override fun toString(): String =
        if (isBound) "CfirErrorNamedValueSymbol(${cfir.name})" else "CfirErrorNamedValueSymbol(unbound)"
}

/**
 * 错误 enum constructor 符号。
 */
class CfirErrorEnumConstructorSymbol(
    callableId: CallableId,
    diagnostic: ConeDiagnostic,
) : CfirErrorNamedValueSymbol(callableId, diagnostic)
