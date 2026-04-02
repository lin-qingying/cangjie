package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.*
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildTuplePattern
import org.cangnova.cangjie.cfir.patterns.builder.buildWildcardPattern
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 声明反序列化器：Decl → CfirDeclaration。
 *
 * 将 .cjo 中的 FlatBuffers Decl 表转换为 CFIR 声明树。
 * 所有反序列化声明的 origin 为 Library，resolveState 初始化为 BODY_RESOLVE。
 */
@OptIn(CfirImplementationDetail::class, ResolveStateAccess::class)
class CfirDeclDeserializer(
    private val context: CfirDeserializationContext,
    private val typeDeserializer: CfirTypeDeserializer,
) {
    private val packageFqName: FqName = FqName(context.header.fullPkgName)
    private val declsUnderDeserialization = HashSet<Int>()

    /**
     * 反序列化指定索引的声明。
     * @param declIndex allDecls 中的索引（0-based）
     */
    fun deserializeDecl(declIndex: Int): CfirDeclaration? {
        context.declCache[declIndex]?.let { return it }
        if (declIndex !in 0 until context.pkg.allDeclsLength) return null
        if (!declsUnderDeserialization.add(declIndex)) {
            return context.declCache[declIndex]
        }

        return try {
            val decl = try {
                context.pkg.allDecls(declIndex)
            } catch (_: IndexOutOfBoundsException) {
                null
            } ?: return null
            val result = convertDecl(decl) ?: return null
            context.declCache[declIndex] = result
            result
        } finally {
            declsUnderDeserialization.remove(declIndex)
        }
    }

    private fun convertDecl(decl: Decl): CfirDeclaration? {
        return when (decl.kind) {
            DeclKind.ClassDecl -> convertClass(decl)
            DeclKind.InterfaceDecl -> convertInterface(decl)
            DeclKind.StructDecl -> convertStruct(decl)
            DeclKind.EnumDecl -> convertEnum(decl)
            DeclKind.FuncDecl -> convertFunctionOrEnumConstructor(decl)
            DeclKind.PropDecl -> convertProperty(decl)
            DeclKind.VarDecl -> convertVariableOrEnumConstructor(decl)
            DeclKind.VarWithPatternDecl -> convertVariableWithPattern(decl)
            DeclKind.ExtendDecl -> convertExtend(decl)
            DeclKind.TypeAliasDecl -> convertTypeAlias(decl)
            DeclKind.GenericParamDecl -> convertTypeParameter(decl)
            DeclKind.FuncParam -> convertValueParameter(decl)
            else -> null // InvalidDecl, BuiltInDecl 暂不处理
        }
    }
    // ---- 属性位域解析 ----

    /**
     * C++ Attribute 枚举位位置（与 AttributePack.h 对应）。
     * attributes 是 [uint64] 数组，bit N 在 attributes[N/64] 的第 N%64 位。
     */
    private object AttrBit {
        const val STATIC = 9
        const val PUBLIC = 10
        const val PRIVATE = 11
        const val PROTECTED = 12

        // EXTERNAL = 13 (不映射到 CFIR)
        const val INTERNAL = 14
        const val OVERRIDE = 15
        const val REDEF = 16
        const val ABSTRACT = 17
        const val SEALED = 18
        const val OPEN = 19
        const val OPERATOR = 20
        const val FOREIGN = 21
        const val UNSAFE = 22
        const val MUT = 23
        val ENUM_CONSTRUCTOR = Attribute.ENUM_CONSTRUCTOR.ordinal
    }

    private fun testAttr(decl: Decl, bit: Int): Boolean {
        val wordIndex = bit / 64
        val bitIndex = bit % 64
        if (wordIndex >= decl.attributesLength) return false
        return (decl.attributes(wordIndex).toLong() shr bitIndex) and 1L == 1L
    }

    /**
     * PackageFormat uses 1-based formatted decl index.
     * 0 and UInt.MAX_VALUE are treated as invalid references.
     */
    private fun decodeDeclRef(rawIndex: UInt): Int? {
        if (rawIndex == 0u || rawIndex == UInt.MAX_VALUE) return null
        val decoded = rawIndex.toInt() - 1
        if (decoded !in 0 until context.pkg.allDeclsLength) return null
        return decoded
    }

    private fun isEnumConstructorDecl(decl: Decl): Boolean =
        testAttr(decl, AttrBit.ENUM_CONSTRUCTOR)

    /** 从 attributes 位域解析可见性 */
    private fun resolveVisibility(decl: Decl): Visibility = when {
        testAttr(decl, AttrBit.PUBLIC) -> Visibilities.Public
        testAttr(decl, AttrBit.PRIVATE) -> Visibilities.Private
        testAttr(decl, AttrBit.PROTECTED) -> Visibilities.Protected
        testAttr(decl, AttrBit.INTERNAL) -> Visibilities.Internal
        else -> Visibilities.Public // 默认 public
    }

    /** 从 attributes 位域解析 modality */
    private fun resolveModality(decl: Decl): Modality = Modality.convertFromFlags(
        sealed = testAttr(decl, AttrBit.SEALED),
        abstract = testAttr(decl, AttrBit.ABSTRACT),
        open = testAttr(decl, AttrBit.OPEN),
    )

    /** 构建 CfirDeclarationStatus */
    private fun buildStatus(decl: Decl): CfirDeclarationStatus {
        val status = CfirDeclarationStatusImpl(
            visibility = resolveVisibility(decl),
            modality = resolveModality(decl),
        )
        status.isStatic = testAttr(decl, AttrBit.STATIC)
        status.isOverride = testAttr(decl, AttrBit.OVERRIDE)
        status.isOperator = testAttr(decl, AttrBit.OPERATOR)
        status.isForeign = testAttr(decl, AttrBit.FOREIGN)
        status.isUnsafe = testAttr(decl, AttrBit.UNSAFE)
        status.isMut = testAttr(decl, AttrBit.MUT)
        status.isRedef = testAttr(decl, AttrBit.REDEF)
        return status
    }

    /** 将 type 字段值转换为 CfirResolvedTypeRef */
    private fun buildTypeRef(typeFieldValue: UInt): CfirResolvedTypeRef {
        val coneType = typeDeserializer.deserializeTypeFromField(typeFieldValue)
        return CfirResolvedTypeRefImpl(
            source = null,
            annotations = MutableOrEmptyList.empty(),
            coneType = coneType,
            delegatedTypeRef = null,
        )
    }

    /** 反序列化泛型参数列表 */
    private fun deserializeTypeParameters(decl: Decl): MutableList<CfirTypeParameter> {
        val generic = decl.generic ?: return mutableListOf()
        val len = generic.typeParametersLength
        if (len == 0) return mutableListOf()
        return (0 until len).mapNotNullTo(mutableListOf()) { i ->
            val paramIndex = decodeDeclRef(generic.typeParameters(i)) ?: return@mapNotNullTo null
            deserializeDecl(paramIndex) as? CfirTypeParameter
        }
    }

    /** 反序列化继承类型列表 */
    private fun deserializeInheritedTypes(
        getter: (Int) -> UInt,
        length: Int,
    ): MutableList<CfirTypeRef> {
        if (length == 0) return mutableListOf()
        return (0 until length).mapTo(mutableListOf()) { buildTypeRef(getter(it)) }
    }

    /** 反序列化成员声明列表（延迟加载） */
    private fun deserializeBody(
        getter: (Int) -> UInt,
        length: Int,
    ): MutableList<CfirDeclaration> {
        if (length == 0) return mutableListOf()
        return (0 until length).mapNotNullTo(mutableListOf()) { i ->
            val declIndex = decodeDeclRef(getter(i)) ?: return@mapNotNullTo null
            deserializeDecl(declIndex)
        }
    }

    /** 设置声明的 resolve 状态为 BODY_RESOLVE（库声明已完全解析） */
    private fun CfirDeclaration.markResolved() {
        initDefaultResolveState()
        replaceResolvePhase(CfirResolvePhase.BODY_RESOLVE)
    }

    // ---- 声明转换方法 ----

    /** ClassDecl → CfirClass */
    private fun convertClass(decl: Decl): CfirClass {
        val name = decl.classLikeName()
        val symbol = CfirClassSymbol(resolveClassId(decl, name))
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)
        val info = decl.info(ClassInfo()) as? ClassInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = info?.let { deserializeBody(it::body, it.bodyLength) } ?: mutableListOf()

        val cfirClass = CfirClassImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            status = status,
            typeParameters = typeParams,
            symbol = symbol,
            superTypeRefs = superTypeRefs,
            declarations = members,
            name = name,

            )
        symbol.bind(cfirClass)
        cfirClass.markResolved()
        return cfirClass
    }

    /** InterfaceDecl → CfirInterface */
    private fun convertInterface(decl: Decl): CfirInterface {
        val name = decl.classLikeName()
        val classId = resolveClassId(decl, name)
        check(classId.relativeClassName.asString().isNotBlank()) {
            "Blank interface ClassId for decl.identifier='${decl.identifier}' pkg='${packageFqName.asString()}'"
        }
        val symbol = CfirInterfaceSymbol(classId)
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)
        val info = decl.info(InterfaceInfo()) as? InterfaceInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = info?.let { deserializeBody(it::body, it.bodyLength) } ?: mutableListOf()

        val cfirInterface = CfirInterfaceImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            declarations = members,
            status = status,
            typeParameters = typeParams,
            symbol = symbol,
            superTypeRefs = superTypeRefs,
            properties = members.filterIsInstance<CfirProperty>().toMutableList(),
            functions = members.filterIsInstance<CfirFunction>().toMutableList(),
            name = name,
        )
        symbol.bind(cfirInterface)
        cfirInterface.markResolved()
        return cfirInterface
    }

    /** StructDecl → CfirStruct */
    private fun convertStruct(decl: Decl): CfirStruct {
        val name = decl.classLikeName()
        val symbol = CfirStructSymbol(resolveClassId(decl, name))
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)
        val info = decl.info(StructInfo()) as? StructInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = info?.let { deserializeBody(it::body, it.bodyLength) } ?: mutableListOf()

        val cfirStruct = CfirStructImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            status = status,
            typeParameters = typeParams,
            symbol = symbol,
            superTypeRefs = superTypeRefs,
            declarations = members,
            name = name,
        )
        symbol.bind(cfirStruct)
        cfirStruct.markResolved()
        return cfirStruct
    }

    /** EnumDecl → CfirEnum */
    private fun convertEnum(decl: Decl): CfirEnum {
        val name = decl.classLikeName()
        val isRefEnum = false
        val symbol = CfirEnumSymbol(resolveClassId(decl, name), isRefEnum)
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)
        val info = decl.info(EnumInfo()) as? EnumInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = info?.let { deserializeBody(it::body, it.bodyLength) } ?: mutableListOf()

        val cfirEnum = CfirEnumImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            status = status,
            typeParameters = typeParams,
            symbol = symbol,
            superTypeRefs = superTypeRefs,
            declarations = members,
            name = name,
            isRefEnum = isRefEnum,
        )
        symbol.bind(cfirEnum)
        cfirEnum.markResolved()
        return cfirEnum
    }

    /** FuncDecl → CfirFunction */
    private fun convertFunctionOrEnumConstructor(decl: Decl): CfirDeclaration {
        if (isEnumConstructorDecl(decl)) {
            return convertEnumConstructorFromFunctionDecl(decl)
        }

        val name = Name.identifier(decl.identifier ?: "???")
        val symbol = CfirNamedFunctionSymbol(CallableId(packageFqName, name))
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)
        val returnTypeRef = buildTypeRef(decl.type)

        // 从 FuncInfo.funcBody.paramLists 获取参数
        val valueParams = mutableListOf<CfirValueParameter>()
        val funcInfo = decl.info(FuncInfo()) as? FuncInfo
        val funcBody = funcInfo?.funcBody
        if (funcBody != null) {
            for (i in 0 until funcBody.paramListsLength) {
                val paramList = funcBody.paramLists(i) ?: continue
                for (j in 0 until paramList.paramsLength) {
                    val paramIndex = decodeDeclRef(paramList.params(j)) ?: continue
                    val param = deserializeDecl(paramIndex) as? CfirValueParameter
                    if (param != null) valueParams.add(param)
                }
            }
        }

        val cfirFunc = CfirNamedFunctionImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = null,
            status = status,
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            name = name,
            valueParameters = valueParams,
            body = null, // 库声明不加载函数体
            isMut = testAttr(decl, AttrBit.MUT),
        )
        symbol.bind(cfirFunc)
        cfirFunc.markResolved()
        return cfirFunc
    }

    /** PropDecl → CfirProperty */
    private fun convertProperty(decl: Decl): CfirProperty {
        val name = Name.identifier(decl.identifier ?: "???")
        val symbol = CfirPropertySymbol(CallableId(packageFqName, name))
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)
        val returnTypeRef = buildTypeRef(decl.type)

        val cfirProp = CfirPropertyImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = null,
            status = status,
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            name = name,
            getter = null,
            setter = null,
        )
        symbol.bind(cfirProp)
        cfirProp.markResolved()
        return cfirProp
    }

    /** VarDecl → CfirVariable */
    private fun convertVariableOrEnumConstructor(decl: Decl): CfirDeclaration {
        if (isEnumConstructorDecl(decl)) {
            return convertEnumConstructorFromVariableDecl(decl)
        }

        val name = Name.identifier(decl.identifier ?: "???")
        val symbol = CfirFieldVariableSymbol(CallableId(packageFqName, name))
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)
        val returnTypeRef = buildTypeRef(decl.type)

        val varInfo = if (decl.infoType == DeclInfo.VarInfo) decl.info(VarInfo()) as? VarInfo else null
        val isVar = varInfo?.isVar ?: false

        val cfirVar = CfirFieldVariableImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = null,
            status = status,
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            name = name,
            initializer = null,
            isVar = isVar,
        )
        symbol.bind(cfirVar)
        cfirVar.markResolved()
        return cfirVar
    }

    /** ExtendDecl → CfirExtend */
    private fun convertVariableWithPattern(decl: Decl): CfirPatternVariable {
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)
        val returnTypeRef = buildTypeRef(decl.type)

        val info = if (decl.infoType == DeclInfo.VarWithPatternInfo) {
            decl.info(VarWithPatternInfo()) as? VarWithPatternInfo
        } else {
            null
        }
        val fallbackName = decl.identifier?.let(Name::identifier) ?: Name.special("<pattern>")
        val symbol = CfirPatternVariableSymbol(CallableId(fallbackName))
        val pattern = deserializeIrrefutablePattern(info?.irrefutablePattern, fallbackName, returnTypeRef)

        val cfirVar = CfirPatternVariableImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = null,
            status = status,
            initializer = null,
            isVar = info?.isVar ?: false,
            symbol = symbol,
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            pattern = pattern,
        )
        symbol.bind(cfirVar)
        cfirVar.markResolved()
        return cfirVar
    }

    private fun deserializeIrrefutablePattern(
        fbPattern: Pattern?,
        fallbackName: Name,
        returnTypeRef: CfirResolvedTypeRef,
    ): CfirPattern {
        if (fbPattern == null) {
            return buildBindingPattern {
                name = fallbackName
                typeRef = returnTypeRef
            }
        }

        return when (fbPattern.kind) {
            PatternKind.VarPattern -> buildBindingPattern {
                name = fallbackName
                typeRef = returnTypeRef
            }

            PatternKind.TuplePattern -> buildTuplePattern {
                for (i in 0 until fbPattern.patternsLength) {
                    elements += deserializeIrrefutablePattern(
                        fbPattern = fbPattern.patterns(i),
                        fallbackName = fallbackName,
                        returnTypeRef = returnTypeRef,
                    )
                }
            }

            PatternKind.WildcardPattern -> buildWildcardPattern()
            else -> buildBindingPattern {
                name = fallbackName
                typeRef = returnTypeRef
            }
        }
    }

    private fun convertExtend(decl: Decl): CfirExtend {
        val symbol = CfirExtendSymbol()
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)

        val extendInfo = decl.info(ExtendInfo()) as? ExtendInfo
        // 被扩展的类型来自 decl.type
        val extendedTypeRef = buildTypeRef(decl.type)
        val superTypeRefs = if (extendInfo != null) {
            deserializeInheritedTypes(extendInfo::inheritedTypes, extendInfo.inheritedTypesLength)
        } else mutableListOf()
        val members = if (extendInfo != null) {
            deserializeBody(extendInfo::body, extendInfo.bodyLength)
        } else mutableListOf()

        val cfirExtend = CfirExtendImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            status = status,
            typeParameters = typeParams,
            extendedTypeRef = extendedTypeRef,
            superTypeRefs = superTypeRefs,
            declarations = members,
        )
        symbol.bind(cfirExtend)
        cfirExtend.markResolved()
        return cfirExtend
    }

    /** TypeAliasDecl → CfirTypeAlias */
    private fun convertTypeAlias(decl: Decl): CfirTypeAlias {
        val name = decl.classLikeName()
        val symbol = CfirTypeAliasSymbol(resolveClassId(decl, name))
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)

        val aliasInfo = decl.info(AliasInfo()) as? AliasInfo
        val expandedTypeRef = if (aliasInfo != null) {
            buildTypeRef(aliasInfo.aliasedTy)
        } else {
            buildTypeRef(decl.type)
        }

        val cfirAlias = CfirTypeAliasImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            declarations = mutableListOf(),
            superTypeRefs = mutableListOf(),
            status = status,
            typeParameters = typeParams,
            name = name,
            expandedTypeRef = expandedTypeRef,
        )
        symbol.bind(cfirAlias)
        cfirAlias.markResolved()
        return cfirAlias
    }

    /** GenericParamDecl → CfirTypeParameter */
    private fun convertTypeParameter(decl: Decl): CfirTypeParameter {
        val name = Name.identifier(decl.identifier ?: "T")
        val symbol = CfirTypeParameterSymbol()

        // 泛型约束来自 decl.generic.constraints
        val bounds = mutableListOf<CfirTypeRef>()
        val generic = decl.generic
        if (generic != null) {
            for (i in 0 until generic.constraintsLength) {
                val constraint = generic.constraints(i) ?: continue
                // constraint.type 是约束类型的 SemaTy 索引
                bounds.add(buildTypeRef(constraint.type))
            }
        }

        val cfirParam = CfirTypeParameterImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            containingDeclarationSymbol = symbol,
            symbol = symbol,
            name = name,
            bounds = bounds,
        )
        symbol.bind(cfirParam)
        cfirParam.markResolved()
        return cfirParam
    }

    /** FuncParam → CfirValueParameter */
    private fun convertEnumConstructorFromVariableDecl(decl: Decl): CfirEnumConstructor {
        val name = Name.identifier(decl.identifier ?: "???")
        val symbol = CfirEnumConstructorSymbol(CallableId(packageFqName, name))
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)

        val enumCtor = CfirEnumConstructorImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = null,
            status = status,
            typeParameters = typeParams,
            returnTypeRef = buildImplicitTypeRef(),
            name = name,
        )
        symbol.bind(enumCtor)
        enumCtor.markResolved()
        return enumCtor
    }

    private fun convertEnumConstructorFromFunctionDecl(decl: Decl): CfirEnumConstructor {
        val name = Name.identifier(decl.identifier ?: "???")
        val symbol = CfirEnumConstructorSymbol(CallableId(packageFqName, name))
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)
        val payloadTypeRef = buildEnumConstructorPayloadTypeRef(decl)

        val enumCtor = CfirEnumConstructorImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = null,
            status = status,
            typeParameters = typeParams,
            returnTypeRef = payloadTypeRef,
            name = name,
        )
        symbol.bind(enumCtor)
        enumCtor.markResolved()
        return enumCtor
    }

    private fun buildEnumConstructorPayloadTypeRef(decl: Decl): CfirTypeRef {
        val funcInfo = decl.info(FuncInfo()) as? FuncInfo ?: return buildImplicitTypeRef()
        val funcBody = funcInfo.funcBody ?: return buildImplicitTypeRef()
        val payloadTypes = mutableListOf<ConeCangJieType>()

        for (i in 0 until funcBody.paramListsLength) {
            val paramList = funcBody.paramLists(i) ?: continue
            for (j in 0 until paramList.paramsLength) {
                val paramIndex = decodeDeclRef(paramList.params(j)) ?: continue
                val param = deserializeDecl(paramIndex) as? CfirValueParameter ?: continue
                val type = (param.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
                payloadTypes += type
            }
        }

        return when (payloadTypes.size) {
            0 -> buildImplicitTypeRef()
            1 -> buildResolvedTypeRef { coneType = payloadTypes.first() }
            else -> buildResolvedTypeRef { coneType = ConeTupleType(payloadTypes) }
        }
    }

    private fun resolveClassId(decl: Decl, fallbackName: Name): ClassId {
        val name = decl.classLikeName(fallbackName)
        return ClassId(packageFqName, name)
    }

    private fun Decl.classLikeName(fallback: Name = Name.identifier("___missing_class_name___")): Name {
        val rawName = identifier?.takeIf { it.isNotBlank() }
        return rawName?.let(Name::identifier) ?: fallback
    }

    private fun convertValueParameter(decl: Decl): CfirValueParameter {
        val info = decl.info(ParamInfo()) as? ParamInfo

        val name = Name.identifier(decl.identifier ?: "_")
        val symbol = CfirValueParameterSymbol(CallableId(name))
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)
        val returnTypeRef = buildTypeRef(decl.type)
        val isNamed = info?.isNamedParam ?: false
        val cfirParam = CfirValueParameterImpl(
            source = null,
            isNamed = isNamed,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = null,
            symbol = symbol,
            status = status,
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            name = name,
            defaultValue = null,
        )
        symbol.bind(cfirParam)
        cfirParam.markResolved()
        return cfirParam
    }
}
