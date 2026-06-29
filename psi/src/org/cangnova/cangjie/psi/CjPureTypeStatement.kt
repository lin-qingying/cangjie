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
 * 定义 `CjPureTypeStatement` 接口，约束仓颉 PSI节点或服务需要暴露的结构能力。
 */
interface CjPureTypeStatement : CjPureElement, CjDeclarationContainer {
    /**
     * 提供 `getName` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getName(): String?

    /**
     * 保存 `superTypeListEntries`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val superTypeListEntries: List<CjSuperTypeListEntry>

    /**
     * 提供 `hasExplicitPrimaryConstructor` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun hasExplicitPrimaryConstructor(): Boolean

    /**
     * 提供 `hasPrimaryConstructor` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun hasPrimaryConstructor(): Boolean

    /**
     * 保存 `primaryConstructor`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val primaryConstructor: CjPrimaryConstructor?
    /**
     * 保存 `annotations`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val annotations: CjAnnotations?
    /**
     * 保存 `primaryConstructorModifierList`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val primaryConstructorModifierList: CjModifierList?

    /**
     * 保存 `primaryConstructorParameters`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val primaryConstructorParameters: List<CjParameter>
    /**
     * 保存 `finalizers`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val finalizers: List<CjFinalizer>
    /**
     * 保存 `secondaryConstructors`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val secondaryConstructors: List<CjSecondaryConstructor>
    /**
     * 保存 `primaryConstructors`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val primaryConstructors: List<CjPrimaryConstructor>

    /**
     * 保存 `body`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val body: CjAbstractClassBody?
}
