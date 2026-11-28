package org.cangnova.cangjie.psi.stubs.elements

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.parsing.CjmpDirectMemberSyntaxFacts
import org.cangnova.cangjie.parsing.CjmpNominalSyntaxFacts
import org.cangnova.cangjie.psi.CjNodeTypes
import java.io.IOException

/**
 * 使用节点类型的声明常量名作为持久标识，避免依赖运行时分配的 element type 索引。
 * 无法识别的节点必须报错，不能把不同声明合并或丢弃。
 */
private object DeclarationTypes {
    private val byName: Map<String, IElementType> by lazy {
        CjNodeTypes::class.java.fields
            .filter { IElementType::class.java.isAssignableFrom(it.type) }
            .associate { it.name to (it.get(null) as IElementType) }
    }
    private val byType: Map<IElementType, String> by lazy {
        byName.entries.sortedBy { it.key }.associate { it.value to it.key }
    }

    fun name(type: IElementType): String =
        byType[type] ?: throw IOException("Unknown Cangjie declaration element type: $type")

    fun type(name: String): IElementType =
        byName[name] ?: throw IOException("Unknown Cangjie declaration element type identifier: $name")
}

internal fun writeCjmpNominalSyntaxFacts(stream: StubOutputStream, facts: CjmpNominalSyntaxFacts?) {
    stream.writeBoolean(facts != null)
    if (facts == null) return
    stream.writeVarInt(facts.members.size)
    for (member in facts.members) {
        stream.writeName(DeclarationTypes.name(member.declarationType))
        stream.writeBoolean(member.isCommon)
        stream.writeBoolean(member.isSpecific)
        stream.writeBoolean(member.isPlatform)
        stream.writeBoolean(member.hasFunctionBody)
        stream.writeBoolean(member.hasPropertyAccessor)
        stream.writeBoolean(member.hasInitializer)
    }
}

internal fun readCjmpNominalSyntaxFacts(stream: StubInputStream): CjmpNominalSyntaxFacts? {
    if (!stream.readBoolean()) return null
    return CjmpNominalSyntaxFacts(List(stream.readVarInt()) {
        val name = stream.readNameString() ?: throw IOException("Missing Cangjie declaration element type identifier")
        CjmpDirectMemberSyntaxFacts(
            DeclarationTypes.type(name),
            stream.readBoolean(),
            stream.readBoolean(),
            stream.readBoolean(),
            stream.readBoolean(),
            stream.readBoolean(),
            stream.readBoolean(),
        )
    })
}
