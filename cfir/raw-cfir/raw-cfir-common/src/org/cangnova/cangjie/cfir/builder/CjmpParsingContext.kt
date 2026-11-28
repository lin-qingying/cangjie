package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.CjmpLanguageFeature
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.supportsCjmpDeclarationModel
import org.cangnova.cangjie.supportsCjmpFeature
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.parsing.CjmpParsingContext

/** 编译入口只转换已确定的调用事实；语法层不依赖 CFIR session 或语义 checker。 */
fun CfirCjmpSettingsComponent.toParsingContext(languageVersionSettings: LanguageVersionSettings): CjmpParsingContext.Compilation =
    CjmpParsingContext.Compilation(
        compileCommon = compileCommon,
        compileSpecific = compileSpecific,
        enabled = languageVersionSettings.supportsCjmpDeclarationModel(),
        mixedCommonSpecificDeclarations = languageVersionSettings.supportsCjmpFeature(
            CjmpLanguageFeature.MixedCommonSpecificDeclarations,
        ),
        commonMemberInSpecificOuter = languageVersionSettings.supportsCjmpFeature(
            CjmpLanguageFeature.CommonMemberInSpecificOuter,
        ),
        genericDeclarationsAvailable = languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.GenericDeclarations),
        parameterDefaultValueRulesAvailable = languageVersionSettings.supportsCjmpFeature(
            CjmpLanguageFeature.ParameterDefaultValueRules,
        ),
        parameterRoleMatching = languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.ParameterRoleMatching),
        modifierSyntaxAvailable = languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.CommonSpecificDeclarations) ||
            languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.PrereleaseSpecificDeclarations),
        platformModifierSyntaxAvailable = languageVersionSettings.supportsCjmpFeature(
            CjmpLanguageFeature.CommonPlatformDeclarations,
        ),
        primaryConstructorRoleModifiersAvailable = languageVersionSettings.supportsCjmpFeature(
            CjmpLanguageFeature.PrimaryConstructorRoleModifiers,
        ),
    )
