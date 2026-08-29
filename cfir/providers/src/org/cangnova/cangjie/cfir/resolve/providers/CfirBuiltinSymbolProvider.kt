package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.common.nullableModuleData
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirBuiltInDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirBuiltInTypeKind
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
import org.cangnova.cangjie.cfir.declarations.EmptyDeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.initDefaultResolveState
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.symbols.CfirBuiltInTypeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperators
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.isExposedBuiltinClassifier
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 将 builtin 类型暴露为合成 class-like 声明。
 *
 * primitive 没有普通声明，而 `RawArray` / `VArray` / `CPointer` / `CString` / `CFunc`
 * 在官方编译器中是注入 `std.core` 的声明。这里为两组类型分别保留其语义身份，
 * 让 provider、scope 与 resolver 沿用统一的 class-like 查询架构；真实 CJO 声明由
 * deserialized provider 优先提供，本 provider 只负责在其缺失时合成对应声明。
 */
class CfirBuiltinSymbolProvider(
    session: CfirSession,
) : CfirSymbolProvider(session) {

    /**
     * builtin 声明使用的 module data。
     *
     * 若 session 已经绑定 module data，则沿用当前 module；否则创建二进制依赖 module data 作为合成 builtin 宿主。
     */
    private val builtinModuleData by lazy(LazyThreadSafetyMode.PUBLICATION) {
        session.nullableModuleData
            ?: CfirBinaryDependenciesModuleData(Name.identifier("<builtins>")).also { it.bindSession(session) }
    }

    /** primitive ClassId 到合成声明的缓存。 */
    private val primitiveDeclarationsByClassId: Map<ClassId, CfirPrimitiveTypeDeclaration> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        PrimitiveTypeKind.entries.associateBy(
            keySelector = { it.classId },
            valueTransform = ::buildPrimitiveDeclaration,
        )
    }

    /** 官方 BuiltInDecl ClassId 到合成声明的缓存。 */
    private val builtInDeclarationsByClassId: Map<ClassId, CfirBuiltInDeclaration> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CfirBuiltInTypeKind.entries.associate { kind ->
            kind.classId to buildBuiltInDeclaration(kind)
        }
    }

    /**
     * builtin provider 的名称索引，同时暴露基础包 primitive 与 `std.core` BuiltInDecl。
     */
    override val symbolNamesProvider: CfirSymbolNamesProvider = BuiltinNamesProvider

    /**
     * 返回 primitive 或官方 BuiltInDecl 对应的合成 class-like symbol。
     */
    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
        builtInDeclarationsByClassId[classId]?.symbol
            ?: primitiveDeclarationsByClassId[classId]?.symbol

    /**
     * builtin provider 不提供顶层 callable symbol。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    /**
     * builtin provider 不提供顶层函数 symbol。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    /**
     * builtin provider 不提供顶层属性 symbol。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    /** builtin 声明只位于基础包或 `std.core`。 */
    override fun hasPackage(fqName: FqName): Boolean =
        fqName == StandardNames.BASIC_PACKAGE_FQ_NAME || fqName == StandardNames.STD_CORE_PACKAGE_FQ_NAME

    /**
     * 构造一个官方 `BuiltInDecl` 的合成 class-like 声明。
     *
     * 这里只承载声明身份、真实类型参数和 `CPointer` 的上界；官方成员和父类型
     * 不属于 BuiltInDecl 本身，必须留给后续专门的语义类型接线处理。
     */
    private fun buildBuiltInDeclaration(kind: CfirBuiltInTypeKind): CfirBuiltInDeclaration {
        val symbol = CfirBuiltInTypeSymbol(kind.classId, kind)
        return CfirBuiltInDeclaration(
            moduleData = builtinModuleData,
            symbol = symbol,
            name = kind.classId.shortClassName,
            kind = kind,
            scopeProvider = session.cangjieScopeProvider,
            typeParameters = buildBuiltInTypeParameters(kind, symbol),
        )
    }

    /**
     * 按官方声明构造 BuiltInDecl 的类型参数。
     *
     * `VArray` 的长度参数属于类型头部，不是声明类型参数；因此五项中只有四项
     * 携带一个名为 `T` 的真实类型参数，`CString` 不携带类型参数。
     */
    private fun buildBuiltInTypeParameters(
        kind: CfirBuiltInTypeKind,
        containingDeclarationSymbol: CfirBuiltInTypeSymbol,
    ): MutableList<CfirTypeParameterRef> = when (kind) {
        CfirBuiltInTypeKind.ARRAY,
        CfirBuiltInTypeKind.VARRAY,
        CfirBuiltInTypeKind.CPOINTER,
        CfirBuiltInTypeKind.CFUNC,
        -> mutableListOf<CfirTypeParameterRef>(
            buildBuiltInTypeParameter(
                containingDeclarationSymbol = containingDeclarationSymbol,
                hasCTypeBound = kind == CfirBuiltInTypeKind.CPOINTER,
            )
        )

        CfirBuiltInTypeKind.CSTRING -> mutableListOf()
    }

    /** 构造单个官方 BuiltInDecl 类型参数及其可选上界。 */
    private fun buildBuiltInTypeParameter(
        containingDeclarationSymbol: CfirBuiltInTypeSymbol,
        hasCTypeBound: Boolean,
    ) = buildTypeParameter {
        moduleData = builtinModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
        this.containingDeclarationSymbol = containingDeclarationSymbol
        symbol = CfirTypeParameterSymbol()
        name = Name.identifier("T")
        if (hasCTypeBound) {
            bounds += ConeClassLikeType(
                lookupTag = StdlibClassIds.CType.toLookupTag(),
                isInterface = true,
            ).toCfirResolvedTypeRef()
        }
    }

    /**
     * 构造一个 primitive 类型的合成 class-like 声明。
     */
    private fun buildPrimitiveDeclaration(kind: PrimitiveTypeKind): CfirPrimitiveTypeDeclaration {
        val symbol = CfirPrimitiveTypeSymbol(kind.classId, kind)
        val declaration = CfirPrimitiveTypeDeclaration(
            moduleData = builtinModuleData,
            symbol = symbol,
            name = kind.classId.shortClassName,
            kind = kind,
            scopeProvider = session.cangjieScopeProvider,
            origin = CfirDeclarationOrigin.Synthetic.Default,
            attributes = CfirDeclarationAttributes.EMPTY,
            declarations = buildPrimitiveMembers(kind).toMutableList(),
            superTypeRefs = mutableListOf(),
        )
        declaration.initDefaultResolveState()

        return declaration
    }

    /**
     * 为 primitive 类型构造合成 operator 成员。
     *
     * 这些成员用于 scope 与调用解析统一看见 primitive 运算能力。
     */
    private fun buildPrimitiveMembers(kind: PrimitiveTypeKind): List<CfirDeclaration> =
        BuiltinPrimitiveOperators.signaturesFor(kind).map { signature ->
            val functionSymbol = CfirNamedFunctionSymbol(CallableId(kind.classId, signature.name))
            val parameters = signature.parameterKinds.mapIndexed { index, parameterKind ->
                val parameterSymbol = CfirValueParameterSymbol(CallableId(signature.name))
                buildValueParameter {
                    moduleData = builtinModuleData
                    resolvePhase = CfirResolvePhase.BODY_RESOLVE
                    origin = CfirDeclarationOrigin.Synthetic.FakeFunction
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = false
                    deprecationsProvider = EmptyDeprecationsProvider
                    dispatchReceiverType = null
                    symbol = parameterSymbol
                    containingDeclarationSymbol = functionSymbol
                    isNamed = false
                    status = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
                    returnTypeRef = buildResolvedTypeRef {
                        coneType = ConePrimitiveType(parameterKind)
                    }
                    name = Name.identifier("p$index")
                }
            }

            val status = CfirDeclarationStatusImpl().apply {
                isOperator = true
            }
            buildNamedFunction {
                moduleData = builtinModuleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.FakeFunction
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                dispatchReceiverType = null
                this.status = status
                returnTypeRef = buildResolvedTypeRef {
                    coneType = ConePrimitiveType(signature.returnKind)
                }
                valueParameters += parameters
                symbol = functionSymbol
                name = signature.name
                isMut = false
            }
        }

    /**
     * builtin provider 的名称索引实现。
     */
    private object BuiltinNamesProvider : CfirSymbolNamesProvider() {
        /**
         * 对外暴露的 primitive classifier 短名集合。
         */
        private val builtinClassifierNames: Set<Name> = PrimitiveTypeKind.entries
            .filter(PrimitiveTypeKind::isExposedBuiltinClassifier)
            .mapTo(linkedSetOf()) { Name.identifier(it.typeName) }

        /**
         * builtin 声明存在于基础包和 `std.core`。
         */
        override fun getPackageNames(): Set<String> =
            setOf(
                StandardNames.BASIC_PACKAGE_FQ_NAME.asString(),
                StandardNames.STD_CORE_PACKAGE_FQ_NAME.asString(),
            )

        /**
         * classifier 包集合直接复用包名集合。
         */
        override val hasSpecificClassifierPackageNamesComputation: Boolean
            get() = false

        /**
         * 返回基础包或 `std.core` 中的 builtin classifier 名称。
         */
        override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
            when (packageFqName) {
                StandardNames.BASIC_PACKAGE_FQ_NAME -> builtinClassifierNames
                StandardNames.STD_CORE_PACKAGE_FQ_NAME -> builtInClassifierNames
                else -> emptySet()
            }

        /** `std.core` 中官方 BuiltInDecl 的短名称集合。 */
        private val builtInClassifierNames: Set<Name> = CfirBuiltInTypeKind.entries
            .mapTo(linkedSetOf()) { it.classId.shortClassName }

        /**
         * builtin provider 不需要专门 callable 包计算。
         */
        override val hasSpecificCallablePackageNamesComputation: Boolean
            get() = false

        /**
         * builtin provider 不提供顶层 callable 名称。
         */
        override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? = emptySet()
    }
}
