package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileTypes.BinaryFileDecompiler
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.psi.stubs.CangJieCompiledFileErrors
import org.cangnova.cangjie.utils.exceptions.shouldIjPlatformExceptionBeRethrown

private val LOG = logger<CjoBinaryFileDecompiler>()

/**
 * `.cjo` 二进制文件的编辑器文本入口。
 *
 * 该类只承担 IntelliJ 平台 `filetype.decompiler` 接点职责：
 * 将 binary editor document 的文本请求导回
 * `PsiManager.findFile(...) -> CjDecompiledFile -> compiled stub -> decompiled text`
 * 主链，不单独实现文本渲染路径。
 */
class CjoBinaryFileDecompiler : BinaryFileDecompiler {
    /**
     * 返回 `.cjo` 文件在编辑器中展示的反编译文本。
     *
     * 本方法是 `LoadTextUtil` 的同步入口，不能向外抛异常（控制流异常除外）：
     * PSI 读取失败或没有产出任何文本时返回 [CangJieCompiledFileErrors] 的同源占位文本，
     * 让编辑器给出可诊断的失败信息而不是空文件。只有“根本没有可尝试的项目上下文”
     * （无项目 / 已释放 / 默认项目）才返回空文本，此时不存在反编译失败这一事实。
     */
    override fun decompile(file: VirtualFile): CharSequence {
        val project = try {
            ProjectLocator.getInstance().guessProjectForFile(file) ?: ProjectLocator.getPreferredProject(file)
        } catch (t: Throwable) {
            if (shouldIjPlatformExceptionBeRethrown(t)) throw t

            LOG.warn("CangJie `.cjo` decompiler could not resolve a project: path=${file.path}", t)
            null
        } ?: return ""

        if (project.isDisposed || project.isDefault) return ""

        val text = try {
            PsiManager.getInstance(project).findFile(file)?.text
        } catch (t: Throwable) {
            if (shouldIjPlatformExceptionBeRethrown(t)) throw t

            LOG.warn("CangJie `.cjo` decompiled text read failed: path=${file.path}", t)
            return failureText(file, "reading the decompiled PSI failed: ${t.javaClass.name}")
        }

        // 空文本等价于“没有任何反编译器产出内容”：view provider 的 factory 在 metadata 不可读
        // 或 decompiler 未注册时返回 null，getPsi 为 null，getContents 为空。
        return text?.takeIf(String::isNotEmpty)
            ?: failureText(file, "no decompiled content is available")
    }

    /**
     * 返回占位文本，并把失败原因记入日志。
     */
    private fun failureText(file: VirtualFile, cause: String): String {
        LOG.warn("CangJie `.cjo` decompiler produced placeholder text: path=${file.path}, cause=$cause")
        return CangJieCompiledFileErrors.decompileFailureText(cause)
    }
}
