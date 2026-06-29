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
 * 定义 `CjCallElement` 接口，约束仓颉 PSI节点或服务需要暴露的结构能力。
 */
interface CjCallElement : CjElement {
    /**
     * 保存 `calleeExpression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val calleeExpression: CjExpression?

    /**
     * 保存 `valueArgumentList`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val valueArgumentList: CjValueArgumentList?

    /**
     * 保存 `valueArguments`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val valueArguments: List<ValueArgument>

    /**
     * 保存 `lambdaArguments`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val lambdaArguments: List<CjLambdaArgument>

    /**
     * 保存 `typeArguments`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeArguments: List<CjTypeProjection>

    /**
     * 保存 `typeArgumentList`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeArgumentList: CjTypeArgumentList?
}
