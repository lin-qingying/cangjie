package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.decompiler.stub.file.CjoBinaryFileReader
import org.cangnova.cangjie.utils.exceptions.shouldIjPlatformExceptionBeRethrown

private val LOG = logger<CangJieDecompiledFileViewProviderFactory>()

/**
 * `.cjo` binary file 到仓颉 decompiled PSI 的平台入口。
 *
 * 这里作为 file type 层入口，只委托仓颉自己的 [CjoFileDecompilers] 协议。
 */
class CangJieDecompiledFileViewProviderFactory : FileViewProviderFactory {
    /**
     * 为 `.cjo` 文件创建反编译 view provider。
     *
     * 工厂只查找已注册的 full decompiler 并委托创建，避免在 file type 层直接依赖具体
     * metadata stub builder 或 PSI 文件实现。
     *
     * 找不到 decompiler 时返回不产出 PSI 的空 provider，而不是抛异常：该工厂位于平台
     * `FileManagerImpl.createFileViewProvider` 调用链中，异常没有 catch，会直接打断
     * `openFilesOnStartup` / `CodeFoldingNecromancer` 的标签页恢复。空 provider 的
     * `getPsi` 返回 `null`，与 [CangJieMetadataDecompiler] 在 metadata 不可读时的行为一致。
     */
    override fun createFileViewProvider(
        file: VirtualFile,
        language: Language?,
        manager: PsiManager,
        eventSystemEnabled: Boolean,
    ): FileViewProvider {
        val decompiler = CjoFileDecompilers.getInstance().find(file, CjoFileDecompilers.Full::class.java)
        if (decompiler != null) {
            return decompiler.createFileViewProvider(file, manager, eventSystemEnabled)
        }

        LOG.warn(describeMissingDecompiler(file))
        return CangJieDecompiledFileViewProvider(manager, file, eventSystemEnabled) { null }
    }

    /**
     * 组装“找不到 `.cjo` decompiler”这一失败的现场快照。
     *
     * 每个字段独立容错，避免诊断本身在工厂里抛异常；但控制流异常必须继续向上传播。
     * `extensions` 与 `readPackageFqName` 分别用来区分“扩展点没加载”与“头部读不出来”两类输入。
     */
    private fun describeMissingDecompiler(file: VirtualFile): String = buildString {
        append("CangJie .cjo decompiler is not registered: path=").append(probe { file.path })
        append(", isValid=").append(probe { file.isValid })
        append(", fileType=").append(probe { file.fileType.name })
        append(", length=").append(probe { file.length })
        append(", extensions=").append(probe { CjoFileDecompilers.EP_NAME.extensionList.size })
        append(", readPackageFqName=").append(probe { CjoBinaryFileReader.readPackageFqName(file) })
    }

    private companion object {
        /**
         * 诊断字段读取失败不改变工厂结果，只把该字段记为 `null`。
         */
        inline fun <T> probe(block: () -> T): T? = try {
            block()
        } catch (t: Throwable) {
            if (shouldIjPlatformExceptionBeRethrown(t)) throw t
            null
        }
    }
}
