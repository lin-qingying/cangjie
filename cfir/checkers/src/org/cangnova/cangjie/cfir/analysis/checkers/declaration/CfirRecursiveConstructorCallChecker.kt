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
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
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
        val cycle = declaration.findRecursiveConstructorCycleOrNull() ?: return
        val firstDeclarationInCycle =
            cycle.declarations.minByOrNull { it.source?.startOffset ?: Int.MAX_VALUE } ?: return
        if (firstDeclarationInCycle.classIdOrNull() != declaration.classIdOrNull()) return

        reporter.reportOn(
            source = cycle.closingCall.calleeReference.source ?: cycle.closingCall.source,
            factory = CfirErrors.RECURSIVE_CONSTRUCTOR_CALL,
        )
    }

    context(context: CheckerContext)
    private fun CfirClassLikeDeclaration.findRecursiveConstructorCycleOrNull(): RecursiveConstructorCycle? {
        val target = classIdOrNull() ?: return null
        return findRecursiveConstructorCycleOrNull(
            target = target,
            path = mutableListOf(),
            visiting = linkedSetOf(),
        )
    }

    context(context: CheckerContext)
    private fun CfirClassLikeDeclaration.findRecursiveConstructorCycleOrNull(
        target: ClassId,
        path: MutableList<CfirClassLikeDeclaration>,
        visiting: MutableSet<ClassId>,
    ): RecursiveConstructorCycle? {
        val currentClassId = classIdOrNull() ?: return null
        if (!visiting.add(currentClassId)) return null
        path += this

        for (member in declarations) {
            for (call in member.constructorCalls()) {
                val targetDeclaration = call.constructedClassLikeDeclarationOrNull() ?: continue
                val targetClassId = targetDeclaration.classIdOrNull() ?: continue
                if (targetClassId == target) {
                    val cycle = RecursiveConstructorCycle(
                        declarations = path.toList(),
                        closingCall = call,
                    )
                    path.removeLast()
                    visiting.remove(currentClassId)
                    return cycle
                }
                if (targetClassId in visiting) continue
                val cycle = targetDeclaration.findRecursiveConstructorCycleOrNull(target, path, visiting)
                if (cycle != null) {
                    path.removeLast()
                    visiting.remove(currentClassId)
                    return cycle
                }
            }
        }

        path.removeLast()
        visiting.remove(currentClassId)
        return null
    }

    context(context: CheckerContext)
    private fun CfirFunctionCall.constructedClassLikeDeclarationOrNull(): CfirClassLikeDeclaration? {
        val symbol = resolvedCallableSymbolOrNull() ?: return null
        val classId = when (symbol) {
            is CfirConstructorSymbol -> symbol.callableId.classId
            is CfirEnumConstructorSymbol -> symbol.callableId.classId
            is CfirClassLikeSymbol<*> -> symbol.classId
            else -> null
        } ?: return null
        return context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir as? CfirClassLikeDeclaration
    }

    private fun CfirDeclaration.constructorCalls(): List<CfirFunctionCall> {
        return when (this) {
            is CfirFieldVariable -> {
                if (status.isStatic) return emptyList()
                initializer?.collectConstructorCalls().orEmpty()
            }

            is CfirConstructor -> body?.collectConstructorCalls().orEmpty()
            else -> emptyList()
        }
    }

    private fun CfirElement.collectConstructorCalls(): List<CfirFunctionCall> {
        val result = mutableListOf<CfirFunctionCall>()
        accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element is CfirFunctionCall && element.resolvedCallableSymbolOrNull().isConstructorLikeSymbol()) {
                    result += element
                }
                element.acceptChildren(this, null)
            }
        }, null)
        return result
    }

    private fun CfirFunctionCall.resolvedCallableSymbolOrNull(): CfirBasedSymbol<*>? =
        when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            is CfirResolvedErrorReference -> reference.resolvedSymbol
            else -> null
        }

    private fun CfirBasedSymbol<*>?.isConstructorLikeSymbol(): Boolean =
        this is CfirConstructorSymbol || this is CfirEnumConstructorSymbol || this is CfirClassLikeSymbol<*>

    private fun CfirClassLikeDeclaration.classIdOrNull(): ClassId? =
        when (this) {
            is CfirClass -> symbol.classId
            is CfirStruct -> symbol.classId
            else -> null
        }

    private data class RecursiveConstructorCycle(
        val declarations: List<CfirClassLikeDeclaration>,
        val closingCall: CfirFunctionCall,
    )
}
