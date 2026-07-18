package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirSpawnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * Spawn 语义检查器
 *
 * 对齐 C++ TypeCheckExpr/SpawnExpr.cpp:
 * - spawn 表达式的 body 类型必须合法
 * - spawn 表达式本身的推断类型不能为错误类型
 *
 * 因为 CfirSpawnExpression 通过 visitAlso 注册到 BasicExpressionChecker，
 * 所以此 checker 需要在 check 方法中手动过滤类型。
 */
object CfirSpawnSemanticsChecker : CfirBasicExpressionChecker() {
    /** 官方要求 spawn 线程上下文提供该零参函数。 */
    private val getSchedulerHandleName = Name.identifier("getSchedulerHandle")

    /** 过滤 spawn 表达式并执行 spawn body 类型与参数有效性检查。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        if (expression !is CfirSpawnExpression) return

        checkSpawnBodyType(expression)
        checkSpawnArgument(expression)
        checkCapturedVariables(expression)
    }

    /**
     * spawn body 中的类型推断必须成功。
     *
     * 对齐 C++ DiagKind::sema_spawn_invalid_argument
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSpawnBodyType(spawn: CfirSpawnExpression) {
        val bodyType = spawn.body.coneTypeOrNull
        if (bodyType is ConeErrorType) {
            reporter.reportOn(
                source = spawn.source,
                factory = CfirErrors.SPAWN_ARG_INVALID,
            )
        }
    }

    /** 对齐官方 `CheckSpawnArgValid`：参数必须是 `ThreadContext` 子类型并提供调度句柄函数。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSpawnArgument(spawn: CfirSpawnExpression) {
        val ctxArg = spawn.threadContextArgument ?: return
        val actualType = ctxArg.coneTypeOrNull?.fullyExpandedType(context.session) ?: return
        if (actualType is ConeErrorType) return

        val threadContextType = ConeClassLikeType(StdlibClassIds.ThreadContext.toLookupTag(), isInterface = true)
        if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, actualType, threadContextType) != true) {
            reporter.reportOn(
                source = ctxArg.source ?: spawn.source,
                factory = CfirErrors.TYPE_MISMATCH,
                a = threadContextType,
                b = actualType,
                c = false,
            )
            return
        }

        if (!actualType.hasValidSchedulerHandle()) {
            reporter.reportOn(
                source = ctxArg.source ?: spawn.source,
                factory = CfirErrors.SPAWN_ARG_INVALID,
            )
        }
    }

    /** 检查 `getSchedulerHandle(): CPointer<Unit>`，父类型中的实现也参与查找。 */
    context(context: CheckerContext)
    private fun ConeCangJieType.hasValidSchedulerHandle(): Boolean {
        val classType = fullyExpandedType(context.session) as? ConeClassLikeType ?: return false
        return classType.classId.hasValidSchedulerHandle(mutableSetOf())
    }

    context(context: CheckerContext)
    private fun ClassId.hasValidSchedulerHandle(visited: MutableSet<ClassId>): Boolean {
        if (!visited.add(this)) return false
        val declaration = context.session.symbolProvider
            .getClassLikeSymbolByClassId(this)
            ?.cfir as? CfirClass
            ?: return false

        if (declaration.declarations.any { member -> member.isValidSchedulerHandleFunction(context) }) {
            return true
        }

        return declaration.superTypeRefs.any { superTypeRef ->
            val superType = (superTypeRef as? CfirResolvedTypeRef)
                ?.coneType
                ?.fullyExpandedType(context.session) as? ConeClassLikeType
                ?: return@any false
            superType.classId.hasValidSchedulerHandle(visited)
        }
    }

    private fun CfirElement.isValidSchedulerHandleFunction(context: CheckerContext): Boolean {
        val function = this as? CfirNamedFunction ?: return false
        if (function.name != getSchedulerHandleName) return false
        if (function.valueParameters.isNotEmpty()) return false
        val returnType = (function.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return false
        val expectedReturnType = ConePointerType(context.session.builtinTypes.unitType)
        return AbstractTypeChecker.isSubtypeOf(context.session.typeContext, returnType, expectedReturnType) == true
    }

    /**
     * spawn 执行闭包不能脱离调用点捕获外层可变局部变量；同时不能在初始化完成前捕获
     * 当前正在初始化的局部 `let`。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCapturedVariables(spawn: CfirSpawnExpression) {
        val localDeclarations = spawn.localVariablesDeclaredInside()
        val initializingVariables = context.callsOrAssignments
            .filterIsInstance<CfirAssignment>()
            .mapNotNull { assignment ->
                val variable = (assignment.lValue as? CfirQualifiedAccessExpression)
                    ?.resolvedVariableSymbolOrNull()
                    ?.takeIf { it.isBound }
                    ?.cfir
                    ?: return@mapNotNull null
                variable.takeIf { !it.isVar && it.isLocal && it.initializer == null }
            }
            .toSet()

        var capturesMutableVariable = false
        val reportedBeforeInitialization = mutableSetOf<CfirVariable>()

        spawn.body.acceptChildren(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                when (element) {
                    is CfirQualifiedAccessExpression -> {
                        val variableSymbol = element.resolvedVariableSymbolOrNull()
                            ?.takeIf { it.isBound }
                        val variable = variableSymbol?.cfir
                        if (variable != null && variable !in localDeclarations) {
                            if (variable in initializingVariables && reportedBeforeInitialization.add(variable)) {
                                reporter.reportOn(
                                    source = element.calleeReference.source ?: element.source,
                                    factory = CfirErrors.CAPTURE_BEFORE_INITIALIZATION,
                                    a = variableSymbol.name,
                                )
                            }
                            if (variable.isVar && variable.isLocal) {
                                capturesMutableVariable = true
                            }
                        }
                    }
                }
                element.acceptChildren(this, null)
            }
        }, null)

        if (capturesMutableVariable) {
            reporter.reportOn(
                source = spawn.source,
                factory = CfirErrors.USE_FUNC_CAPTURE_VAR_ALONE,
                a = "lambda",
            )
        }
    }
}

/** 收集 spawn body 内部声明的局部变量，用于区分闭包捕获与本地使用。 */
private fun CfirSpawnExpression.localVariablesDeclaredInside(): Set<CfirVariable> {
    val variables = linkedSetOf<CfirVariable>()
    body.acceptChildren(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (element is CfirVariable && element.isLocal) {
                variables += element
            }
            element.acceptChildren(this, null)
        }
    }, null)
    return variables
}

/** 从 qualified access 的引用节点读取解析到的变量符号。 */
private fun CfirQualifiedAccessExpression.resolvedVariableSymbolOrNull(): CfirVariableSymbol<*>? =
    when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirVariableSymbol<*>
        is CfirResolvedErrorReference -> reference.resolvedSymbol as? CfirVariableSymbol<*>
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirVariableSymbol<*>
        else -> null
    }
