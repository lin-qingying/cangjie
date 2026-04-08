package org.cangnova.cangjie.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.cangnova.cangjie.psi.stubs.CangJieVarOrEnumPatternStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * `VarOrEnumPattern` 的 Stub 实现。
 *
 * 这里只保存名字文本，不提前做 binding / enum 的语义分流。
 */
class CangJieVarOrEnumPatternStubImpl(
    parent: StubElement<out PsiElement>?,
    private val nameRef: StringRef?,
) : CangJieStubBaseImpl<CjVarOrEnumPattern>(parent, CjStubElementTypes.VAR_OR_ENUM_PATTERN),
    CangJieVarOrEnumPatternStub {

    override fun getName(): String? = StringRef.toString(nameRef)
}
