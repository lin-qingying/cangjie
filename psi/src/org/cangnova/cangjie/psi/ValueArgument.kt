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
import org.cangnova.cangjie.name.*
import org.cangnova.cangjie.name.*

import com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * 定义 `ValueArgument` 接口，约束仓颉 PSI节点或服务需要暴露的结构能力。
 */
interface ValueArgument {
    /**
     * 提供 `getArgumentExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    @IfNotParsed
    fun getArgumentExpression(): CjExpression?

    /**
     * 提供 `getArgumentName` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getArgumentName(): ValueArgumentName?

    /**
     * 提供 `isNamed` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isNamed(): Boolean

    /**
     * 提供 `asElement` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun asElement(): CjElement

    /* 例如foo(*arr)中的‘*’，即将数组作为多个var arg参数传递*/
    fun getSpreadElement(): LeafPsiElement?

    /* 参数放在外部以调用元素*/
    fun isExternal(): Boolean

    companion object
}

/**
 * 定义 `ValueArgumentName` 接口，约束仓颉 PSI节点或服务需要暴露的结构能力。
 */
interface ValueArgumentName {
    /**
     * 保存 `asName`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val asName: Name
    /**
     * 保存 `referenceExpression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val referenceExpression: CjSimpleNameExpression?
}

/**
 * 定义 `LambdaArgument` 接口，约束仓颉 PSI节点或服务需要暴露的结构能力。
 */
interface LambdaArgument : ValueArgument {
    /**
     * 提供 `getLambdaExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getLambdaExpression(): CjLambdaExpression?
}
