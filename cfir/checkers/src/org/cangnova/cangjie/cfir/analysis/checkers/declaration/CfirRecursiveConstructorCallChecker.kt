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

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.name.ClassId

/**
 * 构造递归检查器。
 *
 * 对齐官方 `CheckRecursiveConstructorCall`：
 * - 从 class/struct 体内的非 static 字段和实例构造器出发；
 * - 在初始化器/构造器体中递归扫描构造调用；
 * - 一旦构造依赖回到当前类型，报告闭环上的构造调用；
 * - 同一个 cycle 只由源文件中最早的类型声明报告，保持官方全局 DFS 的单报语义。
 */
object CfirRecursiveConstructorCallChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration !is CfirClass && declaration !is CfirStruct) return
        val cycle = RecursiveConstructorGraph(context).findCycleOrNull(declaration) ?: return
        val reportingDeclaration =
            cycle.declarations.maxByOrNull { it.source?.startOffset ?: Int.MIN_VALUE } ?: return
        if (reportingDeclaration.classIdOrNull() != declaration.classIdOrNull()) return

        reporter.reportOn(
            source = cycle.head.diagnosticSource(context),
            factory = CfirErrors.RECURSIVE_CONSTRUCTOR_CALL,
        )
    }
}

/**
 * 官方递归构造检查的图节点是“具体构造器声明”和“构造器引用”，不是类本身。
 *
 * 每个实例构造器依赖：
 * - 所属类型的非 static 字段初始化器中的构造调用；
 * - 构造器体中直接求值路径上的构造调用。
 *
 * 构造器引用再依赖它解析到的具体目标构造器。这样 `this(x)` 指向同类另一个
 * 非递归构造器时不会被误判为回到当前类。
 */
private class RecursiveConstructorGraph(
    private val context: CheckerContext,
) {
    private val finishedConstructors = mutableSetOf<CfirConstructor>()
    private val visitingConstructors = linkedSetOf<CfirConstructor>()
    private val finishedCalls = mutableSetOf<CfirFunctionCall>()
    private val visitingCalls = linkedSetOf<CfirFunctionCall>()
    private val path = mutableListOf<RecursiveConstructorNode>()
    private val initializerCallsByClassId = mutableMapOf<ClassId, List<CfirFunctionCall>>()

    fun findCycleOrNull(declaration: CfirClassLikeDeclaration): RecursiveConstructorCycle? {
        for (member in declaration.declarations) {
            when (member) {
                is CfirFieldVariable -> {
                    if (member.status.isStatic) continue
                    for (call in member.initializer?.collectConstructorDependencyCalls().orEmpty()) {
                        visitConstructorCall(call)?.let { return it }
                    }
                }

                is CfirConstructor -> visitConstructor(member)?.let { return it }
                else -> Unit
            }
        }
        return null
    }

    private fun visitConstructor(constructor: CfirConstructor): RecursiveConstructorCycle? {
        if (constructor in finishedConstructors) return null

        path += RecursiveConstructorNode.Constructor(constructor)
        if (!visitingConstructors.add(constructor)) {
            return buildCycleFor(constructor)
        }

        for (call in constructor.dependencyCalls()) {
            visitConstructorCall(call)?.let { return it }
        }
        constructor.implicitSuperConstructorDependencyOrNull()?.let { superConstructor ->
            visitConstructor(superConstructor)?.let { return it }
        }

        visitingConstructors.remove(constructor)
        finishedConstructors += constructor
        path.removeLast()
        return null
    }

    private fun visitConstructorCall(call: CfirFunctionCall): RecursiveConstructorCycle? {
        if (call in finishedCalls) return null
        val targetConstructor = call.targetConstructorOrNull() ?: return null

        path += RecursiveConstructorNode.Call(call)
        if (!visitingCalls.add(call)) {
            return buildCycleFor(call)
        }

        if (targetConstructor in visitingConstructors) {
            return buildCycleFor(call)
        }

        val cycle = visitConstructor(targetConstructor)
        if (cycle != null) return cycle

        visitingCalls.remove(call)
        finishedCalls += call
        path.removeLast()
        return null
    }

    private fun buildCycleFor(constructor: CfirConstructor): RecursiveConstructorCycle {
        val startIndex = path.indexOfFirst { node ->
            node is RecursiveConstructorNode.Constructor && node.declaration === constructor
        }.takeIf { it >= 0 } ?: 0
        val cyclePath = path.drop(startIndex)
        return RecursiveConstructorCycle(
            head = RecursiveConstructorNode.Constructor(constructor),
            declarations = cyclePath.ownerDeclarations(),
        )
    }

    private fun buildCycleFor(call: CfirFunctionCall): RecursiveConstructorCycle {
        val startIndex = path.indexOfFirst { node ->
            node is RecursiveConstructorNode.Call && node.call === call
        }.takeIf { it >= 0 } ?: 0
        val cyclePath = path.drop(startIndex)
        return RecursiveConstructorCycle(
            head = RecursiveConstructorNode.Call(call),
            declarations = cyclePath.ownerDeclarations(),
        )
    }

    private fun CfirConstructor.dependencyCalls(): List<CfirFunctionCall> {
        val ownerInitializerCalls = ownerClassLikeDeclarationOrNull()?.initializerConstructorCalls().orEmpty()
        val bodyCalls = body?.collectConstructorDependencyCalls().orEmpty()
        return ownerInitializerCalls + bodyCalls
    }

    private fun CfirConstructor.implicitSuperConstructorDependencyOrNull(): CfirConstructor? {
        if (body?.statements?.firstOrNull().constructorDelegationCallOrNull() != null) return null
        val owner = ownerClassLikeDeclarationOrNull() as? CfirClass ?: return null
        val superDeclaration = owner.directConcreteSuperDeclaration(
            context = context,
            includeLoopInSupertypeError = true,
        ) ?: return null
        return superDeclaration.declarations
            .filterIsInstance<CfirConstructor>()
            .firstOrNull { constructor -> constructor.requiredParameterCount() == 0 }
    }

    private fun CfirClassLikeDeclaration.initializerConstructorCalls(): List<CfirFunctionCall> {
        val classId = classIdOrNull() ?: return emptyList()
        return initializerCallsByClassId.getOrPut(classId) {
            declarations
                .filterIsInstance<CfirFieldVariable>()
                .filterNot { field -> field.status.isStatic }
                .flatMap { field -> field.initializer?.collectConstructorDependencyCalls().orEmpty() }
        }
    }

    private fun CfirElement.collectConstructorDependencyCalls(): List<CfirFunctionCall> {
        val result = mutableListOf<CfirFunctionCall>()
        accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element is CfirFunctionCall && element.targetConstructorOrNull() != null) {
                    result += element
                }
                element.acceptChildren(this, null)
            }

            override fun visitReturnExpression(returnExpression: CfirReturnExpression) {
                // 构造器 return 表达式中的构造调用已经处在非法类型位置，官方不把它作为递归构造依赖边。
            }

            override fun visitFunction(function: CfirFunction) {
                // 局部函数体不是当前构造器的直接求值路径。
            }

            override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) {
                // lambda/匿名函数体延迟执行，不参与当前构造递归依赖图。
            }
        }, null)
        return result
    }

    private fun CfirFunctionCall.targetConstructorOrNull(): CfirConstructor? {
        val symbol = resolvedConstructorSymbolOrNull() ?: return null
        return symbol.cfir as? CfirConstructor
    }

    private fun CfirFunctionCall.resolvedConstructorSymbolOrNull(): CfirConstructorSymbol? =
        when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirConstructorSymbol
            else -> null
        }

    private fun CfirConstructor.ownerClassLikeDeclarationOrNull(): CfirClassLikeDeclaration? {
        val classId = symbol.callableId.classId ?: return null
        return context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir as? CfirClassLikeDeclaration
    }

    private fun CfirClassLikeDeclaration.classIdOrNull(): ClassId? =
        when (this) {
            is CfirClass -> symbol.classId
            is CfirStruct -> symbol.classId
            else -> null
        }

    private fun List<RecursiveConstructorNode>.ownerDeclarations(): List<CfirClassLikeDeclaration> =
        mapNotNullTo(mutableListOf()) { node ->
            when (node) {
                is RecursiveConstructorNode.Constructor -> node.declaration.ownerClassLikeDeclarationOrNull()
                is RecursiveConstructorNode.Call -> node.call.targetConstructorOrNull()?.ownerClassLikeDeclarationOrNull()
            }
        }
            .distinctBy { declaration -> declaration.classIdOrNull() }

}

private sealed class RecursiveConstructorNode {
    class Constructor(val declaration: CfirConstructor) : RecursiveConstructorNode()
    class Call(val call: CfirFunctionCall) : RecursiveConstructorNode()
}

private data class RecursiveConstructorCycle(
    val head: RecursiveConstructorNode,
    val declarations: List<CfirClassLikeDeclaration>,
)

private fun RecursiveConstructorNode.diagnosticSource(context: CheckerContext) = when (this) {
    is RecursiveConstructorNode.Constructor -> declaration.recursiveConstructorDiagnosticSource(context)
    is RecursiveConstructorNode.Call -> call.calleeReference.source ?: call.source
}

private fun CfirConstructor.recursiveConstructorDiagnosticSource(context: CheckerContext) =
    implicitPrimaryConstructorOwner(context)?.classLikeNameDiagnosticSource()
        ?: constructorNameDiagnosticSource()

private fun CfirConstructor.implicitPrimaryConstructorOwner(context: CheckerContext): CfirClassLikeDeclaration? {
    val owner = ownerClassLikeDeclarationOrNull(context) ?: return null
    val constructorSource = source ?: return null
    val ownerSource = owner.source ?: return null
    return owner.takeIf {
        constructorSource.startOffset == ownerSource.startOffset &&
                constructorSource.endOffset == ownerSource.endOffset
    }
}

private fun CfirConstructor.ownerClassLikeDeclarationOrNull(context: CheckerContext): CfirClassLikeDeclaration? {
    val classId = symbol.callableId.classId ?: return null
    return context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir as? CfirClassLikeDeclaration
}

private fun CfirClassLikeDeclaration.classIdOrNull(): ClassId? =
    when (this) {
        is CfirClass -> symbol.classId
        is CfirStruct -> symbol.classId
        else -> null
    }
