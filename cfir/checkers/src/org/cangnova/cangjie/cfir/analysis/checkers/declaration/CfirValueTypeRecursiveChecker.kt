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

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId

/**
 * 值类型递归检查器。
 *
 * 对齐官方 `CheckValueTypeRecursive`：
 * - 只检查 struct 与非 ref enum 这类值语义类型；
 * - struct 只沿非 static 字段递归；
 * - enum 只沿构造器参数递归；
 * - tuple 与 VArray 按元素类型继续展开；
 * - 只有环内全部节点都是 struct 时报告，enum 只参与遍历不触发递归值类型诊断；
 * - class、interface、ref enum 等引用语义类型不会构成值类型递归。
 */
object CfirValueTypeRecursiveChecker : CfirClassLikeChecker() {
    /**
     * 检查当前值类型声明是否参与递归值类型环。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (!declaration.isValueTypeDeclaration()) return
        val cycle = declaration.recursiveValueTypeCycleOrNull() ?: return
        if (!cycle.hasOnlyStructDeclarations()) return
        val firstDeclarationInCycle = cycle.minByOrNull { it.source?.startOffset ?: Int.MAX_VALUE } ?: return
        if (firstDeclarationInCycle.valueTypeClassId() != declaration.valueTypeClassId()) return

        reporter.reportOn(
            source = declaration.source?.firstCharacterDiagnosticSource(),
            factory = CfirErrors.VALUE_TYPE_RECURSIVE,
        )
    }

    /**
     * 从当前值类型声明出发查找递归环。
     */
    context(context: CheckerContext)
    private fun CfirClassLikeDeclaration.recursiveValueTypeCycleOrNull(): List<CfirClassLikeDeclaration>? {
        val target = valueTypeClassId() ?: return null
        return recursiveValueTypeCycleOrNull(
            target = target,
            path = mutableListOf(),
            visiting = linkedSetOf(),
        )
    }

    /**
     * 递归遍历值类型成员图，查找回到 [target] 的声明路径。
     */
    context(context: CheckerContext)
    private fun CfirClassLikeDeclaration.recursiveValueTypeCycleOrNull(
        target: ClassId,
        path: MutableList<CfirClassLikeDeclaration>,
        visiting: MutableSet<ClassId>,
    ): List<CfirClassLikeDeclaration>? {
        val currentClassId = valueTypeClassId() ?: return null
        if (!visiting.add(currentClassId)) {
            return if (currentClassId == target) path.toList() else null
        }
        path += this

        for (type in valueTypeMemberTypes()) {
            val cycle = type.recursiveValueTypeCycleOrNull(target, path, visiting)
            if (cycle != null) {
                path.removeLast()
                visiting.remove(currentClassId)
                return cycle
            }
        }
        path.removeLast()
        visiting.remove(currentClassId)
        return null
    }

    /**
     * 沿类型结构继续查找值类型递归环。
     *
     * struct、非 ref enum、tuple、VArray 会继续展开；引用语义 class-like 类型直接停止。
     */
    context(context: CheckerContext)
    private fun ConeCangJieType.recursiveValueTypeCycleOrNull(
        target: ClassId,
        path: MutableList<CfirClassLikeDeclaration>,
        visiting: MutableSet<ClassId>,
    ): List<CfirClassLikeDeclaration>? {
        return when (val expanded = fullyExpandedType(context.session)) {
            is ConeStructType -> {
                val declaration = expanded.classId.valueTypeDeclarationOrNull() ?: return null
                declaration.recursiveValueTypeCycleOrNull(target, path, visiting)
            }

            is ConeEnumType -> {
                if (expanded.isRefEnum) return null
                val declaration = expanded.classId.valueTypeDeclarationOrNull() ?: return null
                declaration.recursiveValueTypeCycleOrNull(target, path, visiting)
            }

            is ConeTupleType -> expanded.elementTypes.firstNotNullOfOrNull {
                it.recursiveValueTypeCycleOrNull(target, path, visiting)
            }

            is ConeVArrayType -> expanded.elementType.recursiveValueTypeCycleOrNull(target, path, visiting)
            is ConeClassLikeType -> null
            else -> null
        }
    }

    /**
     * 收集当前值类型声明中会参与递归检查的成员类型。
     */
    context(context: CheckerContext)
    private fun CfirClassLikeDeclaration.valueTypeMemberTypes(): List<ConeCangJieType> {
        return when (this) {
            is CfirStruct -> declarations.mapNotNull { declaration ->
                val field = declaration as? CfirFieldVariable ?: return@mapNotNull null
                if (field.status.isStatic) return@mapNotNull null
                field.returnTypeRef.resolvedConeTypeOrNull()
            }

            is CfirEnum -> {
                if (isRefEnum) return emptyList()
                declarations.flatMap { declaration ->
                    val constructor = declaration as? CfirEnumConstructor ?: return@flatMap emptyList()
                    constructor.valueParameters.mapNotNull { valueParameter ->
                        valueParameter.resolvedConeTypeOrNull()
                    }
                }
            }

            else -> emptyList()
        }
    }

    /**
     * 判断声明是否为值语义类型声明。
     */
    private fun CfirClassLikeDeclaration.isValueTypeDeclaration(): Boolean =
        this is CfirStruct || this is CfirEnum && !isRefEnum

    /**
     * 官方 `HasOnlyStructTypeInCycle` 只对全 struct 环报值类型递归。
     *
     * 非 ref enum 的构造器参数仍需要被遍历，以便发现 enum 后方的 struct 环；
     * 但只要实际环中含 enum，官方不会报 `sema_value_type_recursive`。
     */
    private fun List<CfirClassLikeDeclaration>.hasOnlyStructDeclarations(): Boolean =
        all { it is CfirStruct }

    /**
     * 返回值语义类型声明的 ClassId。
     */
    private fun CfirClassLikeDeclaration.valueTypeClassId(): ClassId? =
        when (this) {
            is CfirStruct -> symbol.classId
            is CfirEnum -> if (isRefEnum) null else symbol.classId
            else -> null
        }

    /**
     * 按 ClassId 查找值语义 class-like 声明。
     */
    context(context: CheckerContext)
    private fun ClassId.valueTypeDeclarationOrNull(): CfirClassLikeDeclaration? {
        val declaration = context.session.symbolProvider.getClassLikeSymbolByClassId(this)?.cfir
                as? CfirClassLikeDeclaration ?: return null
        return declaration.takeIf { it.isValueTypeDeclaration() }
    }

    /**
     * 从类型引用中读取已解析 cone 类型。
     */
    private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.resolvedConeTypeOrNull(): ConeCangJieType? =
        (this as? CfirResolvedTypeRef)?.coneType

    /**
     * 从值参数返回类型引用中读取已解析 cone 类型。
     */
    private fun CfirValueParameter.resolvedConeTypeOrNull(): ConeCangJieType? =
        returnTypeRef.resolvedConeTypeOrNull()
}
