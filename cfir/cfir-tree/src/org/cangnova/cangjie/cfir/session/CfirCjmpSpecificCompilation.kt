/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase

/** specific 编译期启用 CJMP 类型/候选行为的唯一 session 判据。 */
public fun CfirSession.isCjmpSpecificCompilationEnabled(): Boolean =
    languageVersionSettings.supportsFeature(LanguageFeature.CommonSpecificDeclarations) &&
            cjmpSettings.mode == CfirCjmpMode.SPECIFIC

/**
 * 返回当前 session 在进入函数体解析前必须达到的阶段。
 *
 * CJMP 只在启用 specific 编译时属于 body resolve 的前置阶段；关闭 CJMP 门时，
 * [CfirResolvePhase.IMPLICIT_TYPES] 已提供完整的声明签名边界。
 */
public val CfirSession.bodyResolvePrerequisitePhase: CfirResolvePhase
    get() = if (isCjmpSpecificCompilationEnabled()) {
        CfirResolvePhase.CJMP_MATCHING
    } else {
        CfirResolvePhase.IMPLICIT_TYPES
    }

/**
 * 判断 common 成员是否已由当前 specific session 中成功配对的 specific 成员替代。
 *
 * 只有对应项从配对存储中可见时才过滤 common 成员；未配对的 common 默认实现仍参与普通查找。
 */
public fun CfirSession.isCjmpShadowedCommonDeclaration(declaration: CfirDeclaration): Boolean {
    if (!isCjmpSpecificCompilationEnabled()) return false
    val member = declaration as? CfirMemberDeclaration ?: return false
    if (!member.status.isCommon) return false
    val storage = cjmpMappingStorage
    return storage.specificBindingsFor(declaration).any { specific ->
        storage.commonCounterpartsFor(specific).any { common -> common === declaration }
    }
}
