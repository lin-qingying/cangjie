package org.cangnova.cangjie.cfir.serialization.cjd

import PackageFormat.*
import PackageFormat.Package
import com.google.flatbuffers.FlatBufferBuilder
import org.cangnova.cangjie.metadata.model.Attribute
import java.nio.ByteBuffer

/** 纯 FlatBuffers 夹具：不启动 IDE/session，不依赖本机 SDK 或宏进程。所有引用均为 1-based。 */
@OptIn(ExperimentalUnsignedTypes::class)
internal class CjdBinaryFixture {
    val builder = FlatBufferBuilder(1024)
    private val declarations = mutableListOf<Int>()
    private val types = mutableListOf<Int>()

    fun primitive(kind: UShort): UInt = addType(SemaTy.createSemaTy(builder, kind, 0, SemaTyInfo.NONE, 0))

    fun named(kind: UShort, declaration: UInt, arguments: List<UInt> = emptyList()): UInt {
        val id = FullId.createFullId(builder, -2, 0, declaration)
        val info = if (kind == TypeKind.Generic) GenericTyInfo.createGenericTyInfo(builder, id, 0)
            else CompositeTyInfo.createCompositeTyInfo(builder, id, false)
        val args = SemaTy.createTypeArgsVector(builder, arguments.toUIntArray())
        return addType(SemaTy.createSemaTy(builder, kind, args,
            if (kind == TypeKind.Generic) SemaTyInfo.GenericTyInfo else SemaTyInfo.CompositeTyInfo, info))
    }

    fun compound(kind: UShort, arguments: List<UInt>, size: Long = 0, result: UInt = 0u): UInt {
        val info = when (kind) {
            TypeKind.VArray -> ArrayTyInfo.createArrayTyInfo(builder, size)
            TypeKind.Func -> FuncTyInfo.createFuncTyInfo(builder, result, false, false)
            else -> 0
        }
        val infoKind = when (kind) {
            TypeKind.VArray -> SemaTyInfo.ArrayTyInfo
            TypeKind.Func -> SemaTyInfo.FuncTyInfo
            else -> SemaTyInfo.NONE
        }
        val args = SemaTy.createTypeArgsVector(builder, arguments.toUIntArray())
        return addType(SemaTy.createSemaTy(builder, kind, args, infoKind, info))
    }

    private fun addType(offset: Int): UInt {
        types += offset
        return types.size.toUInt()
    }

    fun generic(parameters: List<UInt>, constraints: List<Pair<UInt, List<UInt>>> = emptyList()): Int {
        val offsets = constraints.map { (type, upperBounds) ->
            val uppers = Constraint.createUppersVector(builder, upperBounds.toUIntArray())
            Constraint.startConstraint(builder)
            Constraint.addType(builder, type)
            Constraint.addUppers(builder, uppers)
            Constraint.endConstraint(builder)
        }
        val params = Generic.createTypeParametersVector(builder, parameters.toUIntArray())
        val bounds = Generic.createConstraintsVector(builder, offsets.toIntArray())
        return Generic.createGeneric(builder, params, bounds)
    }

    fun function(name: String, parameters: List<UInt>, topLevel: Boolean = true, generic: Int = 0): UInt {
        val params = FuncParamList.createParamsVector(builder, parameters.toUIntArray())
        val list = FuncParamList.createFuncParamList(builder, params, 0)
        val lists = FuncBody.createParamListsVector(builder, intArrayOf(list))
        val body = FuncBody.createFuncBody(builder, lists, 0u, 0u, false, 0u)
        val info = FuncInfo.createFuncInfo(builder, body, 0u, 0u, 0, false, false, false)
        return declaration(name, DeclKind.FuncDecl, topLevel = topLevel, generic = generic, infoKind = DeclInfo.FuncInfo, info = info)
    }

    fun classDecl(name: String, members: List<UInt>, annotationsExported: Boolean = true): UInt {
        val body = ClassInfo.createBodyVector(builder, members.toUIntArray())
        val info = ClassInfo.createClassInfo(builder, 0, body, 0, false, 0u, false, 0u)
        return declaration(name, DeclKind.ClassDecl, attributes = listOf(if (annotationsExported) Attribute.PUBLIC else Attribute.PRIVATE),
            infoKind = DeclInfo.ClassInfo, info = info)
    }

    fun pattern(bindings: List<UInt>): UInt {
        val patterns = bindings.map { binding ->
            val refs = Pattern.createExprsVector(builder, uintArrayOf(binding))
            Pattern.startPattern(builder)
            Pattern.addKind(builder, PatternKind.VarPattern)
            Pattern.addExprs(builder, refs)
            Pattern.endPattern(builder)
        }
        val nested = Pattern.createPatternsVector(builder, patterns.toIntArray())
        Pattern.startPattern(builder)
        Pattern.addKind(builder, PatternKind.TuplePattern)
        Pattern.addPatterns(builder, nested)
        val tuple = Pattern.endPattern(builder)
        val info = VarWithPatternInfo.createVarWithPatternInfo(builder, false, false, tuple, 0u)
        return declaration("pattern", DeclKind.VarWithPatternDecl, infoKind = DeclInfo.VarWithPatternInfo, info = info)
    }

    fun extend(type: UInt, inherited: List<UInt> = emptyList(), generic: Int = 0): UInt {
        val supers = ExtendInfo.createInheritedTypesVector(builder, inherited.toUIntArray())
        val info = ExtendInfo.createExtendInfo(builder, supers, 0)
        return declaration("", DeclKind.ExtendDecl, type, generic = generic, infoKind = DeclInfo.ExtendInfo, info = info)
    }

    fun declaration(
        name: String,
        kind: UShort = DeclKind.VarDecl,
        type: UInt = 0u,
        topLevel: Boolean = true,
        generic: Int = 0,
        attributes: List<Attribute> = listOf(Attribute.PUBLIC),
        infoKind: UByte = DeclInfo.NONE,
        info: Int = 0,
    ): UInt {
        val identifier = builder.createString(name)
        val words = ULongArray((attributes.maxOfOrNull { it.ordinal } ?: 0) / 64 + 1)
        attributes.forEach { words[it.ordinal / 64] = words[it.ordinal / 64] or (1uL shl (it.ordinal % 64)) }
        val attrs = Decl.createAttributesVector(builder, words)
        Decl.startDecl(builder)
        Decl.addIdentifier(builder, identifier)
        Decl.addKind(builder, kind)
        Decl.addType(builder, type)
        Decl.addIsTopLevel(builder, topLevel)
        Decl.addGeneric(builder, generic)
        Decl.addAttributes(builder, attrs)
        Decl.addInfoType(builder, infoKind)
        Decl.addInfo(builder, info)
        declarations += Decl.endDecl(builder)
        return declarations.size.toUInt()
    }

    fun build(packageName: String = "test.pkg"): Package {
        val name = builder.createString(packageName)
        val decls = Package.createAllDeclsVector(builder, declarations.toIntArray())
        val tys = Package.createAllTypesVector(builder, types.toIntArray())
        Package.startPackage(builder)
        Package.addFullPkgName(builder, name)
        Package.addAllDecls(builder, decls)
        Package.addAllTypes(builder, tys)
        Package.finishPackageBuffer(builder, Package.endPackage(builder))
        return Package.getRootAsPackage(ByteBuffer.wrap(builder.sizedByteArray()))
    }
}

internal fun cjdBinaryTestEntry(
    key: DeclarationMatchKey,
    annotations: List<CjdAnnotation> = listOf(cjdBinaryTestAnnotation("APILevel")),
    members: List<CjdDeclarationEntry> = emptyList(),
    parameters: List<List<CjdAnnotation>> = emptyList(),
): CjdDeclarationEntry = CjdDeclarationEntry(key, annotations, members, parameters, CjdSourceRange(0, 1))

internal fun cjdBinaryTestAnnotation(name: String): CjdAnnotation =
    CjdAnnotation(name, false, "@$name", CjdSourceRange(0, name.length + 1), null, emptyList())

internal fun cjdBinaryTestIndex(vararg entries: CjdDeclarationEntry): CjdSidecarIndex =
    CjdSidecarIndexImpl("committed-synthetic-fixture", null, entries.toList(), emptyList())
