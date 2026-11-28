package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.CjmpDeclarationRoleResolver
import org.cangnova.cangjie.CjmpLanguageFeature
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.parsing.CjmpNominalSyntaxFacts
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.supportsCjmpFeature

/** 根据解析完成时的直接成员快照计算 common 容器的默认实现属性。 */
fun CjmpNominalSyntaxFacts.hasCjmpCommonDefault(languageVersionSettings: LanguageVersionSettings): Boolean =
    members.all { member ->
        val role = CjmpDeclarationRoleResolver.resolve(
            member.isCommon, member.isSpecific, member.isPlatform, languageVersionSettings,
        )
        !role.isCommon || member.declarationType == CjNodeTypes.ENUM_CONSTRUCTOR || role.isSpecific ||
            when (member.declarationType) {
                CjNodeTypes.FUNC, CjNodeTypes.SECONDARY_CONSTRUCTOR, CjNodeTypes.FINALIZER -> member.hasFunctionBody
                CjNodeTypes.PROPERTY -> member.hasPropertyAccessor
                CjNodeTypes.FIELD -> member.hasInitializer
                CjNodeTypes.PRIMARY_CONSTRUCTOR -> member.hasFunctionBody &&
                    languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.PrimaryConstructorDefaultImplementation)
                else -> false
            }
    }
