package org.cangnova.cangjie.analysis.decompiler.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjClassBody
import org.cangnova.cangjie.psi.CjEnumBody
import org.cangnova.cangjie.psi.CjInterfaceBody
import org.cangnova.cangjie.psi.CjSuperTypeEntry
import org.cangnova.cangjie.psi.CjSuperTypeList
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.psi.buildExtendId
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.CangJieClassStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieEnumStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieExtendStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieInterfaceStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieNameReferenceExpressionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePlaceHolderStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieStructStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieTypeAliasStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieUserTypeStubImpl

/**
 * `.cjo` type statement stub 构建器。
 *
 * 这里只负责 type statement 自身和其 body/member 的递归物化。
 */
internal fun createExtendStub(
    parent: StubElement<*>,
    declaration: CfirExtend,
    context: CjoStubBuilderContext,
) {
    val receiverTypeName = declaration.extendedTypeRef.shortTypeNameOrRendered()
    val extendName = sanitizeStubSimpleName(extractShortTypeName(receiverTypeName))
    val superTypeTexts = declaration.superTypeRefs.toSuperTypeTexts()
    val extendFqName = if (context.packageFqName.isRoot) {
        org.cangnova.cangjie.name.FqName(extendName)
    } else {
        context.packageFqName.child(org.cangnova.cangjie.name.Name.identifier(extendName))
    }
    val stub = CangJieExtendStubImpl(
        type = CjStubElementTypes.EXTEND,
        parent = parent,
        qualifiedName = StringRef.fromString(extendFqName.asString()),
        classId = ClassId(context.packageFqName, org.cangnova.cangjie.name.Name.identifier(extendName)),
        name = StringRef.fromString(extendName),
        extendIdRef = StringRef.fromString(
            buildExtendId(
                packageFqName = context.packageFqName,
                receiverTypeText = receiverTypeName,
                superTypeTexts = superTypeTexts,
            ),
        ),
        superNames = superTypeTexts.map(StringRef::fromString).toTypedArray(),
        receiverTypeName = receiverTypeName,
    )
    createEmptyDeclarationHeaderStubs(stub)
    createSimpleTypeReferenceStub(stub, extendName)
    createSimpleSuperTypeListStub(stub, superTypeTexts)
    val bodyStub = createTypeStatementBodyStub(stub, CjStubElementTypes.CLASS_BODY)
    val childContext = context.forExtendBody(extendName)
    declaration.declarations.forEach { child ->
        createDeclarationStub(bodyStub, child, childContext)
    }
}

internal fun createClassStub(
    parent: StubElement<*>,
    declaration: CfirClass,
    context: CjoStubBuilderContext,
) {
    val qualifiedName = composeQualifiedName(context.packageFqName, context.owningClassFqName, declaration.name)
    val superTypeRefs = declaration.superTypeRefs.withoutImplicitRootSupertype()
    val stub = CangJieClassStubImpl(
        type = CjStubElementTypes.CLASS,
        parent = parent,
        qualifiedName = StringRef.fromString(qualifiedName.asString()),
        classId = context.owningClassFqName?.let { null } ?: ClassId(context.packageFqName, declaration.name),
        name = StringRef.fromString(declaration.name.asString()),
        superNames = superTypeRefs.toSuperNameRefs(),
    )
    createEmptyDeclarationHeaderStubs(stub)
    createSuperTypeListStub(stub, superTypeRefs)
    createTypeStatementBodyAndMembers(stub, CjStubElementTypes.CLASS_BODY, declaration.declarations, context.child(declaration.name))
}

internal fun createInterfaceStub(
    parent: StubElement<*>,
    declaration: CfirInterface,
    context: CjoStubBuilderContext,
) {
    val qualifiedName = composeQualifiedName(context.packageFqName, context.owningClassFqName, declaration.name)
    val superTypeRefs = declaration.superTypeRefs.withoutImplicitRootSupertype()
    val stub = CangJieInterfaceStubImpl(
        type = CjStubElementTypes.INTERFACE,
        parent = parent,
        qualifiedName = StringRef.fromString(qualifiedName.asString()),
        classId = context.owningClassFqName?.let { null } ?: ClassId(context.packageFqName, declaration.name),
        name = StringRef.fromString(declaration.name.asString()),
        superNames = superTypeRefs.toSuperNameRefs(),
    )
    createEmptyDeclarationHeaderStubs(stub)
    createSuperTypeListStub(stub, superTypeRefs)
    createTypeStatementBodyAndMembers(stub, CjStubElementTypes.INTERFACE_BODY, declaration.declarations, context.child(declaration.name))
}

internal fun createStructStub(
    parent: StubElement<*>,
    declaration: CfirStruct,
    context: CjoStubBuilderContext,
) {
    val qualifiedName = composeQualifiedName(context.packageFqName, context.owningClassFqName, declaration.name)
    val superTypeRefs = declaration.superTypeRefs.withoutImplicitRootSupertype()
    val stub = CangJieStructStubImpl(
        type = CjStubElementTypes.STRUCT,
        parent = parent,
        qualifiedName = StringRef.fromString(qualifiedName.asString()),
        classId = context.owningClassFqName?.let { null } ?: ClassId(context.packageFqName, declaration.name),
        name = StringRef.fromString(declaration.name.asString()),
        superNames = superTypeRefs.toSuperNameRefs(),
    )
    createEmptyDeclarationHeaderStubs(stub)
    createSuperTypeListStub(stub, superTypeRefs)
    createTypeStatementBodyAndMembers(stub, CjStubElementTypes.CLASS_BODY, declaration.declarations, context.child(declaration.name))
}

internal fun createEnumStub(
    parent: StubElement<*>,
    declaration: CfirEnum,
    context: CjoStubBuilderContext,
) {
    val qualifiedName = composeQualifiedName(context.packageFqName, context.owningClassFqName, declaration.name)
    val superTypeRefs = declaration.superTypeRefs.withoutImplicitRootSupertype()
    val stub = CangJieEnumStubImpl(
        type = CjStubElementTypes.ENUM,
        parent = parent,
        qualifiedName = StringRef.fromString(qualifiedName.asString()),
        classId = context.owningClassFqName?.let { null } ?: ClassId(context.packageFqName, declaration.name),
        name = StringRef.fromString(declaration.name.asString()),
        superNames = superTypeRefs.toSuperNameRefs(),
        isNonExhaustive = false,
    )
    createEmptyDeclarationHeaderStubs(stub)
    createSuperTypeListStub(stub, superTypeRefs)
    createTypeStatementBodyAndMembers(stub, CjStubElementTypes.ENUM_BODY, declaration.declarations, context.child(declaration.name))
}

internal fun createTypeAliasStub(
    parent: StubElement<*>,
    declaration: CfirTypeAlias,
    context: CjoStubBuilderContext,
) {
    val qualifiedName = composeQualifiedName(context.packageFqName, context.owningClassFqName, declaration.name)
    val stub = CangJieTypeAliasStubImpl(
        parent = parent,
        name = StringRef.fromString(declaration.name.asString()),
        qualifiedName = StringRef.fromString(qualifiedName.asString()),
        classId = context.owningClassFqName?.let { null } ?: ClassId(context.packageFqName, declaration.name),
    )
    createEmptyDeclarationHeaderStubs(stub)
    context.typeStubBuilder.createDeclaredTypeReferenceStub(stub, declaration.expandedTypeRef)
}

/**
 * compiled type-statement stub 必须保留与源码 PSI 一致的 body 占位层级。
 *
 * 否则 `CjTypeStatement.body` / `CjAbstractClassBody.declarations` 命不中 stub，
 * 会直接回退到 AST 路径，重新解析整份 decompiled text。
 */
private fun createTypeStatementBodyStub(
    parent: StubElement<*>,
    bodyElementType: IStubElementType<*, *>,
): StubElement<*> {
    return when (bodyElementType) {
        CjStubElementTypes.CLASS_BODY -> CangJiePlaceHolderStubImpl<CjClassBody>(parent, CjStubElementTypes.CLASS_BODY)
        CjStubElementTypes.INTERFACE_BODY -> CangJiePlaceHolderStubImpl<CjInterfaceBody>(parent, CjStubElementTypes.INTERFACE_BODY)
        CjStubElementTypes.ENUM_BODY -> CangJiePlaceHolderStubImpl<CjEnumBody>(parent, CjStubElementTypes.ENUM_BODY)
        else -> error("Unsupported type-statement body stub element type: $bodyElementType")
    }
}

private fun createTypeStatementBodyAndMembers(
    parent: StubElement<*>,
    bodyElementType: IStubElementType<*, *>,
    declarations: List<CfirDeclaration>,
    childContext: CjoStubBuilderContext,
) {
    val bodyStub = createTypeStatementBodyStub(parent, bodyElementType)
    declarations.forEach { child ->
        createDeclarationStub(bodyStub, child, childContext)
    }
}

private fun List<CfirTypeRef>.withoutImplicitRootSupertype(): List<CfirTypeRef> {
    val singleClassId = singleOrNull()?.classIdOrNull() ?: return this
    return if (singleClassId == StdlibClassIds.Any || singleClassId == StdlibClassIds.Object) {
        emptyList()
    } else {
        this
    }
}

private fun CfirTypeRef.classIdOrNull(): ClassId? {
    return ((this as? CfirResolvedTypeRef)?.coneType as? ConeClassifierType)?.lookupTag?.classId
}

private fun CfirTypeRef.shortTypeNameOrRendered(): String {
    return classIdOrNull()?.shortClassName?.asString()
        ?: normalizeRenderedTypeText(renderDecompiledTypeRef(this))
            .takeIf { it.isNotBlank() && !it.startsWith("<") }
        ?: "Extend"
}

private fun List<CfirTypeRef>.toSuperNameRefs(): Array<StringRef> {
    return toSuperTypeTexts()
        .map(StringRef::fromString)
        .toTypedArray()
}

private fun List<CfirTypeRef>.toSuperTypeTexts(): List<String> {
    return mapNotNull { typeRef ->
        typeRef.classIdOrNull()?.shortClassName?.asString()
            ?: normalizeRenderedTypeText(renderDecompiledTypeRef(typeRef)).takeIf { it.isNotBlank() && !it.startsWith("<") }
    }
}

private fun createSuperTypeListStub(parent: StubElement<*>, superTypeRefs: List<CfirTypeRef>) {
    if (superTypeRefs.isEmpty()) return
    val superTypeListStub = CangJiePlaceHolderStubImpl<CjSuperTypeList>(parent, CjStubElementTypes.SUPER_TYPE_LIST)
    val typeBuilder = TypeCjoStubBuilder()
    superTypeRefs.forEach { typeRef ->
        val entryStub = CangJiePlaceHolderStubImpl<CjSuperTypeEntry>(superTypeListStub, CjStubElementTypes.SUPER_TYPE_ENTRY)
        typeBuilder.createTypeReferenceStub(entryStub, typeRef)
    }
}

private fun createSimpleTypeReferenceStub(parent: StubElement<*>, name: String) {
    val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
    val userTypeStub = CangJieUserTypeStubImpl(typeReferenceStub)
    CangJieNameReferenceExpressionStubImpl(userTypeStub, StringRef.fromString(name), true)
}

private fun createSimpleSuperTypeListStub(parent: StubElement<*>, superTypeTexts: List<String>) {
    if (superTypeTexts.isEmpty()) return
    val superTypeListStub = CangJiePlaceHolderStubImpl<CjSuperTypeList>(parent, CjStubElementTypes.SUPER_TYPE_LIST)
    superTypeTexts.forEach { superTypeText ->
        val entryStub = CangJiePlaceHolderStubImpl<CjSuperTypeEntry>(superTypeListStub, CjStubElementTypes.SUPER_TYPE_ENTRY)
        createSimpleTypeReferenceStub(entryStub, superTypeText)
    }
}
