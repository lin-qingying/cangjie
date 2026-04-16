package org.cangnova.cangjie.analysis.api.symbols.markers

import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

/**
 * 可作为其他声明容器的公开符号能力接口。
 *
 * 典型实现包括 file、script、class、extend、property。
 */
interface CaDeclarationContainerSymbol : CaSymbol
