@file:OptIn(ExperimentalUnsignedTypes::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.AnnoKind
import PackageFormat.DeclKind
import PackageFormat.ExprKind
import PackageFormat.LitConstKind
import PackageFormat.StringKind
import PackageFormat.SemaTyInfo
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.platformAnnotationDescriptor
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.cjmpHasCommonDefault
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.metadata.model.Attribute as CfirAttribute
import org.cangnova.cangjie.name.ClassId
import java.util.IdentityHashMap

/**
 * Live CFIR-to-CJO metadata producer.
 *
 * This is the serialization owner for source CFIR.  It first freezes one
 * declaration index and then uses that index for every type, parameter,
 * property and annotation reference.  Unsupported CFIR shapes fail at the
 * producer boundary instead of silently emitting a partial CJO.
 */
object CfirCjoPackageMetadataProducer {
    fun produce(files: Collection<CfirFile>): CjoPackageMetadata {
        require(files.isNotEmpty()) { "Cannot serialize an empty CFIR module" }
        return Context(files).produce()
    }

    private class Context(private val files: Collection<CfirFile>) {
        private val declarations = ArrayList<CfirDeclaration>()
        private val declarationIndex = IdentityHashMap<CfirDeclaration, UInt>()
        private val topLevel = IdentityHashMap<CfirDeclaration, Boolean>()
        private val declarationFiles = IdentityHashMap<CfirDeclaration, CfirFile>()
        private val fileIndices = IdentityHashMap<CfirFile, UInt>()
        private val classIndex = linkedMapOf<ClassId, UInt>()
        private val externalImports = linkedSetOf<String>()
        private val types = ArrayList<CjoTypeMetadata>()
        private val typeIndex = HashMap<ConeCangJieType, UInt>()
        private val expressions = ArrayList<CjoExpressionMetadata>()
        private val expressionIndex = IdentityHashMap<Any, UInt>()
        private val languageVersion = files.first().moduleData.session.languageVersionSettings.languageVersion
        private val cjmpSettings = files.first().moduleData.session.cjmpSettings

        /**
         * 本次写出的是否为 CJMP common part cjo（官方 common part 编译 = CHIR 输出模式，
         * `ASTWriter` 此时写 `Package.options` 与 `FileInfo.feature`）。
         */
        private val writesCjmpCommonPart =
            languageVersion >= LanguageVersion.CANGJIE_1_1_0 && cjmpSettings.mode == CfirCjmpMode.COMMON

        fun produce(): CjoPackageMetadata {
            val packageNames = files.map { it.packageDirective.packageFqName.asString() }.distinct()
            require(packageNames.size == 1) { "A CJO package must contain exactly one package" }
            files.forEachIndexed { index, file -> fileIndices[file] = index.toUInt() + 1u }
            collectDeclarations()
            val declarationMetadata = declarations.mapIndexed { index, declaration ->
                declarationMetadata(declaration, index + 1, topLevel[declaration] == true)
            }
            val module = files.first().moduleData.name.asString()
            return CjoPackageMetadata(
                fullPackageName = packageNames.single(),
                moduleName = module,
                schemaProfile = when {
                    writesCjmpCommonPart -> CjoSchemaProfile.OFFICIAL_CJMP
                    languageVersion >= LanguageVersion.CANGJIE_1_1_0 -> CjoSchemaProfile.OFFICIAL_V1_1_3
                    else -> CjoSchemaProfile.OFFICIAL_V1_0_0
                },
                // 官方 `ASTWriter::SaveOptions`：common part cjo 内嵌 debug 与优化级别，供 specific
                // 编译的加载门比对（`ASTLoader::ValidateOptions`）
                options = if (writesCjmpCommonPart) {
                    CjoModuleOptionMetadata(
                        debug = cjmpSettings.moduleDebug,
                        optLevel = CjmpCommonPartLoadGate.optLevelOrdinal(cjmpSettings.moduleOptLevel),
                    )
                } else {
                    null
                },
                declarations = declarationMetadata,
                imports = externalImports.toList(),
                // Match ASTWriter::SaveFileInfo when absolute paths are not
                // requested: package identity plus the source-owned basename.
                // Do not serialize an absolute path or derive a name from PSI
                // text; CfirFile.name is shared by PSI and LightTree lowering.
                // 官方 `SaveFileInfo`：common part（serializingCommon）写源文件完整路径，specific 编译据此定位
                // common 方向诊断（G20）；其余情形写 package/basename
                allFiles = files.map { file ->
                    file.sourceFile?.path?.takeIf { writesCjmpCommonPart }
                        ?: "${packageNames.single()}/${file.name}"
                },
                fileInfo = if (languageVersion >= LanguageVersion.CANGJIE_1_1_0) {
                    files.mapNotNull(::fileInfo)
                } else {
                    emptyList()
                },
                types = types,
                expressions = expressions,
            )
        }

        private fun collectDeclarations() {
            files.forEach { file -> file.declarations.forEach { collect(it, true, file) } }
            require(declarations.isNotEmpty()) { "CFIR package has no serializable declarations" }
        }

        private fun collect(declaration: CfirDeclaration, isTopLevel: Boolean, file: CfirFile) {
            require(declaration.origin == CfirDeclarationOrigin.Source) {
                "Cannot serialize non-source declaration ${declaration::class.simpleName} from live CFIR"
            }
            if (declarationIndex.putIfAbsent(declaration, declarations.size.toUInt() + 1u) == null) {
                declarations += declaration
                topLevel[declaration] = isTopLevel
                declarationFiles[declaration] = file
                if (declaration is CfirClassLikeDeclaration) {
                    classIndex[(declaration.symbol as CfirClassLikeSymbol<*>).classId] = declarationIndex[declaration]!!
                }
                declarationChildren(declaration).forEach { collect(it, false, file) }
            }
        }

        /** Convert a real CFIR source offset to the official package position. */
        private fun position(file: CfirFile, offset: Int): CjoPositionMetadata? {
            val mapping = file.sourceFileLinesMapping ?: return null
            if (offset < 0) return null
            val (line, column) = mapping.getLineAndColumnByOffset(offset.coerceAtMost(mapping.lastOffset))
            if (line < 0 || column < 0) return null
            // 官方 `Position` 行列均 1 基（cjc 1.1.3 cjo 实测 `func f` 位于 3:1）；行映射为 0 基
            return CjoPositionMetadata(
                file = fileIndices.getValue(file),
                pkgId = 0u,
                line = line + 1,
                column = column + 1,
            )
        }

        private fun fileInfo(file: CfirFile): CjoFileInfoMetadata? {
            val mapping = file.sourceFileLinesMapping ?: return null
            val begin = position(file, 0) ?: return null
            val end = position(file, mapping.lastOffset) ?: return null
            // 官方 `SaveFileInfo`：文件带 features 指令时一并写出（仅 CJMP common part 档位可编码）
            val feature = file.featuresDirective
                ?.takeIf { writesCjmpCommonPart }
                ?.let { directive ->
                    CjoFeaturesDirectiveMetadata(directive.featureIds.map { it.split('.') })
                }
            return CjoFileInfoMetadata(fileIndices.getValue(file), begin, end, feature)
        }

        private fun declarationPositions(declaration: CfirDeclaration): Pair<CjoPositionMetadata?, CjoPositionMetadata?> {
            val file = declarationFiles[declaration] ?: return null to null
            val source = declaration.source ?: return null to null
            return position(file, source.startOffset) to position(file, source.endOffset)
        }

        private fun declarationChildren(declaration: CfirDeclaration): List<CfirDeclaration> = buildList {
            when (declaration) {
                is CfirClassLikeDeclaration -> {
                    addAll(declaration.typeParameters.filterIsInstance<CfirTypeParameter>())
                    addAll(declaration.declarations)
                }
                is CfirExtend -> {
                    addAll(declaration.typeParameters)
                    addAll(declaration.declarations)
                }
                is CfirFunction -> {
                    addAll(declaration.typeParameters)
                    addAll(declaration.valueParameters)
                }
                // ASTWriter::SaveGeneric deliberately excludes enum constructors.
                is CfirEnumConstructor -> addAll(declaration.valueParameters)
                is CfirProperty -> {
                    declaration.getter?.let(::add)
                    declaration.setter?.let(::add)
                }
                else -> Unit
            }
        }

        private fun genericMetadata(declaration: CfirDeclaration): CjoGenericMetadata? {
            val parameters = when (declaration) {
                is CfirClassLikeDeclaration -> declaration.typeParameters.filterIsInstance<CfirTypeParameter>()
                is CfirFunction -> declaration.typeParameters
                is CfirExtend -> declaration.typeParameters
                else -> emptyList()
            }
            if (parameters.isEmpty()) return null
            return CjoGenericMetadata(
                typeParameters = parameters.map(::ref),
                constraints = parameters.map { parameter ->
                    CjoConstraintMetadata(
                        type = type(ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag())),
                        upperBounds = parameter.bounds.map(::typeOf).map(::type),
                    )
                },
            )
        }

        private fun declarationMetadata(
            declaration: CfirDeclaration,
            index: Int,
            topLevel: Boolean,
        ): CjoPackageDeclaration {
            val name = declarationName(declaration)
            val kind = declarationKind(declaration)
            val info = declarationInfo(declaration)
            val (begin, end) = declarationPositions(declaration)
            return CjoPackageDeclaration(
                identifier = name,
                kind = kind,
                isTopLevel = topLevel,
                type = declarationType(declaration),
                begin = begin,
                end = end,
                generic = genericMetadata(declaration),
                attributes = attributes(declaration, topLevel),
                annotations = annotations(declaration),
                info = info,
            )
        }

        private fun declarationName(declaration: CfirDeclaration): String = when (declaration) {
            is CfirClassLikeDeclaration -> declaration.name.asString()
            // Official ASTWriter keeps ExtendDecl.identifier empty; CJD matching
            // deliberately ignores it and matches Decl.type + ExtendInfo.
            is CfirExtend -> ""
            is CfirCallableDeclaration -> declaration.symbol.name.asString()
            is CfirTypeParameter -> declaration.name.asString()
            else -> error("Unsupported CJO declaration ${declaration::class.simpleName}")
        }

        private fun declarationKind(declaration: CfirDeclaration): UShort = when (declaration) {
            is CfirClass -> DeclKind.ClassDecl
            is CfirInterface -> DeclKind.InterfaceDecl
            is CfirStruct -> DeclKind.StructDecl
            is CfirEnum -> DeclKind.EnumDecl
            is CfirTypeAlias -> DeclKind.TypeAliasDecl
            is CfirProperty -> DeclKind.PropDecl
            is CfirEnumConstructor -> DeclKind.FuncDecl
            is CfirFunction -> DeclKind.FuncDecl
            is CfirValueParameter -> DeclKind.FuncParam
            is CfirTypeParameter -> DeclKind.GenericParamDecl
            is CfirVariable -> DeclKind.VarDecl
            is CfirExtend -> DeclKind.ExtendDecl
            else -> error("Unsupported CJO declaration ${declaration::class.simpleName}")
        }

        private fun declarationType(declaration: CfirDeclaration): UInt = when (declaration) {
            is CfirClass -> type(ConeClassLikeType(declaration.symbol.toLookupTag()))
            is CfirInterface -> type(ConeClassLikeType(declaration.symbol.toLookupTag(), isInterface = true))
            is CfirStruct -> type(ConeStructType(declaration.symbol.toLookupTag()))
            is CfirEnum -> type(ConeEnumType(declaration.symbol.toLookupTag()))
            // 对齐 ASTWriter::SaveTypeAliasDecl：Decl.type 来自
            // typeManager.ObtainsAliasType(typeAliasDecl.type)，必须写展开后的
            // 语义类型。别名身份由 DeclInfo.AliasInfo 承载；把别名符号编码为
            // Class 类型会丢失该契约，使二进制类型消费者把别名当普通 class。
            is CfirTypeAlias -> typeOf(declaration.expandedTypeRef).let(::type)
            is CfirExtend -> typeOf(declaration.extendedTypeRef).let(::type)
            is CfirTypeParameter -> type(ConeTypeParameterTypeImpl(declaration.symbol.toLookupTag()))
            is CfirEnumConstructor -> type(functionType(declaration.valueParameters, declaration.returnTypeRef, false, false))
            is CfirFunction -> type(functionType(declaration.valueParameters, declaration.returnTypeRef, declaration.interopInfo?.resolvedAbi?.isCFunction == true, declaration.hasVariableLenArg))
            is CfirCallableDeclaration -> type(declaration.returnTypeRef.coneTypeOrNull ?: unsupported("unresolved return type"))
            else -> unsupported("missing declaration type")
        }

        private fun functionType(
            parameters: List<CfirValueParameter>,
            returnType: CfirTypeRef,
            isCFunction: Boolean,
            hasVariableLenArg: Boolean,
        ): ConeFunctionType = ConeFunctionType(
            parameterTypes = parameters.map { typeOf(it.returnTypeRef) },
            returnType = typeOf(returnType),
            isCFunc = isCFunction,
            hasVariableLenArg = hasVariableLenArg,
        )

        private fun typeOf(ref: CfirTypeRef): ConeCangJieType =
            ref.coneTypeOrNull ?: unsupported("unresolved CFIR type reference")

        private fun declarationInfo(declaration: CfirDeclaration): CjoDeclarationInfo? = when (declaration) {
            is CfirEnumConstructor -> CjoFunctionInfo(
                body = CjoFunctionBodyInfo(
                    parameterLists = listOf(declaration.valueParameters.map { ref(it) }),
                    desugaredParameterLists = listOf(declaration.valueParameters.map { ref(it.desugaredParameter ?: it) }),
                    returnType = typeOf(declaration.returnTypeRef).let(::type),
                ),
            )
            is CfirFunction -> CjoFunctionInfo(
                overflowStrategy = declaration.annotationInfo?.overflowStrategy ?: declaration.interopInfo?.overflowStrategy,
                body = CjoFunctionBodyInfo(
                    parameterLists = listOf(declaration.valueParameters.map { ref(it) }),
                    desugaredParameterLists = listOf(declaration.valueParameters.map { ref(it.desugaredParameter ?: it) }),
                    returnType = typeOf(declaration.returnTypeRef).let(::type),
                ),
                isConst = declaration.status.isConst,
                isFastNative = declaration.interopInfo?.isFastNative == true,
            )
            is CfirTypeAlias -> CjoAliasInfo(typeOf(declaration.expandedTypeRef).let(::type))
            is CfirClass -> CjoClassInfo(
                inheritedTypes = declaration.superTypeRefs.map(::typeOf).map(::type),
                body = declaration.declarations.map(::ref),
                isAnnotation = declaration.annotationInfo?.isAnnotation == true,
                annotationTargets = declaration.annotationInfo?.annotationTargets.toCjoTargetMask().first,
                annotationTargets2 = declaration.annotationInfo?.annotationTargets.toCjoTargetMask().second,
                runtimeVisible = declaration.annotationInfo?.runtimeVisible == true,
            )
            is CfirInterface -> CjoInterfaceInfo(
                inheritedTypes = declaration.superTypeRefs.map(::typeOf).map(::type),
                body = declaration.declarations.map(::ref),
            )
            is CfirStruct -> CjoStructInfo(
                inheritedTypes = declaration.superTypeRefs.map(::typeOf).map(::type),
                body = declaration.declarations.map(::ref),
            )
            is CfirEnum -> CjoEnumInfo(
                inheritedTypes = declaration.superTypeRefs.map(::typeOf).map(::type),
                body = declaration.declarations.map(::ref),
                hasArguments = declaration.declarations.filterIsInstance<CfirEnumConstructor>()
                    .any { it.valueParameters.isNotEmpty() },
                nonExhaustive = declaration.isNonExhaustive,
            )
            is CfirProperty -> CjoPropertyInfo(
                isConst = declaration.status.isConst,
                isMutable = declaration.status.isMut,
                getters = listOfNotNull(declaration.getter?.let { ref(it) }),
                setters = listOfNotNull(declaration.setter?.let { ref(it) }),
            )
            is CfirValueParameter -> null
            is CfirTypeParameter -> null
            is CfirVariable -> CjoVariableInfo(isVar = declaration.isVar, isConst = declaration.status.isConst)
            is CfirExtend -> CjoExtendInfo(
                inheritedTypes = declaration.superTypeRefs.map(::typeOf).map(::type),
                body = declaration.declarations.map(::ref),
            )
            else -> null
        }

        private fun ref(declaration: CfirDeclaration): UInt = declarationIndex[declaration]
            ?: error("CJO declaration reference was not indexed: ${declarationName(declaration)}")

        /**
         * 写出 CJMP 属性位（官方 `Attribute::COMMON / SPECIFIC / FROM_COMMON_PART /
         * COMMON_WITH_DEFAULT`）。
         *
         * 证据（cjc 1.1.3 产出的 cjo 实测，`.workbuddy/tmp/cjmp_probe113/full` 目录下的 cjo）：
         * - `specific func` → 只带 `SPECIFIC`；
         * - `common func`（有体）→ `COMMON + FROM_COMMON_PART + COMMON_WITH_DEFAULT`。
         *
         * `COMMON_WITH_DEFAULT` 按官方 `SetCJMPAttrs` 在写侧派生：函数/构造器体、属性访问器、变量初值，
         * 以及 class/interface/struct/enum/extend 的全部 common 成员均有默认实现时置位。
         * `FROM_COMMON_PART` 表示本包元数据来自 common-part 编译，故该档位写出的每个声明都携带此位。
         */
        private fun addCjmpAttributes(
            values: MutableSet<CfirAttribute>,
            declaration: CfirDeclaration,
            status: org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus,
        ) {
            if (writesCjmpCommonPart) values += CfirAttribute.FROM_COMMON_PART
            if (status.isSpecific) values += CfirAttribute.SPECIFIC
            if (!status.isCommon) return
            values += CfirAttribute.COMMON
            if (status.isCommonWithDefault || declaration.cjmpHasCommonDefault()) {
                values += CfirAttribute.COMMON_WITH_DEFAULT
            }
        }

        private fun attributes(declaration: CfirDeclaration, topLevel: Boolean): List<ULong> {
            val values = linkedSetOf<CfirAttribute>()
            if (topLevel) values += CfirAttribute.GLOBAL
            val status = (declaration as? CfirMemberDeclaration)?.status ?: return emptyList()
            // CJO 中构造器同样写为 FuncDecl，必须保留官方身份位供反序列化恢复声明种类。
            if (declaration is CfirConstructor) {
                values += CfirAttribute.CONSTRUCTOR
                if (declaration.isPrimary) values += CfirAttribute.PRIMARY_CONSTRUCTOR
            } else if (declaration is CfirEnumConstructor) {
                values += CfirAttribute.ENUM_CONSTRUCTOR
            }
            if (status.isC || declaration.interopInfo?.resolvedAbi?.kind == CfirAbiKind.C) values += CfirAttribute.C
            if (status.isForeign) values += CfirAttribute.FOREIGN
            if (status.isStatic) values += CfirAttribute.STATIC
            if (status.isAbstract) values += CfirAttribute.ABSTRACT
            if (status.isOpen) values += CfirAttribute.OPEN
            if (status.isSealed) values += CfirAttribute.SEALED
            if (status.isOverride) values += CfirAttribute.OVERRIDE
            if (status.isRedef) values += CfirAttribute.REDEF
            if (status.isUnsafe) values += CfirAttribute.UNSAFE
            if (status.isMut) values += CfirAttribute.MUT
            addCjmpAttributes(values, declaration, status)
            val hasGenericTypeParameters = when (declaration) {
                is CfirClassLikeDeclaration -> declaration.typeParameters.filterIsInstance<CfirTypeParameter>().isNotEmpty()
                is CfirFunction -> declaration.typeParameters.isNotEmpty()
                is CfirExtend -> declaration.typeParameters.isNotEmpty()
                else -> false
            }
            // Attribute.GENERIC 同时标记 class-like 与 extend owner，供 CJO 消费端的实例化筛选使用。
            if (hasGenericTypeParameters) values += CfirAttribute.GENERIC
            if (declaration.annotationInfo?.isIntrinsic == true) values += CfirAttribute.INTRINSIC
            if (declaration.annotationInfo?.isMockSupported == true) values += CfirAttribute.MOCK_SUPPORTED
            if (declaration.status.visibility == org.cangnova.cangjie.descriptors.Visibilities.Public) values += CfirAttribute.PUBLIC
            val max = values.maxOfOrNull { it.ordinal } ?: return emptyList()
            val words = ULongArray(max / 64 + 1)
            values.forEach { words[it.ordinal / 64] = words[it.ordinal / 64] or (1uL shl (it.ordinal % 64)) }
            return words.toList()
        }

        private fun annotations(declaration: CfirDeclaration): List<CjoAnnotationMetadata> =
            declaration.annotations.filterIsInstance<CfirAnnotationCall>().mapNotNull { annotation ->
                val declarationStatus = (declaration as? CfirMemberDeclaration)?.status
                /*
                 * Match the official ASTWriter::SaveAnnotations contract.  The
                 * wire kind is selected from resolved identity, never from the
                 * spelling of the annotation.  C/FFI and parser-only builtins
                 * are represented by Decl attributes/FuncInfo and therefore do
                 * not become Anno records.  Non-visible custom annotations are
                 * likewise not serialized by the official writer.
                 */
                val kind = when (val identity = annotation.annotationIdentity) {
                    is CangjieAnnotationIdentity.LanguageBuiltIn -> when (identity.kind) {
                        BuiltInAnnotationKind.DEPRECATED -> AnnoKind.Deprecated
                        BuiltInAnnotationKind.FROZEN -> AnnoKind.Frozen
                        BuiltInAnnotationKind.ATTRIBUTE ->
                            AnnoKind.TestRegistration.takeIf {
                                declaration.annotationInfo?.attributes?.contains("TEST_REGISTER") == true
                            }
                        BuiltInAnnotationKind.ANNOTATION ->
                            AnnoKind.Annotation.takeIf {
                                languageVersion >= LanguageVersion.CANGJIE_1_1_0 &&
                                    annotation.argumentList.arguments.isNotEmpty()
                            }
                        else -> null
                    }
                    is CangjieAnnotationIdentity.PlatformDerived ->
                        annotation.platformAnnotationDescriptor?.officialKind?.toCjoAnnoKind(languageVersion)
                    is CangjieAnnotationIdentity.Custom ->
                        // 对齐官方 ASTWriter：common/specific 声明上的普通 custom
                        // annotation 也必须保留，供两侧一致性检查比较；不能只用
                        // compile-time-visible 作为完整序列化条件。
                        AnnoKind.Custom.takeIf {
                            annotation.isCompileTimeVisible == true ||
                                declarationStatus?.isCommon == true ||
                                declarationStatus?.isSpecific == true
                        }
                    else -> null
                } ?: return@mapNotNull null
                val identifier = when (val identity = annotation.annotationIdentity) {
                    is CangjieAnnotationIdentity.LanguageBuiltIn -> identity.sourceName
                    is CangjieAnnotationIdentity.Custom ->
                        annotation.annotationSourceName ?: identity.classFqName?.shortName()?.asString()
                    else -> annotation.annotationSourceName
                } ?: unsupported("CJO annotation has no resolved identifier")
                CjoAnnotationMetadata(
                        kind = kind,
                        identifier = identifier,
                    arguments = annotationArguments(annotation, kind),
                )
            }

        private fun annotationArguments(
            annotation: CfirAnnotationCall,
            kind: UShort,
        ): List<CjoAnnotationArgumentMetadata> = annotation.argumentList.arguments.mapNotNull { argument ->
            val named = argument as? org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
            val expression = named?.expression ?: argument
            val index = expressionOrNull(expression)
            if (index == null && kind == AnnoKind.Annotation) {
                unsupported("CJO @Annotation argument is not representable by the expression pool")
            }
            index?.let { CjoAnnotationArgumentMetadata(named?.argumentName?.asString(), it) }
        }

        private fun expressionOrNull(expression: org.cangnova.cangjie.cfir.expressions.CfirExpression): UInt? {
            val index = expressions.size.toUInt() + 1u
            val metadata = when (expression) {
                is CfirLiteralExpression -> CjoExpressionMetadata(
                    kind = ExprKind.LitConstExpr,
                    literal = CjoLiteralExpressionMetadata(
                        value = expression.value?.toString(),
                        constKind = expression.kind.toCjoLiteralKind(),
                        stringKind = StringKind.Normal,
                    ),
                )
                is CfirArrayLiteral -> CjoExpressionMetadata(
                    kind = ExprKind.ArrayLit,
                    operands = expression.elements.map {
                        expressionOrNull(it) ?: unsupported("CJO expression array element is not representable")
                    },
                )
                is org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression -> {
                    val name = (expression.calleeReference as? CfirNamedReference)?.name?.asString()
                        ?: return null
                    CjoExpressionMetadata(
                        kind = ExprKind.RefExpr,
                        reference = CjoReferenceExpressionMetadata(reference = name),
                    )
                }
                else -> return null
            }
            expressions += metadata
            return index
        }

        private fun BuiltInAnnotationKind.toCjoAnnoKind(version: LanguageVersion): UShort? {
            if (version < LanguageVersion.CANGJIE_1_1_0) return null
            return when (this) {
                BuiltInAnnotationKind.JAVA_MIRROR -> AnnoKind.JavaMirror
                BuiltInAnnotationKind.JAVA_IMPL -> AnnoKind.JavaImpl
                BuiltInAnnotationKind.JAVA_HAS_DEFAULT -> AnnoKind.JavaHasDefault
                BuiltInAnnotationKind.OBJ_C_MIRROR -> AnnoKind.ObjCMirror
                BuiltInAnnotationKind.OBJ_C_IMPL -> AnnoKind.ObjCImpl
                BuiltInAnnotationKind.FOREIGN_NAME -> AnnoKind.ForeignName
                else -> null // official ASTWriter skips ObjCInit/Optional/getter/setter
            }
        }

        private fun type(type: ConeCangJieType): UInt {
            typeIndex[type]?.let { return it }
            require(type !in typeIndex) { "Recursive CJO type is not serializable yet: $type" }
            val index = types.size.toUInt() + 1u
            typeIndex[type] = index
            val metadata = when (type) {
                is ConePrimitiveType -> CjoTypeMetadata(type.kind.toCjoTypeKind())
                is ConeTypeParameterType -> {
                    val declaration = type.lookupTag.typeParameterSymbol.cfir
                    val index = declarationIndex[declaration]
                        ?: unsupported("CJO generic type parameter was not indexed: ${declaration.name}")
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Generic,
                        semanticInfo = CjoGenericTypeInfoMetadata(
                            declarationIndex = index,
                            upperBounds = declaration.bounds.map { bound -> type(typeOf(bound)) },
                        ),
                    )
                }
                is ConeClassLikeType -> CjoTypeMetadata(
                    kind = if (type.isInterface) PackageFormat.TypeKind.Interface else PackageFormat.TypeKind.Class,
                    typeArguments = type.typeArguments.map { projectionType(it) },
                    semanticInfo = compositeInfo(type.classId),
                )
                is ConeStructType -> CjoTypeMetadata(
                    kind = PackageFormat.TypeKind.Struct,
                    typeArguments = type.typeArguments.map { projectionType(it) },
                    semanticInfo = compositeInfo(type.classId),
                )
                is ConeEnumType -> CjoTypeMetadata(
                    kind = PackageFormat.TypeKind.Enum,
                    typeArguments = type.typeArguments.map { projectionType(it) },
                    semanticInfo = compositeInfo(type.classId),
                )
                is ConeTupleType -> CjoTypeMetadata(PackageFormat.TypeKind.Tuple, type.elementTypes.map(::type))
                is ConeVArrayType -> CjoTypeMetadata(
                    PackageFormat.TypeKind.VArray,
                    listOf(type(type.elementType)),
                    semanticInfo = CjoArrayTypeInfoMetadata(type.size),
                )
                is ConePointerType -> CjoTypeMetadata(PackageFormat.TypeKind.CPointer, listOf(type(type.pointeeType)))
                is ConeCStringType -> CjoTypeMetadata(PackageFormat.TypeKind.CString)
                is ConeFunctionType -> CjoTypeMetadata(
                    PackageFormat.TypeKind.Func,
                    type.parameterTypes.map(::type),
                    semanticInfo = CjoFunctionTypeInfoMetadata(type(type.returnType), type.isCFunc, type.hasVariableLenArg),
                )
                else -> unsupported("unsupported live CJO type ${type::class.qualifiedName} ($type)")
            }
            types += metadata
            return index
        }

        private fun projectionType(projection: ConeTypeProjection): UInt = type(projection.type)

        private fun compositeInfo(classId: ClassId): CjoCompositeTypeInfoMetadata {
            classIndex[classId]?.let { return CjoCompositeTypeInfoMetadata(it) }
            val packageName = classId.packageFqName.asString()
            val packageIndex = externalImports.indexOf(packageName).let { existing ->
                if (existing >= 0) existing else {
                    externalImports += packageName
                    externalImports.size - 1
                }
            }
            return CjoCompositeTypeInfoMetadata(
                declarationIndex = 0u,
                packageId = packageIndex,
                declarationKey = classId.relativeClassName.asString(),
            )
        }

        private fun CfirLiteralKind.toCjoLiteralKind(): UByte = when (this) {
            CfirLiteralKind.INT, CfirLiteralKind.BYTE -> LitConstKind.Integer
            CfirLiteralKind.FLOAT -> LitConstKind.Float
            CfirLiteralKind.RUNE -> LitConstKind.Rune
            CfirLiteralKind.STRING -> LitConstKind.String
            CfirLiteralKind.BOOLEAN -> LitConstKind.Bool
            CfirLiteralKind.UNIT -> LitConstKind.Unit
        }

        private fun PrimitiveTypeKind.toCjoTypeKind(): UShort = when (this) {
            PrimitiveTypeKind.UNIT -> PackageFormat.TypeKind.Unit
            PrimitiveTypeKind.INT8 -> PackageFormat.TypeKind.Int8
            PrimitiveTypeKind.INT16 -> PackageFormat.TypeKind.Int16
            PrimitiveTypeKind.INT32 -> PackageFormat.TypeKind.Int32
            PrimitiveTypeKind.INT64 -> PackageFormat.TypeKind.Int64
            PrimitiveTypeKind.INT_NATIVE -> PackageFormat.TypeKind.IntNative
            PrimitiveTypeKind.UINT8 -> PackageFormat.TypeKind.UInt8
            PrimitiveTypeKind.UINT16 -> PackageFormat.TypeKind.UInt16
            PrimitiveTypeKind.UINT32 -> PackageFormat.TypeKind.UInt32
            PrimitiveTypeKind.UINT64 -> PackageFormat.TypeKind.UInt64
            PrimitiveTypeKind.UINT_NATIVE -> PackageFormat.TypeKind.UIntNative
            PrimitiveTypeKind.FLOAT16 -> PackageFormat.TypeKind.Float16
            PrimitiveTypeKind.FLOAT32 -> PackageFormat.TypeKind.Float32
            PrimitiveTypeKind.FLOAT64 -> PackageFormat.TypeKind.Float64
            PrimitiveTypeKind.RUNE -> PackageFormat.TypeKind.Rune
            PrimitiveTypeKind.NOTHING -> PackageFormat.TypeKind.Nothing
            PrimitiveTypeKind.BOOLEAN -> PackageFormat.TypeKind.Bool
            PrimitiveTypeKind.IDEAL_INT, PrimitiveTypeKind.IDEAL_FLOAT -> unsupported("ideal type in CJO declaration")
        }

        private fun unsupported(message: String): Nothing = error(message)

        private fun Set<org.cangnova.cangjie.annotations.CangjieAnnotationTarget>?.toCjoTargetMask(): Pair<UByte, UByte> {
            if (isNullOrEmpty()) return 0.toUByte() to 0.toUByte()
            val mask = fold(0) { result, target -> result or (1 shl target.bitPosition) }
            return mask.toUByte() to (mask ushr 8).toUByte()
        }
    }
}
