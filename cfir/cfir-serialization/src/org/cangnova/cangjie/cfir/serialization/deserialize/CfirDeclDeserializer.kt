package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.*
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
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
    /**
     * 反序列化指定索引的声明。
     * @param declIndex allDecls 中的索引（0-based）
     */
    fun deserializeDecl(declIndex: Int): CfirDeclaration? {
        context.declCache[declIndex]?.let { return it }

        val decl = context.pkg.allDecls(declIndex) ?: return null
        val result = convertDecl(decl) ?: return null
        context.declCache[declIndex] = result
        return result
    }

    private fun convertDecl(decl: Decl): CfirDeclaration? {
        return when (decl.kind) {
            DeclKind.ClassDecl -> convertClass(decl, CfirClassKind.CLASS)
            DeclKind.InterfaceDecl -> convertClass(decl, CfirClassKind.INTERFACE)
            DeclKind.StructDecl -> convertClass(decl, CfirClassKind.STRUCT)
            DeclKind.EnumDecl -> convertClass(decl, CfirClassKind.ENUM)
            DeclKind.FuncDecl -> convertFunction(decl)
            DeclKind.PropDecl -> convertProperty(decl)
            DeclKind.VarDecl -> convertVariable(decl)
            DeclKind.ExtendDecl -> convertExtend(decl)
            DeclKind.TypeAliasDecl -> convertTypeAlias(decl)
            DeclKind.GenericParamDecl -> convertTypeParameter(decl)
            DeclKind.FuncParam -> convertValueParameter(decl)
            else -> null // InvalidDecl, VarWithPatternDecl, BuiltInDecl 暂不处理
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
    }

    private fun testAttr(decl: Decl, bit: Int): Boolean {
        val wordIndex = bit / 64
        val bitIndex = bit % 64
        if (wordIndex >= decl.attributesLength) return false
        return (decl.attributes(wordIndex).toLong() shr bitIndex) and 1L == 1L
    }

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
            annotations = emptyList(),
            coneType = coneType,
            delegatedTypeRef = null,
        )
    }

    /** 反序列化泛型参数列表 */
    private fun deserializeTypeParameters(decl: Decl): List<CfirTypeParameter> {
        val generic = decl.generic ?: return emptyList()
        val len = generic.typeParametersLength
        if (len == 0) return emptyList()
        return (0 until len).mapNotNull { i ->
            val paramIndex = generic.typeParameters(i).toInt()
            deserializeDecl(paramIndex) as? CfirTypeParameter
        }
    }

    /** 反序列化继承类型列表 */
    private fun deserializeInheritedTypes(
        getter: (Int) -> UInt,
        length: Int,
    ): List<CfirResolvedTypeRef> {
        if (length == 0) return emptyList()
        return (0 until length).map { buildTypeRef(getter(it)) }
    }

    /** 反序列化成员声明列表（延迟加载） */
    private fun deserializeBody(
        getter: (Int) -> UInt,
        length: Int,
    ): List<CfirDeclaration> {
        if (length == 0) return emptyList()
        return (0 until length).mapNotNull { i ->
            deserializeDecl(getter(i).toInt())
        }
    }

    /** 设置声明的 resolve 状态为 BODY_RESOLVE（库声明已完全解析） */
    private fun CfirDeclaration.markResolved() {
        initDefaultResolveState()
        replaceResolvePhase(CfirResolvePhase.BODY_RESOLVE)
    }

    // ---- 声明转换方法 ----

    /** Class/Interface/Struct/Enum → CfirClass */
    private fun convertClass(decl: Decl, classKind: CfirClassKind): CfirClass {
        val name = Name.identifier(decl.identifier ?: "???")
        val symbol = CfirClassSymbol()
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)

        // 根据 DeclInfo 类型获取继承类型和成员
        val superTypeRefs: List<CfirResolvedTypeRef>
        val members: List<CfirDeclaration>

        when (decl.infoType) {
            DeclInfo.ClassInfo -> {
                val info = decl.info(ClassInfo()) as ClassInfo
                superTypeRefs = deserializeInheritedTypes(info::inheritedTypes, info.inheritedTypesLength)
                members = deserializeBody(info::body, info.bodyLength)
            }
            DeclInfo.InterfaceInfo -> {
                val info = decl.info(InterfaceInfo()) as InterfaceInfo
                superTypeRefs = deserializeInheritedTypes(info::inheritedTypes, info.inheritedTypesLength)
                members = deserializeBody(info::body, info.bodyLength)
            }
            DeclInfo.StructInfo -> {
                val info = decl.info(StructInfo()) as StructInfo
                superTypeRefs = deserializeInheritedTypes(info::inheritedTypes, info.inheritedTypesLength)
                members = deserializeBody(info::body, info.bodyLength)
            }
            DeclInfo.EnumInfo -> {
                val info = decl.info(EnumInfo()) as EnumInfo
                superTypeRefs = deserializeInheritedTypes(info::inheritedTypes, info.inheritedTypesLength)
                members = deserializeBody(info::body, info.bodyLength)
            }
            else -> {
                superTypeRefs = emptyList()
                members = emptyList()
            }
        }

        val cfirClass = CfirClassImpl(
            source = null,
            moduleData = context.moduleData,
            annotations = emptyList(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            status = status,
            typeParameters = typeParams,
            superTypeRefs = superTypeRefs,
            declarations = members,
            name = name,
            classKind = classKind,
        )
        symbol.bind(cfirClass)
        cfirClass.markResolved()
        return cfirClass
    }

    /** FuncDecl → CfirFunction */
    private fun convertFunction(decl: Decl): CfirFunction {
        val name = Name.identifier(decl.identifier ?: "???")
        val symbol = CfirFunctionSymbol()
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
                    val paramIndex = paramList.params(j).toInt()
                    val param = deserializeDecl(paramIndex) as? CfirValueParameter
                    if (param != null) valueParams.add(param)
                }
            }
        }

        val cfirFunc = CfirFunctionImpl(
            source = null,
            moduleData = context.moduleData,
            annotations = emptyList(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
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
        val symbol = CfirPropertySymbol()
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)
        val returnTypeRef = buildTypeRef(decl.type)

        val cfirProp = CfirPropertyImpl(
            source = null,
            moduleData = context.moduleData,
            annotations = emptyList(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            status = status,
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            name = name,
            initializer = null,
            getter = null,
            setter = null,
        )
        symbol.bind(cfirProp)
        cfirProp.markResolved()
        return cfirProp
    }

    /** VarDecl → CfirVariable */
    private fun convertVariable(decl: Decl): CfirVariable {
        val name = Name.identifier(decl.identifier ?: "???")
        val symbol = CfirVariableSymbol()
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)
        val returnTypeRef = buildTypeRef(decl.type)

        val varInfo = decl.info(VarInfo()) as? VarInfo
        val isVar = varInfo?.isVar ?: false

        val cfirVar = CfirVariableImpl(
            source = null,
            moduleData = context.moduleData,
            annotations = emptyList(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
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
    private fun convertExtend(decl: Decl): CfirExtend {
        val symbol = CfirExtendSymbol()
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)

        val extendInfo = decl.info(ExtendInfo()) as? ExtendInfo
        // 被扩展的类型来自 decl.type
        val extendedTypeRef = buildTypeRef(decl.type)
        val superTypeRefs = if (extendInfo != null) {
            deserializeInheritedTypes(extendInfo::inheritedTypes, extendInfo.inheritedTypesLength)
        } else emptyList()
        val members = if (extendInfo != null) {
            deserializeBody(extendInfo::body, extendInfo.bodyLength)
        } else emptyList()

        val cfirExtend = CfirExtendImpl(
            source = null,
            moduleData = context.moduleData,
            annotations = emptyList(),
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
        val name = Name.identifier(decl.identifier ?: "???")
        val symbol = CfirTypeAliasSymbol()
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
            annotations = emptyList(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
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
        val bounds = mutableListOf<CfirResolvedTypeRef>()
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
            annotations = emptyList(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            name = name,
            bounds = bounds,
        )
        symbol.bind(cfirParam)
        cfirParam.markResolved()
        return cfirParam
    }

    /** FuncParam → CfirValueParameter */
    private fun convertValueParameter(decl: Decl): CfirValueParameter {
        val name = Name.identifier(decl.identifier ?: "_")
        val symbol = CfirValueParameterSymbol()
        val status = buildStatus(decl)
        val typeParams = deserializeTypeParameters(decl)
        val returnTypeRef = buildTypeRef(decl.type)

        val cfirParam = CfirValueParameterImpl(
            source = null,
            moduleData = context.moduleData,
            annotations = emptyList(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
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
