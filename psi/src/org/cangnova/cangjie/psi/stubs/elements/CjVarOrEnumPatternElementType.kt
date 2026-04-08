package org.cangnova.cangjie.psi.stubs.elements

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.cangnova.cangjie.psi.stubs.CangJieVarOrEnumPatternStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieVarOrEnumPatternStubImpl
import java.io.IOException

/**
 * 裸名字歧义模式 ElementType。
 *
 * 该节点只保存词法层名字，避免 parser 在 stub 阶段就提前退化成 binding pattern。
 */
class CjVarOrEnumPatternElementType(debugName: String) :
    CjStubElementType<CangJieVarOrEnumPatternStub, CjVarOrEnumPattern>(
        debugName,
        CjVarOrEnumPattern::class.java,
        CangJieVarOrEnumPatternStub::class.java,
    ) {

    override fun createStub(psi: CjVarOrEnumPattern, parentStub: StubElement<*>?): CangJieVarOrEnumPatternStub {
        return CangJieVarOrEnumPatternStubImpl(parentStub, StringRef.fromString(psi.nameAsSafeName.asString()))
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieVarOrEnumPatternStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getName())
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieVarOrEnumPatternStub {
        return CangJieVarOrEnumPatternStubImpl(parentStub, dataStream.readName())
    }

    override fun indexStub(stub: CangJieVarOrEnumPatternStub, sink: IndexSink) {
        // 歧义模式本身不参与索引；后续语义阶段再决议为 binding / enum。
    }
}
