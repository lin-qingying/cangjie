/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildConstructor
import org.cangnova.cangjie.cfir.declarations.builder.buildPrimaryConstructor
import org.cangnova.cangjie.cfir.declarations.impl.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedArgumentExpression
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.builder.*
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.*
import org.cangnova.cangjie.name.OperatorNameConventions.asOperatorName

/**
 * 声明反序列化器：Decl → CfirDeclaration。
 *
 * 将 .cjo 中的 FlatBuffers Decl 表转换为 CFIR 声明树。
 * 所有反序列化声明的 origin 为 Library，resolveState 初始化为 BODY_RESOLVE。
 */
@OptIn(CfirImplementationDetail::class, ResolveStateAccess::class)
class CfirDeclDeserializer(
    /** 当前 `.cjo` 包的反序列化上下文、缓存与跨包解析入口。 */
    private val context: CfirDeserializationContext,
    /** 与当前声明反序列化器配套的类型反序列化器。 */
    private val typeDeserializer: CfirTypeDeserializer,
) {
    /** 当前正在反序列化的 enum owner 上下文。 */
    private data class EnumOwnerContext(
        /** enum 声明的 classId，用于构造 enum constructor callable id 与返回类型。 */
        val classId: ClassId,
        /** enum 声明自身的类型参数，enum constructor 返回类型需要复用这些 symbol。 */
        val typeParameters: List<CfirTypeParameter>,
        /** 是否为 ref enum，用于恢复 [ConeEnumType] 语义。 */
        val isRefEnum: Boolean,
    )

    /** 当前正在反序列化的 class-like owner 上下文。 */
    private data class ClassLikeOwnerContext(
        /** class-like 声明的 classId，用于构造构造器返回类型。 */
        val classId: ClassId,
        /** class-like 声明自身的类型参数，构造器返回类型需要复用这些 symbol。 */
        val typeParameters: List<CfirTypeParameter>,
    )

    /** 当前包完整包名。 */
    private val packageFqName: FqName = FqName(context.header.fullPkgName)
    /** 当前调用栈正在反序列化的 `allDecls` 索引集合，用于避免声明递归重入。 */
    private val declsUnderDeserialization = HashSet<Int>()
    /** enum owner 栈，用于 enum constructor 和 enum 成员反序列化。 */
    private val enumOwnerStack = mutableListOf<EnumOwnerContext>()
    /** class-like owner 栈，用于构造器返回类型和成员 callableId 构造。 */
    private val classLikeOwnerStack = mutableListOf<ClassLikeOwnerContext>()
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
        val lock = context.declMaterializationLock(declIndex)
        synchronized(lock) {
            context.declCache[declIndex]?.let { return it }
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
                context.declCache.putIfAbsent(declIndex, result)
                context.declCache[declIndex] ?: result
            } finally {
                declsUnderDeserialization.remove(declIndex)
                context.releaseDeclMaterializationLock(declIndex, lock)
            }
        }
    }

    /** 按 FlatBuffers [Decl.kind] 分派到具体 CFIR 声明转换器。 */
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
            DeclKind.BuiltInDecl -> convertBuiltIn(decl)
            else -> null // InvalidDecl
        }
    }
    // ---- 属性位域解析 ----

    /**
     * common-part `.cjo` 的 Decl.attributes 直接序列化自 AST AttributePack。
     *
     * 官方 ASTWriter 直接写 `AttributePack.GetRawAttrs()`，
     * ASTLoader 也按同一组 bitset 原样恢复，因此这里必须使用 AST Attribute 的枚举位，
     * 不能套用 CHIR 的 `Attribute::VIRTUAL` 等另一套位布局。
     */
    private object AttrBit {
        /** `static` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val STATIC = Attribute.STATIC.ordinal
        /** `public` 可见性在 AST AttributePack 中的 bit 下标。 */
        val PUBLIC = Attribute.PUBLIC.ordinal
        /** `private` 可见性在 AST AttributePack 中的 bit 下标。 */
        val PRIVATE = Attribute.PRIVATE.ordinal
        /** `protected` 可见性在 AST AttributePack 中的 bit 下标。 */
        val PROTECTED = Attribute.PROTECTED.ordinal
        /** `internal` 可见性在 AST AttributePack 中的 bit 下标。 */
        val INTERNAL = Attribute.INTERNAL.ordinal
        /** `override` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val OVERRIDE = Attribute.OVERRIDE.ordinal
        /** `redef` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val REDEF = Attribute.REDEF.ordinal
        /** `abstract` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val ABSTRACT = Attribute.ABSTRACT.ordinal
        /** `sealed` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val SEALED = Attribute.SEALED.ordinal
        /** `open` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val OPEN = Attribute.OPEN.ordinal
        /** `operator` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val OPERATOR = Attribute.OPERATOR.ordinal
        /** `foreign` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val FOREIGN = Attribute.FOREIGN.ordinal
        /** `unsafe` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val UNSAFE = Attribute.UNSAFE.ordinal
        /** `mut` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val MUT = Attribute.MUT.ordinal
        /** 主构造器标记在 AST AttributePack 中的 bit 下标。 */
        val PRIMARY_CONSTRUCTOR = Attribute.PRIMARY_CONSTRUCTOR.ordinal
        /** 构造器标记在 AST AttributePack 中的 bit 下标。 */
        val CONSTRUCTOR = Attribute.CONSTRUCTOR.ordinal
        /** enum constructor 标记在 AST AttributePack 中的 bit 下标。 */
        val ENUM_CONSTRUCTOR = Attribute.ENUM_CONSTRUCTOR.ordinal
        /** 参数具有默认值的标记；非字面量默认表达式不会写入 ParamInfo.defaultVal。 */
        val HAS_INITIAL = Attribute.HAS_INITIAL.ordinal
    }

    /** 测试 [decl] 的原始 AttributePack 中是否包含指定 bit。 */
    private fun testAttr(decl: Decl, bit: Int): Boolean {
        val wordIndex = bit / 64
        val bitIndex = bit % 64
        if (wordIndex >= decl.attributesLength) return false
        return (decl.attributes(wordIndex).toLong() shr bitIndex) and 1L == 1L
    }

    /**
     * 解码 PackageFormat 中 1-based 的声明引用索引。
     *
     * `0` 与 [UInt.MAX_VALUE] 都表示无效引用；返回值是 `allDecls` 的 0-based 下标。
     */
    private fun decodeDeclRef(rawIndex: UInt): Int? {
        if (rawIndex == 0u || rawIndex == UInt.MAX_VALUE) return null
        val decoded = rawIndex.toInt() - 1
        if (decoded !in 0 until context.pkg.allDeclsLength) return null
        return decoded
    }

    /**
     * 解码 PackageFormat 中 1-based 的表达式引用索引。
     *
     * `0` 与 [UInt.MAX_VALUE] 都表示无效引用；返回值是 `allExprs` 的 0-based 下标。
     */
    private fun decodeExprRef(rawIndex: UInt): Int? {
        if (rawIndex == 0u || rawIndex == UInt.MAX_VALUE) return null
        val decoded = rawIndex.toInt() - 1
        if (decoded !in 0 until context.pkg.allExprsLength) return null
        return decoded
    }

    /**
     * 将 CJO 声明上的全部注解恢复为 CFIR annotation call。
     *
     * target FullId 是注解的语义身份；identifier 只用于构造可读 callee 名，不参与平台注解匹配。
     * FlatBuffers 当前只序列化 LitConstExpr，因此这里完整恢复其支持的字面量及命名/位置参数。
     */
    private fun deserializeAnnotations(
        decl: Decl,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
    ): MutableOrEmptyList<CfirAnnotation> {
        if (decl.annotationsLength == 0) return MutableOrEmptyList.empty()

        val result = mutableListOf<CfirAnnotation>()
        for (index in 0 until decl.annotationsLength) {
            val serialized = decl.annotations(index) ?: continue
            deserializeAnnotation(serialized, containingDeclarationSymbol)?.let(result::add)
        }
        return result.toMutableOrEmpty()
    }

    /** 恢复单个 CJO 注解调用。 */
    private fun deserializeAnnotation(
        serialized: Anno,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
    ): CfirAnnotationCall? {
        val rawIdentifier = serialized.identifier.orEmpty()
        val targetClassId = serialized.target?.let(context.fullIdResolver::resolveClassId)
        val shortName = targetClassId?.shortClassName
            ?: Name.identifierIfValid(
                rawIdentifier
                    .removePrefix("@!")
                    .removePrefix("@")
                    .substringAfterLast('.'),
            )
            ?: Name.ERROR_NAME

        val annotationTypeRef: CfirTypeRef = if (targetClassId != null) {
            buildResolvedTypeRef {
                customRenderer = false
                coneType = ConeClassLikeType(
                    lookupTag = ConeClassLikeLookupTagImpl(targetClassId),
                    typeArguments = emptyList(),
                )
            }
        } else {
            buildImplicitTypeRef {
                customRenderer = false
            }
        }

        val arguments = buildList {
            for (argumentIndex in 0 until serialized.argsLength) {
                val serializedArgument = serialized.args(argumentIndex) ?: continue
                val expression = deserializeAnnotationLiteral(serializedArgument.expr) ?: continue
                val argumentName = serializedArgument.name?.takeIf(String::isNotBlank)
                add(
                    if (argumentName == null) {
                        expression
                    } else {
                        buildNamedArgumentExpression {
                            this.expression = expression
                            this.argumentName = Name.identifier(argumentName)
                        }
                    }
                )
            }
        }

        return buildAnnotationCall {
            typeRef = annotationTypeRef
            this.arguments.addAll(arguments)
            argumentList = buildArgumentList {
                this.arguments.addAll(arguments)
            }
            calleeReference = buildNamedReference {
                name = shortName
            }
            this.containingDeclarationSymbol = containingDeclarationSymbol
        }
    }

    /** 恢复 CJO 注解参数允许的 LitConstExpr。 */
    private fun deserializeAnnotationLiteral(rawExprIndex: UInt): CfirExpression? {
        val exprIndex = decodeExprRef(rawExprIndex) ?: return null
        val expr = context.pkg.allExprs(exprIndex) ?: return null
        if (expr.kind != ExprKind.LitConstExpr || expr.infoType != ExprInfo.LitConstInfo) return null
        val literal = expr.info(LitConstInfo()) as? LitConstInfo ?: return null
        val rawValue = literal.strValue.orEmpty()

        val (kind, value) = when (literal.constKind) {
            LitConstKind.Integer -> CfirLiteralKind.INT to rawValue
            LitConstKind.Float -> CfirLiteralKind.FLOAT to rawValue
            // 官方 RUNE_BYTE 定型 UInt8，与 Rune 分开建模。
            LitConstKind.RuneByte -> CfirLiteralKind.BYTE to rawValue
            LitConstKind.Rune,
            -> CfirLiteralKind.RUNE to rawValue
            LitConstKind.String,
            LitConstKind.JString,
            -> CfirLiteralKind.STRING to rawValue
            LitConstKind.Bool -> CfirLiteralKind.BOOLEAN to rawValue.toBooleanStrictOrNull()
            LitConstKind.Unit -> CfirLiteralKind.UNIT to null
            else -> return null
        }

        return buildLiteralExpression {
            coneTypeOrNull = typeDeserializer.deserializeTypeFromField(expr.type)
            this.kind = kind
            this.value = value
        }
    }

    /** 判断声明是否被官方 AST AttributePack 标记为 enum constructor。 */
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
        val visibility = resolveVisibility(decl)
        val modality = resolveModality(decl)
        val status = CfirDeclarationStatusImpl(
            visibility = visibility,
            modality = modality,
        )
        status.isVisibilityExplicit = visibility != Visibilities.Public
        status.isModalityExplicit = modality != Modality.FINAL
        status.isAbstract = testAttr(decl, AttrBit.ABSTRACT)
        status.isOpen = testAttr(decl, AttrBit.OPEN)
        status.isSealed = testAttr(decl, AttrBit.SEALED)
        status.isStatic = testAttr(decl, AttrBit.STATIC)
        status.isConst = deserializeConstStatus(decl)
        status.isOverride = testAttr(decl, AttrBit.OVERRIDE)
        status.isOperator = testAttr(decl, AttrBit.OPERATOR)
        status.isForeign = testAttr(decl, AttrBit.FOREIGN)
        status.isUnsafe = testAttr(decl, AttrBit.UNSAFE)
        status.isMut = testAttr(decl, AttrBit.MUT)
        status.isRedef = testAttr(decl, AttrBit.REDEF)
        return status
    }

    /**
     * 从声明专属 info 中恢复 const 语义。
     *
     * PackageFormat 不把 const 放进 AttributePack，而是分别保存在函数、变量、模式变量和
     * 属性的 info 表中。这里集中恢复该状态，保证 Library CFIR 与 Source CFIR 的声明状态等价，
     * 下游常量求值、构造器和成员检查器无需重新猜测序列化语义。
     */
    private fun deserializeConstStatus(decl: Decl): Boolean = when (decl.infoType) {
        DeclInfo.FuncInfo -> checkNotNull(decl.info(FuncInfo()) as? FuncInfo) {
            "FuncInfo is missing for declaration '${decl.identifier ?: "<anonymous>"}'"
        }.isConst

        DeclInfo.VarInfo -> checkNotNull(decl.info(VarInfo()) as? VarInfo) {
            "VarInfo is missing for declaration '${decl.identifier ?: "<anonymous>"}'"
        }.isConst

        DeclInfo.VarWithPatternInfo -> checkNotNull(decl.info(VarWithPatternInfo()) as? VarWithPatternInfo) {
            "VarWithPatternInfo is missing for declaration '${decl.identifier ?: "<anonymous>"}'"
        }.isConst

        DeclInfo.PropInfo -> checkNotNull(decl.info(PropInfo()) as? PropInfo) {
            "PropInfo is missing for declaration '${decl.identifier ?: "<anonymous>"}'"
        }.isConst

        else -> false
    }

    /** 将 type 字段值转换为 CfirResolvedTypeRef */
    private fun buildTypeRef(typeFieldValue: UInt): CfirResolvedTypeRef {
        val coneType = typeDeserializer.deserializeTypeFromField(typeFieldValue)
        return CfirResolvedTypeRefImpl(
            source = null,
            annotations = MutableOrEmptyList.empty(),
            customRenderer = false,
            coneType = coneType,
            delegatedTypeRef = null,
        )
    }

    /**
     * `FuncDecl.type` 是完整函数类型 `(P1, P2, ...) -> R`，而 CFIR `CfirFunction.returnTypeRef`
     * 只表示 callable 的结果类型 `R`。官方 ASTLoader 也是把 `decl.ty` 与
     * `funcBody.retType` 分开恢复，这里必须保持同样的声明形状。
     */
    private fun buildFunctionReturnTypeRef(decl: Decl): CfirResolvedTypeRef {
        val funcInfo = decl.info(FuncInfo()) as? FuncInfo
            ?: error("FuncDecl '${decl.identifier ?: "<anonymous>"}' must contain FuncInfo")
        val encodedReturnType = funcInfo.funcBody?.retType ?: 0u
        if (encodedReturnType != 0u) {
            return buildTypeRef(encodedReturnType)
        }

        val functionType = typeDeserializer.deserializeTypeFromField(decl.type) as? ConeFunctionType
            ?: error("FuncDecl '${decl.identifier ?: "<anonymous>"}' must carry FuncTy in Decl.type")
        return buildResolvedTypeRef {
            customRenderer = false
            coneType = functionType.returnType
        }
    }

    /** 反序列化泛型参数列表 */
    private fun deserializeTypeParameters(decl: Decl): MutableList<CfirTypeParameter> {
        val generic = decl.generic ?: return mutableListOf()
        val len = generic.typeParametersLength
        if (len == 0) return mutableListOf()
        val typeParameters = (0 until len).mapNotNullTo(mutableListOf()) { i ->
            val paramIndex = decodeDeclRef(generic.typeParameters(i)) ?: return@mapNotNullTo null
            deserializeDecl(paramIndex) as? CfirTypeParameter
        }
        applyOwnerGenericConstraints(generic, typeParameters)
        return typeParameters
    }

    /**
     * `.cjo` 的泛型约束挂在 owner `Decl.generic.constraints` 上，而不是
     * `GenericParamDecl.generic` 上。constraint.type 是被约束的类型参数，
     * constraint.uppers 才是其上界；反序列化后必须写回对应 CfirTypeParameter。
     */
    @Suppress("UNCHECKED_CAST")
    private fun applyOwnerGenericConstraints(
        generic: Generic,
        typeParameters: List<CfirTypeParameter>,
    ) {
        if (generic.constraintsLength == 0 || typeParameters.isEmpty()) return

        val typeParametersBySymbol = typeParameters.associateBy { it.symbol }
        for (i in 0 until generic.constraintsLength) {
            val constraint = generic.constraints(i) ?: continue
            val constrainedType = typeDeserializer.deserializeTypeFromField(constraint.type)
            val constrainedSymbol = (constrainedType as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol
                ?: continue
            val typeParameter = typeParametersBySymbol[constrainedSymbol] ?: continue
            val mutableBounds = typeParameter.bounds as? MutableList<CfirTypeRef> ?: continue

            for (upperIndex in 0 until constraint.uppersLength) {
                mutableBounds += buildTypeRef(constraint.uppers(upperIndex))
            }
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

    /** 在 enum 声明反序列化期间压入 owner 上下文。 */
    private inline fun <R> withEnumOwner(owner: EnumOwnerContext, block: () -> R): R {
        enumOwnerStack += owner
        return try {
            block()
        } finally {
            enumOwnerStack.removeAt(enumOwnerStack.lastIndex)
        }
    }

    /** 当前 enum owner；不在 enum 声明内部时为 null。 */
    private val currentEnumOwner: EnumOwnerContext?
        get() = enumOwnerStack.lastOrNull()

    /** 在 class-like 声明反序列化期间压入 owner 上下文。 */
    private inline fun <R> withClassLikeOwner(owner: ClassLikeOwnerContext, block: () -> R): R {
        classLikeOwnerStack += owner
        return try {
            block()
        } finally {
            classLikeOwnerStack.removeAt(classLikeOwnerStack.lastIndex)
        }
    }

    /** 当前 class-like owner；不在 class/interface/struct/enum 内部时为 null。 */
    private val currentClassLikeOwner: ClassLikeOwnerContext?
        get() = classLikeOwnerStack.lastOrNull()

    /**
     * 在反序列化子声明期间压入真实宿主声明符号。
     *
     * 类型参数和值参数在二进制里是独立 Decl，但 CFIR 必须把它们绑定回函数、构造器或 class-like symbol。
     */
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

    /** 当前子声明应绑定到的宿主声明符号。 */
    private val currentContainingDeclarationSymbol: CfirBasedSymbol<*>?
        get() = containingDeclarationSymbolStack.lastOrNull()

    /** 设置声明的 resolve 状态为 BODY_RESOLVE（库声明已完全解析）。 */
    private fun CfirDeclaration.markResolved() {
        initDefaultResolveState()
        replaceResolvePhase(CfirResolvePhase.BODY_RESOLVE)
    }

    // ---- 声明转换方法 ----

    /**
     * `BuiltInDecl` → [CfirBuiltInDeclaration]。
     *
     * 官方 `ASTLoader::LoadBuiltInDecl` 只接受 `BuiltInInfo` union，并按 FlatBuffers
     * `BuiltInType` 恢复声明种类、通用参数和约束。这里保持同样的严格边界：格式中的
     * union 类型、内建编号、名称/class id、泛型参数数量和约束形状任一不一致都属于
     * CJO 格式错误，不能静默跳过或降级为普通 class-like 声明。
     */
    private fun convertBuiltIn(decl: Decl): CfirBuiltInDeclaration {
        check(decl.infoType == DeclInfo.BuiltInInfo) {
            "BuiltInDecl '${decl.identifier ?: "<anonymous>"}' must contain BuiltInInfo, " +
                "actual infoType=${decl.infoType}"
        }
        val info = decl.info(BuiltInInfo()) as? BuiltInInfo
            ?: error("BuiltInDecl '${decl.identifier ?: "<anonymous>"}' has no BuiltInInfo payload")
        val kind = when (info.builtInType) {
            BuiltInType.Array -> CfirBuiltInTypeKind.ARRAY
            BuiltInType.VArray -> CfirBuiltInTypeKind.VARRAY
            BuiltInType.CPointer -> CfirBuiltInTypeKind.CPOINTER
            BuiltInType.CString -> CfirBuiltInTypeKind.CSTRING
            BuiltInType.CFunc -> CfirBuiltInTypeKind.CFUNC
            else -> error(
                "Unknown BuiltInType=${info.builtInType} for '${decl.identifier ?: "<anonymous>"}'",
            )
        }
        val name = decl.classLikeName()
        check(name == kind.classId.shortClassName) {
            "BuiltInType ${kind.name} is encoded as '${name.asString()}', " +
                "expected '${kind.classId.shortClassName.asString()}'"
        }
        val classId = resolveClassId(decl, name)
        check(classId == kind.classId) {
            "BuiltInType ${kind.name} has classId $classId, expected ${kind.classId}"
        }

        val generic = decl.generic
        val serializedTypeParameterCount = generic?.typeParametersLength ?: 0
        check(serializedTypeParameterCount == kind.typeParameterCount) {
            "BuiltInType ${kind.name} has $serializedTypeParameterCount serialized type parameters, " +
                "expected ${kind.typeParameterCount}"
        }
        val expectedConstraintCount = if (kind == CfirBuiltInTypeKind.CPOINTER) 1 else 0
        val serializedConstraintCount = generic?.constraintsLength ?: 0
        check(serializedConstraintCount == expectedConstraintCount) {
            "BuiltInType ${kind.name} has $serializedConstraintCount serialized constraints, " +
                "expected $expectedConstraintCount"
        }

        val symbol = CfirBuiltInTypeSymbol(classId, kind)
        val typeParameters = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        check(typeParameters.size == kind.typeParameterCount) {
            "BuiltInType ${kind.name} materialized ${typeParameters.size} type parameters, " +
                "expected ${kind.typeParameterCount}"
        }
        val typeParameterRefs: MutableList<CfirTypeParameterRef> = typeParameters
            .mapTo(mutableListOf()) { it }

        val declaration = CfirBuiltInDeclaration(
            moduleData = context.moduleData,
            symbol = symbol,
            name = name,
            kind = kind,
            scopeProvider = context.moduleData.session.cangjieScopeProvider,
            annotations = deserializeAnnotations(decl, symbol),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            typeParameters = typeParameterRefs,
            status = buildStatus(decl),
            deprecationsProvider = EmptyDeprecationsProvider,
            declarations = mutableListOf(),
            superTypeRefs = mutableListOf(),
        )
        declaration.markResolved()
        return declaration
    }

    /** ClassDecl → CfirClass */
    private fun convertClass(decl: Decl): CfirClass {
        val name = decl.classLikeName()
        val classId = resolveClassId(decl, name)
        val symbol = CfirClassSymbol(classId)
        val status = buildStatus(decl).resolvedForStatuslessDeclaration()
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val info = decl.info(ClassInfo()) as? ClassInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = info?.let {
            withClassLikeOwner(ClassLikeOwnerContext(classId, typeParams)) {
                withContainingDeclarationSymbol(symbol) {
                    deserializeBody(it::body, it.bodyLength)
                }
            }
        } ?: mutableListOf()

        val cfirClass = CfirClassImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializeAnnotations(decl, symbol),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = EmptyDeprecationsProvider,
            status = status,
            typeParameters = typeParams,
            symbol = symbol,
            superTypeRefs = superTypeRefs,
            declarations = members,
            name = name,
            scopeProvider = context.moduleData.session.cangjieScopeProvider,
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
            withClassLikeOwner(ClassLikeOwnerContext(classId, typeParams)) {
                withContainingDeclarationSymbol(symbol) {
                    deserializeBody(it::body, it.bodyLength)
                }
            }
        } ?: mutableListOf()

        val cfirInterface = CfirInterfaceImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializeAnnotations(decl, symbol),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = EmptyDeprecationsProvider,
            declarations = members,
            status = status,
            typeParameters = typeParams,
            symbol = symbol,
            superTypeRefs = superTypeRefs,
            name = name,
            scopeProvider = context.moduleData.session.cangjieScopeProvider,
        )
        symbol.bind(cfirInterface)
        cfirInterface.markResolved()
        return cfirInterface
    }

    /** StructDecl → CfirStruct */
    private fun convertStruct(decl: Decl): CfirStruct {
        val name = decl.classLikeName()
        val classId = resolveClassId(decl, name)
        val symbol = CfirStructSymbol(classId)
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val info = decl.info(StructInfo()) as? StructInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = info?.let {
            withClassLikeOwner(ClassLikeOwnerContext(classId, typeParams)) {
                withContainingDeclarationSymbol(symbol) {
                    deserializeBody(it::body, it.bodyLength)
                }
            }
        } ?: mutableListOf()

        val cfirStruct = CfirStructImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializeAnnotations(decl, symbol),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = EmptyDeprecationsProvider,
            status = status,
            typeParameters = typeParams,
            symbol = symbol,
            superTypeRefs = superTypeRefs,
            declarations = members,
            name = name,
            scopeProvider = context.moduleData.session.cangjieScopeProvider,
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
        val isNonExhaustive = info?.nonExhaustive == true
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = info?.let {
            withClassLikeOwner(ClassLikeOwnerContext(classId, typeParams)) {
                withEnumOwner(EnumOwnerContext(classId, typeParams, isRefEnum)) {
                    withContainingDeclarationSymbol(symbol) {
                        deserializeBody(it::body, it.bodyLength)
                    }
                }
            }
        } ?: mutableListOf()

        val cfirEnum = CfirEnumImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializeAnnotations(decl, symbol),
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
            isNonExhaustive = isNonExhaustive,
            scopeProvider = context.moduleData.session.cangjieScopeProvider,
        )
        symbol.bind(cfirEnum)
        cfirEnum.markResolved()
        return cfirEnum
    }

    /** FuncDecl → CfirFunction */
    private fun convertFunctionOrEnumConstructor(decl: Decl): CfirDeclaration {
        convertConstructorIfNeeded(decl)?.let { return it }
        if (isEnumConstructorDecl(decl)) {
            return convertEnumConstructorFromFunctionDecl(decl)
        }

        val status = buildStatus(decl)
        val name = normalizedLibraryFunctionName(decl, status)
        val symbol = CfirNamedFunctionSymbol(callableIdForCurrentOwner(name))
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val returnTypeRef = buildFunctionReturnTypeRef(decl)

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
            annotations = deserializeAnnotations(decl, symbol),
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

    /**
     * `.cjo` 中 operator 函数的 identifier 仍是源代码文本（如 `==`、`!=`、`[]`）。
     * PSI 与 LightTree raw CFIR 会先归一化到 `OperatorNameConventions`，库声明也必须恢复成同一名称，
     * 否则 std.core 中的真实 operator 成员无法被调用解析命中。
     */
    private fun normalizedLibraryFunctionName(decl: Decl, status: CfirDeclarationStatus): Name {
        val rawName = decl.identifier ?: "???"
        if (!status.isOperator) return Name.identifier(rawName)

        val valueParameterDecls = functionValueParameterDecls(decl)
        return when (rawName) {
            "-", OperatorNameConventions.MINUS.asString(), OperatorNameConventions.UNARY_MINUS.asString() ->
                if (valueParameterDecls.isEmpty()) OperatorNameConventions.UNARY_MINUS else OperatorNameConventions.MINUS

            "+", OperatorNameConventions.PLUS.asString(), OperatorNameConventions.UNARY_PLUS.asString() ->
                if (valueParameterDecls.isEmpty()) OperatorNameConventions.UNARY_PLUS else OperatorNameConventions.PLUS

            "[]", OperatorNameConventions.GET.asString(), OperatorNameConventions.SET.asString() ->
                if (valueParameterDecls.lastOrNull().isNamedValueParameter()) OperatorNameConventions.SET else OperatorNameConventions.GET

            else -> rawName.asOperatorName()
        }
    }

    /** 读取函数声明所有参数列表中的原始参数 Decl。 */
    private fun functionValueParameterDecls(decl: Decl): List<Decl> {
        val funcInfo = decl.info(FuncInfo()) as? FuncInfo ?: return emptyList()
        val funcBody = funcInfo.funcBody ?: return emptyList()
        val parameterDecls = mutableListOf<Decl>()

        for (i in 0 until funcBody.paramListsLength) {
            val paramList = funcBody.paramLists(i) ?: continue
            for (j in 0 until paramList.paramsLength) {
                val paramIndex = decodeDeclRef(paramList.params(j)) ?: continue
                val paramDecl = try {
                    context.pkg.allDecls(paramIndex)
                } catch (_: IndexOutOfBoundsException) {
                    null
                } ?: continue
                parameterDecls += paramDecl
            }
        }

        return parameterDecls
    }

    /** 判断参数 Decl 是否为命名参数 `value!`，用于区分 `[]` 的 get/set operator。 */
    private fun Decl?.isNamedValueParameter(): Boolean {
        val param = this ?: return false
        val info = param.info(ParamInfo()) as? ParamInfo ?: return false
        return info.isNamedParam && param.identifier == "value"
    }

    /** 根据当前 owner 上下文构造 callable id，成员 callable 使用 classId，顶层 callable 使用包名。 */
    private fun callableIdForCurrentOwner(name: Name): CallableId {
        val containingClass = currentContainingDeclarationSymbol as? CfirClassLikeSymbol<*>
        return containingClass?.let { CallableId(it.classId, name) } ?: CallableId(packageFqName, name)
    }

    /**
     * common-part `.cjo` 会把普通构造器序列化成 `FuncDecl`，
     * 但语义身份仍由 `Attribute.CONSTRUCTOR` / `Attribute.PRIMARY_CONSTRUCTOR` 标注。
     * 这里必须把 primary/secondary 形态一并恢复，后续作用域、检查器、analysis API 才能看到真实 constructor shape。
     */
    private fun convertConstructorIfNeeded(decl: Decl): CfirConstructor? {
        if (!testAttr(decl, AttrBit.CONSTRUCTOR)) return null
        val containingClass = currentContainingDeclarationSymbol as? CfirClassLikeSymbol<*> ?: return null
        val isPrimary = testAttr(decl, AttrBit.PRIMARY_CONSTRUCTOR)

        val symbol = CfirConstructorSymbol(CallableId(containingClass.classId, SpecialNames.INIT))
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val valueParams = withContainingDeclarationSymbol(symbol) {
            deserializeFunctionParameters(decl)
        }
        val returnTypeRef = buildClassConstructorReturnTypeRef(currentClassLikeOwner)

        val constructor = if (isPrimary) {
            buildPrimaryConstructor {
                source = null
                moduleData = context.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                annotations.addAll(deserializeAnnotations(decl, symbol))
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                deprecationsProvider = EmptyDeprecationsProvider
                dispatchReceiverType = null
                this.status = status
                this.typeParameters.addAll(typeParams)
                this.returnTypeRef = returnTypeRef
                this.valueParameters.addAll(valueParams)
                body = null
                this.symbol = symbol
            }
        } else {
            buildConstructor {
                source = null
                moduleData = context.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                annotations.addAll(deserializeAnnotations(decl, symbol))
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                deprecationsProvider = EmptyDeprecationsProvider
                dispatchReceiverType = null
                this.status = status
                this.typeParameters.addAll(typeParams)
                this.returnTypeRef = returnTypeRef
                this.valueParameters.addAll(valueParams)
                body = null
                this.symbol = symbol
            }
        }
        symbol.bind(constructor)
        constructor.markResolved()
        return constructor
    }

    /** PropDecl → CfirProperty */
    private fun convertProperty(decl: Decl): CfirProperty {
        val name = Name.identifier(decl.identifier ?: "???")
        val symbol = CfirPropertySymbol(callableIdForCurrentOwner(name))
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val returnTypeRef = buildTypeRef(decl.type)

        val cfirProp = CfirPropertyImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializeAnnotations(decl, symbol),
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
        val symbol = CfirFieldVariableSymbol(callableIdForCurrentOwner(name))
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
            annotations = deserializeAnnotations(decl, symbol),
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
            annotations = deserializeAnnotations(decl, symbol),
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

    /**
     * 反序列化不可反驳模式。
     *
     * 支持变量、tuple、类型、enum 与 wildcard 模式；缺失或未知模式使用绑定模式恢复声明骨架。
     */
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

    /**
     * 反序列化模式绑定变量。
     *
     * 如果模式引用了独立 binding Decl，则优先使用该 Decl 的名称、状态和类型；否则继承外层变量声明信息。
     */
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
            annotations = bindingDecl
                ?.let { deserializeAnnotations(it, symbol) }
                ?: MutableOrEmptyList.empty(),
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

    /**
     * 深复制声明状态。
     *
     * 模式绑定变量需要继承外层变量状态，但每个绑定变量必须拥有独立的可变 status 实例。
     */
    private fun cloneStatus(status: CfirDeclarationStatus): CfirDeclarationStatusImpl {
        return CfirDeclarationStatusImpl(
            visibility = status.visibility,
            modality = status.modality,
        ).also { copied ->
            copied.isVisibilityExplicit = status.isVisibilityExplicit
            copied.isModalityExplicit = status.isModalityExplicit
            copied.isAbstractExplicit = status.isAbstractExplicit
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
            copied.isDefault = status.isDefault
            copied.isAbstract = status.isAbstract
            copied.isOpen = status.isOpen
            copied.isSealed = status.isSealed
        }
    }

    /** 从 `allExprs` 中的 reference expression 读取原始引用文本。 */
    private fun extractReferenceName(exprIndex: Int): String? {
        val expr = context.pkg.allExprs(exprIndex) ?: return null
        if (expr.infoType != ExprInfo.ReferenceInfo) return null
        val referenceInfo = expr.info(ReferenceInfo()) as? ReferenceInfo ?: return null
        return referenceInfo.reference
    }

    /**
     * 将 ExtendDecl 反序列化为 [CfirExtend]。
     *
     * extend 的目标类型来自 `Decl.type`，继承类型和成员声明来自 [ExtendInfo]。
     */
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
            annotations = deserializeAnnotations(decl, symbol),
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
            annotations = deserializeAnnotations(decl, symbol),
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
            scopeProvider = context.moduleData.session.cangjieScopeProvider,
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

        val bounds = mutableListOf<CfirTypeRef>()

        val cfirParam = CfirTypeParameterImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializeAnnotations(decl, symbol),
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

    /** 将 VarDecl 形式的 enum constructor 反序列化为 [CfirEnumConstructor]。 */
    private fun convertEnumConstructorFromVariableDecl(decl: Decl): CfirEnumConstructor {
        val name = Name.identifier(decl.identifier ?: "???")
        val owner = currentEnumOwner
        val symbol = CfirEnumConstructorSymbol(owner?.let { CallableId(it.classId, name) } ?: CallableId(packageFqName, name))
        val status = buildStatus(decl)
        val typeParams = if (owner != null) mutableListOf() else deserializeTypeParameters(decl)

        val enumCtor = CfirEnumConstructorImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializeAnnotations(decl, symbol),
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

    /** 将 FuncDecl 形式的 enum constructor 反序列化为 [CfirEnumConstructor]。 */
    private fun convertEnumConstructorFromFunctionDecl(decl: Decl): CfirEnumConstructor {
        val name = Name.identifier(decl.identifier ?: "???")
        val owner = currentEnumOwner
        val symbol = CfirEnumConstructorSymbol(owner?.let { CallableId(it.classId, name) } ?: CallableId(packageFqName, name))
        val status = buildStatus(decl)
        val typeParams = if (owner != null) mutableListOf() else withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val valueParameters = withContainingDeclarationSymbol(symbol) {
            deserializeFunctionParameters(decl)
        }

        val enumCtor = CfirEnumConstructorImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializeAnnotations(decl, symbol),
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

    /** 反序列化函数、构造器或 enum constructor 的值参数列表。 */
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

    /** 构造 enum constructor 的返回类型引用，位于 enum owner 内时返回对应 enum 类型。 */
    private fun buildEnumConstructorReturnTypeRef(owner: EnumOwnerContext?): CfirTypeRef {
        owner ?: return buildImplicitTypeRef {
            customRenderer = false
        }

        val typeArguments = owner.typeParameters.map { parameter ->
            ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag())
        }
        return buildResolvedTypeRef {
            customRenderer = false
            coneType = ConeEnumType(
                lookupTag = owner.classId.toLookupTag(),
                typeArguments = typeArguments,
                isRefEnum = owner.isRefEnum,
            )
        }
    }

    /** 构造 class-like 构造器返回类型引用，位于 owner 内时返回当前 class-like 类型。 */
    private fun buildClassConstructorReturnTypeRef(owner: ClassLikeOwnerContext?): CfirTypeRef {
        owner ?: return buildImplicitTypeRef {
            customRenderer = false
        }

        val typeArguments = owner.typeParameters.map { parameter ->
            ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag())
        }
        return buildResolvedTypeRef {
            customRenderer = false
            coneType = ConeClassLikeType(
                lookupTag = owner.classId.toLookupTag(),
                typeArguments = typeArguments,
            )
        }
    }

    /** 根据当前包名和声明名解析 class-like 声明的 [ClassId]。 */
    private fun resolveClassId(decl: Decl, fallbackName: Name): ClassId {
        val name = decl.classLikeName(fallbackName)
        return ClassId(packageFqName, name)
    }

    /** 提取 class-like 声明名，缺失时使用稳定 fallback。 */
    private fun Decl.classLikeName(fallback: Name = Name.identifier("___missing_class_name___")): Name {
        val rawName = identifier?.takeIf { it.isNotBlank() }
        return rawName?.let(Name::identifier) ?: fallback
    }

    /** 将 FuncParam 声明反序列化为 [CfirValueParameter]。 */
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
        val serializedDefaultValue = info?.defaultVal?.let(::decodeExprRef)
        val defaultValue = if (testAttr(decl, AttrBit.HAS_INITIAL) || serializedDefaultValue != null) {
            buildLibraryDefaultValueMarker(returnTypeRef)
        } else {
            null
        }
        val cfirParam = CfirValueParameterImpl(
            source = null,
            isNamed = isNamed,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializeAnnotations(decl, symbol),
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
            defaultValue = defaultValue,

        )
        symbol.bind(cfirParam)
        cfirParam.markResolved()
        return cfirParam
    }

    /**
     * `.cjo` 的 `HAS_INITIAL` 属性是参数具有默认值的权威标记；`ParamInfo.defaultVal`
     * 只保存工具生成或常量默认表达式，普通库默认表达式可能没有可加载的表达式索引。
     *
     * 官方 ASTLoader 会把 defaultVal 反序列化回 FuncParam.assignment；当前 CFIR 反序列化层尚未实现
     * flatbuffer 表达式到 CFIR 表达式的完整转换，因此这里构造一个带参数类型的占位表达式，保留调用匹配语义。
     */
    private fun buildLibraryDefaultValueMarker(typeRef: CfirResolvedTypeRef): CfirExpression = buildLiteralExpression {
        coneTypeOrNull = typeRef.coneType
        kind = CfirLiteralKind.UNIT
        value = null
    }
}
