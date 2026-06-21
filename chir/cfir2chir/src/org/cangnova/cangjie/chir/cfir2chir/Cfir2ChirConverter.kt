package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.cfir.declarations.CfirFile

/**
 * CFIR 到 CHIR 的模块级转换入口。
 *
 * 该接口对应 Kotlin 编译器中的 Fir2IrConverter 职责：先建立文件与声明骨架，
 * 再转换函数体，最终产出可交给后端继续降级的 CHIR package。
 */
interface Cfir2ChirConverter {
    fun convert(files: List<CfirFile>): ChirPackage
}

class DefaultCfir2ChirConverter : Cfir2ChirConverter {
    override fun convert(files: List<CfirFile>): ChirPackage {
        if (files.isEmpty()) {
            throw Cfir2ChirConversionException("CFIR to CHIR conversion requires at least one file")
        }

        val components = Cfir2ChirComponents()
        val packageNames = files.map { it.packageDirective.packageFqName.asString() }.distinct()
        if (packageNames.size != 1) {
            throw Cfir2ChirConversionException("single CHIR package cannot be built from multiple CFIR packages: $packageNames")
        }

        val packageName = packageNames.single().ifBlank { "<root>" }
        files.forEach { file ->
            Cfir2ChirFileConverter(components, packageName).registerFileAndDeclarations(file)
        }
        val modules = files.map { file ->
            Cfir2ChirFileConverter(components, packageName).convertFile(file)
        }

        return ChirPackage(
            semanticId = Cfir2ChirIds.packageId(packageName),
            name = packageName,
            modules = modules,
        )
    }
}
