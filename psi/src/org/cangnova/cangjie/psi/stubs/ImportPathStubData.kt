package org.cangnova.cangjie.psi.stubs

import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjImportGroup
import org.cangnova.cangjie.psi.CjImportItem

/** 局部路径的 Stub 协议；存在位区分组织、分隔符和包路径在不完整源码中的角色。 */
data class ImportPathStubData(
    val organizationName: Name?,
    val localFqName: FqName?,
    val hasOrganizationReference: Boolean,
    val hasOrganizationQualifier: Boolean,
    val hasPathReference: Boolean,
    val isValidSyntax: Boolean,
)

interface CangJieImportGroupStub : StubElement<CjImportGroup> {
    val path: ImportPathStubData
}

interface CangJieImportItemStub : StubElement<CjImportItem> {
    val path: ImportPathStubData
    val isAllUnder: Boolean
    val aliasName: String?
}
