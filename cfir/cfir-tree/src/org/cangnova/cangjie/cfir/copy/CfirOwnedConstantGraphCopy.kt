package org.cangnova.cangjie.cfir.copy

import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import java.util.Collections
import java.util.IdentityHashMap

/** 已求值值图独立于来源语法；先登记合法壳，再回填值边，保留共享对象及循环。 */
internal class CfirOwnedConstantGraphCopy(private val context: CfirOwnedGraphCopyContext) {
    private val copies = IdentityHashMap<CfirConstantValue, CfirConstantValue>()

    fun copy(root: CfirConstantValue): CfirConstantValue {
        copies[root]?.let { return it }
        val pending = ArrayDeque<() -> Unit>()
        fun allocate(value: CfirConstantValue): CfirConstantValue {
            copies[value]?.let { return it }
            val type = context.type(value.type)
            val result = when (value) {
                is CfirConstantValue.Primitive -> value.copy(type = type)
                is CfirConstantValue.ObjectValue -> {
                    val fields = linkedMapOf<CfirCallableSymbol<*>, CfirConstantValue>()
                    val storageTypes = linkedMapOf<CfirCallableSymbol<*>, ConeCangJieType>()
                    pending.addLast {
                        value.fields.forEach { (symbol, child) ->
                            val target = context.remapSymbol(symbol)
                            check(target !in fields) { "Constant field rebinding collapsed distinct storage slots" }
                            fields[target] = allocate(child)
                            storageTypes[target] = context.type(value.storageTypes.getValue(symbol))
                        }
                        check(fields.size == value.fields.size) { "Constant storage shape changed during owned copying" }
                    }
                    CfirConstantValue.ObjectValue(type, Collections.unmodifiableMap(fields), value.identity,
                        Collections.unmodifiableMap(storageTypes))
                }
                is CfirConstantValue.Tuple -> {
                    val elements = value.elements.toMutableList()
                    pending.addLast { value.elements.forEachIndexed { index, child -> elements[index] = allocate(child) } }
                    CfirConstantValue.Tuple(type, Collections.unmodifiableList(elements))
                }
                is CfirConstantValue.ArrayValue -> {
                    val elements = value.elements.toMutableList()
                    pending.addLast { value.elements.forEachIndexed { index, child -> elements[index] = allocate(child) } }
                    CfirConstantValue.ArrayValue(type, Collections.unmodifiableList(elements))
                }
                is CfirConstantValue.VArray -> {
                    check(type is ConeVArrayType && type.size == value.type.size) {
                        "Type remapper must preserve the VArray constant storage shape"
                    }
                    // 构造时即满足长度不变量；临时旧值仅存在于未发布壳中。
                    val elements = value.elements.toMutableList()
                    pending.addLast { value.elements.forEachIndexed { index, child -> elements[index] = allocate(child) } }
                    CfirConstantValue.VArray(type, Collections.unmodifiableList(elements))
                }
                is CfirConstantValue.EnumValue -> {
                    val arguments = value.arguments.toMutableList()
                    pending.addLast { value.arguments.forEachIndexed { index, child -> arguments[index] = allocate(child) } }
                    CfirConstantValue.EnumValue(type, context.remapSymbol(value.constructor), Collections.unmodifiableList(arguments))
                }
            }
            copies[value] = result
            return result
        }
        val result = allocate(root)
        while (pending.isNotEmpty()) pending.removeFirst().invoke()
        return result
    }
}
