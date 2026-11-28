package org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import org.cangnova.cangjie.psi.CjFile

/** 明确撤销的 source generation 终止当前读请求；宏编排可按此类型重新捕获，不能包装任意 stale PSI 异常。 */
class LLCfirSourcePsiInvalidatedException internal constructor() : ProcessCanceledException() {
    override val message: String get() = "Source PSI generation was revoked"
}

/**
 * source CFIR 和宏构造共同使用的真实 PSI generation。
 *
 * file pointer 只用于恢复当前文件并校验 identity，不按名字、ClassId 或 offset 修补旧 CFIR。
 * 宏输入还记录内容版本，任一文件变化即撤销整个批次。FileStructure 只固定 provider/PSI identity，
 * 其同一 generation 内的 in-block 内容变更继续由正式 declaration invalidation 协议处理。
 */
class LLCfirSourcePsiGeneration private constructor(private val entries: List<Entry>, private val compareModificationStamps: Boolean) {
    val files: List<CjFile> = entries.map { it.file }

    private class Entry(
        val file: CjFile,
        val provider: FileViewProvider,
        val modificationStamp: Long,
        val pointer: SmartPsiElementPointer<CjFile>,
    ) {
        fun isCurrent(compareModificationStamps: Boolean): Boolean {
            if (!file.isValid || pointer.element !== file) return false
            if (file.viewProvider !== provider || compareModificationStamps && file.modificationStamp != modificationStamp) return false
            val virtualFile = pointer.virtualFile ?: return !file.isPhysical
            if (file.isPhysical && PsiManager.getInstance(file.project).findFile(virtualFile) !== file) return false
            return true
        }
    }

    fun isCurrent(): Boolean = ReadAction.compute<Boolean, RuntimeException> { entries.all { it.isCurrent(compareModificationStamps) } }

    /** 读取已捕获的 CFIR source 之前先验证整个批次，不把失效对象交给 raw builder/reparse/splice。 */
    fun requireCurrent() {
        if (!isCurrent()) throw LLCfirSourcePsiInvalidatedException()
    }

    companion object {
        /** 仅在读取当前 source files 的同一读访问中创建 generation，不接受失效的初始 PSI。 */
        fun capture(files: List<CjFile>, compareModificationStamps: Boolean = true): LLCfirSourcePsiGeneration {
            check(ApplicationManager.getApplication().isReadAccessAllowed) { "Macro PSI generation must be captured under read access" }
            val entries = files.map { file ->
                if (!file.isValid) throw LLCfirSourcePsiInvalidatedException()
                if (file.isPhysical) {
                    val virtualFile = checkNotNull(file.virtualFile) { "Physical source PSI must have a virtual file" }
                    if (PsiManager.getInstance(file.project).findFile(virtualFile) !== file) throw LLCfirSourcePsiInvalidatedException()
                }
                Entry(file, file.viewProvider, file.modificationStamp,
                    SmartPointerManager.getInstance(file.project).createSmartPsiElementPointer(file))
            }
            return LLCfirSourcePsiGeneration(entries, compareModificationStamps)
        }
    }
}
