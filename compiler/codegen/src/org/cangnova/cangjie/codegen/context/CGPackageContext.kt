package org.cangnova.cangjie.codegen.context

import org.cangnova.cangjie.chir.core.model.ChirPackage

class CGPackageContext(
    val chirPackage: ChirPackage,
) {
    val packageName: String get() = chirPackage.name
    val packageInitFunctionId get() = chirPackage.packageInitFunctionId
    val packageLiteralInitFunctionId get() = chirPackage.packageLiteralInitFunctionId
}

