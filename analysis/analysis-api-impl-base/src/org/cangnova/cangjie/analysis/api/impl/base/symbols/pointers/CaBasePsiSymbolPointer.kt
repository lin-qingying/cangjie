package org.cangnova.cangjie.analysis.api.impl.base.symbols.pointers

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Segment
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.psi.CjElement
import java.lang.ref.SoftReference
import kotlin.reflect.KClass

/**
 * 对齐 Kotlin `KaBasePsiSymbolPointer` 的通用 PSI 指针实现。
 *
 * 这层负责：
 * 1. 把 source PSI 通过 smart pointer 跨分析会话保存；
 * 2. 在 restore 时重新走 Analysis API 的 PSI -> symbol 主入口；
 * 3. 为 source-only 声明提供统一指针基座，而不是继续按符号种类各写一套恢复逻辑。
 */
@CaImplementationDetail
class CaBasePsiSymbolPointer<S : CaSymbol> private constructor(
    /**
     * 指向 source PSI 的 smart pointer。
     */
    private val psiPointer: SmartPsiElementPointer<out CjElement>,
    /**
     * 恢复后期望得到的符号类型。
     */
    private val expectedClass: KClass<S>,
    /**
     * 从 PSI 恢复符号的 Analysis API 入口。
     */
    private val restoreSymbolByPsi: CaSession.(CjElement) -> CaSymbol?,
    originalSymbol: S?,
) : CaBaseCachedSymbolPointer<S>(originalSymbol) {
    /**
     * 在缓存不可用时通过 PSI pointer 恢复符号。
     */
    override fun restoreIfNotCached(session: CaSession): S? {
        val psi = psiPointer.element ?: return null
        val symbol = session.restoreSymbolByPsi(psi) ?: return null
        if (!expectedClass.isInstance(symbol)) return null

        @Suppress("UNCHECKED_CAST")
        return symbol as S
    }

    /**
     * 判断另一个 pointer 是否指向同一 PSI 与同一符号类型。
     */
    fun pointsToTheSameSymbolAs(other: CaSymbolPointer<CaSymbol>): Boolean = this === other ||
        other is CaBasePsiSymbolPointer<*> &&
        other.expectedClass == expectedClass &&
        other.psiPointer == psiPointer

    /**
     * 通过 PSI 元素创建兼容宿主的 PSI symbol pointer。
     */
    constructor(
        psi: CjElement,
        expectedClass: KClass<S>,
        restoreSymbolByPsi: CaSession.(CjElement) -> CaSymbol?,
        originalSymbol: S?,
    ) : this(
        createCompatibleSmartPointer(psi),
        expectedClass,
        restoreSymbolByPsi,
        originalSymbol,
    )

    @CaImplementationDetail
    companion object {
        /**
         * 为 source-origin 符号创建 PSI pointer。
         */
        fun <S : CaSymbol> createForSymbolFromSource(
            symbol: S,
            expectedClass: KClass<S>,
            restoreSymbolByPsi: CaSession.(CjElement) -> CaSymbol?,
        ): CaBasePsiSymbolPointer<S>? {
            if (symbol.origin != CaSymbolOrigin.SOURCE) return null

            val psi = symbol.psi as? CjElement ?: return null
            return CaBasePsiSymbolPointer(psi, expectedClass, restoreSymbolByPsi, symbol)
        }

        /**
         * 为给定 PSI 元素创建 PSI pointer。
         */
        fun <S : CaSymbol> createForSymbolFromPsi(
            element: CjElement,
            expectedClass: KClass<S>,
            restoreSymbolByPsi: CaSession.(CjElement) -> CaSymbol?,
            originalSymbol: S?,
        ): CaBasePsiSymbolPointer<S> {
            return CaBasePsiSymbolPointer(element, expectedClass, restoreSymbolByPsi, originalSymbol)
        }
    }
}

/**
 * 某些宿主文件不适合交给 IntelliJ 常规 smart pointer 管理时，
 * 可以通过这个标记接口切换到软引用 pointer。
 */
@CaImplementationDetail
interface SmartPointerIncompatiblePsiFile

/**
 * 根据包含文件能力创建常规 smart pointer 或软引用 pointer。
 */
@CaImplementationDetail
fun createCompatibleSmartPointer(element: CjElement): SmartPsiElementPointer<out CjElement> {
    val containingFile = element.containingCjFile

    if (containingFile is SmartPointerIncompatiblePsiFile) {
        return SoftSmartPsiElementPointer(element, containingFile)
    }

    return SmartPointerManager.getInstance(containingFile.project)
        .createSmartPsiElementPointer(element, containingFile)
}

/**
 * 以软引用保存 PSI 元素和文件的 lightweight pointer。
 */
private class SoftSmartPsiElementPointer<T : PsiElement>(
    element: T,
    containingFile: PsiFile,
) : SmartPsiElementPointer<T> {
    /**
     * PSI 所属 project。
     */
    private val project = containingFile.project

    /**
     * PSI 元素软引用。
     */
    private val elementRef = SoftReference(element)

    /**
     * 包含文件软引用。
     */
    private val containingFileRef = SoftReference(containingFile)

    /**
     * 返回当前仍可达的 PSI 元素。
     */
    override fun getElement(): T? = elementRef.get()

    /**
     * 返回当前仍可达的包含文件。
     */
    override fun getContainingFile(): PsiFile? = containingFileRef.get()

    /**
     * 返回包含文件对应的虚文件。
     */
    override fun getVirtualFile(): VirtualFile? = containingFile?.virtualFile

    /**
     * 返回 PSI 所属 project。
     */
    override fun getProject(): Project = project

    /**
     * 软引用 pointer 不支持 PSI range。
     */
    override fun getPsiRange(): Segment? = throw UnsupportedOperationException("Not supported")

    /**
     * 软引用 pointer 不支持文档 range。
     */
    override fun getRange(): Segment? = throw UnsupportedOperationException("Not supported")
}
