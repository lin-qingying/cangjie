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

package org.cangnova.cangjie

/** 由版本化 CJMP 修饰符解析出的声明角色。 */
public data class CjmpDeclarationRole(
    /** 声明使用 common 角色。 */
    public val isCommon: Boolean,
    /** 声明使用 specific 角色；早期 alpha 的 platform 修饰符映射到此角色。 */
    public val isSpecific: Boolean,
)

/** PSI 与 LightTree 共用的 CJMP 声明角色映射。 */
public object CjmpDeclarationRoleResolver {
    /**
     * 将当前语言版本可用的角色修饰符映射到 CFIR 的稳定 common/specific 模型。
     *
     * alpha.34 至 beta.20 的源码拼写 `platform` 与后续 `specific` 表达同一个声明角色；
     * 该映射由版本 feature 决定，不能仅凭 token 名称推断。
     */
    public fun resolve(
        hasCommonModifier: Boolean,
        hasSpecificModifier: Boolean,
        hasPlatformModifier: Boolean,
        languageVersionSettings: LanguageVersionSettings,
    ): CjmpDeclarationRole = CjmpDeclarationRole(
        isCommon = hasCommonModifier,
        isSpecific = hasSpecificModifier ||
                (hasPlatformModifier && languageVersionSettings.supportsCjmpFeature(
                    CjmpLanguageFeature.CommonPlatformDeclarations,
                )),
    )
}
