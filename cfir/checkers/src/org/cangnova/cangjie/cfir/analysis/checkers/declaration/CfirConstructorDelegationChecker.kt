package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * 对齐 Kotlin FIR 的 constructor delegation issues checker 思路：
 * - 构造器委托调用是“构造器语义”，不是普通函数调用；
 * - 参数匹配仍复用调用解析基础设施；
 * - delegation 的位置、循环与父类构造器要求在专门的 constructor checker 中统一处理。
 */
object CfirConstructorDelegationChecker : CfirConstructorChecker() {
    /**
     * 检查单个构造器的委托调用语义。
     *
     * 入口会统一处理重复主构造器、`this`/`super` 委托位置、委托参数中的成员访问、
     * 显式委托解析以及无显式委托时的隐式父构造器要求。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirConstructor) {
        val owner = context.findClosestDeclaration<CfirClassLikeDeclaration>() ?: return
        val body = declaration.body

        declaration.checkMultiplePrimaryConstructors(owner)

        val delegationCalls = body?.collectDelegationCalls().orEmpty()
        val firstStatementDelegation = body?.statements?.firstOrNull().asDelegationCallOrNull()

        delegationCalls
            .filter { delegation -> delegation.call !== firstStatementDelegation?.call }
            .forEach { delegation ->
                reporter.reportOn(
                    source = delegation.call.delegationDiagnosticSource()?.firstCharacterDiagnosticSource()
                        ?: declaration.source?.firstCharacterDiagnosticSource(),
                    factory = CfirErrors.ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER,
                    a = delegation.kind.keyword,
                )
            }

        firstStatementDelegation?.checkArgumentMemberAccessBeforeInitialization(owner)

        when (firstStatementDelegation?.kind) {
            ConstructorDelegationCallKind.THIS -> {
                if (declaration.isPrimary) {
                    reporter.reportOn(
                        source = firstStatementDelegation.call.delegationDiagnosticSource()?.firstCharacterDiagnosticSource()
                            ?: declaration.source?.firstCharacterDiagnosticSource(),
                        factory = CfirErrors.ILLEGAL_PLACE_OF_CALLING_THIS_PRIMARY_CONSTRUCTOR,
                    )
                    return
                }
                checkThisDelegation(owner, declaration, firstStatementDelegation.call)
            }
            ConstructorDelegationCallKind.SUPER -> checkSuperDelegation(owner, declaration, firstStatementDelegation.call)
            null -> checkImplicitSuperRequirement(owner, declaration)
        }
    }

    /**
     * 检查同一 class-like 声明内是否出现多个主构造器。
     *
     * 第一个主构造器按源码偏移保留，后续主构造器报告 `MULTIPLE_PRIMARY_CONSTRUCTORS`。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirConstructor.checkMultiplePrimaryConstructors(owner: CfirClassLikeDeclaration) {
        if (!isPrimary) return
        val primaryConstructors = owner.declarations
            .asSequence()
            .filterIsInstance<CfirConstructor>()
            .filter(CfirConstructor::isPrimary)
            .toList()
        val firstPrimary = primaryConstructors.minByOrNull { constructor ->
            constructor.source?.startOffset ?: Int.MAX_VALUE
        } ?: return
        if (this === firstPrimary) return

        reporter.reportOn(
            source = source?.firstCharacterDiagnosticSource(),
            factory = CfirErrors.MULTIPLE_PRIMARY_CONSTRUCTORS,
        )
    }

    /**
     * 检查二级构造器的 `this(...)` 委托目标。
     *
     * 解析成功时只接受当前声明所属 class-like 内的构造器；解析缺失时回退到参数数量
     * 匹配，以便在没有候选时优先报告更精确的实参数量诊断。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkThisDelegation(
        owner: CfirClassLikeDeclaration,
        declaration: CfirConstructor,
        call: CfirFunctionCall,
    ) {
        val constructors = owner.declarations.filterIsInstance<CfirConstructor>()
        val resolvedConstructor = call.resolvedDelegatedConstructorOrNull()?.takeIf { constructor -> constructor in constructors }
        val candidates = resolvedConstructor?.let(::listOf)
            ?: constructors.filter { constructor -> constructor.matchesDelegationCall(call) }

        when {
            candidates.isEmpty() -> {
                if (reportConstructorArgumentCountMismatch(listOf(declaration) + constructors.filter { it !== declaration }, call)) {
                    return
                }
                reporter.reportOn(
                    source = call.delegationDiagnosticSource() ?: declaration.source,
                    factory = CfirErrors.NO_CONSTRUCTOR,
                )
            }

            candidates.size > 1 -> reporter.reportOn(
                source = call.delegationDiagnosticSource() ?: declaration.source,
                factory = CfirErrors.AMBIGUOUS_CONSTRUCTOR_CALL,
                a = owner.classLikeName(),
            )
        }
    }

    /**
     * 检查构造器的 `super(...)` 委托目标。
     *
     * 只有存在实际非接口父类时才检查父类构造器；无父类场景保持官方语义，不额外产生
     * `NO_CONSTRUCTOR`。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSuperDelegation(
        owner: CfirClassLikeDeclaration,
        declaration: CfirConstructor,
        call: CfirFunctionCall,
    ) {
        // 官方实现只在存在实际父类时检查父类构造器；无显式父类的 `super()` 不产生 NO_CONSTRUCTOR。
        val superDeclaration = owner.directConcreteSuperDeclaration(context) ?: return
        val constructors = superDeclaration.declarations.filterIsInstance<CfirConstructor>()
        val resolvedConstructor = call.resolvedDelegatedConstructorOrNull()?.takeIf { constructor -> constructor in constructors }
        val candidates = resolvedConstructor?.let(::listOf)
            ?: constructors.filter { constructor -> constructor.matchesDelegationCall(call) }

        when {
            candidates.isEmpty() -> {
                if (reportConstructorArgumentCountMismatch(constructors, call)) {
                    return
                }
                reporter.reportOn(
                    source = call.delegationDiagnosticSource() ?: declaration.source,
                    factory = CfirErrors.NO_CONSTRUCTOR,
                )
            }

            candidates.size > 1 -> reporter.reportOn(
                source = call.delegationDiagnosticSource() ?: declaration.source,
                factory = CfirErrors.AMBIGUOUS_CONSTRUCTOR_CALL,
                a = superDeclaration.classLikeName(),
            )
        }
    }

    /**
     * 在构造器没有显式委托调用时检查是否允许隐式调用父类无参构造器。
     *
     * 该规则只作用于 class；struct、interface、enum 等其他 class-like 声明不在这里
     * 承担隐式 super 构造器约束。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkImplicitSuperRequirement(
        owner: CfirClassLikeDeclaration,
        declaration: CfirConstructor,
    ) {
        if (owner !is CfirClass) return

        val superDeclaration = owner.directConcreteSuperDeclaration(context) ?: return
        val hasImplicitSuper = superDeclaration.declarations
            .filterIsInstance<CfirConstructor>()
            .any { constructor -> constructor.requiredParameterCount() == 0 }
        if (hasImplicitSuper) return

        reporter.reportOn(
            source = declaration.constructorNameDiagnosticSource(),
            factory = CfirErrors.NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS,
        )
    }
}

/**
 * 根据构造器候选集合与委托调用实参数量报告数量类诊断。
 *
 * 返回 true 表示已经报告 `TOO_MANY_ARGUMENTS` 或 `NO_VALUE_FOR_PARAMETER`，
 * 调用方不应再报告兜底的 `NO_CONSTRUCTOR`。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun reportConstructorArgumentCountMismatch(
    constructors: List<CfirConstructor>,
    call: CfirFunctionCall,
): Boolean {
    val argumentCount = call.argumentList.arguments.size
    val tooManyTarget = constructors.firstOrNull { constructor -> argumentCount > constructor.valueParameters.size }
    if (tooManyTarget != null) {
        val source = call.argumentList.arguments.getOrNull(tooManyTarget.valueParameters.size)?.source
            ?: call.delegationDiagnosticSource()
        reporter.reportOn(
            source = source,
            factory = CfirErrors.TOO_MANY_ARGUMENTS,
            a = call.delegationName(),
        )
        return true
    }

    val missingTarget = constructors.firstOrNull { constructor -> argumentCount < constructor.requiredParameterCount() }
        ?: return false
    val missingParameter = missingTarget.valueParameters
        .drop(argumentCount)
        .firstOrNull { parameter -> parameter.defaultValue == null }
        ?: return false
    reporter.reportOn(
        source = call.source ?: call.delegationDiagnosticSource(),
        factory = CfirErrors.NO_VALUE_FOR_PARAMETER,
        a = missingParameter.name,
    )
    return true
}

/**
 * 构造器委托调用的源码形态。
 */
internal enum class ConstructorDelegationCallKind(
    /**
     * 用于诊断参数的源码关键字文本。
     */
    val keyword: String,
) {
    /**
     * 当前类内部的 `this(...)` 构造器委托。
     */
    THIS("this"),

    /**
     * 指向直接父类构造器的 `super(...)` 构造器委托。
     */
    SUPER("super"),
}

/**
 * 从 CFIR 表达式中抽取出的构造器委托调用信息。
 */
private data class ConstructorDelegationCall(
    /**
     * 委托调用的种类，用于决定后续语义规则和诊断关键字。
     */
    val kind: ConstructorDelegationCallKind,

    /**
     * 承载 `this(...)` 或 `super(...)` 的函数调用节点。
     */
    val call: CfirFunctionCall,
)

/**
 * 检查构造器委托调用实参中的成员访问是否发生在对象初始化完成之前。
 *
 * `this(...)` 与 `super(...)` 的参数位置分别映射到不同的访问场景，后续由成员初始化
 * 检查器复用同一套 before-initialization 诊断逻辑。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun ConstructorDelegationCall.checkArgumentMemberAccessBeforeInitialization(owner: CfirClassLikeDeclaration) {
    val place = when (kind) {
        ConstructorDelegationCallKind.THIS -> ConstructorMemberAccessPlace.THIS_DELEGATION_ARGUMENT
        ConstructorDelegationCallKind.SUPER -> ConstructorMemberAccessPlace.SUPER_DELEGATION_ARGUMENT
    }
    call.argumentList.arguments.forEach { argument ->
        argument.checkConstructorMemberAccessBeforeInitialization(owner, place)
    }
}

/**
 * 构造器 delegation 相关诊断都应该尽量锚定在 `this` / `super` 关键字本身，
 * 这样既贴近 Kotlin FIR 的报错体验，也能避免把整段调用都染成同一类构造器语义错误。
 */
private fun CfirFunctionCall.delegationDiagnosticSource() = calleeReference.source ?: source

/**
 * 取得委托调用在诊断中展示的名称。
 *
 * 正常情况下使用 callee 引用的简单名；当引用不是命名引用时回退到构造器专用占位名。
 */
private fun CfirFunctionCall.delegationName(): Name =
    (calleeReference as? CfirNamedReference)?.name ?: Name.special("<constructor>")

/**
 * 将任意 CFIR 元素识别为构造器委托调用。
 *
 * 包装表达式会被展开；只有可识别为 `this` 或 `super` 构造器委托的函数调用才会返回。
 */
private fun CfirElement?.asDelegationCallOrNull(): ConstructorDelegationCall? {
    val call = constructorDelegationCallOrNull() ?: return null
    val kind = call.constructorDelegationKindOrNull() ?: return null
    return ConstructorDelegationCall(kind, call)
}

/**
 * 从元素中抽取构造器委托函数调用。
 *
 * 该函数作为内部工具开放给其他构造器规则复用，统一处理 wrapped expression 与
 * `CfirFunctionCallOrigin` / callee 名称两种识别来源。
 */
internal fun CfirElement?.constructorDelegationCallOrNull(): CfirFunctionCall? {
    if (this is CfirWrappedExpression) {
        return expression.constructorDelegationCallOrNull()
    }
    val call = this as? CfirFunctionCall ?: return null
    return call.takeIf { it.constructorDelegationKindOrNull() != null }
}

/**
 * 判断函数调用是否表示 `this(...)` 或 `super(...)` 构造器委托。
 *
 * 优先使用解析阶段写入的调用 origin；当 origin 不完整时，再根据 callee 名称兼容
 * 早期或错误恢复路径生成的 CFIR。
 */
internal fun CfirFunctionCall.constructorDelegationKindOrNull(): ConstructorDelegationCallKind? {
    return when (origin.toDelegationKindOrNull()) {
        ConstructorDelegationCallKind.THIS -> ConstructorDelegationCallKind.THIS
        ConstructorDelegationCallKind.SUPER -> ConstructorDelegationCallKind.SUPER
        null -> when ((calleeReference as? CfirNamedReference)?.name?.asString()) {
            "this" -> ConstructorDelegationCallKind.THIS
            "super" -> ConstructorDelegationCallKind.SUPER
            else -> null
        }
    }
}

/**
 * 将函数调用 origin 映射为构造器委托种类。
 */
private fun CfirFunctionCallOrigin.toDelegationKindOrNull(): ConstructorDelegationCallKind? {
    return when (this) {
        CfirFunctionCallOrigin.ConstructorDelegationThis -> ConstructorDelegationCallKind.THIS
        CfirFunctionCallOrigin.ConstructorDelegationSuper -> ConstructorDelegationCallKind.SUPER
        else -> null
    }
}

/**
 * 收集构造器 body 中出现的所有 `this(...)` / `super(...)` 委托调用。
 *
 * 该遍历会深入子表达式，因此可以发现非法位置上的委托调用，随后由入口检查器统一报告。
 */
private fun org.cangnova.cangjie.cfir.expressions.CfirBlock.collectDelegationCalls(): List<ConstructorDelegationCall> {
    val result = mutableListOf<ConstructorDelegationCall>()
    acceptChildren(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            element.asDelegationCallOrNull()?.let(result::add)
            element.acceptChildren(this, null)
        }
    }, null)
    return result
}

/**
 * 以参数数量判断构造器是否可能匹配某个委托调用。
 *
 * 这里只做数量层面的快速匹配，类型兼容与默认参数语义由调用解析或更精确的诊断逻辑处理。
 */
private fun CfirConstructor.matchesDelegationCall(call: CfirFunctionCall): Boolean {
    val argumentCount = call.argumentList.arguments.size
    val minimum = requiredParameterCount()
    val maximum = valueParameters.size
    return argumentCount in minimum..maximum
}

/**
 * 从委托调用的 callee reference 中取得已经解析到的构造器声明。
 *
 * 同时支持成功解析引用和带候选符号的错误恢复引用；真正的诊断 holder 不作为可用候选。
 */
private fun CfirFunctionCall.resolvedDelegatedConstructorOrNull(): CfirConstructor? {
    return when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol.cfir as? CfirConstructor
        is CfirNamedReferenceWithCandidateBase ->
            reference.takeUnless { it is CfirDiagnosticHolder }?.candidateSymbol?.cfir as? CfirConstructor
        else -> null
    }
}

/**
 * 计算构造器调用必须提供实参的参数数量。
 *
 * 带默认值的参数不计入必填数量，用于实参数量诊断和隐式无参构造器判断。
 */
internal fun CfirConstructor.requiredParameterCount(): Int =
    valueParameters.count { it.defaultValue == null }

/**
 * 解析 class-like 声明的直接具体父类声明。
 *
 * 接口父类型会被过滤；当 includeLoopInSupertypeError 为 true 时，允许从
 * `LoopInSupertype` 错误类型的 delegated typeRef 中恢复父类型，供循环继承诊断使用。
 */
internal fun CfirClassLikeDeclaration.directConcreteSuperDeclaration(
    context: CheckerContext,
    includeLoopInSupertypeError: Boolean = false,
): CfirClassLikeDeclaration? {
    return superTypeRefs
        .mapNotNull { superTypeRef ->
            superTypeRef.toResolvedSuperDeclaration(
                context = context,
                includeLoopInSupertypeError = includeLoopInSupertypeError,
            )
        }
        .firstOrNull { superDeclaration -> superDeclaration !is CfirInterface }
}

/**
 * 将 super typeRef 解析为可用于构造器语义检查的父 class-like 声明。
 *
 * 普通 resolved typeRef 直接使用其 cone type；循环继承错误恢复路径只在调用方显式允许时启用。
 */
private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.toResolvedSuperDeclaration(
    context: CheckerContext,
    includeLoopInSupertypeError: Boolean,
): CfirClassLikeDeclaration? {
    val resolvedTypeRef = this as? CfirResolvedTypeRef ?: return null
    val coneType = resolvedTypeRef.coneType
    val effectiveTypeRef = if (coneType is ConeErrorType) {
        if (!includeLoopInSupertypeError) return null
        val diagnostic = coneType.diagnostic as? ConeSimpleDiagnostic ?: return null
        if (diagnostic.kind != DiagnosticKind.LoopInSupertype) return null
        resolvedTypeRef.delegatedTypeRef as? CfirResolvedTypeRef ?: return null
    } else {
        resolvedTypeRef
    }
    return effectiveTypeRef.coneType.toResolvedSuperDeclaration(context)
}

/**
 * 根据 cone 类型解析对应的 class-like 声明。
 *
 * 类型别名会先完全展开；解析优先使用 CFIR provider，缺失时再查询 symbol provider，
 * 覆盖源码声明与反序列化声明两类来源。
 */
private fun ConeCangJieType.toResolvedSuperDeclaration(context: CheckerContext): CfirClassLikeDeclaration? {
    val expanded = fullyExpandTypeAlias(context)
    val classId = when (expanded) {
        is ConePrimitiveType -> expanded.kind.classId
        is ConeClassLikeType -> expanded.classId
        is ConeStructType -> expanded.classId
        is ConeEnumType -> expanded.classId
        is ConeTypeAliasType -> expanded.classId
        else -> null
    } ?: return null

    return context.session.cfirProvider.getCfirClassifierByFqName(classId)
        ?: context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
}

/**
 * 在构造器父类型解析场景下递归展开类型别名。
 *
 * visitedAliases 用于阻止循环类型别名导致无限展开；当内嵌 expandedType 缺失时回到
 * provider 查询 typealias 声明并读取其 resolved expanded typeRef。
 */
private fun ConeCangJieType.fullyExpandTypeAlias(context: CheckerContext): ConeCangJieType {
    var current = this
    val visitedAliases = linkedSetOf<ClassId>()
    while (current is ConeTypeAliasType && visitedAliases.add(current.classId)) {
        val embeddedExpandedType = current.expandedType
        if (embeddedExpandedType != null) {
            current = embeddedExpandedType
            continue
        }
        val typeAlias = context.session.cfirProvider.getCfirClassifierByFqName(current.classId) as? CfirTypeAlias
            ?: context.session.symbolProvider.getClassLikeSymbolByClassId(current.classId)?.cfir as? CfirTypeAlias
            ?: break
        val expandedType = (typeAlias.expandedTypeRef as? CfirResolvedTypeRef)?.coneType ?: break
        current = expandedType
    }
    return current
}

/**
 * 取得 class-like 声明用于构造器诊断展示的名称。
 */
private fun CfirClassLikeDeclaration.classLikeName(): Name = when (this) {
    is CfirPrimitiveTypeDeclaration -> name
    is CfirClass -> name
    is CfirInterface -> name
    is CfirStruct -> name
    is CfirEnum -> name
    is CfirTypeAlias -> name
}

/**
 * 将 CFIR 原始类型种类映射为对应的内建 classId。
 */
private val org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.classId: ClassId
    get() = ClassId.fromString(typeName)
