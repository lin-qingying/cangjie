package org.cangnova.cangjie.analysis.api.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.psi.*

/**
 * "原始 PSI" 关联协议(已过时入口)。
 *
 * 设计要点/职责:
 * - 在 dependent session 下,某些声明/文件以 fake copy 形式参与分析,
 *   本协议负责在 fake 与 original 之间维护双向关联。
 * - 整个协议为遗留 API,后续应由 dependent session 体系自身追踪关联,不在新代码中调用。
 *
 * 对齐 Kotlin Analysis API 的 `KaOriginalPsiProvider`(同样标记为 obsolete)。
 */
interface CaOriginalPsiProvider : CaLifetimeOwner {
    /**
     * 若该声明位于 dependent session 的 fake 文件中,返回其在原始文件中的对应声明,否则返回 `null`。
     */
    @Deprecated("Obsolete API")
    fun CjDeclaration.getOriginalDeclaration(): CjDeclaration?

    /**
     * 若该文件是 dependent session 中的 fake 文件,返回其对应的原始文件,否则返回 `null`。
     */
    @Deprecated("Obsolete API")
    fun CjFile.getOriginalCjFile(): CjFile?

    /**
     * 记录 [declaration] 为该声明的原始声明,后续可通过 [getOriginalDeclaration] 取回。
     */
    @Deprecated("Obsolete API")
    fun CjDeclaration.recordOriginalDeclaration(declaration: CjDeclaration)

    /**
     * 记录 [file] 为该文件的原始文件,后续可通过 [getOriginalCjFile] 取回。
     */
    @Deprecated("Obsolete API")
    fun CjFile.recordOriginalCjFile(file: CjFile)
}
