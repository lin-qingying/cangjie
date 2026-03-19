package org.cangnova.cangjie.cli.common

import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.extensions.ExtensionPointDescriptor
import java.io.File

abstract class CollectAdditionalSourceFilesExtension {
    companion object : ExtensionPointDescriptor<CollectAdditionalSourceFilesExtension>(
        "org.cangnova.cangjie.cfir.collectAdditionalSourceFilesExtension",
        CollectAdditionalSourceFilesExtension::class.java,
    )

    abstract fun isApplicable(configuration: CompilerConfiguration): Boolean

    abstract fun collectSources(
        environment: Any,
        configuration: CompilerConfiguration,
        findVirtualFile: (File) -> VirtualFile?,
        sources: Iterable<CjSourceFile>,
    ): Iterable<CjSourceFile>
}
