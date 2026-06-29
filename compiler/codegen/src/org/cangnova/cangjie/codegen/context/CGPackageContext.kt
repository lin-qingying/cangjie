package org.cangnova.cangjie.codegen.context

import org.cangnova.cangjie.chir.core.model.ChirPackage

/**
 * 包级 codegen 上下文。
 */
class CGPackageContext(
    /**
     * 当前 CHIR package。
     */
    val chirPackage: ChirPackage,
) {
    /**
     * 包名。
     */
    val packageName: String get() = chirPackage.name
    /**
     * 包初始化函数 semanticId。
     */
    val packageInitFunctionId get() = chirPackage.packageInitFunctionId
    /**
     * 包字面量初始化函数 semanticId。
     */
    val packageLiteralInitFunctionId get() = chirPackage.packageLiteralInitFunctionId
}
