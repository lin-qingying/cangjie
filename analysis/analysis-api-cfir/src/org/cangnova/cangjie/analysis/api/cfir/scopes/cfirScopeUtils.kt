package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.signatures.CaCallableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeParameterScope
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.name.Name


/**
 * 从 CFIR scope 中按名称集合收集公开 callable 符号。
 */
internal fun CfirScope.getCallableSymbols(
    callableNames: Collection<Name>,
    builder: CaSymbolByCfirBuilder
): Sequence<CaCallableSymbol> = sequence {
    callableNames.forEach { name ->
        yieldList {
            processFunctionsByName(name) { firSymbol ->
                add(builder.functionBuilder.buildNamedFunctionSymbol(firSymbol))
            }
        }
        yieldList {
            processPropertiesByName(name) { firSymbol ->
                add(builder.callableBuilder.buildCallableSymbol(firSymbol))
            }
        }
        yieldList {
            processVariablesByName(name) { firSymbol ->
                add(builder.callableBuilder.buildCallableSymbol(firSymbol))
            }
        }
        yieldList {
            processCallablesByName(name) { firSymbol ->
                if (firSymbol is CfirEnumConstructorSymbol) {
                    add(builder.callableBuilder.buildCallableSymbol(firSymbol))
                }
            }
        }
    }
}

/**
 * 从 CFIR scope 中按名称集合收集公开 callable 签名。
 */
internal fun CfirScope.getCallableSignatures(
    callableNames: Collection<Name>,
    builder: CaSymbolByCfirBuilder
): Sequence<CaCallableSignature<*>> = sequence {
    callableNames.forEach { name ->
        yieldList {
            processFunctionsByName(name) { cfirSymbol ->
                add(builder.functionBuilder.buildFunctionSignature(cfirSymbol))
            }
        }
        yieldList {
            processPropertiesByName(name) { cfirSymbol ->
                add(builder.variableBuilder.buildVariableLikeSignature(cfirSymbol))
            }
        }
    }
}

/**
 * 从 CFIR scope 中按 class-like 名称集合收集公开 classifier 符号。
 */
internal fun CfirScope.getClassifierSymbols(classLikeNames: Collection<Name>, builder: CaSymbolByCfirBuilder): Sequence<CaClassifierSymbol> =
    sequence {
        classLikeNames.forEach { name ->
            yieldList {
                processClassifiersByName(name) { firSymbol ->
                    add(builder.classifierBuilder.buildClassifierSymbol(firSymbol))
                }
            }
        }
    }

/**
 * 从 CFIR scope 中收集公开类型参数符号。
 *
 * 必须走 [CfirTypeParameterScope.processTypeParametersByName] 而不是
 * [CfirScope.processClassifiersByName]：`CfirTypeParameterScopeImpl` 的后者是**空实现**
 * （类型参数不是 class-like），走错通道的结果是类型参数永远枚举不到。
 *
 * 类型参数 scope 是 `CfirTypeParameterScope` 这个**子类**，不是 `CfirScope` 本身，
 * 所以这里要向下转型；非类型参数 scope 返回空序列。
 */
internal fun CfirScope.getTypeParameterSymbols(
    typeParameterNames: Collection<Name>,
    builder: CaSymbolByCfirBuilder,
): Sequence<CaTypeParameterSymbol> {
    val typeParameterScope = this as? CfirTypeParameterScope ?: return emptySequence()
    return sequence {
        typeParameterNames.forEach { name ->
            yieldList {
                typeParameterScope.processTypeParametersByName(name) { cfirSymbol ->
                    add(builder.classifierBuilder.buildTypeParameterSymbol(cfirSymbol))
                }
            }
        }
    }
}

/**
 * 从 CFIR scope 中收集构造器符号。
 */
internal fun CfirScope.getConstructors(builder: CaSymbolByCfirBuilder): Sequence<CaConstructorSymbol> =
    sequence {
        yieldList {
            processDeclaredConstructors { firSymbol ->
                add(builder.functionBuilder.buildConstructorSymbol(firSymbol))
            }
        }
    }

/**
 * 将临时 list 构造结果逐项产出到 sequence。
 */
private suspend inline fun <T> SequenceScope<T>.yieldList(listBuilder: MutableList<T>.() -> Unit) {
    yieldAll(buildList(listBuilder))
}
