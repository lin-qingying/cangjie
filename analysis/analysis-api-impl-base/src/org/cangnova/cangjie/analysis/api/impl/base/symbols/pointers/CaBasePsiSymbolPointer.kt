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
    private val psiPointer: SmartPsiElementPointer<out CjElement>,
    private val expectedClass: KClass<S>,
    private val restoreSymbolByPsi: CaSession.(CjElement) -> CaSymbol?,
    originalSymbol: S?,
) : CaSymbolPointer<S> {
    private val originalSymbolRef = SoftReference(originalSymbol)

    override fun restoreSymbol(session: CaSession): S? {
        originalSymbolRef.get()?.let { symbol ->
            @Suppress("UNCHECKED_CAST")
            return symbol as S
        }

        val psi = psiPointer.element ?: return null
        val symbol = session.restoreSymbolByPsi(psi) ?: return null
        if (!expectedClass.isInstance(symbol)) return null

        @Suppress("UNCHECKED_CAST")
        return symbol as S
    }

    fun pointsToTheSameSymbolAs(other: CaSymbolPointer<CaSymbol>): Boolean = this === other ||
        other is CaBasePsiSymbolPointer<*> &&
        other.expectedClass == expectedClass &&
        other.psiPointer == psiPointer

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
        fun <S : CaSymbol> createForSymbolFromSource(
            symbol: S,
            expectedClass: KClass<S>,
            restoreSymbolByPsi: CaSession.(CjElement) -> CaSymbol?,
        ): CaBasePsiSymbolPointer<S>? {
            if (symbol.origin != CaSymbolOrigin.SOURCE) return null

            val psi = symbol.psi as? CjElement ?: return null
            return CaBasePsiSymbolPointer(psi, expectedClass, restoreSymbolByPsi, symbol)
        }

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

@CaImplementationDetail
fun createCompatibleSmartPointer(element: CjElement): SmartPsiElementPointer<out CjElement> {
    val containingFile = element.containingCjFile

    if (containingFile is SmartPointerIncompatiblePsiFile) {
        return SoftSmartPsiElementPointer(element, containingFile)
    }

    return SmartPointerManager.getInstance(containingFile.project)
        .createSmartPsiElementPointer(element, containingFile)
}

private class SoftSmartPsiElementPointer<T : PsiElement>(
    element: T,
    containingFile: PsiFile,
) : SmartPsiElementPointer<T> {
    private val project = containingFile.project
    private val elementRef = SoftReference(element)
    private val containingFileRef = SoftReference(containingFile)

    override fun getElement(): T? = elementRef.get()

    override fun getContainingFile(): PsiFile? = containingFileRef.get()

    override fun getVirtualFile(): VirtualFile? = containingFile?.virtualFile

    override fun getProject(): Project = project

    override fun getPsiRange(): Segment? = throw UnsupportedOperationException("Not supported")

    override fun getRange(): Segment? = throw UnsupportedOperationException("Not supported")
}
