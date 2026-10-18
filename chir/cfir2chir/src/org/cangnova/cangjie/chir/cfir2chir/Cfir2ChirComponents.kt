package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration

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
    /**
     * 函数体转换期间产出的辅助函数声明收集器。
     *
     * 匿名函数与局部函数的 body 只在引用点被触发转换，转换产物不在
     * CFIR 文件顶层声明列表中；这里按生成顺序登记，最终由包组装阶段
     * 附加到 module 声明列表，保证函数值引用的目标真实存在。
     */
    val auxiliaryFunctions: Cfir2ChirAuxiliaryFunctionCollector = Cfir2ChirAuxiliaryFunctionCollector(),
)

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
