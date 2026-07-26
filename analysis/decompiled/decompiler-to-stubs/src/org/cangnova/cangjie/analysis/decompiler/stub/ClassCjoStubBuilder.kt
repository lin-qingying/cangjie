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
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
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
    // 用完整 extendedTypeRef 建 type stub：primitive 走 BASIC_TYPE，避免被当成 user type 再经
    // renderIdentifier 包成 `Unit` / `Int64`
    TypeCjoStubBuilder().createDeclaredTypeReferenceStub(stub, declaration.extendedTypeRef)
    createSimpleSuperTypeListStub(stub, superTypeTexts)
    val bodyStub = createTypeStatementBodyStub(stub, CjStubElementTypes.CLASS_BODY)
    val childContext = context.forExtendBody(extendName)
    declaration.declarations.forEach { child ->
        createDeclarationStub(bodyStub, child, childContext)
    }
}

/**
 * 为 class 声明构建反编译 PSI stub。
 *
 * 该入口保留类全限定名、ClassId、类型参数、显式父类型和成员 body，
 * 嵌套类型通过上下文记录 owner，避免错误注册为顶层 ClassId。
 */
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
    createEmptyDeclarationHeaderStubs(stub, createDeclarationModifierMask(declaration.status))
    createTypeParameterListStub(stub, declaration.typeParameters)
    createSuperTypeListStub(stub, superTypeRefs)
    createTypeStatementBodyAndMembers(stub, CjStubElementTypes.CLASS_BODY, declaration.declarations, context.child(declaration.name))
}

/**
 * 为 interface 声明构建反编译 PSI stub。
 *
 * 接口使用独立的 interface body stub，并复用类型参数、父类型和成员递归构建规则。
 */
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
    createEmptyDeclarationHeaderStubs(stub, createDeclarationModifierMask(declaration.status))
    createTypeParameterListStub(stub, declaration.typeParameters)
    createSuperTypeListStub(stub, superTypeRefs)
    createTypeStatementBodyAndMembers(stub, CjStubElementTypes.INTERFACE_BODY, declaration.declarations, context.child(declaration.name))
}

/**
 * 为 struct 声明构建反编译 PSI stub。
 *
 * struct 与 class 共享 class body 形状，但使用 `STRUCT` element type 和 struct stub 实现。
 */
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
    createEmptyDeclarationHeaderStubs(stub, createDeclarationModifierMask(declaration.status))
    createTypeParameterListStub(stub, declaration.typeParameters)
    createSuperTypeListStub(stub, superTypeRefs)
    createTypeStatementBodyAndMembers(stub, CjStubElementTypes.CLASS_BODY, declaration.declarations, context.child(declaration.name))
}

/**
 * 为 enum 声明构建反编译 PSI stub。
 *
 * enum body 中既可以包含枚举构造项，也可以包含普通成员声明，二者统一通过 declaration dispatcher 递归构建。
 */
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
        isNonExhaustive = declaration.isNonExhaustive,
    )
    createEmptyDeclarationHeaderStubs(stub, createDeclarationModifierMask(declaration.status))
    createTypeParameterListStub(stub, declaration.typeParameters)
    createSuperTypeListStub(stub, superTypeRefs)
    createTypeStatementBodyAndMembers(stub, CjStubElementTypes.ENUM_BODY, declaration.declarations, context.child(declaration.name))
}

/**
 * 为 type alias 声明构建反编译 PSI stub。
 *
 * 该 stub 保留 alias 名称、ClassId、类型参数以及展开后的目标类型引用。
 */
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
    createEmptyDeclarationHeaderStubs(stub, createDeclarationModifierMask(declaration.status))
    createTypeParameterListStub(stub, declaration.typeParameters)
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

/**
 * 创建 type statement 的 body stub，并递归构建其成员声明。
 */
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

/**
 * 去除序列化时隐含的根父类型。
 *
 * 单独继承 `Any` 或 `Object` 时，反编译文本应与源码 PSI 一样省略该 supertype 列表。
 */
private fun List<CfirTypeRef>.withoutImplicitRootSupertype(): List<CfirTypeRef> {
    val singleClassId = singleOrNull()?.classIdOrNull() ?: return this
    return if (singleClassId == StdlibClassIds.Any || singleClassId == StdlibClassIds.Object) {
        emptyList()
    } else {
        this
    }
}

/**
 * 从 CFIR 类型引用中提取 classifier class id。
 */
private fun CfirTypeRef.classIdOrNull(): ClassId? {
    return ((this as? CfirResolvedTypeRef)?.coneType as? ConeClassifierType)?.lookupTag?.classId
}

/**
 * 提取适合作为 extend 名称的短类型名；无法解析 classifier 时回退到可读类型文本。
 *
 * primitive（Unit/Int64 等）不是 [ConeClassifierType]，必须单独取 [PrimitiveTypeKind.typeName]，
 * 否则会丢失真实被扩展类型。
 */
private fun CfirTypeRef.shortTypeNameOrRendered(): String {
    val coneType = (this as? CfirResolvedTypeRef)?.coneType
    when (coneType) {
        is ConePrimitiveType -> return coneType.kind.typeName
        is ConeClassifierType -> return coneType.lookupTag.classId.shortClassName.asString()
        else -> Unit
    }
    return classIdOrNull()?.shortClassName?.asString()
        ?: normalizeRenderedTypeText(renderDecompiledTypeRef(this))
            .takeIf { it.isNotBlank() && !it.startsWith("<") }
        ?: "Extend"
}

/**
 * 将父类型引用转换为 class stub 中保存的 super name 引用数组。
 */
private fun List<CfirTypeRef>.toSuperNameRefs(): Array<StringRef> {
    return toSuperTypeTexts()
        .map(StringRef::fromString)
        .toTypedArray()
}

/**
 * 将父类型引用转换为反编译文本和索引都能使用的父类型文本列表。
 */
private fun List<CfirTypeRef>.toSuperTypeTexts(): List<String> {
    return mapNotNull { typeRef ->
        typeRef.classIdOrNull()?.shortClassName?.asString()
            ?: normalizeRenderedTypeText(renderDecompiledTypeRef(typeRef)).takeIf { it.isNotBlank() && !it.startsWith("<") }
    }
}

/**
 * 为类型声明创建完整的 supertype list stub。
 */
private fun createSuperTypeListStub(parent: StubElement<*>, superTypeRefs: List<CfirTypeRef>) {
    if (superTypeRefs.isEmpty()) return
    val superTypeListStub = CangJiePlaceHolderStubImpl<CjSuperTypeList>(parent, CjStubElementTypes.SUPER_TYPE_LIST)
    val typeBuilder = TypeCjoStubBuilder()
    superTypeRefs.forEach { typeRef ->
        val entryStub = CangJiePlaceHolderStubImpl<CjSuperTypeEntry>(superTypeListStub, CjStubElementTypes.SUPER_TYPE_ENTRY)
        typeBuilder.createTypeReferenceStub(entryStub, typeRef)
    }
}

/**
 * 创建只包含单个名称引用的简单类型引用 stub。
 */
private fun createSimpleTypeReferenceStub(parent: StubElement<*>, name: String) {
    val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
    val userTypeStub = CangJieUserTypeStubImpl(typeReferenceStub)
    CangJieNameReferenceExpressionStubImpl(userTypeStub, StringRef.fromString(name), true)
}

/**
 * 根据已经渲染好的父类型文本创建简单 supertype list stub。
 */
private fun createSimpleSuperTypeListStub(parent: StubElement<*>, superTypeTexts: List<String>) {
    if (superTypeTexts.isEmpty()) return
    val superTypeListStub = CangJiePlaceHolderStubImpl<CjSuperTypeList>(parent, CjStubElementTypes.SUPER_TYPE_LIST)
    superTypeTexts.forEach { superTypeText ->
        val entryStub = CangJiePlaceHolderStubImpl<CjSuperTypeEntry>(superTypeListStub, CjStubElementTypes.SUPER_TYPE_ENTRY)
        createSimpleTypeReferenceStub(entryStub, superTypeText)
    }
}
