package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import java.util.IdentityHashMap

/**
 * CFIR 声明到 CHIR 声明的转换缓存。
 *
 * Kotlin FIR2IR 先缓存文件、类和函数符号，再处理 body。这里同样先登记函数签名，
 * 保证函数体中的前向/跨文件调用不会依赖遍历顺序。
 */
class Cfir2ChirDeclarationStorage {
    private val modulesByFile = IdentityHashMap<CfirFile, ChirModule>()
    private val functionHeadersBySymbol = IdentityHashMap<CfirBasedSymbol<*>, ChirFunctionHeader>()
    private val functionsBySymbol = IdentityHashMap<CfirBasedSymbol<*>, ChirFunctionDeclaration>()
    private val parametersBySymbol = IdentityHashMap<CfirBasedSymbol<*>, ChirParameterHeader>()
    private val localVariablesBySymbol = IdentityHashMap<CfirBasedSymbol<*>, ChirLocalVariableHeader>()

    fun registerModule(file: CfirFile, module: ChirModule) {
        check(modulesByFile.put(file, module) == null) { "CFIR file is already registered: ${file.name}" }
    }

    fun getModule(file: CfirFile): ChirModule =
        modulesByFile[file] ?: throw Cfir2ChirConversionException("unregistered CFIR file: ${file.name}", file)

    fun registerFunctionHeader(symbol: CfirBasedSymbol<*>, header: ChirFunctionHeader) {
        check(functionHeadersBySymbol.put(symbol, header) == null) {
            "CFIR function symbol is already registered: ${symbol.debugName}"
        }
    }

    fun registerFunction(symbol: CfirBasedSymbol<*>, declaration: ChirFunctionDeclaration) {
        check(functionsBySymbol.put(symbol, declaration) == null) {
            "CFIR function declaration is already converted: ${symbol.debugName}"
        }
    }

    fun getFunctionHeader(symbol: CfirBasedSymbol<*>): ChirFunctionHeader =
        functionHeadersBySymbol[symbol]
            ?: throw Cfir2ChirConversionException("unregistered CFIR function symbol: ${symbol.debugName}")

    fun getFunction(symbol: CfirBasedSymbol<*>): ChirFunctionDeclaration =
        functionsBySymbol[symbol]
            ?: throw Cfir2ChirConversionException("unconverted CFIR function symbol: ${symbol.debugName}")

    fun registerParameter(symbol: CfirBasedSymbol<*>, parameter: ChirParameterHeader) {
        check(parametersBySymbol.put(symbol, parameter) == null) {
            "CFIR parameter symbol is already registered: ${symbol.debugName}"
        }
    }

    fun getParameter(symbol: CfirBasedSymbol<*>): ChirParameterHeader =
        parametersBySymbol[symbol]
            ?: throw Cfir2ChirConversionException("unregistered CFIR value parameter symbol: ${symbol.debugName}")

    fun registerLocalVariable(symbol: CfirBasedSymbol<*>, localVariable: ChirLocalVariableHeader) {
        check(localVariablesBySymbol.put(symbol, localVariable) == null) {
            "CFIR local variable symbol is already registered: ${symbol.debugName}"
        }
    }

    fun getLocalVariable(symbol: CfirBasedSymbol<*>): ChirLocalVariableHeader =
        localVariablesBySymbol[symbol]
            ?: throw Cfir2ChirConversionException("unregistered CFIR local variable symbol: ${symbol.debugName}")
}

data class ChirFunctionHeader(
    val semanticId: ChirSemanticId,
    val name: String,
    val returnType: ChirTypeRef,
    val parameters: List<ChirVariableDeclaration>,
) {
    val functionType: ChirResolvedTypeRef =
        ChirResolvedTypeRef(ChirFunctionType(parameters.map { it.type }, returnType))

    fun asValue(): ChirFunctionValue =
        ChirFunctionValue(
            semanticId = semanticId,
            type = functionType,
            name = name,
        )
}

data class ChirParameterHeader(
    val declaration: ChirVariableDeclaration,
    val ownerFunctionId: ChirSemanticId,
) {
    fun asValue(): ChirParameterValue =
        ChirParameterValue(
            semanticId = declaration.semanticId,
            type = declaration.type,
            name = declaration.name,
            ownerFunctionId = ownerFunctionId,
        )

    fun asAddressValue(): ChirLocalValue =
        ChirLocalValue(
            semanticId = declaration.semanticId,
            type = ChirResolvedTypeRef(ChirRefType(declaration.type, declaration.mutable)),
            name = declaration.name,
        )
}

data class ChirLocalVariableHeader(
    val declaration: ChirVariableDeclaration,
    val ownerFunctionId: ChirSemanticId,
) {
    fun asAddressValue(): ChirLocalValue =
        ChirLocalValue(
            semanticId = declaration.semanticId,
            type = ChirResolvedTypeRef(ChirRefType(declaration.type, declaration.mutable)),
            name = declaration.name,
        )
}
