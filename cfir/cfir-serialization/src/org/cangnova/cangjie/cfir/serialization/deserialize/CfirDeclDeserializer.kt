package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.*
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildEnumPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildTypePattern
import org.cangnova.cangjie.cfir.patterns.builder.buildTuplePattern
import org.cangnova.cangjie.cfir.patterns.builder.buildWildcardPattern
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
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
    private data class EnumOwnerContext(
        val classId: ClassId,
        val typeParameters: List<CfirTypeParameter>,
        val isRefEnum: Boolean,
    )

    private val packageFqName: FqName = FqName(context.header.fullPkgName)
    private val declsUnderDeserialization = HashSet<Int>()
    private val enumOwnerStack = mutableListOf<EnumOwnerContext>()
    /**
     * 记录“当前正在为哪个声明反序列化子声明”。
     *
     * `GenericParamDecl` / `FuncParam` 在 FlatBuffers 中是独立 Decl，
     * 但它们的 CFIR 语义必须绑定到真实宿主声明符号，不能退化为自引用。
     */
    private val containingDeclarationSymbolStack = mutableListOf<CfirBasedSymbol<*>>()

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

    private fun decodeExprRef(rawIndex: UInt): Int? {
        if (rawIndex == 0u || rawIndex == UInt.MAX_VALUE) return null
        val decoded = rawIndex.toInt() - 1
        if (decoded !in 0 until context.pkg.allExprsLength) return null
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
    private inline fun <R> withEnumOwner(owner: EnumOwnerContext, block: () -> R): R {
        enumOwnerStack += owner
        return try {
            block()
        } finally {
            enumOwnerStack.removeAt(enumOwnerStack.lastIndex)
        }
    }

    private val currentEnumOwner: EnumOwnerContext?
        get() = enumOwnerStack.lastOrNull()

    private inline fun <R> withContainingDeclarationSymbol(
        symbol: CfirBasedSymbol<*>,
        block: () -> R,
    ): R {
        containingDeclarationSymbolStack += symbol
        return try {
            block()
        } finally {
            containingDeclarationSymbolStack.removeAt(containingDeclarationSymbolStack.lastIndex)
        }
    }

    private val currentContainingDeclarationSymbol: CfirBasedSymbol<*>?
        get() = containingDeclarationSymbolStack.lastOrNull()

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
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val info = decl.info(ClassInfo()) as? ClassInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = info?.let {
            withContainingDeclarationSymbol(symbol) {
                deserializeBody(it::body, it.bodyLength)
            }
        } ?: mutableListOf()

        val cfirClass = CfirClassImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = EmptyDeprecationsProvider,
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
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val info = decl.info(InterfaceInfo()) as? InterfaceInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = info?.let {
            withContainingDeclarationSymbol(symbol) {
                deserializeBody(it::body, it.bodyLength)
            }
        } ?: mutableListOf()

        val cfirInterface = CfirInterfaceImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = EmptyDeprecationsProvider,
            declarations = members,
            status = status,
            typeParameters = typeParams,
            symbol = symbol,
            superTypeRefs = superTypeRefs,
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
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val info = decl.info(StructInfo()) as? StructInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = info?.let {
            withContainingDeclarationSymbol(symbol) {
                deserializeBody(it::body, it.bodyLength)
            }
        } ?: mutableListOf()

        val cfirStruct = CfirStructImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = EmptyDeprecationsProvider,
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
        val classId = resolveClassId(decl, name)
        val symbol = CfirEnumSymbol(classId, isRefEnum)
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val info = decl.info(EnumInfo()) as? EnumInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = info?.let {
            withEnumOwner(EnumOwnerContext(classId, typeParams, isRefEnum)) {
                withContainingDeclarationSymbol(symbol) {
                    deserializeBody(it::body, it.bodyLength)
                }
            }
        } ?: mutableListOf()

        val cfirEnum = CfirEnumImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = EmptyDeprecationsProvider,
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
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val returnTypeRef = buildTypeRef(decl.type)

        // 从 FuncInfo.funcBody.paramLists 获取参数
        val valueParams = mutableListOf<CfirValueParameter>()
        val funcInfo = decl.info(FuncInfo()) as? FuncInfo
        val funcBody = funcInfo?.funcBody
        if (funcBody != null) {
            withContainingDeclarationSymbol(symbol) {
                for (i in 0 until funcBody.paramListsLength) {
                    val paramList = funcBody.paramLists(i) ?: continue
                    for (j in 0 until paramList.paramsLength) {
                        val paramIndex = decodeDeclRef(paramList.params(j)) ?: continue
                        val param = deserializeDecl(paramIndex) as? CfirValueParameter
                        if (param != null) valueParams.add(param)
                    }
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
            deprecationsProvider = EmptyDeprecationsProvider,
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
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
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
            deprecationsProvider = EmptyDeprecationsProvider,
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            name = name,
            getter = null,
            setter = null,
            bodyResolveState = CfirPropertyBodyResolveState.ALL_BODIES_RESOLVED,
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
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
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
            deprecationsProvider = EmptyDeprecationsProvider,
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
        val returnTypeRef = buildTypeRef(decl.type)

        val info = if (decl.infoType == DeclInfo.VarWithPatternInfo) {
            decl.info(VarWithPatternInfo()) as? VarWithPatternInfo
        } else {
            null
        }
        val fallbackName = decl.identifier?.let(Name::identifier) ?: Name.special("<pattern>")
        val symbol = CfirPatternVariableSymbol(CallableId(Name.special("<pattern-variable>")))
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val pattern = deserializeIrrefutablePattern(
            fbPattern = info?.irrefutablePattern,
            fallbackName = fallbackName,
            defaultTypeRef = returnTypeRef,
            outerStatus = status,
            outerIsLocal = false,
            outerIsVar = info?.isVar ?: false,
        )

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
            deprecationsProvider = EmptyDeprecationsProvider,
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
        defaultTypeRef: CfirResolvedTypeRef,
        outerStatus: CfirDeclarationStatus,
        outerIsLocal: Boolean,
        outerIsVar: Boolean,
    ): CfirPattern {
        if (fbPattern == null) {
            val bindingVariable = deserializePatternBindingVariable(
                rawDeclRef = null,
                fallbackName = fallbackName,
                defaultTypeRef = defaultTypeRef,
                outerStatus = outerStatus,
                outerIsLocal = outerIsLocal,
                outerIsVar = outerIsVar,
            )
            return buildBindingPattern {
                name = bindingVariable.name
                typeRef = bindingVariable.returnTypeRef
                this.bindingVariable = bindingVariable
            }
        }

        return when (fbPattern.kind) {
            PatternKind.VarPattern -> {
                val bindingVariable = deserializePatternBindingVariable(
                    rawDeclRef = fbPattern.exprs(0),
                    fallbackName = fallbackName,
                    defaultTypeRef = defaultTypeRef,
                    outerStatus = outerStatus,
                    outerIsLocal = outerIsLocal,
                    outerIsVar = outerIsVar,
                )
                buildBindingPattern {
                    name = bindingVariable.name
                    typeRef = bindingVariable.returnTypeRef
                    this.bindingVariable = bindingVariable
                }
            }

            PatternKind.TuplePattern -> buildTuplePattern {
                for (i in 0 until fbPattern.patternsLength) {
                    elements += deserializeIrrefutablePattern(
                        fbPattern = fbPattern.patterns(i),
                        fallbackName = fallbackName,
                        defaultTypeRef = defaultTypeRef,
                        outerStatus = outerStatus,
                        outerIsLocal = outerIsLocal,
                        outerIsVar = outerIsVar,
                    )
                }
            }

            PatternKind.TypePattern -> {
                val resolvedTypeRef = buildTypeRef(fbPattern.types(0))
                val nestedPattern = fbPattern.patterns(0)
                val nestedBindingVariable = nestedPattern
                    ?.takeIf { it.kind == PatternKind.VarPattern }
                    ?.let {
                        deserializePatternBindingVariable(
                            rawDeclRef = it.exprs(0),
                            fallbackName = fallbackName,
                            defaultTypeRef = resolvedTypeRef,
                            outerStatus = outerStatus,
                            outerIsLocal = outerIsLocal,
                            outerIsVar = outerIsVar,
                        )
                    }
                buildTypePattern {
                    typeRef = resolvedTypeRef
                    bindingName = nestedBindingVariable?.name
                    bindingVariable = nestedBindingVariable
                }
            }

            PatternKind.EnumPattern -> buildEnumPattern {
                val referenceName = decodeExprRef(fbPattern.exprs(0))
                    ?.let(::extractReferenceName)
                    ?.takeIf { it.isNotBlank() }
                    ?.substringAfterLast('.')
                    ?.let(Name::identifier)
                    ?: Name.special("<enum-pattern>")
                constructorReference = buildNamedReference {
                    name = referenceName
                    source = null
                }
                for (i in 0 until fbPattern.patternsLength) {
                    arguments += deserializeIrrefutablePattern(
                        fbPattern = fbPattern.patterns(i),
                        fallbackName = fallbackName,
                        defaultTypeRef = defaultTypeRef,
                        outerStatus = outerStatus,
                        outerIsLocal = outerIsLocal,
                        outerIsVar = outerIsVar,
                    )
                }
            }

            PatternKind.WildcardPattern -> buildWildcardPattern()
            else -> buildBindingPattern {
                val bindingVariable = deserializePatternBindingVariable(
                    rawDeclRef = null,
                    fallbackName = fallbackName,
                    defaultTypeRef = defaultTypeRef,
                    outerStatus = outerStatus,
                    outerIsLocal = outerIsLocal,
                    outerIsVar = outerIsVar,
                )
                name = bindingVariable.name
                typeRef = bindingVariable.returnTypeRef
                this.bindingVariable = bindingVariable
            }
        }
    }

    private fun deserializePatternBindingVariable(
        rawDeclRef: UInt?,
        fallbackName: Name,
        defaultTypeRef: CfirTypeRef,
        outerStatus: CfirDeclarationStatus,
        outerIsLocal: Boolean,
        outerIsVar: Boolean,
    ): CfirPatternBindingVariable {
        val bindingDecl = rawDeclRef?.let(::decodeDeclRef)?.let(context.pkg::allDecls)
        val bindingName = bindingDecl?.identifier?.let(Name::identifier) ?: fallbackName
        val status = bindingDecl?.let(::buildStatus)?.let(::cloneStatus) ?: cloneStatus(outerStatus)
        val returnTypeRef = bindingDecl
            ?.takeIf { it.type != 0u }
            ?.let { buildTypeRef(it.type) }
            ?: defaultTypeRef
        val symbol = CfirPatternBindingSymbol(
            if (outerIsLocal) CallableId(bindingName) else CallableId(packageFqName, bindingName),
        )

        return CfirPatternBindingVariableImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = outerIsLocal,
            dispatchReceiverType = null,
            status = status,
            deprecationsProvider = EmptyDeprecationsProvider,
            initializer = null,
            isVar = outerIsVar,
            symbol = symbol,
            typeParameters = mutableListOf(),
            returnTypeRef = returnTypeRef,
            name = bindingName,
        ).also(symbol::bind)
    }

    private fun cloneStatus(status: CfirDeclarationStatus): CfirDeclarationStatusImpl {
        return CfirDeclarationStatusImpl(
            visibility = status.visibility,
            modality = status.modality,
        ).also { copied ->
            copied.isVisibilityExplicit = status.isVisibilityExplicit
            copied.isModalityExplicit = status.isModalityExplicit
            copied.isOverride = status.isOverride
            copied.isOperator = status.isOperator
            copied.isStatic = status.isStatic
            copied.isConst = status.isConst
            copied.isMut = status.isMut
            copied.isUnsafe = status.isUnsafe
            copied.isForeign = status.isForeign
            copied.isCommon = status.isCommon
            copied.isSpecific = status.isSpecific
            copied.isRedef = status.isRedef
            copied.isAbstract = status.isAbstract
            copied.isOpen = status.isOpen
            copied.isSealed = status.isSealed
        }
    }

    private fun extractReferenceName(exprIndex: Int): String? {
        val expr = context.pkg.allExprs(exprIndex) ?: return null
        if (expr.infoType != ExprInfo.ReferenceInfo) return null
        val referenceInfo = expr.info(ReferenceInfo()) as? ReferenceInfo ?: return null
        return referenceInfo.reference
    }

    private fun convertExtend(decl: Decl): CfirExtend {
        val symbol = CfirExtendSymbol()
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }

        val extendInfo = decl.info(ExtendInfo()) as? ExtendInfo
        // 被扩展的类型来自 decl.type
        val extendedTypeRef = buildTypeRef(decl.type)
        val superTypeRefs = if (extendInfo != null) {
            deserializeInheritedTypes(extendInfo::inheritedTypes, extendInfo.inheritedTypesLength)
        } else mutableListOf()
        val members = if (extendInfo != null) {
            withContainingDeclarationSymbol(symbol) {
                deserializeBody(extendInfo::body, extendInfo.bodyLength)
            }
        } else mutableListOf()

        val cfirExtend = CfirExtendImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
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
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }

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
            deprecationsProvider = EmptyDeprecationsProvider,
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
        val containingDeclarationSymbol = currentContainingDeclarationSymbol
            ?: error("Type parameter '${name.asString()}' must be deserialized inside a containing declaration")

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
            containingDeclarationSymbol = containingDeclarationSymbol,
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
        val owner = currentEnumOwner
        val symbol = CfirEnumConstructorSymbol(owner?.let { CallableId(it.classId, name) } ?: CallableId(packageFqName, name))
        val status = buildStatus(decl)
        val typeParams = owner?.typeParameters?.toMutableList() ?: deserializeTypeParameters(decl)

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
            deprecationsProvider = EmptyDeprecationsProvider,
            typeParameters = typeParams,
            returnTypeRef = buildEnumConstructorReturnTypeRef(owner),
            valueParameters = mutableListOf(),
            name = name,
        )
        symbol.bind(enumCtor)
        enumCtor.markResolved()
        return enumCtor
    }

    private fun convertEnumConstructorFromFunctionDecl(decl: Decl): CfirEnumConstructor {
        val name = Name.identifier(decl.identifier ?: "???")
        val owner = currentEnumOwner
        val symbol = CfirEnumConstructorSymbol(owner?.let { CallableId(it.classId, name) } ?: CallableId(packageFqName, name))
        val status = buildStatus(decl)
        val typeParams = owner?.typeParameters?.toMutableList() ?: withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val valueParameters = withContainingDeclarationSymbol(symbol) {
            deserializeFunctionParameters(decl)
        }

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
            deprecationsProvider = EmptyDeprecationsProvider,
            typeParameters = typeParams,
            returnTypeRef = buildEnumConstructorReturnTypeRef(owner),
            valueParameters = valueParameters,
            name = name,
        )
        symbol.bind(enumCtor)
        enumCtor.markResolved()
        return enumCtor
    }

    private fun deserializeFunctionParameters(decl: Decl): MutableList<CfirValueParameter> {
        val funcInfo = decl.info(FuncInfo()) as? FuncInfo ?: return mutableListOf()
        val funcBody = funcInfo.funcBody ?: return mutableListOf()
        val valueParameters = mutableListOf<CfirValueParameter>()

        for (i in 0 until funcBody.paramListsLength) {
            val paramList = funcBody.paramLists(i) ?: continue
            for (j in 0 until paramList.paramsLength) {
                val paramIndex = decodeDeclRef(paramList.params(j)) ?: continue
                val param = deserializeDecl(paramIndex) as? CfirValueParameter ?: continue
                valueParameters += param
            }
        }

        return valueParameters
    }

    private fun buildEnumConstructorReturnTypeRef(owner: EnumOwnerContext?): CfirTypeRef {
        owner ?: return buildImplicitTypeRef()

        val typeArguments = owner.typeParameters.map { parameter ->
            ConeTypeProjection(ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag()))
        }
        return buildResolvedTypeRef {
            coneType = ConeEnumType(
                lookupTag = owner.classId.toLookupTag(),
                typeArguments = typeArguments,
                isRefEnum = owner.isRefEnum,
            )
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
        val containingDeclarationSymbol = currentContainingDeclarationSymbol
            ?: error("Value parameter '${name.asString()}' must be deserialized inside a containing declaration")
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
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
            containingDeclarationSymbol = containingDeclarationSymbol,
            status = status,
            deprecationsProvider = EmptyDeprecationsProvider,
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
