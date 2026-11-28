package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.signatures.CaCallableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.cfir.scopes.CfirScope
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
            // 与 body lookup 共用完整 callable 入口，包含字段与 enum 构造项，不重复拼接分类通道。
            processCallablesByName(name) { cfirSymbol ->
                add(builder.callableBuilder.buildCallableSymbol(cfirSymbol))
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
): Sequence<CaCallableSignature<*>> = getCallableSymbols(callableNames, builder).map { symbol ->
    with(builder.analysisSession) {
        symbol.asSignature()
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
