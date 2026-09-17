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

package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext

/**
 * 判断当前检查的声明是否来自声明文件（`.cj.d`）。
 *
 * 取自**元素自身的来源文件**，不读任何会话级开关（`compileCjd` 是会话级布尔，
 * 在 IDE 的"` .cj` 源码 + SDK 的 `.cj.d` 同时可见"混合场景下无法表达逐文件的差异；
 * `CfirSession.Kind` 是 `{Source, Library}`，与声明模式正交）。
 *
 * 回指路径：`CheckerContext.containingFileSymbol` → `CfirFileSymbol.sourceFile` →
 * `CjSourceFile.sourceKind`。`CjSourceFile.sourceKind` 是统一入口——能自证种类的实现
 * （`CjPsiSourceFile` 委托给 PSI 侧的 `CjSourceKindCarrier`，即 `CjFile` 的 FileType）
 * 优先，其余按文件名后缀判定；与解析层同源（同一个 `CjSourceKind`），不产生第二个真源。
 *
 * 供生成的 `DeclarationCheckersDiagnosticComponent` 的统一分派处使用：
 * `if (checker.requiresImplementation && isFromDeclarationFile(context)) continue`。
 */
internal fun isFromDeclarationFile(context: CheckerContext): Boolean =
    context.containingFileSymbol?.sourceFile?.sourceKind?.isDeclaration == true
