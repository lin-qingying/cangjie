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

package org.cangnova.cangjie.cfir.tree.generator

import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.model.Implementation
import org.cangnova.cangjie.generators.tree.config.AbstractBuilderConfigurator

class BuilderConfigurator(model: Model) : AbstractBuilderConfigurator<Element, Implementation, Field>(model) {
    override val namePrefix: String
        get() = "Cfir"

    override val defaultBuilderPackage: String
        get() = "org.cangnova.cangjie.cfir.tree.builder"

    override fun configureBuilders() = with(CfirTree) {
        concreteElements().forEach { element ->
            builder(element) {
                withCopy()
            }
        }

        // constructor 有两个具名实现，需要分别指定
        builder(constructor, "CfirConstructorImpl") {
            withCopy()
        }
        builder(constructor, "CfirPrimaryConstructor") {
            withCopy()
        }
        val callBuilder by builder {
            fields from call
        }
        val qualifiedAccessExpressionBuilder by builder {
            fields from qualifiedAccessExpression without "calleeReference"
        }
        val abstractFunctionCallBuilder by builder {
            parents += qualifiedAccessExpressionBuilder
            parents += callBuilder
            fields from functionCall
        }
        val configurationForFunctionCallBuilder: LeafBuilderConfigurationContext.() -> Unit = {
            parents += abstractFunctionCallBuilder
            defaultNoReceivers()

            openBuilder()
            default("argumentList") {
                value = "CfirEmptyArgumentList"
            }
            additionalImports(emptyArgumentListType)
        }
        builder(argumentList) {
            withCopy()
        }
        builder(functionCall) {
            configurationForFunctionCallBuilder()
            default("origin") {
                value = "CfirFunctionCallOrigin.Regular"
            }
            defaultFalse("hasTrailingLambda")

            withCopy()
        }
        // resolvedImportDirective 单独配置
        builder(resolvedImportDirective) {
            withCopy()
        }

        builder(property) {
            default("bodyResolveState", "CfirPropertyBodyResolveState.NOTHING_RESOLVED")
        }

        builder(matchExpression) {
            default("exhaustiveness", "CfirMatchExhaustivenessStatus.Unknown")
        }

        builder(valueParameter) {
            default("status", "DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS")
            additionalImports(defaultStatusForStatuslessDeclarationsType)
        }

        // lazyBlock / lazyExpression 不需要 builder（占位节点，不对外构造）


        configureFieldInAllLeafBuilders(
            field = "resolvePhase",
            builderPredicate = { it.wantsCopy },
        ) {
            additionalImports(resolvePhaseExtensionImport)
        }

        // deprecationsProvider: source 构建路径统一默认 UnresolvedDeprecationProvider,
        // 反序列化路径会覆写为具体 provider。对齐 Kotlin FIR 同机制。
        configureFieldInAllLeafBuilders(
            field = "deprecationsProvider",
        ) {
            default("deprecationsProvider", "UnresolvedDeprecationProvider")
        }

        // TypeRef 体系默认不启用 custom renderer。
        // 这里在所有落地 builder 上统一施加默认值，确保生成代码与运行时构造约定一致。
        configureFieldInAllLeafBuilders(
            field = "customRenderer",
        ) {
            defaultFalse("customRenderer")
        }
    }

    private fun CfirTree.concreteElements(): List<Element> = listOf(
        packageDirective,
        // importDirective 已改为抽象基类，由 resolvedImportDirective 替代
        // constructor 已单独配置为两个具名实现

        // -------- 声明节点 --------
        file, classDeclaration, interfaceDeclaration,structDeclaration,enumDeclaration,  enumConstructor, extend, typeAlias, namedFunction, anonymousFunction, mainFunction, macroDeclaration, finalizer,
        codeFragment, invalidDeclaration, property, propertyAccessor, fieldVariable, patternVariable, patternBindingVariable, valueParameter, typeParameter,

        // -------- 语句 / 表达式节点 --------
        block,
        literalExpression, stringInterpolation, functionCall, namedAccessExpression, qualifiedAccessExpression, assignment, binaryOp,
        comparisonExpression,
        typeOperator,
        typeConversion,
        letPatternExpression,
        ifExpression,
        matchExpression,
        matchBranch,
        catchPattern,
        catchClause,
        loopExpression,
        forInExpression,
        tryExpression,
        throwExpression, returnExpression, breakExpression, continueExpression, anonymousFunctionExpression, rangeExpression, arrayLiteral, tupleLiteral,
        spawnExpression, synchronizedExpression, unsafeExpression, quoteExpression, subscriptExpression, errorExpression,
        inoutArgumentExpression,

        // -------- 模式节点 --------
        constPattern, wildcardPattern, bindingPattern, varOrEnumPattern, tuplePattern, enumPattern, typePattern,

        // -------- 类型引用节点 --------
        qualifierPart, resolvedTypeRef, userTypeRef, basicTypeRef, implicitTypeRef, functionTypeRef, tupleTypeRef, varrayTypeRef,

        // -------- 引用节点 --------
        namedReference, resolvedNamedReference, errorReference, thisReference,
    )

    protected fun BuilderConfigurationContext.defaultNoReceivers(notNullExplicitReceiver: Boolean = false) {
        if (!notNullExplicitReceiver) {
            defaultNull("explicitReceiver")
        }
        defaultNull("dispatchReceiver")
    }
}
