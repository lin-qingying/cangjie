@file:OptIn(ExperimentalUnsignedTypes::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.AnnoKind
import PackageFormat.DeclKind
import PackageFormat.ExprKind
import PackageFormat.LitConstKind
import PackageFormat.StringKind
import PackageFormat.SemaTyInfo
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builtInDescriptor
import org.cangnova.cangjie.cfir.expressions.platformAnnotationKind
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.metadata.model.Attribute as CfirAttribute
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
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
        private val classIndex = linkedMapOf<ClassId, UInt>()
        private val types = ArrayList<CjoTypeMetadata>()
        private val typeIndex = HashMap<ConeCangJieType, UInt>()
        private val expressions = ArrayList<CjoExpressionMetadata>()
        private val expressionIndex = IdentityHashMap<Any, UInt>()

        fun produce(): CjoPackageMetadata {
            val packageNames = files.map { it.packageDirective.packageFqName.asString() }.distinct()
            require(packageNames.size == 1) { "A CJO package must contain exactly one package" }
            collectDeclarations()
            val declarationMetadata = declarations.mapIndexed { index, declaration ->
                declarationMetadata(declaration, index + 1, topLevel[declaration] == true)
            }
            val module = files.first().moduleData.name.asString()
            return CjoPackageMetadata(
                fullPackageName = packageNames.single(),
                moduleName = module,
                schemaProfile = CjoSchemaProfile.REPOSITORY_EXTENDED,
                declarations = declarationMetadata,
                types = types,
                expressions = expressions,
            )
        }

        private fun collectDeclarations() {
            files.flatMap { it.declarations }.forEach { collect(it, true) }
            require(declarations.isNotEmpty()) { "CFIR package has no serializable declarations" }
        }

        private fun collect(declaration: CfirDeclaration, isTopLevel: Boolean) {
            require(declaration.origin == CfirDeclarationOrigin.Source) {
                "Cannot serialize non-source declaration ${declaration::class.simpleName} from live CFIR"
            }
            if (declarationIndex.putIfAbsent(declaration, declarations.size.toUInt() + 1u) == null) {
                declarations += declaration
                topLevel[declaration] = isTopLevel
                if (declaration is CfirClassLikeDeclaration) {
                    classIndex[(declaration.symbol as CfirClassLikeSymbol<*>).classId] = declarationIndex[declaration]!!
                }
                declarationChildren(declaration).forEach { collect(it, false) }
            }
        }

        private fun declarationChildren(declaration: CfirDeclaration): List<CfirDeclaration> = buildList {
            when (declaration) {
                is CfirClassLikeDeclaration -> addAll(declaration.declarations)
                is CfirExtend -> addAll(declaration.declarations)
                is CfirFunction -> addAll(declaration.valueParameters)
                is CfirEnumConstructor -> addAll(declaration.valueParameters)
                is CfirProperty -> {
                    declaration.getter?.let(::add)
                    declaration.setter?.let(::add)
                }
                else -> Unit
            }
        }

        private fun declarationMetadata(
            declaration: CfirDeclaration,
            index: Int,
            topLevel: Boolean,
        ): CjoPackageDeclaration {
            val name = declarationName(declaration)
            val kind = declarationKind(declaration)
            val info = declarationInfo(declaration)
            return CjoPackageDeclaration(
                identifier = name,
                kind = kind,
                isTopLevel = topLevel,
                type = declarationType(declaration),
                attributes = attributes(declaration, topLevel),
                annotations = annotations(declaration),
                info = info,
            )
        }

        private fun declarationName(declaration: CfirDeclaration): String = when (declaration) {
            is CfirClassLikeDeclaration -> declaration.name.asString()
            is CfirCallableDeclaration -> declaration.symbol.name.asString()
            else -> error("Unsupported CJO declaration ${declaration::class.simpleName}")
        }

        private fun declarationKind(declaration: CfirDeclaration): UShort = when (declaration) {
            is CfirClass -> DeclKind.ClassDecl
            is CfirInterface -> DeclKind.InterfaceDecl
            is CfirStruct -> DeclKind.StructDecl
            is CfirEnum -> DeclKind.EnumDecl
            is CfirTypeAlias -> DeclKind.TypeAliasDecl
            is CfirProperty -> DeclKind.PropDecl
            is CfirFunction -> DeclKind.FuncDecl
            is CfirEnumConstructor -> DeclKind.FuncDecl
            is CfirValueParameter -> DeclKind.FuncParam
            is CfirVariable -> DeclKind.VarDecl
            is CfirExtend -> error("CJO producer does not yet encode ExtendInfo")
            else -> error("Unsupported CJO declaration ${declaration::class.simpleName}")
        }

        private fun declarationType(declaration: CfirDeclaration): UInt = when (declaration) {
            is CfirClass -> type(ConeClassLikeType(declaration.symbol.toLookupTag()))
            is CfirInterface -> type(ConeClassLikeType(declaration.symbol.toLookupTag(), isInterface = true))
            is CfirStruct -> type(ConeStructType(declaration.symbol.toLookupTag()))
            is CfirEnum -> type(ConeEnumType(declaration.symbol.toLookupTag()))
            is CfirTypeAlias -> type(ConeClassLikeType(declaration.symbol.toLookupTag()))
            is CfirFunction -> type(functionType(declaration.valueParameters, declaration.returnTypeRef, declaration.interopInfo?.resolvedAbi?.isCFunction == true, declaration.hasVariableLenArg))
            is CfirEnumConstructor -> type(functionType(declaration.valueParameters, declaration.returnTypeRef, false, false))
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
            is CfirEnumConstructor -> CjoFunctionInfo(
                body = CjoFunctionBodyInfo(
                    parameterLists = listOf(declaration.valueParameters.map { ref(it) }),
                    desugaredParameterLists = listOf(declaration.valueParameters.map { ref(it.desugaredParameter ?: it) }),
                    returnType = typeOf(declaration.returnTypeRef).let(::type),
                ),
            )
            is CfirProperty -> CjoPropertyInfo(
                isConst = declaration.status.isConst,
                isMutable = declaration.status.isMut,
                getters = listOfNotNull(declaration.getter?.let { ref(it) }),
                setters = listOfNotNull(declaration.setter?.let { ref(it) }),
            )
            is CfirValueParameter -> null
            is CfirVariable -> CjoVariableInfo(isVar = declaration.isVar, isConst = declaration.status.isConst)
            else -> null
        }

        private fun ref(declaration: CfirDeclaration): UInt = declarationIndex[declaration]
            ?: error("CJO declaration reference was not indexed: ${declarationName(declaration)}")

        private fun attributes(declaration: CfirDeclaration, topLevel: Boolean): List<ULong> {
            val values = linkedSetOf<CfirAttribute>()
            if (topLevel) values += CfirAttribute.GLOBAL
            val status = (declaration as? CfirMemberDeclaration)?.status
                ?: unsupported("declaration has no status: ${declaration::class.simpleName}")
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
            if (declaration is CfirFunction && declaration.typeParameters.isNotEmpty()) values += CfirAttribute.GENERIC
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
                val descriptor = annotation.builtInDescriptor
                val sourceName = descriptor?.sourceName ?: annotation.annotationSourceName.orEmpty()
                val kind = when (sourceName) {
                    "Deprecated" -> AnnoKind.Deprecated
                    "Frozen" -> AnnoKind.Frozen
                    "Annotation" -> AnnoKind.Annotation
                    "C", "CallingConv", "FastNative", "Intrinsic",
                    "OverflowThrowing", "OverflowWrapping", "OverflowSaturating",
                    -> return@mapNotNull null // declaration attributes/FuncInfo carry these facts

                    else -> {
                        if (annotation.platformAnnotationKind != null) return@mapNotNull null
                        unsupported("CJO producer cannot encode annotation @$sourceName")
                    }
                }
                CjoAnnotationMetadata(
                    kind = kind,
                    identifier = sourceName,
                    arguments = annotation.argumentList.arguments.map { argument ->
                        val expression = (argument as? org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression)?.expression ?: argument
                        CjoAnnotationArgumentMetadata(
                            name = (argument as? org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression)?.argumentName?.asString(),
                            expr = expression(expression),
                        )
                    },
                )
            }

        private fun expression(expression: org.cangnova.cangjie.cfir.expressions.CfirExpression): UInt {
            require(expression is CfirLiteralExpression) { "CJO annotation argument is not a literal expression" }
            val index = expressions.size.toUInt() + 1u
            val literal = CjoLiteralExpressionMetadata(
                value = expression.value?.toString(),
                constKind = expression.kind.toCjoLiteralKind(),
                stringKind = StringKind.Normal,
            )
            expressions += CjoExpressionMetadata(kind = ExprKind.LitConstExpr, literal = literal)
            return index
        }

        private fun type(type: ConeCangJieType): UInt {
            typeIndex[type]?.let { return it }
            require(type !in typeIndex) { "Recursive CJO type is not serializable yet: $type" }
            val index = types.size.toUInt() + 1u
            typeIndex[type] = index
            val metadata = when (type) {
                is ConePrimitiveType -> CjoTypeMetadata(type.kind.toCjoTypeKind())
                is ConeClassLikeType -> CjoTypeMetadata(
                    kind = if (type.isInterface) PackageFormat.TypeKind.Interface else PackageFormat.TypeKind.Class,
                    typeArguments = type.typeArguments.map { projectionType(it) },
                    semanticInfo = CjoCompositeTypeInfoMetadata(classIndex[type.classId] ?: unsupported("external class type ${type.classId}")),
                )
                is ConeStructType -> CjoTypeMetadata(
                    kind = PackageFormat.TypeKind.Struct,
                    typeArguments = type.typeArguments.map { projectionType(it) },
                    semanticInfo = CjoCompositeTypeInfoMetadata(classIndex[type.classId] ?: unsupported("external struct type ${type.classId}")),
                )
                is ConeEnumType -> CjoTypeMetadata(
                    kind = PackageFormat.TypeKind.Enum,
                    typeArguments = type.typeArguments.map { projectionType(it) },
                    semanticInfo = CjoCompositeTypeInfoMetadata(classIndex[type.classId] ?: unsupported("external enum type ${type.classId}")),
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
                else -> unsupported("unsupported live CJO type $type")
            }
            types += metadata
            return index
        }

        private fun projectionType(projection: ConeTypeProjection): UInt = type(projection.type)

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
    }
}
