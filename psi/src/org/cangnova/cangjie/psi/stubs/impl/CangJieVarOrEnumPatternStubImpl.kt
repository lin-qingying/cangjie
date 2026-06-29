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
    /**
     * 保存 `nameRef` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val nameRef: StringRef?,
) : CangJieStubBaseImpl<CjVarOrEnumPattern>(parent, CjStubElementTypes.VAR_OR_ENUM_PATTERN),
    CangJieVarOrEnumPatternStub {

    /**
     * 实现 `getName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String? = StringRef.toString(nameRef)

    /**
     * 实现 `copyInto` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun copyInto(newParent: StubElement<*>?): CangJieVarOrEnumPatternStubImpl = CangJieVarOrEnumPatternStubImpl(
        parent = newParent,
        nameRef = nameRef,
    )
}
