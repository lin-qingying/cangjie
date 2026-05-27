package org.cangnova.cangjie.analysis.decompiler.stub

import PackageFormat.PackageKind
import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirErrorFunction
import org.cangnova.cangjie.cfir.declarations.CfirErrorNamedValue
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirInvalidDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.serialization.cjo.CjoImportEntry
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjAnnotations
import org.cangnova.cangjie.psi.CjDeclarationModifierList
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjImportList
import org.cangnova.cangjie.psi.CjTypeParameterList
import org.cangnova.cangjie.psi.stubs.CangJieImportDirectiveStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieImportAliasStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieImportDirectiveStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieModifierListStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieNameReferenceExpressionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePackageDirectiveStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePlaceHolderStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin
import org.cangnova.cangjie.psi.stubs.impl.CangJieTypeParameterStubImpl
import org.cangnova.cangjie.psi.stubs.impl.ModifierMaskUtils
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.util.io.StringRef

/**
 * `.cjo` file stub 构建入口。
 *
 * 这里只保留 file-stub 和 declaration 分发，不承载 class/callable/type 的细节构造。
 */
internal fun createDecompiledFileStub(
    loadedPackage: LoadedCjoPackage,
    declarations: List<CfirDeclaration>,
): CangJieFileStubImpl {
    val hasTopLevelCallables = declarations.any {
        it is CfirNamedFunction ||
            it is CfirMainFunction ||
            it is CfirProperty ||
            it is CfirFieldVariable ||
            it is CfirPatternVariable ||
            it is CfirMacroDeclaration ||
            it is CfirErrorFunction ||
            it is CfirErrorNamedValue
    }
    val stubKind = DecompiledFileStubKinds.inferKind(
        packageFqName = loadedPackage.packageFqName,
        sourceFiles = loadedPackage.header.allFiles,
        hasTopLevelCallables = hasTopLevelCallables,
    )
    val fileStub = CangJieFileStubImpl(
        file = null,
        kind = stubKind,
    )
    val packageDirectiveStub = CangJiePackageDirectiveStubImpl(
        parent = fileStub,
        isMacroPackage = loadedPackage.header.kind == PackageKind.Macro,
    )
    createPackageNameExpressionStubs(packageDirectiveStub, loadedPackage.packageFqName.pathSegments())
    val importListStub = CangJiePlaceHolderStubImpl<CjImportList>(fileStub, CjStubElementTypes.IMPORT_LIST)
    createImportDirectiveStubs(importListStub, fileStub.getPackageFqName(), loadedPackage)

    val context = CjoStubBuilderContext(
        packageFqName = loadedPackage.packageFqName,
        packageFacadeOrigin = stubKind.facadeOrigin(),
    )
    declarations.forEach { declaration ->
        createDeclarationStub(fileStub, declaration, context)
    }
    return fileStub
}

private fun org.cangnova.cangjie.psi.stubs.CangJieFileStubKind.facadeOrigin(): CangJieStubOrigin.Facade? {
    val facade = this as? org.cangnova.cangjie.psi.stubs.CangJieFileStubKind.WithPackage.Facade ?: return null
    return CangJieStubOrigin.Facade(facade.facadeFqName.asString().replace('.', '/'))
}

private fun createImportDirectiveStubs(
    parent: StubElement<*>,
    packageFqName: FqName,
    loadedPackage: LoadedCjoPackage,
) {
    val importItems = if (loadedPackage.header.fileImportEntries.isNotEmpty()) {
        loadedPackage.header.fileImportEntries.mapNotNull(::toStubImportItemInfo)
    } else {
        loadedPackage.header.imports.filter(String::isNotBlank).map { importPath ->
            CangJieImportDirectiveStub.ImportItemInfo(
                importedFqName = FqName(importPath.removeSuffix(".*")),
                isAllUnder = importPath.endsWith(".*"),
                aliasName = null,
            )
        }
    }
    importItems.forEach { item ->
        val importDirectiveStub = CangJieImportDirectiveStubImpl(parent, packageFqName, listOf(item))
        createImportNameExpressionStubs(
            importDirectiveStub,
            item.importedFqName?.pathSegments().orEmpty(),
            item.isAllUnder,
        )
        item.aliasName?.takeIf(String::isNotBlank)?.let { aliasName ->
            CangJieImportAliasStubImpl(importDirectiveStub, StringRef.fromString(aliasName))
        }
    }
}

private fun createPackageNameExpressionStubs(parent: StubElement<*>, segments: List<Name>) {
    when (segments.size) {
        0 -> Unit
        1 -> createNameReferenceStub(parent, segments.single())
        else -> {
            val dotQualifiedExpressionStub = CangJiePlaceHolderStubImpl<CjDotQualifiedExpression>(
                parent,
                CjStubElementTypes.DOT_QUALIFIED_EXPRESSION,
            )
            createPackageNameExpressionStubs(dotQualifiedExpressionStub, segments.dropLast(1))
            createNameReferenceStub(dotQualifiedExpressionStub, segments.last())
        }
    }
}

private fun createFlatNameReferenceStubs(parent: StubElement<*>, segments: List<Name>) {
    segments.forEach { segment ->
        createNameReferenceStub(parent, segment)
    }
}

private fun createImportNameExpressionStubs(parent: StubElement<*>, segments: List<Name>, isAllUnder: Boolean) {
    if (segments.size <= 2) {
        createFlatNameReferenceStubs(parent, segments)
        return
    }

    createPackageNameExpressionStubs(parent, segments.dropLast(1))
    createNameReferenceStub(parent, segments.last())
}

private fun createNameReferenceStub(parent: StubElement<*>, name: Name) {
    CangJieNameReferenceExpressionStubImpl(parent, StringRef.fromString(name.asString()))
}

/**
 * 源码 parser 会为声明级节点稳定生成空注解列表和空 modifier list。
 *
 * `.cjo` binary stub 也必须保留这一层级，否则 `CjDecompiledFile.calcStubTree()`
 * 会在 binary stub 与 decompiled text AST stub 之间发生结构不一致。
 */
internal fun createEmptyDeclarationHeaderStubs(parent: StubElement<*>, modifierMask: Long = 0) {
    CangJiePlaceHolderStubImpl<CjAnnotations>(parent, CjStubElementTypes.ANNOTATIONS)
    CangJieModifierListStubImpl(parent, modifierMask, CjStubElementTypes.MODIFIER_LIST)
}

/**
 * `.cjo` 声明 stub 必须物化源码 PSI 同构的类型参数列表。
 *
 * Low-level stub 反序列化会通过 [org.cangnova.cangjie.psi.CjTypeParameterListOwner.typeParameters]
 * 建立声明局部类型参数作用域；缺失这一层时，签名中的 `T` 会被误判为包名片段。
 */
internal fun createTypeParameterListStub(parent: StubElement<*>, typeParameters: List<CfirTypeParameter>) {
    if (typeParameters.isEmpty()) return
    val typeParameterListStub = CangJiePlaceHolderStubImpl<CjTypeParameterList>(parent, CjStubElementTypes.TYPE_PARAMETER_LIST)
    typeParameters.forEach { typeParameter ->
        CangJieTypeParameterStubImpl(typeParameterListStub, StringRef.fromString(typeParameter.name.asString()))
    }
}

internal fun createCallableModifierMask(isOperator: Boolean): Long {
    return createCallableModifierMask(
        isOperator = isOperator,
        isAbstract = false,
        isStatic = false,
        isForeign = false,
    )
}

internal fun createDeclarationModifierMask(
    status: CfirDeclarationStatus,
    isOperator: Boolean = false,
): Long {
    return ModifierMaskUtils.computeMask(
        hasModifier = { modifier ->
            when (modifier) {
                CjTokens.ABSTRACT_KEYWORD -> status.isAbstract
                CjTokens.OPEN_KEYWORD -> status.isOpen
                CjTokens.STATIC_KEYWORD -> status.isStatic
                CjTokens.MUT_KEYWORD -> status.isMut
                CjTokens.OVERRIDE_KEYWORD -> status.isOverride
                CjTokens.REDEF_KEYWORD -> status.isRedef
                CjTokens.UNSAFE_KEYWORD -> status.isUnsafe
                CjTokens.OPERATOR_KEYWORD -> isOperator
                else -> false
            }
        },
        hasAdditionalModifier = { keyword ->
            keyword == CjTokens.FOREIGN_KEYWORD && status.isForeign
        },
    )
}

internal fun createCallableModifierMask(
    isOperator: Boolean = false,
    isAbstract: Boolean = false,
    isOpen: Boolean = false,
    isStatic: Boolean = false,
    isForeign: Boolean = false,
): Long {
    return ModifierMaskUtils.computeMask(
        hasModifier = { modifier ->
            when (modifier) {
                CjTokens.OPERATOR_KEYWORD -> isOperator
                CjTokens.ABSTRACT_KEYWORD -> isAbstract
                CjTokens.OPEN_KEYWORD -> isOpen
                CjTokens.STATIC_KEYWORD -> isStatic
                else -> false
            }
        },
        hasAdditionalModifier = { keyword ->
            keyword == CjTokens.FOREIGN_KEYWORD && isForeign
        },
    )
}

/**
 * 当前 PSI import stub 只能稳定承载 dot-qualified import。
 *
 * `org::pkg` 这类组织名双冒号语义当前仍保留在 decompiled text 中，
 * 这里不强行折叠成错误的 `FqName`，避免把协议层信息错误映射到 PSI 层。
 */
private fun toStubImportItemInfo(entry: CjoImportEntry): CangJieImportDirectiveStub.ImportItemInfo? {
    if (entry.hasDoubleColon) return null
    val importedPath = entry.renderImportedPath().takeIf(String::isNotBlank) ?: return null
    return CangJieImportDirectiveStub.ImportItemInfo(
        importedFqName = FqName(importedPath.removeSuffix(".*")),
        isAllUnder = entry.isAllUnder,
        aliasName = entry.aliasName,
    )
}

internal fun createDeclarationStub(
    parent: StubElement<*>,
    declaration: CfirDeclaration,
    context: CjoStubBuilderContext,
) {
    when (declaration) {
        is CfirMainFunction -> createMainFunctionStub(parent, declaration, context)
        is CfirFinalizer -> createFinalizerStub(parent, declaration, context)
        is CfirErrorFunction -> createErrorFunctionStub(parent, declaration, context)
        is CfirErrorNamedValue -> createErrorNamedValueStub(parent, declaration, context)
        is CfirEnumConstructor -> createEnumConstructorStub(parent, declaration, context)
        is CfirEnum -> createEnumStub(parent, declaration, context)
        is CfirExtend -> createExtendStub(parent, declaration, context)
        is CfirClass -> createClassStub(parent, declaration, context)
        is CfirInterface -> createInterfaceStub(parent, declaration, context)
        is CfirStruct -> createStructStub(parent, declaration, context)
        is CfirTypeAlias -> createTypeAliasStub(parent, declaration, context)
        is CfirNamedFunction -> createFunctionStub(parent, declaration, context)
        is CfirMacroDeclaration -> createMacroStub(parent, declaration, context)
        is CfirProperty -> createPropertyStub(parent, declaration, context)
        is CfirFieldVariable -> createFieldStub(parent, declaration, context)
        is CfirPatternVariable -> createPatternVariableStub(parent, declaration, context)
        is CfirConstructor -> createConstructorStub(parent, declaration, context)
        is CfirPropertyAccessor,
        is CfirCodeFragment,
        is CfirFile,
        is CfirInvalidDeclaration,
        is org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction,
        is org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable,
        is org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration,
        is org.cangnova.cangjie.cfir.declarations.CfirTypeParameter,
        is org.cangnova.cangjie.cfir.declarations.CfirValueParameter,
        -> Unit
    }
}
