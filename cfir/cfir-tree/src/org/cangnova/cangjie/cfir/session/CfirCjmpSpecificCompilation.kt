/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.CjmpLanguageFeature
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.supportsCjmpDeclarationModel
import org.cangnova.cangjie.supportsCjmpFeature

/** specific 编译期启用 CJMP 类型/候选行为的唯一 session 判据。 */
public fun CfirSession.isCjmpSpecificCompilationEnabled(): Boolean =
    languageVersionSettings.supportsCjmpDeclarationModel() &&
            cjmpSettings.mode == CfirCjmpMode.SPECIFIC

/** alpha.66 的签名匹配必须等声明级隐式类型推断完成；后续版本在早期匹配阶段绑定。 */
public fun CfirSession.requiresDeferredCjmpFinalMatching(): Boolean =
    isCjmpSpecificCompilationEnabled() &&
            !languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.PostMatchReturnTypeCheck)

/** 只有 language-version 启用且本次输入确实包含多个 common parent 时才采用逐 parent 匹配。 */
public fun CfirSession.hasMultipleCjmpCommonParents(): Boolean =
    isCjmpSpecificCompilationEnabled() &&
            languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.MultipleCommonParents) &&
            moduleData.refinementDependencies.size > 1
