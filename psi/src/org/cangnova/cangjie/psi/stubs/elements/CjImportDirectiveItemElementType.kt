/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */
package org.cangnova.cangjie.psi.stubs.elements

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.*
import org.cangnova.cangjie.psi.stubs.impl.*

/** 语句 Stub 不再保存重复的扁平导入列表。 */
class CjImportDirectiveElementType(debugName: String) :
    CjStubElementType<CangJieImportDirectiveStub, CjImportDirective>(
        debugName, CjImportDirective::class.java, CangJieImportDirectiveStub::class.java,
    ) {
    override fun createStub(psi: CjImportDirective, parentStub: StubElement<*>?): CangJieImportDirectiveStub =
        CangJieImportDirectiveStubImpl(requireNotNull(parentStub), psi.containingCjFile.packageFqName, psi.isValidSyntax)

    override fun serialize(stub: CangJieImportDirectiveStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getPackageFqName()?.asString())
        dataStream.writeBoolean(stub.isValidSyntax)
    }
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieImportDirectiveStub =
        CangJieImportDirectiveStubImpl(parentStub, dataStream.readNameString()?.let(::FqName), dataStream.readBoolean())

    override fun indexStub(stub: CangJieImportDirectiveStub, sink: IndexSink) {
        StubIndexService.getInstance().indexImports(stub, sink)
    }
}

/** 花括号分组与单项都是真实 Stub 节点，局部路径协议由两者共享。 */
class CjImportGroupElementType(debugName: String) :
    CjStubElementType<CangJieImportGroupStub, CjImportGroup>(
        debugName, CjImportGroup::class.java, CangJieImportGroupStub::class.java,
    ) {
    override fun createStub(psi: CjImportGroup, parentStub: StubElement<*>?): CangJieImportGroupStub =
        CangJieImportGroupStubImpl(parentStub, pathData(psi, psi.hasOrganizationQualifier))
    override fun serialize(stub: CangJieImportGroupStub, dataStream: StubOutputStream) = writePath(dataStream, stub.path)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieImportGroupStub =
        CangJieImportGroupStubImpl(parentStub, readPath(dataStream))
}

class CjImportItemElementType(debugName: String) :
    CjStubElementType<CangJieImportItemStub, CjImportItem>(
        debugName, CjImportItem::class.java, CangJieImportItemStub::class.java,
    ) {
    override fun createStub(psi: CjImportItem, parentStub: StubElement<*>?): CangJieImportItemStub =
        CangJieImportItemStubImpl(parentStub, pathData(psi, psi.hasOrganizationQualifier), psi.isAllUnder, psi.aliasName)
    override fun serialize(stub: CangJieImportItemStub, dataStream: StubOutputStream) {
        writePath(dataStream, stub.path)
        dataStream.writeBoolean(stub.isAllUnder)
        dataStream.writeName(stub.aliasName)
    }
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieImportItemStub =
        CangJieImportItemStubImpl(parentStub, readPath(dataStream), dataStream.readBoolean(), dataStream.readNameString())
}

private fun pathData(psi: CjImportPathOwner, hasQualifier: Boolean): ImportPathStubData = ImportPathStubData(
    psi.organizationReference?.referencedNameAsName,
    importFqName(psi.importedReference),
    psi.organizationReference != null,
    hasQualifier,
    psi.importedReference != null,
    !hasImportContainerErrors(psi),
)

/** 名称与存在位独立编码，损坏路径仍能保留其原始角色。 */
private fun writePath(stream: StubOutputStream, path: ImportPathStubData) {
    stream.writeName(path.organizationName?.asString())
    stream.writeName(path.localFqName?.asString())
    stream.writeBoolean(path.hasOrganizationReference)
    stream.writeBoolean(path.hasOrganizationQualifier)
    stream.writeBoolean(path.hasPathReference)
    stream.writeBoolean(path.isValidSyntax)
}

private fun readPath(stream: StubInputStream): ImportPathStubData = ImportPathStubData(
    stream.readNameString()?.let(Name::identifier),
    stream.readNameString()?.let(::FqName),
    stream.readBoolean(), stream.readBoolean(), stream.readBoolean(), stream.readBoolean(),
)
