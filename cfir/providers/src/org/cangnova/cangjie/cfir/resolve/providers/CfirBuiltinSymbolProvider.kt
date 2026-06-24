package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.common.nullableModuleData
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
import org.cangnova.cangjie.cfir.declarations.EmptyDeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.initDefaultResolveState
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperators
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.isExposedBuiltinClassifier
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 将 builtin primitive 类型暴露为合成 class-like 声明。
 *
 * 这样 provider、scope 与 resolver 可以沿用普通 classifier 的统一架构，
 * 不需要在类型系统外层为 primitive 类型另开查询通道；整体设计对齐 Kotlin FIR builtin provider。
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

    /**
     * primitive ClassId 到合成声明的缓存。
     */
    private val primitiveDeclarationsByClassId: Map<ClassId, CfirPrimitiveTypeDeclaration> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        PrimitiveTypeKind.entries.associateBy(
            keySelector = { it.classId },
            valueTransform = ::buildPrimitiveDeclaration,
        )
    }

    /**
     * builtin provider 的名称索引，只暴露基础包下的 primitive classifier。
     */
    override val symbolNamesProvider: CfirSymbolNamesProvider = BuiltinNamesProvider

    /**
     * 返回 primitive 类型对应的合成 class-like symbol。
     */
    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
        primitiveDeclarationsByClassId[classId]?.symbol

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

    /**
     * builtin primitive 声明只位于基础包。
     */
    override fun hasPackage(fqName: FqName): Boolean =
        fqName == StandardNames.BASIC_PACKAGE_FQ_NAME

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
         * builtin 声明只存在于基础包。
         */
        override fun getPackageNames(): Set<String> =
            setOf(StandardNames.BASIC_PACKAGE_FQ_NAME.asString())

        /**
         * classifier 包集合直接复用包名集合。
         */
        override val hasSpecificClassifierPackageNamesComputation: Boolean
            get() = false

        /**
         * 返回基础包中的 primitive classifier 名称。
         */
        override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
            if (packageFqName == StandardNames.BASIC_PACKAGE_FQ_NAME) builtinClassifierNames else emptySet()

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
