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
 */

package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol

/**
 * 已匹配 specific nominal 的声明成员视图。
 *
 * 官方在 `MergeCommonIntoSpecific` 中先把未被替代的 common 声明并入 specific 声明体，
 * 再让常规继承、覆盖和调用解析消费该成员集合。仓颉 CFIR 保留两份物理声明，因此这里
 * 从不可变 family snapshot 取逻辑成员列表，并复用普通 declared-member 索引。
 * Kotlin FIR 没有这一层：Kotlin 的 actual 声明必须实现 expect 声明要求的成员。
 *
 * @param classSymbol specific 的物理 class-like symbol，作为逻辑成员 scope 的 owner。
 * @param declaration 已完成匹配的 specific 物理声明。
 * @param declarations provider 完成的 merged 与 generated 成员输入，包含后置实例默认构造器。
 */
internal class CfirCjmpMergedDeclaredMemberScope(
    classSymbol: CfirClassLikeSymbol<*>,
    declaration: CfirClassLikeDeclaration,
    declarations: List<CfirDeclaration>,
) : CfirClassDeclaredMemberScope(
    classSymbol = classSymbol,
    declarations = declarations,
) {
    init {
        require(classSymbol.cfir === declaration) {
            "A CJMP merged declared scope must use its specific declaration as the physical owner"
        }
    }
}
