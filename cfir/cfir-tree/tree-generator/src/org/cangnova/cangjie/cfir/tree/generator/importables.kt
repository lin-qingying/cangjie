

package org.cangnova.cangjie.cfir.tree.generator

import org.cangnova.cangjie.generators.tree.imports.ArbitraryImportable

/**
 * 生成代码中引用 resolve phase 到 resolve state 转换扩展的导入项。
 */
val phaseAsResolveStateExtentionImport =
    ArbitraryImportable("org.cangnova.cangjie.cfir.declarations", "asResolveState")
/**
 * 生成代码中引用 resolve state 到 phase 扩展的导入项。
 */
val resolvePhaseExtensionImport =
    ArbitraryImportable("org.cangnova.cangjie.cfir.declarations", "resolvePhase")

/**
 * 生成代码中引用可见性工具对象的导入项。
 */
val visibilitiesImport = ArbitraryImportable("org.cangnova.cangjie.descriptors", "Visibilities")
