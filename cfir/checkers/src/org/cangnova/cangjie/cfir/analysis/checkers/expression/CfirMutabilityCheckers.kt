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

package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.declaredUpperBoundTypesInCurrentContext
import org.cangnova.cangjie.cfir.analysis.checkers.hasInvalidDeclaredUpperBoundsInCurrentContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.name.Name

/**
 * 对齐官方 `mut/immutable` 核心语义的第一步：
 * 在 struct 的非 mut 成员函数中，`this` 视角下的可变操作必须被拦截。
 *
 * 这一批先只处理最稳定的两类行为：
 * 1. 赋值到当前实例字段；
 * 2. 调用当前实例上的 mut 成员函数。
 */
object CfirImmutableFunctionCannotModifyFieldChecker : CfirAssignmentChecker() {
    /**
     * 检查不可变 struct 成员函数中是否写入当前实例的可变字段。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirAssignment) {
        val mutationContext = context.currentImmutableStructMutationContext() ?: return

        val lValue = expression.lValue as? CfirQualifiedAccessExpression ?: return
        val root = lValue.currentStructMutationRoot(mutationContext.owner) ?: return

        reporter.reportOn(
            source = root.access.calleeReference.source ?: root.access.source ?: expression.source,
            factory = CfirErrors.CANNOT_MODIFY_VAR,
            a = root.field.name,
        )
    }
}

/**
 * 不可变 struct/interface 成员函数中禁止调用当前实例的 mut 成员函数。
 */
object CfirImmutableFunctionCannotAccessMutableFunctionChecker : CfirFunctionCallChecker() {
    /**
     * 检查当前实例接收者上的 mut 函数调用。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val targetSymbol = expression.resolvedFunctionSymbolOrNull() ?: return
        val targetFunction = targetSymbol.takeIf { it.isBound }?.cfir as? CfirNamedFunction ?: return
        if (!targetFunction.status.isMut || targetFunction.status.isConst) return

        if (expression.isCurrentStructReceiverAccess()) {
            val currentFunction = context.currentImmutableStructFunction() ?: return
            reporter.reportOn(
                source = expression.calleeReference.source ?: expression.source,
                factory = CfirErrors.IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION,
                a = currentFunction.name,
                b = targetFunction.name,
            )
            return
        }

        val mutationContext = context.currentImmutableStructMutationContext() ?: return
        val receiver = expression.explicitReceiver ?: expression.dispatchReceiver ?: return
        val root = receiver.currentStructMutationRoot(mutationContext.owner) ?: return
        reporter.reportOn(
            source = root.access.calleeReference.source ?: root.access.source ?: receiver.source,
            factory = CfirErrors.CANNOT_MODIFY_VAR,
            a = root.field.name,
        )
    }
}

/**
 * 检查 struct 构造器或 mut 成员函数中的嵌套函数是否捕获当前实例字段。
 *
 * 官方把这类引用从普通不可变函数修改规则中分离：构造器使用
 * `ILLEGAL_CAPTURE_THIS`，mut 函数使用 `CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC`。
 */
object CfirStructInstanceFieldCaptureChecker : CfirQualifiedAccessChecker() {
    /** 对当前实例字段的嵌套捕获报告专用诊断。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (!expression.isCurrentStructReceiverAccess()) return
        val field = expression.resolvedFieldSymbolOrNull()
            ?.takeIf { it.isBound }
            ?.cfir as? CfirFieldVariable ?: return
        // static 字段属于类型存储，不通过当前 struct 实例捕获；其访问合法性由
        // static/global 初始化与访问 checker 负责。不能把无显式 receiver 的静态
        // 名称误判成隐式 this 字段，否则构造器中的合法 lambda/local function 会
        // 被错误报告为 ILLEGAL_CAPTURE_THIS。
        if (field.status.isStatic) return
        val captureContext = context.currentStructCaptureContext(field) ?: return
        val source = expression.calleeReference.source ?: expression.source

        when (val outerFunction = captureContext.outerFunction) {
            is CfirConstructor -> reporter.reportOn(
                source = source,
                factory = CfirErrors.ILLEGAL_CAPTURE_THIS,
                a = "struct",
            )

            is CfirNamedFunction -> if (outerFunction.status.isMut) {
                reporter.reportOn(
                    source = source,
                    factory = CfirErrors.CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC,
                    a = field.name,
                    b = "mutable function '${outerFunction.name.asString()}'",
                )
            }

            else -> Unit
        }
    }
}

/**
 * 不可变 struct 值禁止调用 mut 成员函数。
 *
 * 对齐官方 `TypeCheckAccess::CheckLetInstanceAccessMutableFunc`：`let`/`const`
 * 的 struct 值、属性值以及非 class-like 的临时值，不能作为 mut 函数调用的接收者。
 */
object CfirImmutableValueCannotAccessMutableFunctionChecker : CfirFunctionCallChecker() {
    /**
     * 检查不可变 struct 值作为接收者调用 mut 成员函数的场景。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val receiver = expression.explicitReceiver ?: return
        val targetFunction = expression.resolvedOrDeclaredUpperBoundMutFunctionOrNull() ?: return
        if (!targetFunction.status.isMut || targetFunction.status.isConst) return
        if (!receiver.isImmutableStructValueAccess()) return

        reporter.reportOn(
            source = expression.calleeReference.source ?: expression.source ?: receiver.source,
            factory = CfirErrors.IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION,
            a = receiver.diagnosticNameOr(targetFunction.name),
            b = targetFunction.name,
        )
    }
}

/**
 * 查找当前所在的不可变值类型成员函数。
 *
 * 当前函数必须位于 struct 或 interface 中且自身没有 `mut` 标记。
 */
private fun CheckerContext.currentImmutableStructFunction(): CfirNamedFunction? {
    val ownerIndex = containingDeclarations.indexOfLast { declaration ->
        declaration is CfirStruct || declaration is CfirInterface || declaration is CfirExtend
    }
    if (ownerIndex < 0) return null
    val function = containingDeclarations
        .drop(ownerIndex + 1)
        .filterIsInstance<CfirNamedFunction>()
        .firstOrNull() ?: return null
    return function.takeUnless { it.status.isMut }
}

/** 当前 struct owner 及其最外层成员函数语境。 */
private data class StructMutationContext(
    val owner: CfirStruct,
    val outerFunction: CfirFunction?,
)

/** 当前实例字段在值类型访问链中的根节点。 */
private data class StructMutationRoot(
    val field: CfirFieldVariable,
    val access: CfirQualifiedAccessExpression,
)

/** struct 嵌套函数捕获检查所需的最外层成员函数。 */
private data class StructCaptureContext(
    val outerFunction: CfirFunction,
)

/**
 * 解析当前声明栈对应的 struct owner；extend 成员通过 extend 目标恢复真实 struct。
 */
private fun CheckerContext.currentStructOwnerAndIndex(): Pair<CfirStruct, Int>? {
    for (index in containingDeclarations.indices.reversed()) {
        when (val declaration = containingDeclarations[index]) {
            is CfirStruct -> return declaration to index
            is CfirExtend -> {
                val target = CfirExtendSemantics.targetDeclaration(this, declaration) as? CfirStruct ?: continue
                return target to index
            }

            else -> Unit
        }
    }
    return null
}

/**
 * 返回当前不可变 struct 成员的修改语境。
 *
 * 判断使用 owner 之后的第一个函数声明，等价于官方的最外层函数：嵌套函数不能把
 * 构造器、mut 成员或 static 成员伪装成普通不可变实例函数；字段初始化 lambda 则仍属于
 * 不可变语境。属性访问器的 static 性属于所属属性，不能依赖 accessor 自身未继承的 status。
 */
private fun CheckerContext.currentImmutableStructMutationContext(): StructMutationContext? {
    val (owner, ownerIndex) = currentStructOwnerAndIndex() ?: return null
    val outerFunction = containingDeclarations
        .drop(ownerIndex + 1)
        .filterIsInstance<CfirFunction>()
        .firstOrNull()
    if (outerFunction is CfirConstructor || outerFunction?.status?.isMut == true) return null
    if (outerFunction?.isStaticStructMemberContext() == true) return null
    return StructMutationContext(owner, outerFunction)
}

/**
 * 判断函数语境是否属于 static 成员。
 *
 * 普通函数直接读取自身 status；getter/setter 则统一读取所属属性的 static 契约，因为访问器
 * 的 resolved status 当前只继承属性的可见性和模态，不继承 static 标记。
 */
private fun CfirFunction.isStaticStructMemberContext(): Boolean =
    status.isStatic || this is CfirPropertyAccessor && propertySymbol.cfir.status.isStatic

/**
 * 判断字段引用是否位于构造器或 mut 函数的嵌套函数中。
 */
private fun CheckerContext.currentStructCaptureContext(field: CfirFieldVariable): StructCaptureContext? {
    val (owner, ownerIndex) = currentStructOwnerAndIndex() ?: return null
    if (field.status.isStatic) return null
    if (owner.declarations.filterIsInstance<CfirFieldVariable>().none { it.symbol == field.symbol }) return null
    val functions = containingDeclarations
        .drop(ownerIndex + 1)
        .filterIsInstance<CfirFunction>()
    if (functions.size < 2) return null
    val outerFunction = functions.first()
    if (outerFunction !is CfirConstructor && outerFunction.status.isMut.not()) return null
    return StructCaptureContext(outerFunction)
}

/**
 * 沿 qualified-access 的值类型接收者链查找当前 struct 的根字段。
 *
 * class/interface/Array 等引用类型在官方实现中会截断修改传播；只有 struct 值链继续向根部追溯。
 */
context(context: CheckerContext)
private fun CfirExpression.currentStructMutationRoot(owner: CfirStruct): StructMutationRoot? {
    val ownerFieldSymbols = owner.declarations
        .filterIsInstance<CfirFieldVariable>()
        .mapTo(linkedSetOf()) { field -> field.symbol }
    var current: CfirExpression = this

    while (current is CfirQualifiedAccessExpression) {
        val field = current.resolvedFieldSymbolOrNull()
            ?.takeIf { it.isBound }
            ?.cfir as? CfirFieldVariable
        if (field != null && field.symbol in ownerFieldSymbols) {
            return StructMutationRoot(field, current)
        }

        val receiver = current.explicitReceiver ?: current.dispatchReceiver ?: return null
        val receiverType = receiver.coneTypeOrNull?.fullyExpandedType(context.session)
        if (receiverType is ConeClassLikeType) return null
        current = receiver
    }
    return null
}

/**
 * 判断 qualified access 是否访问当前 struct 实例。
 */
private fun CfirQualifiedAccessExpression.isCurrentStructReceiverAccess(): Boolean {
    return explicitReceiver == null || explicitReceiver is CfirThisReceiverExpression
}

/**
 * 从 qualified access 中解析字段符号。
 */
private fun CfirQualifiedAccessExpression.resolvedFieldSymbolOrNull(): CfirFieldVariableSymbol? {
    return when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirFieldVariableSymbol
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirFieldVariableSymbol
        else -> null
    }
}

/**
 * 从 qualified access 中解析目标函数符号。
 */
private fun CfirQualifiedAccessExpression.resolvedFunctionSymbolOrNull(): CfirFunctionSymbol<*>? {
    return when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirFunctionSymbol<*>
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirFunctionSymbol<*>
        else -> null
    }
}

/**
 * 从 qualified access 中解析变量或属性符号。
 */
private fun CfirQualifiedAccessExpression.resolvedVariableOrPropertySymbolOrNull(): org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>? {
    return when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
        else -> null
    }
}

/**
 * 判断表达式是否表示不可变 struct 值访问。
 *
 * `this`/`super` 不视为不可变值；变量、属性和临时值按类型及声明可变性递归判断。
 */
context(context: CheckerContext)
internal fun CfirExpression.isImmutableStructValueAccess(): Boolean {
    if (this is CfirThisReceiverExpression || this is CfirSuperReceiverExpression) return false
    if (!coneTypeOrNull.mayBeStructValueTypeInCurrentContext()) return false

    val access = this as? CfirQualifiedAccessExpression ?: return true
    val symbol = access.resolvedVariableOrPropertySymbolOrNull()
    // 类型限定符（例如 `A.a` 中的 `A`）不是一个可被复制/冻结的 struct 值。
    // static 成员写入的可变性由目标成员自身决定，不能沿限定符伪造值接收者限制。
    if (symbol is CfirClassLikeSymbol<*>) return false
    val receiver = access.explicitReceiver ?: access.dispatchReceiver
    return when (symbol) {
        is CfirVariableSymbol<*> -> {
            val variable = symbol.takeIf { it.isBound }?.cfir ?: return true
            if (!variable.isVar) return true
            if (access.coneTypeOrNull.mayBeStructTypeParameterInCurrentContext()) return true
            receiver?.isImmutableStructValueAccess() == true
        }

        is CfirPropertySymbol -> true
        else -> true
    }
}

/**
 * 类型参数 receiver 即使声明为 `var` 形式的参数，也不能获得可写 struct 左值能力。
 */
context(context: CheckerContext)
private fun ConeCangJieType?.mayBeStructTypeParameterInCurrentContext(): Boolean =
    this is ConeTypeParameterType && mayBeStructValueTypeInCurrentContext()

/**
 * 取得不可变值诊断中用于展示的接收者名称。
 */
private fun CfirExpression.diagnosticNameOr(defaultName: Name): Name {
    val access = this as? CfirQualifiedAccessExpression ?: return defaultName
    return when (val symbol = access.resolvedVariableOrPropertySymbolOrNull()) {
        is CfirVariableSymbol<*> -> symbol.name
        is CfirPropertySymbol -> (symbol.takeIf { it.isBound }?.cfir as? CfirProperty)?.name ?: symbol.name
        else -> defaultName
    }
}

/**
 * 返回已解析函数；若普通解析因非法声明上界中断，则按官方错误恢复语义从声明上界查询 mut 函数。
 */
context(context: CheckerContext)
internal fun CfirQualifiedAccessExpression.resolvedOrDeclaredUpperBoundMutFunctionOrNull(): CfirNamedFunction? {
    resolvedFunctionSymbolOrNull()
        ?.takeIf { it.isBound }
        ?.cfir
        ?.let { return it as? CfirNamedFunction }

    return declaredUpperBoundMutFunctionOrNull()
}

/**
 * 非法 struct 上界已报告后，官方仍使用声明上界继续产生 mut 访问诊断。
 */
context(context: CheckerContext)
internal fun CfirQualifiedAccessExpression.declaredUpperBoundMutFunctionOrNull(): CfirNamedFunction? {
    val receiver = explicitReceiver ?: dispatchReceiver ?: return null
    val calleeName = (calleeReference as? CfirNamedReference)?.name ?: return null
    return receiver.coneTypeOrNull.findMutFunctionInInvalidDeclaredUpperBounds(calleeName)
}

/**
 * 结合 resolved bounds 与当前声明栈中的 raw upper-bound refs 判断值类型可能性。
 */
context(context: CheckerContext)
private fun ConeCangJieType?.mayBeStructValueTypeInCurrentContext(
    visitedTypeParameters: MutableSet<CfirTypeParameterSymbol> = linkedSetOf(),
): Boolean = when (this) {
    null -> false
    is ConeStructType -> true
    is ConeTypeParameterType -> {
        val typeParameter = lookupTag.typeParameterSymbol
        when {
            mayBeStructValueType() -> true
            !typeParameter.cfir.hasInvalidDeclaredUpperBoundsInCurrentContext() -> false
            !visitedTypeParameters.add(typeParameter) -> false
            else -> typeParameter.cfir.declaredUpperBoundTypesInCurrentContext()
                .any { upperBound -> upperBound.mayBeStructValueTypeInCurrentContext(visitedTypeParameters) }
        }
    }
    is ConeClassLikeType -> isInterface
    else -> false
}

/**
 * 从非法声明上界链中恢复指定名称的 mut 函数。
 */
context(context: CheckerContext)
private fun ConeCangJieType?.findMutFunctionInInvalidDeclaredUpperBounds(
    name: Name,
    visitedTypeParameters: MutableSet<CfirTypeParameterSymbol> = linkedSetOf(),
): CfirNamedFunction? {
    val typeParameterType = this as? ConeTypeParameterType ?: return null
    val typeParameter = typeParameterType.lookupTag.typeParameterSymbol
    if (!typeParameter.cfir.hasInvalidDeclaredUpperBoundsInCurrentContext()) return null
    if (!visitedTypeParameters.add(typeParameter)) return null

    for (upperBound in typeParameter.cfir.declaredUpperBoundTypesInCurrentContext()) {
        upperBound.findMutFunctionInUpperBoundOrChain(name, visitedTypeParameters)?.let { return it }
    }

    return null
}

/**
 * 在上界类型或后续类型参数上界链中查询 mut 函数。
 */
context(context: CheckerContext)
private fun ConeCangJieType.findMutFunctionInUpperBoundOrChain(
    name: Name,
    visitedTypeParameters: MutableSet<CfirTypeParameterSymbol>,
): CfirNamedFunction? {
    if (this is ConeTypeParameterType) {
        return findMutFunctionInInvalidDeclaredUpperBounds(name, visitedTypeParameters)
    }

    val scope = useSiteMemberScopeForInvalidStructUpperBoundOrNull() ?: return null
    var result: CfirNamedFunction? = null
    scope.processFunctionsByName(name) { functionSymbol ->
        if (result != null) return@processFunctionsByName
        val function = functionSymbol.takeIf { it.isBound }?.cfir as? CfirNamedFunction ?: return@processFunctionsByName
        if (function.status.isMut && !function.status.isConst) {
            result = function
        }
    }
    return result
}

/**
 * 为非法 struct 声明上界构造成员查询 scope；非 struct 上界不参与此错误恢复。
 */
context(context: CheckerContext)
private fun ConeCangJieType.useSiteMemberScopeForInvalidStructUpperBoundOrNull(): CfirTypeScope? {
    val expandedType = fullyExpandedType(context.session)
    if (expandedType !is ConeStructType) return null
    val classSymbol = expandedType.toSymbol(context.session) as? CfirClassLikeSymbol<*> ?: return null
    if (!classSymbol.isBound) return null
    classSymbol.lazyResolveToPhase(CfirResolvePhase.TYPES)

    val rawScope = CfirClassUseSiteMemberScope(
        session = context.session,
        classSymbol = classSymbol,
        symbolProvider = context.session.symbolProvider,
        extendProvider = context.session.extendProvider,
        directSupertypeProvider = context.session.directSupertypeProviderOrNull,
        ownerType = expandedType,
        dispatchReceiverType = expandedType,
        scopeKind = CfirClassMemberScopeKind.USE_SITE,
    )
    return CfirClassSubstitutionScope(
        session = context.session,
        useSiteMemberScope = rawScope,
        dispatchReceiverType = expandedType,
        substitutionOwnerType = expandedType,
    )
}
