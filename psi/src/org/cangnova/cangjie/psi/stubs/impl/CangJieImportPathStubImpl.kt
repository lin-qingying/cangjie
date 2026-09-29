package org.cangnova.cangjie.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.psi.CjImportGroup
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.stubs.CangJieImportGroupStub
import org.cangnova.cangjie.psi.stubs.CangJieImportItemStub
import org.cangnova.cangjie.psi.stubs.ImportPathStubData
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/** 分组 Stub 只保存自身前缀，导入项保持为子 Stub。 */
class CangJieImportGroupStubImpl(parent: StubElement<*>?, override val path: ImportPathStubData) :
    CangJieStubBaseImpl<CjImportGroup>(parent, CjStubElementTypes.IMPORT_GROUP), CangJieImportGroupStub {
    override fun copyInto(newParent: StubElement<*>?): CangJieImportGroupStubImpl = CangJieImportGroupStubImpl(newParent, path)
}

/** 单项 Stub 保存局部信息，不复制其祖先的前缀。 */
class CangJieImportItemStubImpl(
    parent: StubElement<*>?,
    override val path: ImportPathStubData,
    override val isAllUnder: Boolean,
    override val aliasName: String?,
) : CangJieStubBaseImpl<CjImportItem>(parent, CjStubElementTypes.IMPORT_ITEM), CangJieImportItemStub {
    override fun copyInto(newParent: StubElement<*>?): CangJieImportItemStubImpl =
        CangJieImportItemStubImpl(newParent, path, isAllUnder, aliasName)
}
