package org.cangnova.cangjie.psi.stubs.elements

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import java.io.IOException
import java.util.UUID
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjmpRoleInputCompleteness
import org.cangnova.cangjie.CjmpRoleModifiers
import org.cangnova.cangjie.parsing.CjmpFileRole
import org.cangnova.cangjie.parsing.CjmpFileRoleSyntaxSnapshot
import org.cangnova.cangjie.parsing.CjmpParsedRoleEvent
import org.cangnova.cangjie.parsing.CjmpParsedRoleEventId
import org.cangnova.cangjie.parsing.CjmpParsingContext
import org.cangnova.cangjie.parsing.CjModifierKind
import org.cangnova.cangjie.parsing.CjParsedDeclarationModifiers
import org.cangnova.cangjie.parsing.CjSourceTokenId
import org.cangnova.cangjie.parsing.CjmpModifierDeclarationKind
import org.cangnova.cangjie.parsing.CjScopeKind
import org.cangnova.cangjie.parsing.CjLexicalSourceDescriptor

/** 文件 stub 的角色语法快照编码；空值表示没有可用来源，而不是空事件文件。 */
internal object CjmpFileRoleSnapshotCodec {
    private const val VERSION = 7

    fun write(out: StubOutputStream, snapshot: CjmpFileRoleSyntaxSnapshot?) {
        out.writeBoolean(snapshot != null)
        if (snapshot == null) return
        out.writeByte(VERSION)
        out.writeBoolean(snapshot.sourceFingerprint != null)
        snapshot.sourceFingerprint?.let(out::writeUTF)
        out.writeBoolean(snapshot.lexicalSource != null)
        snapshot.lexicalSource?.let { descriptor ->
            out.writeLong(descriptor.sourceIdentity.mostSignificantBits)
            out.writeLong(descriptor.sourceIdentity.leastSignificantBits)
            out.writeInt(descriptor.tokenCount)
        }
        when (val context = snapshot.parsingContext) {
            CjmpParsingContext.SyntaxOnly -> out.writeByte(0)
            is CjmpParsingContext.Compilation -> {
                out.writeByte(1)
                out.writeBoolean(context.compileCommon)
                out.writeBoolean(context.compileSpecific)
                out.writeBoolean(context.enabled)
                out.writeByte(roleTag(context.initialFileRole))
                out.writeBoolean(context.mixedCommonSpecificDeclarations)
                out.writeBoolean(context.commonMemberInSpecificOuter)
                out.writeBoolean(context.parameterRoleMatching)
                out.writeBoolean(context.genericDeclarationsAvailable)
                out.writeBoolean(context.parameterDefaultValueRulesAvailable)
                out.writeBoolean(context.modifierSyntaxAvailable)
                out.writeBoolean(context.platformModifierSyntaxAvailable)
                out.writeBoolean(context.primaryConstructorRoleModifiersAvailable)
            }
        }
        out.writeByte(roleTag(snapshot.entryFileRole))
        out.writeByte(when (snapshot.completeness) {
            CjmpRoleInputCompleteness.COMPLETE -> 0
            CjmpRoleInputCompleteness.PREFIX -> 1
            CjmpRoleInputCompleteness.PARTIAL -> 2
        })
        out.writeInt(snapshot.events.size)
        for (event in snapshot.events) {
            writeEventId(out, event.id)
            out.writeInt(event.declarationOffset)
            when (val kind = event.kind) {
                is CjmpRoleEventKind.Check -> { out.writeByte(0); writeModifiers(out, kind.modifiers) }
                is CjmpRoleEventKind.Extend -> { out.writeByte(1); writeModifiers(out, kind.modifiers); out.writeInt(kind.distinctModifierCount) }
                CjmpRoleEventKind.Skipped -> out.writeByte(2)
                CjmpRoleEventKind.Unknown -> out.writeByte(3)
            }
        }
        out.writeInt(snapshot.stubBindings.size)
        for ((path, ids) in snapshot.stubBindings) {
            out.writeInt(path.size)
            path.forEach(out::writeInt)
            out.writeInt(ids.size)
            ids.forEach { writeEventId(out, it) }
        }
        out.writeInt(snapshot.stubDeclarationModifiers.size)
        for ((path, modifiers) in snapshot.stubDeclarationModifiers) {
            out.writeInt(path.size)
            path.forEach(out::writeInt)
            out.writeLong(modifiers.rootIdentity.mostSignificantBits)
            out.writeLong(modifiers.rootIdentity.leastSignificantBits)
            out.writeInt(modifiers.attributes.size)
            modifiers.attributes.forEach { out.writeUTF(it.name) }
            out.writeBoolean(modifiers.hasConstModifier)
            out.writeLong(modifiers.sourceToken.sourceIdentity.mostSignificantBits)
            out.writeLong(modifiers.sourceToken.sourceIdentity.leastSignificantBits)
            out.writeInt(modifiers.sourceToken.ordinal)
            out.writeUTF(modifiers.declarationKind.name)
            out.writeUTF(modifiers.scope.name)
        }
    }

    fun read(input: StubInputStream): CjmpFileRoleSyntaxSnapshot? {
        if (!readFlag(input)) return null
        if (input.readUnsignedByte() != VERSION) throw IOException("Unsupported CJMP role snapshot version")
        val sourceFingerprint = if (readFlag(input)) input.readUTF() else null
        val lexicalSource = if (readFlag(input)) {
            val identity = UUID(input.readLong(), input.readLong())
            val tokenCount = input.readInt()
            if (tokenCount < 0) throw IOException("Invalid lexical source token count")
            CjLexicalSourceDescriptor(identity, tokenCount)
        } else null
        val context = when (input.readUnsignedByte()) {
            0 -> CjmpParsingContext.SyntaxOnly
            1 -> CjmpParsingContext.Compilation(
                readFlag(input), readFlag(input), readFlag(input), readRole(input),
                readFlag(input), readFlag(input), readFlag(input), readFlag(input),
                readFlag(input), readFlag(input), readFlag(input), readFlag(input),
            )
            else -> throw IOException("Invalid CJMP parsing context")
        }
        val entryRole = readRole(input)
        val completeness = when (input.readUnsignedByte()) {
            0 -> CjmpRoleInputCompleteness.COMPLETE
            1 -> CjmpRoleInputCompleteness.PREFIX
            2 -> CjmpRoleInputCompleteness.PARTIAL
            else -> throw IOException("Invalid CJMP role completeness")
        }
        val count = input.readInt()
        if (count < 0) throw IOException("Invalid CJMP role event count")
        val events = ArrayList<CjmpParsedRoleEvent>()
        repeat(count) {
            val id = readEventId(input)
            val offset = input.readInt()
            val kind = when (input.readUnsignedByte()) {
                0 -> CjmpRoleEventKind.Check(readModifiers(input))
                1 -> {
                    val modifiers = readModifiers(input)
                    val modifierCount = input.readInt()
                    if (modifierCount < listOf(modifiers.common, modifiers.specific, modifiers.platform).count { it }) {
                        throw IOException("Invalid CJMP extend modifier count")
                    }
                    CjmpRoleEventKind.Extend(modifiers, modifierCount)
                }
                2 -> CjmpRoleEventKind.Skipped
                3 -> CjmpRoleEventKind.Unknown
                else -> throw IOException("Invalid CJMP role event")
            }
            if (offset < 0) throw IOException("Invalid CJMP role event offset")
            events.add(CjmpParsedRoleEvent(id, offset, kind))
        }
        val bindingCount = input.readInt()
        if (bindingCount < 0 || bindingCount > count) throw IOException("Invalid CJMP role binding count")
        val bindings = linkedMapOf<List<Int>, List<CjmpParsedRoleEventId>>()
        repeat(bindingCount) {
            val pathLength = input.readInt()
            if (pathLength <= 0 || pathLength > 65536) throw IOException("Invalid CJMP stub path length")
            val path = List(pathLength) { input.readInt() }
            val sequenceCount = input.readInt()
            if (sequenceCount <= 0 || sequenceCount > count) throw IOException("Invalid CJMP role binding size")
            val ids = List(sequenceCount) { readEventId(input) }
            if (bindings.put(path, ids) != null) throw IOException("Duplicate CJMP stub path")
        }
        val modifierCount = input.readInt()
        if (modifierCount < 0) throw IOException("Invalid declaration modifier binding count")
        val declarationModifiers = linkedMapOf<List<Int>, CjParsedDeclarationModifiers>()
        repeat(modifierCount) {
            val pathLength = input.readInt()
            if (pathLength <= 0 || pathLength > 65536) throw IOException("Invalid declaration modifier stub path length")
            val path = List(pathLength) { input.readInt() }
            val rootIdentity = UUID(input.readLong(), input.readLong())
            val attributeCount = input.readInt()
            if (attributeCount < 0 || attributeCount > CjModifierKind.entries.size) throw IOException("Invalid declaration modifier attribute count")
            val attributes = linkedSetOf<CjModifierKind>()
            repeat(attributeCount) {
                val name = input.readUTF()
                val attribute = CjModifierKind.entries.singleOrNull { it.name == name }
                    ?: throw IOException("Invalid declaration modifier attribute: $name")
                if (!attributes.add(attribute)) throw IOException("Duplicate declaration modifier attribute")
            }
            val hasConstModifier = readFlag(input)
            val sourceIdentity = UUID(input.readLong(), input.readLong())
            val sourceOrdinal = input.readInt()
            if (sourceOrdinal < 0) throw IOException("Invalid declaration source token ordinal")
            val kindName = input.readUTF()
            val scopeName = input.readUTF()
            val declarationKind = CjmpModifierDeclarationKind.entries.singleOrNull { it.name == kindName }
                ?: throw IOException("Invalid declaration grammar kind: $kindName")
            val scope = CjScopeKind.entries.singleOrNull { it.name == scopeName }
                ?: throw IOException("Invalid declaration grammar scope: $scopeName")
            val modifiers = try { CjParsedDeclarationModifiers(rootIdentity, context, attributes, hasConstModifier,
                CjSourceTokenId(sourceIdentity, sourceOrdinal), declarationKind, scope) }
                catch (e: IllegalArgumentException) { throw IOException("Invalid declaration modifier attributes", e) }
            if (declarationModifiers.put(path, modifiers) != null) throw IOException("Duplicate declaration modifier stub path")
        }
        return try {
            CjmpFileRoleSyntaxSnapshot(context, entryRole, completeness, events, bindings, sourceFingerprint, declarationModifiers,
                lexicalSource)
        } catch (e: IllegalArgumentException) {
            throw IOException("Invalid CJMP role events or binding", e)
        }
    }

    /** 原序写出完整身份，不能用快照位置或 UUID 排序替代检查顺序。 */
    private fun writeEventId(out: StubOutputStream, id: CjmpParsedRoleEventId) {
        out.writeLong(id.rootIdentity.mostSignificantBits)
        out.writeLong(id.rootIdentity.leastSignificantBits)
        out.writeLong(id.sequence)
    }

    private fun readEventId(input: StubInputStream): CjmpParsedRoleEventId {
        val rootIdentity = UUID(input.readLong(), input.readLong())
        val sequence = input.readLong()
        if (sequence < 0) throw IOException("Invalid CJMP role event identity")
        return CjmpParsedRoleEventId(rootIdentity, sequence)
    }

    private fun writeModifiers(out: StubOutputStream, modifiers: CjmpRoleModifiers) {
        out.writeBoolean(modifiers.common)
        out.writeBoolean(modifiers.specific)
        out.writeBoolean(modifiers.platform)
    }

    private fun readModifiers(input: StubInputStream): CjmpRoleModifiers =
        CjmpRoleModifiers(readFlag(input), readFlag(input), readFlag(input))

    private fun readFlag(input: StubInputStream): Boolean = when (input.readUnsignedByte()) {
        0 -> false
        1 -> true
        else -> throw IOException("Invalid CJMP role flag")
    }

    private fun roleTag(role: CjmpFileRole): Int = when (role) {
        CjmpFileRole.NONE -> 0
        CjmpFileRole.COMMON -> 1
        CjmpFileRole.SPECIFIC -> 2
        CjmpFileRole.BOTH -> 3
    }

    private fun readRole(input: StubInputStream): CjmpFileRole = when (input.readUnsignedByte()) {
        0 -> CjmpFileRole.NONE
        1 -> CjmpFileRole.COMMON
        2 -> CjmpFileRole.SPECIFIC
        3 -> CjmpFileRole.BOTH
        else -> throw IOException("Invalid CJMP file role")
    }
}
