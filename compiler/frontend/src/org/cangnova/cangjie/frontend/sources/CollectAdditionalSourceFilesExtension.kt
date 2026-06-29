package org.cangnova.cangjie.frontend.sources

import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.extensions.ExtensionPointDescriptor
import java.io.File

/**
 * 允许编译器插件在前端源文件收集阶段追加源文件的扩展点。
 */
abstract class CollectAdditionalSourceFilesExtension {
    /**
     * 附加源文件扩展点描述符。
     */
    companion object : ExtensionPointDescriptor<CollectAdditionalSourceFilesExtension>(
        "org.cangnova.cangjie.cfir.collectAdditionalSourceFilesExtension",
        CollectAdditionalSourceFilesExtension::class.java,
    )

    /**
     * 判断当前扩展是否适用于给定编译配置。
     */
    abstract fun isApplicable(configuration: CompilerConfiguration): Boolean

    /**
     * 基于已有源文件集合收集额外源文件。
     */
    abstract fun collectSources(
        environment: Any,
        configuration: CompilerConfiguration,
        findVirtualFile: (File) -> VirtualFile?,
        sources: Iterable<CjSourceFile>,
    ): Iterable<CjSourceFile>
}
