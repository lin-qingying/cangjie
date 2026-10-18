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
    /**
     * 将同一包内的一组 CFIR 文件转换为一个 CHIR package。
     */
    fun convert(files: List<CfirFile>): ChirPackage
}

/**
 * 默认 CFIR 到 CHIR 转换器实现。
 */
class DefaultCfir2ChirConverter : Cfir2ChirConverter {
    /**
     * 先注册所有文件和声明 header，再逐文件转换 CHIR module。
     */
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

        // 函数体转换期间产出的匿名/局部函数附加到最后一个 module，
        // 保证函数值引用（MethodHandle 物化）的目标声明真实存在。
        val auxiliaryFunctions = components.auxiliaryFunctions.drain()
        val modulesWithAuxiliaryFunctions = if (auxiliaryFunctions.isEmpty()) {
            modules
        } else {
            modules.dropLast(1) + listOf(
                modules.last().copy(declarations = modules.last().declarations + auxiliaryFunctions),
            )
        }

        return ChirPackage(
            semanticId = Cfir2ChirIds.packageId(packageName),
            name = packageName,
            modules = modulesWithAuxiliaryFunctions,
        )
    }
}
