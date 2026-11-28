package org.cangnova.cangjie

/** CJMP 与 CJO 加载协议使用的版本特性。 */
public enum class CjmpLanguageFeature(
    internal val languageFeature: LanguageFeature,
) {
    CommonSpecificDeclarations(LanguageFeature.CommonSpecificDeclarations),
    CommonPlatformDeclarations(LanguageFeature.CjmpCommonPlatformDeclarations),
    PrimaryConstructorRoleModifiers(LanguageFeature.CjmpPrimaryConstructorRoleModifiers),
    PrimaryConstructorDefaultImplementation(LanguageFeature.CjmpPrimaryConstructorDefaultImplementation),
    PrereleaseSpecificDeclarations(LanguageFeature.CjmpPrereleaseSpecificDeclarations),
    MixedCommonSpecificDeclarations(LanguageFeature.CjmpMixedCommonSpecificDeclarations),
    CommonMemberInSpecificOuter(LanguageFeature.CjmpCommonMemberInSpecificOuter),
    GenericDeclarations(LanguageFeature.CjmpGenericDeclarations),
    ParameterDefaultValueRules(LanguageFeature.CjmpParameterDefaultValueRules),
    PostMatchReturnTypeCheck(LanguageFeature.CjmpPostMatchReturnTypeCheck),
    ParameterRoleMatching(LanguageFeature.CjmpParameterRoleMatching),
    MultipleCommonParents(LanguageFeature.CjmpMultipleCommonParents),
    DedicatedCommonPartChirInputs(LanguageFeature.CjmpDedicatedCommonPartChirInputs),
    CommonPartFeaturesAndOptionsGate(LanguageFeature.CjmpCommonPartFeaturesAndOptionsGate),
    CjoFormatVersionCompatibility(LanguageFeature.CjoFormatVersionCompatibility),
}

/**
 * CJMP/CJO 版本行为的唯一语言特性入口。
 *
 * 调用方表达所需协议能力，不自行选择底层 LanguageFeature 或直接查询版本设置，
 * 以便精确预发布版本边界始终由同一映射决定。
 */
public object CjmpLanguageFeatureGate {
    public fun supports(
        languageVersionSettings: LanguageVersionSettings,
        feature: CjmpLanguageFeature,
    ): Boolean =
        languageVersionSettings.featureSupportStatus(feature.languageFeature) == LanguageFeatureSupportStatus.SUPPORTED

    public fun languageFeature(feature: CjmpLanguageFeature): LanguageFeature = feature.languageFeature
}

/**
 * CJMP 声明语义在早期 alpha、预发布 specific 与稳定 common/specific 表面的统一能力门。
 *
 * 共同的用户覆盖始终优先；可用的声明角色语法由各自的精确版本 feature 提供。
 */
public fun LanguageVersionSettings.supportsCjmpDeclarationModel(): Boolean =
    getCustomizedLanguageFeatures()[LanguageFeature.CommonSpecificDeclarations] != LanguageFeature.State.DISABLED &&
            (supportsCjmpFeature(CjmpLanguageFeature.CommonSpecificDeclarations) ||
                    supportsCjmpFeature(CjmpLanguageFeature.CommonPlatformDeclarations) ||
                    supportsCjmpFeature(CjmpLanguageFeature.PrereleaseSpecificDeclarations))

public fun LanguageVersionSettings.supportsCjmpFeature(feature: CjmpLanguageFeature): Boolean =
    CjmpLanguageFeatureGate.supports(this, feature)
