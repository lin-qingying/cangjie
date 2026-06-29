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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.*
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubKindImpl
import java.io.IOException

/**
 * 表示 `StubIndexService`，承载PSI Stub中的语法节点、索引桩或辅助模型。
 */
open class StubIndexService protected constructor() {
    /**
     * 提供 `indexFile` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexFile(stub: CangJieFileStub, sink: IndexSink) {
    }

    /**
     * 提供 `indexEnumConstructor` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexEnumConstructor(stub: CangJieEnumConstructorStub, sink: IndexSink) {
    }

    /**
     * 提供 `indexEnum` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexEnum(stub: CangJieEnumStub, sink: IndexSink) {
    }

    /**
     * 提供 `indexImports` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexImports(stub: CangJieImportDirectiveStub, sink: IndexSink) {
    }

    /**
     * 提供 `indexClass` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexClass(stub: CangJieClassStub, sink: IndexSink) {
    }

    /**
     * 提供 `indexExtend` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexExtend(stub: CangJieExtendStub, sink: IndexSink) {
    }

    /**
     * 提供 `indexMacroFunction` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexMacroFunction(stub: CangJieMacroStub, sink: IndexSink) {
    }

    /**
     * 提供 `indexFunction` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexFunction(stub: CangJieNamedFunctionStub, sink: IndexSink) {
    }

    /**
     * 提供 `indexMainFunction` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexMainFunction(stub: CangJieMainFunctionStub, sink: IndexSink) {
    }

    /**
     * 提供 `indexTypeAlias` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexTypeAlias(stub: CangJieTypeAliasStub, sink: IndexSink) {
    }

    /**
     * 提供 `indexStruct` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexStruct(stub: CangJieStructStub, sink: IndexSink) {
    }

    /**
     * 提供 `indexPatternVariable` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexPatternVariable(stub: CangJieVariableStub, sink: IndexSink) {
    }

    /**
     * 提供 `indexFieldVariable` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexFieldVariable(stub: CangJieFieldStub, sink: IndexSink) {
    }

    /**
     * 提供 `indexProperty` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexProperty(stub: CangJiePropertyStub, sink: IndexSink) {
    }

    /**
     * 提供 `indexParameter` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexParameter(stub: CangJieParameterStubBase<*>, sink: IndexSink) {
    }

    /**
     * 提供 `indexInterface` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexInterface(stub: CangJieInterfaceStub, sink: IndexSink) {
    }

    /**
     * 提供 `indexAnnotation` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun indexAnnotation(stub: CangJieAnnotationStub, sink: IndexSink) {
    }

    /**
     * 提供 `createFileStub` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    open fun createFileStub(file: CjFile): CangJieFileStub {
        return CangJieFileStubImpl(file, file.packageFqNameByTree.asString())
    }

    /**
     * 提供 `serializeFileStub` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    @Throws(IOException::class)
    open fun serializeFileStub(stub: CangJieFileStub, dataStream: StubOutputStream) {
        CangJieFileStubKindImpl.serialize(stub.kind, dataStream)
    }

    /**
     * 提供 `deserializeFileStub` 操作，封装PSI Stub节点的访问、构造或判断逻辑。
     */
    @Throws(IOException::class)
    open fun deserializeFileStub(dataStream: StubInputStream): CangJieFileStub {
        val kind = CangJieFileStubKindImpl.deserialize(dataStream)
        return CangJieFileStubImpl(null, kind)
    }

    companion object {
        @JvmStatic
        fun getInstance(): StubIndexService {
            return ApplicationManager.getApplication().getService(StubIndexService::class.java) ?: NO_INDEX
        }

        private val NO_INDEX = StubIndexService()
    }
}
