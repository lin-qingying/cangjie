package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.resolve.services.isClassIdReachableByImports
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance
import org.cangnova.cangjie.cfir.scopes.CfirCallableWithLookupProvenance
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.processCallablesByNameWithLookupProvenance
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.extendExportSurfaceService
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * 所有可见性判断的显式使用点。
 *
 * 解析器不能再通过线程局部状态猜测当前文件。没有源码文件的合成使用点也必须显式
 * 创建本对象；此时非 public 声明不具备可见性依据，因此不会被宽松地暴露出来。
 */
data class CfirAccessContext(
    /** 当前访问所属文件；合成访问可为 null。 */
    val useSiteFile: CfirFile?,
    /** 由外到内排列的声明链，用于 private/protected 成员规则。 */
    val containingDeclarations: List<CfirDeclaration> = emptyList(),
    /**
     * 显式成员访问的接收者类型。
     *
     * 当前 protected 规则的 receiver 限制尚未由官方语义要求，但把它作为 use-site
     * 身份的一部分传递，避免今后把 receiver-sensitive 规则偷放回 member scope cache。
     */
    val receiverType: ConeCangJieType? = null,
    /**
     * 静态/类型限定符对应的 class-like 符号。
     *
     * 和 [receiverType] 一样，这是 use-site 事实，不属于 declaration scope。
     */
    val qualifierSymbol: CfirClassLikeSymbol<*>? = null,
    /** 候选到达统一 checker 的结构性查找来源。 */
    val lookupOrigin: CfirLookupOrigin = CfirLookupOrigin.LEXICAL,
    /** 当前入口消费声明的方式。 */
    val kind: CfirAccessKind,
)

/**
 * 候选的结构性发现来源。
 *
 * 该信息只描述名字如何到达声明，不携带可见性结论；导入、包和成员 scope 因此仍可安全缓存。
 */
enum class CfirLookupOrigin {
    /** 当前文件、局部声明或其它词法 scope。 */
    LEXICAL,
    /** 当前文件所属包的 package scope。 */
    PACKAGE,
    /** 源码显式 import binding。 */
    EXPLICIT_IMPORT,
    /** 语言默认 import binding。 */
    DEFAULT_IMPORT,
    /** 显式包限定名解析。 */
    QUALIFIED_PACKAGE,
    /** class/static/extend member scope。 */
    MEMBER,
}

/** 使用点的语义入口，而不是 declaration kind 的临时替代。 */
enum class CfirAccessKind {
    /** 类型/分类器查找。 */
    TYPE,
    /** 函数、构造器和操作符调用。 */
    CALLABLE,
    /** 属性、变量和函数值的命名值访问。 */
    NAMED_VALUE,
    /** extend 导出面查询。 */
    EXTEND,
}

/** 不可访问声明进入各解析入口后的统一处置。 */
enum class CfirLookupDisposition {
    /** 不参与名字/成员发现，保留普通 unresolved 或 not-member 路径。 */
    NOT_DISCOVERABLE,
    /** 不参与函数重载集，保留调用层的 no-match 结果。 */
    EXCLUDE_CALLABLE,
    /** 保留解析目标，由诊断层报告访问控制错误。 */
    REPORT_ACCESS_ERROR,
}

/** 共享 checker 的结构化结果。 */
sealed interface CfirAccessibilityResult {
    data object Accessible : CfirAccessibilityResult

    data class Inaccessible(
        /** 应作为诊断 owner 的真实声明符号。 */
        val reportingOwner: CfirBasedSymbol<*>,
        /** 当前入口如何消费不可访问结果。 */
        val disposition: CfirLookupDisposition,
    ) : CfirAccessibilityResult
}

/**
 * CFIR 唯一的语言级可见性 owner。
 *
 * 这里集中官方 `IsLegalAccess` 所需的文件、包、外围声明、继承和 extend owner
 * 关系；scope、tower 与类型解析器只把各自已经拥有的 use-site 信息传入本服务。
 */
class CfirAccessibilityChecker(
    private val session: CfirSession,
) : CfirSessionComponent {
    /** 判断分类器能否参与当前类型/限定符查找。 */
    fun checkClassLike(
        symbol: CfirClassLikeSymbol<*>,
        context: CfirAccessContext,
    ): CfirAccessibilityResult {
        val declaration = symbol.cfir as? CfirMemberDeclaration ?: return CfirAccessibilityResult.Accessible
        return checkMember(
            candidateSymbol = symbol,
            declarationSymbol = symbol,
            declaration = declaration,
            context = context,
        )
    }

    /** 判断 callable 能否参与当前调用或命名值访问。 */
    fun checkCallable(
        symbol: CfirCallableSymbol<*>,
        context: CfirAccessContext,
        provenance: CfirCallableLookupProvenance = CfirCallableLookupProvenance.None,
    ): CfirAccessibilityResult {
        val declarationSymbol = symbol.unwrapCallableForDeclarationMetadataLookup()
        val declaration = declarationSymbol.cfir as? CfirMemberDeclaration
            ?: return CfirAccessibilityResult.Accessible
        return checkMember(
            candidateSymbol = symbol,
            declarationSymbol = declarationSymbol,
            declaration = declaration,
            context = context,
            provenance = provenance,
        )
    }

    /**
     * 从结构 scope 的完整来源集合中处理当前使用点真正可访问的 callable。
     *
     * 结构 scope 可以安全缓存 declaration、extend 与父类型输入，但不能在缺少文件和
     * 外围声明链时提前归并访问敏感结果。override、继承和 shadow 等声明级消费者必须
     * 通过本入口先按完整 provenance 过滤，再执行各自的签名合并。
     */
    fun processAccessibleCallablesByName(
        scope: CfirScope,
        name: Name,
        context: CfirAccessContext,
        processor: (CfirCallableWithLookupProvenance) -> Unit,
    ) {
        scope.processCallablesByNameWithLookupProvenance(name) { candidate ->
            if (checkCallable(candidate.symbol, context, candidate.provenance) is CfirAccessibilityResult.Accessible) {
                processor(candidate)
            }
        }
    }

    /**
     * 判断 extend 是否对指定使用点导出。
     *
     * provider 只给出 owner、包和目标索引；这里根据显式 context 判断 target、
     * 继承接口与上界的导出面。反序列化 provider 因此不会错误地拿 library
     * session 当作 consumer use-site。
     *
     * extend 节点上的通用 declaration status 不是独立的语言级访问门槛。官方
     * `ExtendDecl::IsExportedDecl` 与 `ImportManager::IsExtendAccessible` 只按目标类型、
     * 接口和泛型上界计算导出；成员自身的 private/internal/protected 则随后由
     * [checkDeclarationVisibility] 处理。两层规则不能混为一次 declaration visibility 检查。
     */
    fun checkExtend(
        extend: CfirExtend,
        context: CfirAccessContext,
    ): CfirAccessibilityResult {
        val extendPackage = extend.getDeclarationPackage()
            ?: return notDiscoverable(extend.symbol)
        val useSiteFile = context.useSiteFile ?: return notDiscoverable(extend.symbol)
        val useSitePackage = useSiteFile.packageDirective.packageFqName

        if (useSitePackage == extendPackage) return CfirAccessibilityResult.Accessible

        val view = extend.accessViewOrNull(extendPackage) ?: return inaccessibleExtend(extend)
        if (!isExtendExported(extend)) return inaccessibleExtend(extend)
        if (!extend.allUpperBoundsAccessible(context)) return inaccessibleExtend(extend)

        val targetClassId = view.targetClassId
        val targetSharesExtendPackage = targetClassId?.packageFqName == extendPackage ||
            targetClassId == null && extendPackage.asString() == STDLIB_CORE_PACKAGE

        val accessible = if (targetSharesExtendPackage) {
            targetClassId == null || isClassIdVisibleFromPackage(targetClassId, context)
        } else {
            view.inheritedInterfaceClassIds.any { isClassIdReachableAndVisible(it, context) } &&
                (targetClassId == null || isClassIdReachableAndVisible(targetClassId, context))
        }
        return if (accessible) CfirAccessibilityResult.Accessible else inaccessibleExtend(extend)
    }

    /**
     * 判断 extend 是否形成语言级导出声明面。
     *
     * 该结果只依赖声明目标、接口与泛型上界，不依赖某个 consumer 文件；继承检查据此
     * 判断 exported extend 是否把真实实现来源间接暴露出去。
     */
    fun isExtendExported(extend: CfirExtend): Boolean {
        val extendPackage = extend.getDeclarationPackage() ?: return false
        val view = extend.accessViewOrNull(extendPackage) ?: return false
        return isExported(view, extendPackage) && extend.allUpperBoundsExported()
    }

    /** 判断 class-like 声明能否作为 exported requirement owner。 */
    fun isClassLikeExported(symbol: CfirClassLikeSymbol<*>): Boolean =
        isClassIdExported(symbol.classId)

    private fun checkMember(
        candidateSymbol: CfirBasedSymbol<*>,
        declarationSymbol: CfirBasedSymbol<*>,
        declaration: CfirMemberDeclaration,
        context: CfirAccessContext,
        provenance: CfirCallableLookupProvenance = CfirCallableLookupProvenance.None,
    ): CfirAccessibilityResult {
        /*
         * local 声明的可达性已经由词法 scope 完整决定。它们没有文件/包级 visibility
         * owner，不能再按默认 internal/private 状态进入跨文件访问控制。
         */
        if (declaration is CfirCallableDeclaration && declaration.isLocal) {
            return CfirAccessibilityResult.Accessible
        }

        val ownerExtend = (declarationSymbol as? CfirCallableSymbol<*>)
            ?.getContainingExtend()
            ?: provenance.sourceExtend
        if (ownerExtend != null) {
            if (checkExtend(ownerExtend, context) !is CfirAccessibilityResult.Accessible) {
                return notDiscoverable(declarationSymbol)
            }
            val callable = candidateSymbol as? CfirCallableSymbol<*>
            val extendPackage = ownerExtend.getDeclarationPackage()
            val useSitePackage = context.useSiteFile?.packageDirective?.packageFqName
            if (callable != null && extendPackage != null && useSitePackage != extendPackage) {
                when (val exportSurface = session.extendExportSurfaceService.classifyMember(
                    extend = ownerExtend,
                    callable = callable,
                    provenance = provenance,
                    receiverType = context.receiverType,
                )) {
                    CfirExtendMemberExportSurface.DirectlyAvailable -> Unit
                    CfirExtendMemberExportSurface.NotExported -> return notDiscoverable(declarationSymbol)
                    is CfirExtendMemberExportSurface.InterfaceRequirements -> {
                        val hasAccessibleRequirement = exportSurface.requirements.any { requirement ->
                            isClassIdReachableAndVisible(requirement.interfaceClassId, context)
                        }
                        if (!hasAccessibleRequirement) {
                            return CfirAccessibilityResult.Inaccessible(
                                reportingOwner = declarationSymbol,
                                disposition = when (context.kind) {
                                    CfirAccessKind.CALLABLE -> CfirLookupDisposition.EXCLUDE_CALLABLE
                                    CfirAccessKind.NAMED_VALUE -> CfirLookupDisposition.REPORT_ACCESS_ERROR
                                    CfirAccessKind.TYPE,
                                    CfirAccessKind.EXTEND,
                                    -> CfirLookupDisposition.NOT_DISCOVERABLE
                                },
                            )
                        }
                    }
                }
            }
        }

        /*
         * 成员的 public 修饰符不能穿透 private/internal/protected 的外围类。
         * 所有 container 逐层走同一判断，reporting owner 保留第一个实际不可访问
         * 的外围声明，调用方不会再把根因误归到 public member 本身。
         */
        var containingClass = declarationSymbol.getContainingClass()
        while (containingClass != null) {
            val containingDeclaration = containingClass.cfir as? CfirMemberDeclaration
            if (containingDeclaration != null) {
                val containingResult = checkDeclarationVisibility(
                    symbol = containingClass,
                    declaration = containingDeclaration,
                    context = context,
                )
                if (containingResult != null) return containingResult
            }
            containingClass = containingClass.getContainingClass()
        }

        return checkDeclarationVisibility(declarationSymbol, declaration, context)
            ?: CfirAccessibilityResult.Accessible
    }

    /** `null` 表示 [declaration] 对 [context] 可访问。 */
    private fun checkDeclarationVisibility(
        symbol: CfirBasedSymbol<*>,
        declaration: CfirMemberDeclaration,
        context: CfirAccessContext,
    ): CfirAccessibilityResult.Inaccessible? {
        val declarationFile = symbol.getContainingFile()
        val useSiteFile = context.useSiteFile
        val declarationPackage = symbol.getDeclarationPackage()
        val useSitePackage = useSiteFile?.packageDirective?.packageFqName
        val accessible = when (declaration.status.visibility) {
            Visibilities.Public -> true
            Visibilities.Internal -> packageAccessible(
                useSitePackage,
                declarationPackage,
                ::canAccessPackageInternalDeclaration,
            )

            Visibilities.Private -> privateAccessible(symbol, useSiteFile, declarationFile, context)
            Visibilities.Protected -> protectedAccessible(symbol, useSitePackage, declarationPackage, context)
            else -> false
        }
        if (accessible) return null
        return CfirAccessibilityResult.Inaccessible(symbol, dispositionFor(symbol, declaration, context))
    }

    private fun notDiscoverable(symbol: CfirBasedSymbol<*>): CfirAccessibilityResult.Inaccessible =
        CfirAccessibilityResult.Inaccessible(symbol, CfirLookupDisposition.NOT_DISCOVERABLE)

    private fun inaccessibleExtend(extend: CfirExtend): CfirAccessibilityResult.Inaccessible =
        CfirAccessibilityResult.Inaccessible(extend.symbol, CfirLookupDisposition.NOT_DISCOVERABLE)

    /** extend 可访问性消费的、与 declaration storage 无关的归一化视图。 */
    private data class ExtendAccessView(
        val targetClassId: ClassId?,
        val inheritedInterfaceClassIds: List<ClassId>,
    )

    /**
     * extend view 本身只是结构数据；导出判断必须由 session checker 完成，不能把
     * checker 的状态捕获进 scope/provider 的缓存对象。
     */
    private fun isExported(
        view: ExtendAccessView,
        extendPackage: org.cangnova.cangjie.name.FqName,
    ): Boolean {
        val targetClassId = view.targetClassId
        val inheritedInterfaceClassIds = view.inheritedInterfaceClassIds
        val targetSharesExtendPackage = targetClassId?.packageFqName == extendPackage ||
            targetClassId == null && extendPackage.asString() == STDLIB_CORE_PACKAGE
        if (inheritedInterfaceClassIds.isEmpty()) {
            if (extendPackage.asString() == STDLIB_CORE_PACKAGE) return true
            return targetSharesExtendPackage && targetClassId?.let { classId ->
                isClassIdExported(classId)
            } == true
        }
        if (targetSharesExtendPackage) {
            return targetClassId == null || isClassIdExported(targetClassId)
        }
        return inheritedInterfaceClassIds.any { classId -> isClassIdExported(classId) }
    }

    private fun CfirExtend.accessViewOrNull(
        extendPackage: org.cangnova.cangjie.name.FqName,
    ): ExtendAccessView? {
        val targetClassId = extendedTypeRef.coneTypeOrNull?.classIdOrPrimitiveClassId
        val inheritedInterfaceClassIds = superTypeRefs.mapNotNull { superTypeRef ->
            val type = superTypeRef.coneTypeOrNull ?: return@mapNotNull null
            if (!isInterfaceTypeShape(type)) return@mapNotNull null
            type.classIdOrPrimitiveClassId
        }
        /*
         * 无 nominal target 的 primitive extend 仅 std.core 可以导出；其它情况缺少
         * 可验证的目标身份，必须拒绝而不是以“可能是 primitive”宽松暴露。
         */
        if (targetClassId == null && extendPackage.asString() != STDLIB_CORE_PACKAGE) return null
        return ExtendAccessView(targetClassId, inheritedInterfaceClassIds)
    }

    private fun CfirExtend.allUpperBoundsExported(): Boolean =
        typeParameters.all { parameter ->
            parameter.bounds.all { bound -> isTypeRefExported(bound) }
        }

    private fun CfirExtend.allUpperBoundsAccessible(context: CfirAccessContext): Boolean =
        typeParameters.all { parameter ->
            parameter.bounds.all { bound ->
                bound.coneTypeOrNull?.classIdOrPrimitiveClassId
                    ?.let { isClassIdReachableAndVisible(it, context) }
                    ?: true
            }
        }

    private fun isTypeRefExported(typeRef: CfirTypeRef): Boolean =
        typeRef.coneTypeOrNull?.classIdOrPrimitiveClassId
            ?.let { classId -> isClassIdExported(classId) }
            ?: true

    private fun isClassIdExported(classId: ClassId): Boolean {
        val declaration = session.symbolProvider.getClassLikeSymbolByClassId(classId)
            ?.cfir as? CfirMemberDeclaration
            ?: return false
        return declaration.status.visibility in EXPORTED_VISIBILITIES
    }

    private fun isClassIdVisibleFromPackage(classId: ClassId, context: CfirAccessContext): Boolean {
        val symbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return false
        return checkClassLike(symbol, context.copy(kind = CfirAccessKind.TYPE)) is CfirAccessibilityResult.Accessible
    }

    private fun isClassIdReachableAndVisible(classId: ClassId, context: CfirAccessContext): Boolean {
        val file = context.useSiteFile ?: return false
        if (!isClassIdVisibleFromPackage(classId, context)) return false
        return file.isClassIdReachableByImports(session, classId)
    }

    private fun isInterfaceTypeShape(type: ConeCangJieType): Boolean = when (type) {
        is ConeClassLikeType -> type.isInterface
        is ConeTypeAliasType -> type.expandedType?.let { expandedType ->
            isInterfaceTypeShape(expandedType)
        } == true
        else -> false
    }

    /**
     * 访问失败后的查找处置同样属于语言语义，不能由 tower stage 根据 call kind
     * 或某个 fixture 的期望再次推断。
     *
     * 官方在名字/成员发现阶段移除 private function；而 `internal`、`protected`
     * function 先形成调用目标再被调用候选过滤，最终得到 no-match。属性和值访问
     * 则保留目标，交由诊断层报告访问控制错误。
     */
    private fun dispositionFor(
        symbol: CfirBasedSymbol<*>,
        declaration: CfirMemberDeclaration,
        context: CfirAccessContext,
    ): CfirLookupDisposition = when (context.kind) {
        CfirAccessKind.TYPE,
        CfirAccessKind.EXTEND,
        -> CfirLookupDisposition.NOT_DISCOVERABLE

        CfirAccessKind.CALLABLE -> callableDisposition(symbol, declaration, context)

        CfirAccessKind.NAMED_VALUE -> when (declaration) {
            /*
             * 独立函数值/函数引用保留结构目标并报告访问控制错误；只有普通调用 discovery
             * 才把 internal/protected 函数排除成 no-match。private 顶层函数跨文件仍然没有
             * 可发现的声明目标，因此继续保持 NOT_DISCOVERABLE。
             */
            is CfirFunction -> when (callableDisposition(symbol, declaration, context)) {
                CfirLookupDisposition.NOT_DISCOVERABLE -> CfirLookupDisposition.NOT_DISCOVERABLE
                CfirLookupDisposition.EXCLUDE_CALLABLE,
                CfirLookupDisposition.REPORT_ACCESS_ERROR,
                -> CfirLookupDisposition.REPORT_ACCESS_ERROR
            }

            else -> CfirLookupDisposition.REPORT_ACCESS_ERROR
        }
    }

    /** 函数/构造器不可访问时在名字发现与 overload 排除之间选择统一处置。 */
    private fun callableDisposition(
        symbol: CfirBasedSymbol<*>,
        declaration: CfirMemberDeclaration,
        context: CfirAccessContext,
    ): CfirLookupDisposition {
        if (declaration.status.visibility != Visibilities.Private) {
            return CfirLookupDisposition.EXCLUDE_CALLABLE
        }

        /*
         * 普通 private 顶层函数只在声明文件的词法 scope 中存在，因此访问失败时不再可发现。
         * private 类成员则以包导出边界区分：同包源码成员仍在 receiver 的结构成员集中，
         * 由调用候选过滤后形成 no-match；跨包成员不进入导出成员面，保持 not-member。
         * extend 成员由目标类型的结构性 member scope 提供；跨包不可导出的 extend 成员已在
         * export-surface 检查中移除，能到达这里的同包成员继续走调用候选排除。
        */
        val callable = symbol as? CfirCallableSymbol<*>
        val declarationPackage = symbol.getDeclarationPackage()
        val isSamePackageClassMember = symbol.getContainingClass() != null &&
            declarationPackage != null &&
            declarationPackage == context.useSiteFile?.packageDirective?.packageFqName
        return if (callable?.getContainingExtend() != null || isSamePackageClassMember) {
            CfirLookupDisposition.EXCLUDE_CALLABLE
        } else {
            CfirLookupDisposition.NOT_DISCOVERABLE
        }
    }

    private fun packageAccessible(
        useSitePackage: org.cangnova.cangjie.name.FqName?,
        declarationPackage: org.cangnova.cangjie.name.FqName?,
        relation: (org.cangnova.cangjie.name.FqName, org.cangnova.cangjie.name.FqName) -> Boolean,
    ): Boolean {
        if (useSitePackage == null || declarationPackage == null) return false
        return relation(useSitePackage, declarationPackage)
    }

    private fun privateAccessible(
        symbol: CfirBasedSymbol<*>,
        useSiteFile: CfirFile?,
        declarationFile: CfirFile?,
        context: CfirAccessContext,
    ): Boolean {
        val callable = symbol as? CfirCallableSymbol<*>
        val ownerExtend = callable?.getContainingExtend()
        if (ownerExtend != null) return context.containingDeclarations.any { it === ownerExtend }

        val ownerClass = symbol.getContainingClass()
        if (ownerClass == null) return useSiteFile != null && useSiteFile == declarationFile
        return context.containingDeclarations
            .asSequence()
            .filterIsInstance<CfirClassLikeDeclaration>()
            .any { it.symbol.classId == ownerClass.classId }
    }

    private fun protectedAccessible(
        symbol: CfirBasedSymbol<*>,
        useSitePackage: org.cangnova.cangjie.name.FqName?,
        declarationPackage: org.cangnova.cangjie.name.FqName?,
        context: CfirAccessContext,
    ): Boolean {
        if (packageAccessible(useSitePackage, declarationPackage, ::canAccessPackageProtectedDeclaration)) return true

        val callable = symbol as? CfirCallableSymbol<*>
        val ownerExtend = callable?.getContainingExtend()
        if (ownerExtend != null && context.containingDeclarations.any { it === ownerExtend }) return true

        val ownerClassId = symbol.getContainingClass()?.classId
            ?: ownerExtend?.extendedTypeRef?.coneTypeOrNull?.classIdOrPrimitiveClassId
            ?: return false
        return context.containingDeclarations
            .asReversed()
            .asSequence()
            .filterIsInstance<CfirClassLikeDeclaration>()
            .map { it.symbol }
            .any { current ->
                current.classId == ownerClassId || current.constructType().hasSupertypeWithClassId(ownerClassId)
            }
    }

    private fun ConeCangJieType.hasSupertypeWithClassId(ownerClassId: ClassId): Boolean {
        val supertypeProvider = session.typeAwareSupertypeProviderOrNull ?: return false
        val visited = linkedSetOf<ConeCangJieType>()
        val queue = ArrayDeque<ConeCangJieType>()
        queue += this
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current)) continue
            if (current.classIdOrPrimitiveClassId == ownerClassId) return true
            queue.addAll(supertypeProvider.getDirectSupertypes(current))
        }
        return false
    }

    private companion object {
        const val STDLIB_CORE_PACKAGE: String = "std.core"
        val EXPORTED_VISIBILITIES = setOf(Visibilities.Public, Visibilities.Internal, Visibilities.Protected)
    }
}
