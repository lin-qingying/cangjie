package org.cangnova.cangjie.analysis.api.symbols.markers

import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

/**
 * 标记型 trait：声明该符号可作为其他声明的容器。
 *
 * 该接口本身不暴露子声明列表（取决于具体容器形态），只用于在类型系统中
 * 标识"可承载内嵌声明"这一能力。典型实现：file、class、extend、property。
 */
interface CaDeclarationContainerSymbol : CaSymbol
