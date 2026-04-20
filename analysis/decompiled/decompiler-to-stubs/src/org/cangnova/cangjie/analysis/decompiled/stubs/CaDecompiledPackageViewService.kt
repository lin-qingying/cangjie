package org.cangnova.cangjie.analysis.decompiled.stubs

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.builtins.StandardNames.MAIN
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.decompiled.filestubs.CaDecompiledBinarySupport
import org.cangnova.cangjie.analysis.decompiled.filestubs.CaLoadedCjoPackage
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
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirExpressionPattern
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirVarOrEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.serialization.cjo.CjoImportEntry
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjImportList
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.CjSecondaryConstructor
import org.cangnova.cangjie.psi.buildExtendId
import org.cangnova.cangjie.psi.stubs.PatternKind
import org.cangnova.cangjie.psi.stubs.CangJieCompiledFileErrors
import org.cangnova.cangjie.psi.stubs.CangJieImportDirectiveStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieBindingPatternStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieConstantPatternStubImpl
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.CangJieClassStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieConstructorStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieEnumConstructorStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieEnumPatternStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieEnumStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieExtendStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFieldStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubKindImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFinalizerStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieImportDirectiveStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieInterfaceStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieMainFunctionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieMacroStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieNamedFunctionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePackageDirectiveStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePlaceHolderStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePropertyStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieStructStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieTypeAliasStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieTypePatternStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieTuplePatternStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieVarOrEnumPatternStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieVariableStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieWildcardPatternStubImpl
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirTupleTypeRef
import java.util.concurrent.ConcurrentHashMap

/**
 * 二进制包 -> compiled stub / decompiled text 的统一视图服务。
 */
class CaDecompiledPackageViewService(
    private val project: Project,
) {
    @Volatile
    private var knownModificationCount: Long = Long.MIN_VALUE

    private val viewsByBinaryUrl = ConcurrentHashMap<String, CaDecompiledPackageView?>()

    private val binarySupport: CaDecompiledBinarySupport
        get() = project.getService(CaDecompiledBinarySupport::class.java)

    fun getPackageView(binaryFile: VirtualFile): CaDecompiledPackageView? {
        refreshIfNeeded()
        return viewsByBinaryUrl.computeIfAbsent(binaryFile.url) {
            buildPackageView(binarySupport.loadPackageData(binaryFile) ?: return@computeIfAbsent null)
        }
    }

    fun getPackageView(module: CaLibraryModule, packageFqName: FqName): CaDecompiledPackageView? {
        val binaryFile = binarySupport.findBinaryFile(module, packageFqName) ?: return null
        return getPackageView(binaryFile)
    }

    fun getPackageView(module: CaBuiltinsModule, packageFqName: FqName): CaDecompiledPackageView? {
        val binaryFile = binarySupport.findBinaryFile(module, packageFqName) ?: return null
        return getPackageView(binaryFile)
    }

    private fun buildPackageView(loadedPackage: CaLoadedCjoPackage): CaDecompiledPackageView {
        if (!loadedPackage.isVersionSupported) {
            return CaDecompiledPackageView(
                owningBinaryFile = loadedPackage.binaryFile,
                packageFqName = loadedPackage.packageFqName,
                fileStub = CangJieFileStubImpl.forInvalid(CangJieCompiledFileErrors.NEWER_VERSION_DECOMPILE_ERROR),
                renderedText = CangJieCompiledFileErrors.NEWER_VERSION_DECOMPILE_ERROR,
            )
        }

        return runCatching {
            val declarations = CaCjoDeclarationLoader.loadDeclarations(loadedPackage)
            CaDecompiledPackageView(
                owningBinaryFile = loadedPackage.binaryFile,
                packageFqName = loadedPackage.packageFqName,
                fileStub = buildFileStub(loadedPackage, declarations),
                renderedText = CaDecompiledTextRendering.renderPackageText(loadedPackage, declarations),
            )
        }.getOrElse { throwable ->
            val errorText = buildString {
                appendLine("// Could not decompile .cjo package: ${loadedPackage.packageFqName.asString()}")
                appendLine("// ${throwable::class.simpleName}: ${throwable.message.orEmpty()}")
            }.trimEnd()
            CaDecompiledPackageView(
                owningBinaryFile = loadedPackage.binaryFile,
                packageFqName = loadedPackage.packageFqName,
                fileStub = CangJieFileStubImpl.forInvalid(errorText),
                renderedText = errorText,
            )
        }
    }

    private fun buildFileStub(
        loadedPackage: CaLoadedCjoPackage,
        declarations: List<CfirDeclaration>,
    ): CangJieFileStubImpl {
        val packageFqName = loadedPackage.packageFqName
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
        val fileStub = createCompiledFileStub(loadedPackage, hasTopLevelCallables)

        CangJiePackageDirectiveStubImpl(fileStub)
        val importListStub = CangJiePlaceHolderStubImpl<CjImportList>(fileStub, CjStubElementTypes.IMPORT_LIST)
        createImportDirectiveStub(importListStub, fileStub.getPackageFqName(), loadedPackage)
        declarations.forEach { declaration ->
            createDeclarationStub(declaration, fileStub, packageFqName, null, null)
        }
        return fileStub
    }

    /**
     * 统一根据包头里的文件切分信息推导 compiled file kind。
     *
     * `.cjo` 虽然按 package 存储，但包头已经保留 `allFiles`，
     * 因此 decompiled 层不能再把所有含顶层 callable 的 package 都压成 simple facade。
     */
    private fun createCompiledFileStub(
        loadedPackage: CaLoadedCjoPackage,
        hasTopLevelCallables: Boolean,
    ): CangJieFileStubImpl {
        val packageFqName = loadedPackage.packageFqName
        return CangJieFileStubImpl(
            file = null,
            kind = CaDecompiledFileStubKinds.inferKind(
                packageFqName = packageFqName,
                sourceFiles = loadedPackage.header.allFiles,
                hasTopLevelCallables = hasTopLevelCallables,
            ),
        )
    }

    private fun createImportDirectiveStub(
        parent: StubElement<*>,
        packageFqName: FqName,
        loadedPackage: CaLoadedCjoPackage,
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
            CangJieImportDirectiveStubImpl(parent, packageFqName, listOf(item))
        }
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

    private fun createDeclarationStub(
        declaration: CfirDeclaration,
        parent: StubElement<*>,
        packageFqName: FqName,
        owningClassFqName: FqName?,
        owningClassSimpleName: String?,
    ) {
        when (declaration) {
            is CfirMainFunction -> createMainFunctionStub(parent, packageFqName)
            is CfirFinalizer -> createFinalizerStub(declaration, parent, owningClassSimpleName ?: "finalizer")
            is CfirErrorFunction -> createErrorFunctionStub(declaration, parent, packageFqName, owningClassFqName)
            is CfirErrorNamedValue -> createErrorNamedValueStub(declaration, parent, packageFqName, owningClassFqName)
            is CfirEnumConstructor -> createEnumConstructorStub(declaration, parent, owningClassFqName)
            is CfirEnum -> createEnumStub(declaration, parent, packageFqName, owningClassFqName)
            is CfirExtend -> createExtendStub(declaration, parent, packageFqName)
            is CfirClass -> createClassStub(declaration, parent, packageFqName, owningClassFqName)
            is CfirInterface -> createInterfaceStub(declaration, parent, packageFqName, owningClassFqName)
            is CfirStruct -> createStructStub(declaration, parent, packageFqName, owningClassFqName)
            is CfirTypeAlias -> createTypeAliasStub(declaration, parent, packageFqName, owningClassFqName)
            is CfirNamedFunction -> createFunctionStub(declaration, parent, packageFqName, owningClassFqName)
            is CfirMacroDeclaration -> createMacroStub(declaration, parent, packageFqName, owningClassFqName)
            is CfirProperty -> createPropertyStub(declaration, parent, packageFqName, owningClassFqName)
            is CfirPropertyAccessor -> Unit
            is CfirFieldVariable -> createFieldStub(declaration, parent, packageFqName, owningClassFqName)
            is CfirPatternVariable -> createPatternVariableStub(declaration, parent)
            is CfirConstructor -> createConstructorStub(declaration, parent, owningClassSimpleName ?: declaration.symbol.callableId.callableName.asString())
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

    private fun createMainFunctionStub(
        parent: StubElement<*>,
        packageFqName: FqName,
    ) {
        val fqName = packageFqName.firstSegment()?.let { firstSegment ->
            FqName(firstSegment.asString()).child(MAIN)
        } ?: packageFqName.child(MAIN)
        CangJieMainFunctionStubImpl(
            parent = parent,
            element = CjStubElementTypes.MAIN_FUNC,
            nameRef = StringRef.fromString(MAIN.asString()),
            fqName = fqName,
            origin = null,
        )
    }

    /**
     * 错误函数仍然需要参与 callable 级 stub 视图，
     * 否则包级名字索引会在出错情况下丢失声明轮廓。
     */
    private fun createErrorFunctionStub(
        declaration: CfirErrorFunction,
        parent: StubElement<*>,
        packageFqName: FqName,
        owningClassFqName: FqName?,
    ) {
        val fallbackName = declaration.symbol.name
        CangJieNamedFunctionStubImpl(
            parent = parent,
            element = CjStubElementTypes.FUNCTION,
            nameRef = StringRef.fromString(fallbackName.asString()),
            isTopLevel = parent is CangJieFileStubImpl,
            fqName = composeQualifiedName(packageFqName, owningClassFqName, fallbackName),
            hasBlockBody = false,
            hasBody = false,
            hasTypeParameterListBeforeFunctionName = false,
            origin = null,
        )
    }

    /**
     * 错误 named-value 统一投影为 property 级 stub，
     * 保证索引层至少保留“名字 + 包归属 + 返回类型存在性”这组稳定轮廓。
     */
    private fun createErrorNamedValueStub(
        declaration: CfirErrorNamedValue,
        parent: StubElement<*>,
        packageFqName: FqName,
        owningClassFqName: FqName?,
    ) {
        CangJiePropertyStubImpl(
            parent = parent,
            name = StringRef.fromString(declaration.name.asString()),
            fqName = composeQualifiedName(packageFqName, owningClassFqName, declaration.name),
            hasReturnTypeRef = declaration.returnTypeRef !is CfirImplicitTypeRef,
        )
    }

    private fun createFinalizerStub(
        declaration: CfirFinalizer,
        parent: StubElement<*>,
        containingClassSimpleName: String,
    ) {
        CangJieFinalizerStubImpl(
            parent = parent,
            elementType = CjStubElementTypes.FINALIZER,
            containingClassName = StringRef.fromString(containingClassSimpleName),
            hasBody = declaration.body != null,
        )
    }

    private fun createExtendStub(
        declaration: CfirExtend,
        parent: StubElement<*>,
        packageFqName: FqName,
    ) {
        val receiverTypeName = normalizeRenderedTypeText(renderTypeRef(declaration.extendedTypeRef))
        val extendName = sanitizeStubSimpleName(extractShortTypeName(receiverTypeName))
        val extendFqName = if (packageFqName.isRoot) FqName(extendName) else packageFqName.child(Name.identifier(extendName))
        val stub = CangJieExtendStubImpl(
            type = CjStubElementTypes.EXTEND,
            parent = parent,
            qualifiedName = StringRef.fromString(extendFqName.asString()),
            classId = ClassId(packageFqName, Name.identifier(extendName)),
            name = StringRef.fromString(extendName),
            extendIdRef = StringRef.fromString(
                buildExtendId(
                    packageFqName = packageFqName,
                    receiverTypeText = receiverTypeName,
                    superTypeTexts = declaration.superTypeRefs.map(::renderTypeRef).map(::normalizeRenderedTypeText),
                ),
            ),
            superNames = declaration.superTypeRefs.map(::renderTypeRef).map(::normalizeRenderedTypeText).map(StringRef::fromString).toTypedArray(),
            receiverTypeName = receiverTypeName,
        )
        declaration.declarations.forEach { child ->
            createDeclarationStub(child, stub, packageFqName, null, extendName)
        }
    }

    private fun createClassStub(
        declaration: CfirClass,
        parent: StubElement<*>,
        packageFqName: FqName,
        owningClassFqName: FqName?,
    ) {
        val stub = CangJieClassStubImpl(
            type = CjStubElementTypes.CLASS,
            parent = parent,
            qualifiedName = StringRef.fromString(composeQualifiedName(packageFqName, owningClassFqName, declaration.name).asString()),
            classId = owningClassFqName?.let { null } ?: ClassId(packageFqName, declaration.name),
            name = StringRef.fromString(declaration.name.asString()),
            superNames = declaration.superTypeRefs.map(::renderTypeRef).map(::normalizeRenderedTypeText).map(StringRef::fromString).toTypedArray(),
        )
        declaration.declarations.forEach { child ->
            createDeclarationStub(child, stub, packageFqName, composeQualifiedName(packageFqName, owningClassFqName, declaration.name), declaration.name.asString())
        }
    }

    private fun createInterfaceStub(
        declaration: CfirInterface,
        parent: StubElement<*>,
        packageFqName: FqName,
        owningClassFqName: FqName?,
    ) {
        val qualifiedName = composeQualifiedName(packageFqName, owningClassFqName, declaration.name)
        val stub = CangJieInterfaceStubImpl(
            type = CjStubElementTypes.INTERFACE,
            parent = parent,
            qualifiedName = StringRef.fromString(qualifiedName.asString()),
            classId = owningClassFqName?.let { null } ?: ClassId(packageFqName, declaration.name),
            name = StringRef.fromString(declaration.name.asString()),
            superNames = declaration.superTypeRefs.map(::renderTypeRef).map(::normalizeRenderedTypeText).map(StringRef::fromString).toTypedArray(),
        )
        declaration.declarations.forEach { child ->
            createDeclarationStub(child, stub, packageFqName, qualifiedName, declaration.name.asString())
        }
    }

    private fun createStructStub(
        declaration: CfirStruct,
        parent: StubElement<*>,
        packageFqName: FqName,
        owningClassFqName: FqName?,
    ) {
        val qualifiedName = composeQualifiedName(packageFqName, owningClassFqName, declaration.name)
        val stub = CangJieStructStubImpl(
            type = CjStubElementTypes.STRUCT,
            parent = parent,
            qualifiedName = StringRef.fromString(qualifiedName.asString()),
            classId = owningClassFqName?.let { null } ?: ClassId(packageFqName, declaration.name),
            name = StringRef.fromString(declaration.name.asString()),
            superNames = declaration.superTypeRefs.map(::renderTypeRef).map(::normalizeRenderedTypeText).map(StringRef::fromString).toTypedArray(),
        )
        declaration.declarations.forEach { child ->
            createDeclarationStub(child, stub, packageFqName, qualifiedName, declaration.name.asString())
        }
    }

    private fun createEnumStub(
        declaration: CfirEnum,
        parent: StubElement<*>,
        packageFqName: FqName,
        owningClassFqName: FqName?,
    ) {
        val qualifiedName = composeQualifiedName(packageFqName, owningClassFqName, declaration.name)
        val stub = CangJieEnumStubImpl(
            type = CjStubElementTypes.ENUM,
            parent = parent,
            qualifiedName = StringRef.fromString(qualifiedName.asString()),
            classId = owningClassFqName?.let { null } ?: ClassId(packageFqName, declaration.name),
            name = StringRef.fromString(declaration.name.asString()),
            superNames = declaration.superTypeRefs.map(::renderTypeRef).map(::normalizeRenderedTypeText).map(StringRef::fromString).toTypedArray(),
            isNonExhaustive = false,
        )
        declaration.declarations.forEach { child ->
            createDeclarationStub(child, stub, packageFqName, qualifiedName, declaration.name.asString())
        }
    }

    private fun createTypeAliasStub(
        declaration: CfirTypeAlias,
        parent: StubElement<*>,
        packageFqName: FqName,
        owningClassFqName: FqName?,
    ) {
        val qualifiedName = composeQualifiedName(packageFqName, owningClassFqName, declaration.name)
        CangJieTypeAliasStubImpl(
            parent = parent,
            name = StringRef.fromString(declaration.name.asString()),
            qualifiedName = StringRef.fromString(qualifiedName.asString()),
            classId = owningClassFqName?.let { null } ?: ClassId(packageFqName, declaration.name),
        )
    }

    private fun createFunctionStub(
        declaration: CfirNamedFunction,
        parent: StubElement<*>,
        packageFqName: FqName,
        owningClassFqName: FqName?,
    ) {
        CangJieNamedFunctionStubImpl(
            parent = parent,
            element = CjStubElementTypes.FUNCTION,
            nameRef = StringRef.fromString(declaration.name.asString()),
            isTopLevel = parent is CangJieFileStubImpl,
            fqName = composeQualifiedName(packageFqName, owningClassFqName, declaration.name),
            hasBlockBody = declaration.body != null,
            hasBody = declaration.body != null,
            hasTypeParameterListBeforeFunctionName = false,
            origin = null,
        )
    }

    private fun createMacroStub(
        declaration: CfirMacroDeclaration,
        parent: StubElement<*>,
        packageFqName: FqName,
        owningClassFqName: FqName?,
    ) {
        CangJieMacroStubImpl(
            parent = parent,
            element = CjStubElementTypes.MACRO,
            nameRef = StringRef.fromString(declaration.name.asString()),
            isTopLevel = parent is CangJieFileStubImpl,
            fqName = composeQualifiedName(packageFqName, owningClassFqName, declaration.name),
            hasBlockBody = declaration.body != null,
            hasBody = declaration.body != null,
            hasTypeParameterListBeforeFunctionName = false,
            origin = null,
        )
    }

    private fun createPropertyStub(
        declaration: CfirProperty,
        parent: StubElement<*>,
        packageFqName: FqName,
        owningClassFqName: FqName?,
    ) {
        CangJiePropertyStubImpl(
            parent = parent,
            name = StringRef.fromString(declaration.name.asString()),
            fqName = composeQualifiedName(packageFqName, owningClassFqName, declaration.name),
            hasReturnTypeRef = declaration.returnTypeRef !is CfirImplicitTypeRef,
        )
    }

    private fun createFieldStub(
        declaration: CfirFieldVariable,
        parent: StubElement<*>,
        packageFqName: FqName,
        owningClassFqName: FqName?,
    ) {
        CangJieFieldStubImpl(
            parent = parent,
            name = StringRef.fromString(declaration.name.asString()),
            fqName = composeQualifiedName(packageFqName, owningClassFqName, declaration.name),
            isVar = declaration.isVar,
            isConst = declaration.status.isConst,
            hasInitializer = declaration.initializer != null,
            hasReturnTypeRef = declaration.returnTypeRef !is CfirImplicitTypeRef,
            origin = null,
        )
    }

    private fun createPatternVariableStub(
        declaration: CfirPatternVariable,
        parent: StubElement<*>,
    ) {
        val variableStub = CangJieVariableStubImpl(
            parent = parent,
            patternKind = declaration.pattern.toPatternKind(),
            isVar = declaration.isVar,
            isTopLevel = parent is CangJieFileStubImpl,
            hasInitializer = declaration.initializer != null,
            hasReturnTypeRef = declaration.returnTypeRef !is CfirImplicitTypeRef,
            origin = null,
        )
        createPatternStub(
            pattern = declaration.pattern,
            parent = variableStub,
        )
    }

    private fun createConstructorStub(
        declaration: CfirConstructor,
        parent: StubElement<*>,
        containingClassSimpleName: String,
    ) {
        val isPrimary = declaration.javaClass.simpleName.contains("Primary", ignoreCase = true)
        if (isPrimary) {
            CangJieConstructorStubImpl<CjPrimaryConstructor>(
                parent = parent,
                elementType = CjStubElementTypes.PRIMARY_CONSTRUCTOR,
                containingClassName = StringRef.fromString(containingClassSimpleName),
                hasBody = declaration.body != null,
                isPrimary = true,
            )
        } else {
            CangJieConstructorStubImpl<CjSecondaryConstructor>(
                parent = parent,
                elementType = CjStubElementTypes.SECONDARY_CONSTRUCTOR,
                containingClassName = StringRef.fromString(containingClassSimpleName),
                hasBody = declaration.body != null,
                isPrimary = false,
            )
        }
    }

    private fun createEnumConstructorStub(
        declaration: CfirEnumConstructor,
        parent: StubElement<*>,
        owningClassFqName: FqName?,
    ) {
        CangJieEnumConstructorStubImpl(
            type = CjStubElementTypes.ENUM_CONSTRUCTOR,
            parent = parent,
            name = StringRef.fromString(declaration.name.asString()),
            typeCount = when (val returnTypeRef = declaration.returnTypeRef) {
                is CfirImplicitTypeRef -> 0
                is CfirTupleTypeRef -> returnTypeRef.elementTypeRefs.size
                else -> 1
            },
            enumFqName = StringRef.fromString(owningClassFqName?.asString()),
        )
    }

    private fun createPatternStub(
        pattern: CfirPattern,
        parent: StubElement<*>,
    ) {
        when (pattern) {
            is CfirBindingPattern -> {
                val bindingStub = CangJieBindingPatternStubImpl(
                    parent = parent,
                    nameRef = StringRef.fromString(pattern.name.asString()),
                    fqName = pattern.bindingVariable
                        ?.takeIf { !it.isLocal }
                        ?.symbol
                        ?.callableId
                        ?.packageName
                        ?.child(pattern.name),
                )
                pattern.nestedPattern?.let { nestedPattern ->
                    createPatternStub(nestedPattern, bindingStub)
                }
            }

            is CfirTuplePattern -> {
                val tupleStub = CangJieTuplePatternStubImpl(parent)
                pattern.elements.forEach { element ->
                    createPatternStub(element, tupleStub)
                }
            }

            is CfirEnumPattern -> {
                val enumStub = CangJieEnumPatternStubImpl(parent)
                pattern.arguments.forEach { argument ->
                    createPatternStub(argument, enumStub)
                }
            }

            is CfirWildcardPattern -> {
                CangJieWildcardPatternStubImpl(parent)
            }

            is CfirTypePattern -> {
                CangJieTypePatternStubImpl(
                    parent = parent,
                    name = StringRef.fromString(
                        pattern.bindingName?.asString()
                            ?: pattern.bindingVariable?.name?.asString(),
                    ),
                )
            }

            is CfirVarOrEnumPattern -> {
                CangJieVarOrEnumPatternStubImpl(
                    parent = parent,
                    nameRef = StringRef.fromString(pattern.name.asString()),
                )
            }

            is CfirConstPattern -> {
                CangJieConstantPatternStubImpl(parent)
            }

            is CfirExpressionPattern -> Unit

            is CfirOrPattern -> {
                pattern.alternatives.forEach { alternative ->
                    createPatternStub(alternative, parent)
                }
            }
        }
    }

    private fun renderTypeRef(typeRef: org.cangnova.cangjie.cfir.types.CfirTypeRef): String {
        return CaDecompiledTextRendering.renderTypeRef(typeRef)
    }

    private fun normalizeRenderedTypeText(rendered: String): String {
        return rendered.removePrefix("R|").removeSuffix("|").trim()
    }

    private fun extractShortTypeName(typeText: String): String {
        return typeText
            .substringBefore('<')
            .substringBefore('?')
            .substringAfterLast('.')
            .ifBlank { "Extend" }
    }

    private fun sanitizeStubSimpleName(name: String): String {
        val sanitized = name.replace(Regex("[^A-Za-z0-9_]"), "_").trim('_')
        return sanitized.ifBlank { "Extend" }
    }

    private fun CfirPattern.toPatternKind(): PatternKind = when (this) {
        is CfirBindingPattern -> PatternKind.BINDING
        is CfirTuplePattern -> PatternKind.TUPLE
        is CfirEnumPattern -> PatternKind.ENUM
        is CfirWildcardPattern -> PatternKind.WILDCARD
        is CfirTypePattern -> PatternKind.BINDING
        is CfirVarOrEnumPattern -> PatternKind.BINDING
        is CfirConstPattern -> PatternKind.BINDING
        is CfirExpressionPattern -> PatternKind.BINDING
        is CfirOrPattern -> PatternKind.BINDING
    }

    private fun composeQualifiedName(
        packageFqName: FqName,
        owningClassFqName: FqName?,
        name: Name,
    ): FqName {
        return when {
            owningClassFqName != null -> FqName("${owningClassFqName.asString()}.${name.asString()}")
            packageFqName.isRoot -> FqName.topLevel(name)
            else -> packageFqName.child(name)
        }
    }

    private fun refreshIfNeeded() {
        val modificationCount = project.getService(CaModificationTracker::class.java)?.modificationCount ?: 0L
        if (knownModificationCount == modificationCount) return
        viewsByBinaryUrl.clear()
        knownModificationCount = modificationCount
    }
}

data class CaDecompiledPackageView(
    val owningBinaryFile: VirtualFile,
    val packageFqName: FqName,
    val fileStub: CangJieFileStubImpl,
    val renderedText: String,
)
