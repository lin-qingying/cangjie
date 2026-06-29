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

package org.cangnova.cangjie.psi
/**
 * 提供 `importDirectiveVisitor` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun importDirectiveVisitor(block: (CjImportDirective) -> Unit) =
    /**
     * 提供 `declaration` 单例，集中承载仓颉 PSI的共享状态、工厂或工具行为。
     */
    object : CjVisitorUnit() {
        /**
         * 实现 `visitImportDirective` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun visitImportDirective(importDirective: CjImportDirective) {
            block(importDirective)
        }
    }

/**
 * 提供 `visitDotQualifiedExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun visitDotQualifiedExpression(block: (CjDotQualifiedExpression) -> Unit) =
    /**
     * 提供 `declaration` 单例，集中承载仓颉 PSI的共享状态、工厂或工具行为。
     */
    object : CjVisitorUnit() {
        /**
         * 实现 `visitDotQualifiedExpression` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun visitDotQualifiedExpression(expression: CjDotQualifiedExpression) {
            block(expression)
        }
    }

/**
 * 提供 `packageDirectiveVisitor` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun packageDirectiveVisitor(block: (CjPackageDirective) -> Unit) =
    /**
     * 提供 `declaration` 单例，集中承载仓颉 PSI的共享状态、工厂或工具行为。
     */
    object : CjVisitorUnit() {
        /**
         * 实现 `visitPackageDirective` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun visitPackageDirective(packageDirective: CjPackageDirective) {
            block(packageDirective)
        }
    }
/**
 * 提供 `namedDeclarationVisitor` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun namedDeclarationVisitor(block: (CjNamedDeclaration) -> Unit) =
    /**
     * 提供 `declaration` 单例，集中承载仓颉 PSI的共享状态、工厂或工具行为。
     */
    object : CjVisitorUnit() {
        /**
         * 实现 `visitNamedDeclaration` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun visitNamedDeclaration(declaration: CjNamedDeclaration) {
            block(declaration)
        }
    }

/**
 * 提供 `declarationVisitor` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun declarationVisitor(block: (CjDeclaration) -> Unit) =
    /**
     * 提供 `declaration` 单例，集中承载仓颉 PSI的共享状态、工厂或工具行为。
     */
    object : CjVisitorUnit() {
        /**
         * 实现 `visitDeclaration` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun visitDeclaration(dcl: CjDeclaration) {
            block(dcl)
        }
    }
