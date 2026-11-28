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
import org.cangnova.cangjie.chir.core.value.ChirGlobalValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirThisOwnerSymbol
import java.util.IdentityHashMap

/**
 * CFIR 声明到 CHIR 声明的转换缓存。
 *
 * Kotlin FIR2IR 先缓存文件、类和函数符号，再处理 body。这里同样先登记函数签名，
 * 保证函数体中的前向/跨文件调用不会依赖遍历顺序。
 */
class Cfir2ChirDeclarationStorage {
    /**
     * CFIR 文件到 CHIR module 的映射。
     */
    private val modulesByFile = IdentityHashMap<CfirFile, ChirModule>()

    /**
     * CFIR 函数符号到 CHIR 函数 header 的映射。
     */
    private val functionHeadersBySymbol = IdentityHashMap<CfirBasedSymbol<*>, ChirFunctionHeader>()

    /**
     * CFIR 函数符号到完整 CHIR 函数声明的映射。
     */
    private val functionsBySymbol = IdentityHashMap<CfirBasedSymbol<*>, ChirFunctionDeclaration>()

    /**
     * CFIR 变量符号到 CHIR 全局/成员变量 header 的映射。
     */
    private val variablesBySymbol = IdentityHashMap<CfirBasedSymbol<*>, ChirVariableHeader>()

    /**
     * CFIR 参数符号到 CHIR 参数 header 的映射。
     */
    private val parametersBySymbol = IdentityHashMap<CfirBasedSymbol<*>, ChirParameterHeader>()

    /**
     * CFIR 局部变量符号到 CHIR 局部变量 header 的映射。
     */
    private val localVariablesBySymbol = IdentityHashMap<CfirBasedSymbol<*>, ChirLocalVariableHeader>()

    /**
     * 登记 CFIR 文件对应的 CHIR module 骨架。
     */
    fun registerModule(file: CfirFile, module: ChirModule) {
        check(modulesByFile.put(file, module) == null) { "CFIR file is already registered: ${file.name}" }
    }

    /**
     * 获取已经登记的 CHIR module。
     */
    fun getModule(file: CfirFile): ChirModule =
        modulesByFile[file] ?: throw Cfir2ChirConversionException("unregistered CFIR file: ${file.name}", file)

    /**
     * 登记 CFIR 函数符号的 CHIR header。
     */
    fun registerFunctionHeader(symbol: CfirBasedSymbol<*>, header: ChirFunctionHeader) {
        check(functionHeadersBySymbol.put(symbol, header) == null) {
            "CFIR function symbol is already registered: ${symbol.debugName}"
        }
    }

    /**
     * 判断指定 CFIR 函数符号是否已经登记 header。
     */
    fun hasFunctionHeader(symbol: CfirBasedSymbol<*>): Boolean =
        functionHeadersBySymbol.containsKey(symbol)

    /**
     * 登记已经完成 body 转换的 CHIR 函数声明。
     */
    fun registerFunction(symbol: CfirBasedSymbol<*>, declaration: ChirFunctionDeclaration) {
        check(functionsBySymbol.put(symbol, declaration) == null) {
            "CFIR function declaration is already converted: ${symbol.debugName}"
        }
    }

    /**
     * 判断指定 CFIR 函数符号是否已经完成完整函数声明转换。
     */
    fun hasFunction(symbol: CfirBasedSymbol<*>): Boolean =
        functionsBySymbol.containsKey(symbol)

    /**
     * 获取指定 CFIR 函数符号对应的 CHIR 函数 header。
     */
    fun getFunctionHeader(symbol: CfirBasedSymbol<*>): ChirFunctionHeader =
        functionHeadersBySymbol[symbol]
            ?: throw Cfir2ChirConversionException("unregistered CFIR function symbol: ${symbol.debugName}")

    /**
     * 尝试获取指定 CFIR 函数符号对应的 CHIR 函数 header。
     */
    fun getFunctionHeaderOrNull(symbol: CfirBasedSymbol<*>): ChirFunctionHeader? =
        functionHeadersBySymbol[symbol]

    /**
     * 获取指定 CFIR 函数符号对应的完整 CHIR 函数声明。
     */
    fun getFunction(symbol: CfirBasedSymbol<*>): ChirFunctionDeclaration =
        functionsBySymbol[symbol]
            ?: throw Cfir2ChirConversionException("unconverted CFIR function symbol: ${symbol.debugName}")

    /**
     * 登记 CFIR 变量符号对应的 CHIR 变量 header。
     */
    fun registerVariable(symbol: CfirBasedSymbol<*>, variable: ChirVariableHeader) {
        check(variablesBySymbol.put(symbol, variable) == null) {
            "CFIR variable symbol is already registered: ${symbol.debugName}"
        }
    }

    /**
     * 尝试获取指定 CFIR 变量符号对应的 CHIR 变量 header。
     */
    fun getVariableOrNull(symbol: CfirBasedSymbol<*>): ChirVariableHeader? =
        variablesBySymbol[symbol]

    /**
     * 登记 CFIR 值参数符号对应的 CHIR 参数 header。
     */
    fun registerParameter(symbol: CfirBasedSymbol<*>, parameter: ChirParameterHeader) {
        check(parametersBySymbol.put(symbol, parameter) == null) {
            "CFIR parameter symbol is already registered: ${symbol.debugName}"
        }
    }

    /**
     * 获取指定 CFIR 参数符号对应的 CHIR 参数 header。
     */
    fun getParameter(symbol: CfirBasedSymbol<*>): ChirParameterHeader =
        parametersBySymbol[symbol]
            ?: throw Cfir2ChirConversionException("unregistered CFIR value parameter symbol: ${symbol.debugName}")

    /**
     * 登记 CFIR 局部变量符号对应的 CHIR 局部变量 header。
     */
    fun registerLocalVariable(symbol: CfirBasedSymbol<*>, localVariable: ChirLocalVariableHeader) {
        check(localVariablesBySymbol.put(symbol, localVariable) == null) {
            "CFIR local variable symbol is already registered: ${symbol.debugName}"
        }
    }

    /** Update the SSA address when lowering another copy of the same finally body. */
    internal fun replaceLocalVariableAddress(
        symbol: CfirBasedSymbol<*>,
        previous: ChirLocalVariableHeader,
        replacement: ChirLocalVariableHeader,
    ) {
        check(replacement.declaration === previous.declaration && replacement.ownerFunctionId == previous.ownerFunctionId) {
            "Relowered local variable changed its declaration owner: ${symbol.debugName}"
        }
        check(localVariablesBySymbol.replace(symbol, previous, replacement))
    }

    /**
     * 获取指定 CFIR 局部变量符号对应的 CHIR 局部变量 header。
     */
    fun getLocalVariable(symbol: CfirBasedSymbol<*>): ChirLocalVariableHeader =
        localVariablesBySymbol[symbol]
            ?: throw Cfir2ChirConversionException("unregistered CFIR local variable symbol: ${symbol.debugName}")

    /**
     * 尝试获取指定 CFIR 局部变量符号对应的 CHIR 局部变量 header。
     */
    fun getLocalVariableOrNull(symbol: CfirBasedSymbol<*>): ChirLocalVariableHeader? =
        localVariablesBySymbol[symbol]
}

/**
 * 函数 body 转换前可用的 CHIR 函数签名信息。
 */
data class ChirFunctionHeader(
    /**
     * 函数声明的稳定语义 ID。
     */
    val semanticId: ChirSemanticId,
    /**
     * 函数在 CHIR 中使用的名称。
     */
    val name: String,
    /**
     * 函数返回类型。
     */
    val returnType: ChirTypeRef,
    /**
     * 函数形参声明列表。
     */
    val parameters: List<ChirVariableDeclaration>,
    /** 实例成员 ABI 中排在用户形参之前的隐式 dispatch receiver。 */
    val dispatchReceiverParameter: ChirVariableDeclaration? = null,
    /** 该 receiver 绑定的 CFIR this owner；用于拒绝把外层 receiver 错当成当前 this。 */
    val receiverOwnerSymbol: CfirThisOwnerSymbol<*>? = null,
    /**
     * 成员函数或扩展函数的接收者类型；普通函数为空。
     */
    val receiverType: ChirTypeRef? = null,
    /** 声明所属 class-like 或 extend 类型；静态成员也必须保留。 */
    val ownerType: ChirTypeRef? = null,
    /** 函数声明是否带有静态成员语义。 */
    val isStatic: Boolean = false,
    /** 函数自身声明的泛型参数身份。 */
    val genericTypeParameters: List<ChirTypeRef> = emptyList(),
) {
    /** 按官方 CHIR ABI 顺序排列的函数形参。 */
    val abiParameters: List<ChirVariableDeclaration> =
        listOfNotNull(dispatchReceiverParameter) + parameters

    /**
     * 由 ABI 形参与返回类型组合出的 CHIR 函数类型。
     */
    val functionType: ChirResolvedTypeRef =
        ChirResolvedTypeRef(ChirFunctionType(abiParameters.map { it.type }, returnType))

    /**
     * 将函数 header 转换为可在表达式中引用的函数值。
     */
    fun asValue(): ChirFunctionValue =
        ChirFunctionValue(
            semanticId = semanticId,
            type = functionType,
            name = name,
        )
}

/**
 * 全局或成员变量的 CHIR header。
 */
data class ChirVariableHeader(
    /**
     * 变量对应的 CHIR 声明。
     */
    val declaration: ChirVariableDeclaration,
    /**
     * 若该变量归属于函数局部作用域，则记录所属函数 ID。
     */
    val ownerFunctionId: ChirSemanticId? = null,
) {
    /**
     * 将变量 header 转换为全局值引用。
     */
    fun asValue(): ChirGlobalValue =
        ChirGlobalValue(
            semanticId = declaration.semanticId,
            type = declaration.type,
            name = declaration.name,
        )

    /**
     * 将变量 header 转换为可写地址值。
     */
    fun asAddressValue(): ChirLocalValue =
        ChirLocalValue(
            semanticId = declaration.semanticId,
            type = ChirResolvedTypeRef(ChirRefType(declaration.type, declaration.mutable)),
            name = declaration.name,
        )
}

/**
 * 函数参数的 CHIR header。
 */
data class ChirParameterHeader(
    /**
     * 参数对应的 CHIR 变量声明。
     */
    val declaration: ChirVariableDeclaration,
    /**
     * 参数所属函数的语义 ID。
     */
    val ownerFunctionId: ChirSemanticId,
) {
    /**
     * 将参数 header 转换为参数值引用。
     */
    fun asValue(): ChirParameterValue =
        ChirParameterValue(
            semanticId = declaration.semanticId,
            type = declaration.type,
            name = declaration.name,
            ownerFunctionId = ownerFunctionId,
        )

    /**
     * 将参数 header 转换为可写地址值。
     */
    fun asAddressValue(): ChirLocalValue =
        ChirLocalValue(
            semanticId = declaration.semanticId,
            type = ChirResolvedTypeRef(ChirRefType(declaration.type, declaration.mutable)),
            name = declaration.name,
        )
}

/**
 * 局部变量的 CHIR header。
 */
data class ChirLocalVariableHeader(
    /**
     * 局部变量对应的 CHIR 变量声明。
     */
    val declaration: ChirVariableDeclaration,
    /**
     * 局部变量所属函数的语义 ID。
     */
    val ownerFunctionId: ChirSemanticId,
    /**
     * 局部变量地址的 SSA 身份；它必须与创建该地址的 ALLOCA 表达式身份相同。
     */
    val addressSemanticId: ChirSemanticId,
) {
    /**
     * 将局部变量 header 转换为可写地址值。
     */
    fun asAddressValue(): ChirLocalValue =
        ChirLocalValue(
            semanticId = addressSemanticId,
            type = ChirResolvedTypeRef(ChirRefType(declaration.type, declaration.mutable)),
            name = declaration.name,
        )
}
