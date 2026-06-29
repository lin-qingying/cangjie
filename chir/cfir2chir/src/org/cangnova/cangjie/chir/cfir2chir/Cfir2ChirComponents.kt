package org.cangnova.cangjie.chir.cfir2chir

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
)
