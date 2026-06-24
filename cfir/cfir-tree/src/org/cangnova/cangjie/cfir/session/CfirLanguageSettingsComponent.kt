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

package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.LanguageVersionSettings

/**
 * 语言配置 session 组件。
 *
 * 承载编译器特性开关、目标平台配置、实验性功能控制等。
 * 对齐 K2 的 FirLanguageSettingsComponent，但移除了 isMetadataCompilation
 * （仓颉没有独立的 metadata 编译模式）。
 *
 * @property languageVersionSettings 当前 session 使用的语言版本配置。
 */
class CfirLanguageSettingsComponent(
    val languageVersionSettings: LanguageVersionSettings,
) : CfirSessionComponent

/**
 * prelude 导入策略 session 组件。
 *
 * @property noPrelude 为 `true` 时禁用默认 prelude 注入。
 */
class CfirPreludeSettingsComponent(
    val noPrelude: Boolean,
) : CfirSessionComponent

/**
 * 程序入口检查配置。
 *
 * 仓颉官方编译器只在可执行编译目标中检查缺失 `main`，IDE/type-check 场景不启用。
 *
 * @property checkProgramEntry 是否执行程序入口检查。
 */
class CfirProgramEntrySettingsComponent(
    val checkProgramEntry: Boolean,
) : CfirSessionComponent

/**
 * 当前 session 的语言配置组件。
 */
private val CfirSession.languageSettingsComponent: CfirLanguageSettingsComponent
    by CfirSession.sessionComponentAccessor()

/**
 * 当前 session 的 prelude 配置组件；未注册时表示采用默认 prelude 行为。
 */
private val CfirSession.preludeSettingsComponent: CfirPreludeSettingsComponent?
    by CfirSession.nullableSessionComponentAccessor()

/**
 * 当前 session 的程序入口检查配置组件；未注册时表示不检查入口。
 */
private val CfirSession.programEntrySettingsComponent: CfirProgramEntrySettingsComponent?
        by CfirSession.nullableSessionComponentAccessor()

/**
 * 当前 session 的语言版本设置。
 */
val CfirSession.languageVersionSettings: LanguageVersionSettings
    get() = languageSettingsComponent.languageVersionSettings

/**
 * 当前 session 是否禁用默认 prelude。
 */
val CfirSession.noPrelude: Boolean
    get() = preludeSettingsComponent?.noPrelude == true

/**
 * 当前 session 是否检查程序入口。
 */
val CfirSession.checkProgramEntry: Boolean
    get() = programEntrySettingsComponent?.checkProgramEntry == true
