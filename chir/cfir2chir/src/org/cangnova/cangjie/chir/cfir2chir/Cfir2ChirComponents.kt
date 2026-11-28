package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirThisOwnerSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirTypeRef

/**
 * CFIR -> CHIR 转换阶段共享组件。
 *
 * 对齐 Kotlin Fir2IrComponents 的职责边界：转换器、声明存储、类型映射器共享同一批缓存，
 * 不把符号解析状态散落到各个 visitor 方法里。
 */
class Cfir2ChirComponents(
    /**
     * CFIR 符号到 CHIR 声明/header/value 的共享登记表。
     */
    val declarationStorage: Cfir2ChirDeclarationStorage = Cfir2ChirDeclarationStorage(),
    /**
     * CFIR 类型到 CHIR 类型的共享映射器。
     */
    val typeMapper: Cfir2ChirTypeMapper = Cfir2ChirTypeMapper(),
    /** 跨包 actualization 提供的声明身份事实。 */
    val semanticIdentityProvider: Cfir2ChirSemanticIdentityProvider = Cfir2ChirSemanticIdentityProvider { null },
    /**
     * 函数体转换期间产出的辅助函数声明收集器。
     *
     * 匿名函数与局部函数的 body 只在引用点被触发转换，转换产物不在
     * CFIR 文件顶层声明列表中；这里按生成顺序登记，最终由包组装阶段
     * 附加到 module 声明列表，保证函数值引用的目标真实存在。
     */
    val auxiliaryFunctions: Cfir2ChirAuxiliaryFunctionCollector = Cfir2ChirAuxiliaryFunctionCollector(),
)

/** 为声明选择跨包身份；未被 actualization 覆盖的声明继续使用本地 CFIR 身份。 */
internal fun Cfir2ChirComponents.declarationId(symbol: CfirBasedSymbol<*>): ChirSemanticId =
    semanticIdentityProvider.identityOf(symbol) ?: Cfir2ChirIds.declarationId(symbol)

/** 为 callable header 选择与其引用点相同的跨包身份。 */
internal fun Cfir2ChirComponents.callableId(declaration: CfirCallableDeclaration): ChirSemanticId =
    semanticIdentityProvider.semanticIdentityOf(declaration.symbol)

/** 将非静态成员的隐式 receiver 建模为官方 ABI 中的首个函数参数。 */
internal fun Cfir2ChirComponents.dispatchReceiverParameter(
    function: CfirFunction,
    functionId: ChirSemanticId,
): ChirVariableDeclaration? {
    if (function.status.isStatic) return null
    val receiverType = function.dispatchReceiverType ?: return null
    return DefaultChirVariableDeclaration(
        semanticId = Cfir2ChirIds.dispatchReceiverId(functionId),
        name = "this",
        type = typeMapper.mapConeTypeRef(receiverType),
        mutable = false,
    )
}

/** 将 `this` owner 映射为声明类型实例，保留 class-like 与 extend 的泛型身份。 */
internal fun Cfir2ChirComponents.ownerType(owner: CfirThisOwnerSymbol<*>): ChirTypeRef =
    when (owner) {
        is CfirClassLikeSymbol<*> -> typeMapper.mapConeTypeRef(
            owner.constructType(owner.cfir.typeParameters.map { it.symbol.constructType() }),
        )
        is CfirExtendSymbol -> typeMapper.mapTypeRef(owner.cfir.extendedTypeRef)
        is CfirTypeParameterSymbol -> throw Cfir2ChirConversionException(
            "a type parameter cannot own a callable receiver",
            owner.cfir,
        )
    }
/**
 * 辅助函数声明的按需收集器。
 *
 * 同一符号只收集一次：调用方以 `Cfir2ChirDeclarationStorage.hasFunction`
 * 的幂等性保证不重复转换。
 */
class Cfir2ChirAuxiliaryFunctionCollector {
    /**
     * 按生成顺序累积的辅助函数声明。
     */
    private val collected = mutableListOf<DefaultChirFunctionDeclaration>()

    /**
     * 登记一个函数体转换产出的辅助函数声明。
     */
    fun register(declaration: DefaultChirFunctionDeclaration) {
        collected += declaration
    }

    /**
     * 取出并清空当前累积的全部辅助函数声明。
     */
    fun drain(): List<DefaultChirFunctionDeclaration> {
        val result = collected.toList()
        collected.clear()
        return result
    }
}
