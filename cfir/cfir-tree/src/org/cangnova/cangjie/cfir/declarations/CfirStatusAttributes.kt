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
 */

package org.cangnova.cangjie.cfir.declarations

/**
 * 返回声明原始 CFIR 中是否带有 ABSTRACT 属性。
 *
 * STATUS 可以清除冲突修饰符形成的有效 `isAbstract`，但会保留显式源码位；此投影供必须按原始
 * AST 属性判定的诊断使用，避免把 STATUS 的有效模态误当成语法事实。
 */
val CfirDeclarationStatus.hasAbstractAttribute: Boolean
    get() = isAbstract || isAbstractExplicit
