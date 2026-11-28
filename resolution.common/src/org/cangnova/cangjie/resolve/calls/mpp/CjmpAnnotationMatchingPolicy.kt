package org.cangnova.cangjie.resolve.calls.mpp

import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjiePackageDirectiveKind
import org.cangnova.cangjie.annotations.officialKind

/** 注解在 CJMP 兼容性流水线中的共享分类。 */
public enum class CjmpAnnotationMatchingDisposition {
    /** 参与 common/specific 注解身份与实参的一对一比较。 */
    MATCH,

    /** 由 CJO 声明属性位承载，比较时不读取普通注解向量。 */
    DECLARATION_ATTRIBUTE,

    /** 由 Deprecated、Attribute 等专用序列化/语义规则处理。 */
    SPECIAL_HANDLING,

    /** 官方禁止出现在 common/specific 声明上的注解 kind。 */
    UNSUPPORTED_ON_COMMON_SPECIFIC,
}

/** E2 的匹配入口与 E7 的声明合法性共同消费的注解分类事实。 */
public data class CjmpAnnotationClassification(
    val matchingDisposition: CjmpAnnotationMatchingDisposition,
)

/**
 * CJMP 注解比较中由声明属性、互操作语义或官方不支持规则处理的注解 kind。
 *
 * 该策略与共享 CJMP matcher 同属 resolution.common；具体前端只提供语言版本和注解 kind，
 * 不在注解模型模块中承载跨平台匹配策略。
 */
object CjmpAnnotationMatchingPolicy {
    private val matchable = CjmpAnnotationClassification(CjmpAnnotationMatchingDisposition.MATCH)
    private val specialHandling = CjmpAnnotationClassification(CjmpAnnotationMatchingDisposition.SPECIAL_HANDLING)
    private val unsupportedOnCommonSpecific = CjmpAnnotationClassification(
        CjmpAnnotationMatchingDisposition.UNSUPPORTED_ON_COMMON_SPECIFIC,
    )

    /** 返回指定版本中以 CJO declaration attributes 表示的注解身份及其版本边界。 */
    fun nonSerializedAnnotationAttributeKinds(
        languageVersion: LanguageVersion,
    ): List<BuiltInAnnotationKind> = BuiltInAnnotationKind.values().filter { kind ->
        classification(kind, languageVersion).matchingDisposition == CjmpAnnotationMatchingDisposition.DECLARATION_ATTRIBUTE
    }

    /**
     * 按官方版本表分类 AST 注解 kind。
     *
     * v1.1.3 的属性/禁止集合取自官方 CheckCJMPAnnotations.cpp；alpha.20 与 alpha.02 的边界
     * 由本地官方版本取证记录确定。语言层会先门控不支持的 CJMP 版本。
     */
    fun classification(
        kind: BuiltInAnnotationKind,
        languageVersion: LanguageVersion,
    ): CjmpAnnotationClassification = when (kind) {
        BuiltInAnnotationKind.C,
        BuiltInAnnotationKind.JAVA_HAS_DEFAULT,
        BuiltInAnnotationKind.OBJ_C_MIRROR,
        -> CjmpAnnotationClassification(CjmpAnnotationMatchingDisposition.DECLARATION_ATTRIBUTE)

        BuiltInAnnotationKind.JAVA_MIRROR ->
            if (languageVersion < LanguageVersion.CANGJIE_1_3_0_ALPHA_02) {
                CjmpAnnotationClassification(CjmpAnnotationMatchingDisposition.DECLARATION_ATTRIBUTE)
            } else {
                matchable
            }

        BuiltInAnnotationKind.OBJ_C_INIT,
        BuiltInAnnotationKind.OBJ_C_OPTIONAL,
        -> if (languageVersion >= LanguageVersion.CANGJIE_1_2_0_ALPHA_20) {
            CjmpAnnotationClassification(CjmpAnnotationMatchingDisposition.DECLARATION_ATTRIBUTE)
        } else {
            matchable
        }

        BuiltInAnnotationKind.DEPRECATED,
        BuiltInAnnotationKind.ATTRIBUTE,
        -> specialHandling

        BuiltInAnnotationKind.JAVA,
        BuiltInAnnotationKind.CALLING_CONV,
        BuiltInAnnotationKind.CONSTSAFE,
        BuiltInAnnotationKind.ENSURE_PREPARED_TO_MOCK,
        -> unsupportedOnCommonSpecific

        BuiltInAnnotationKind.FOREIGN_GETTER_NAME,
        BuiltInAnnotationKind.FOREIGN_SETTER_NAME,
        BuiltInAnnotationKind.NON_PRODUCT,
        -> if (languageVersion >= LanguageVersion.CANGJIE_1_2_0_ALPHA_20) {
            unsupportedOnCommonSpecific
        } else {
            matchable
        }

        BuiltInAnnotationKind.JAVA_IMPL,
        BuiltInAnnotationKind.OBJ_C_IMPL,
        BuiltInAnnotationKind.FOREIGN_NAME,
        BuiltInAnnotationKind.NUMERIC_OVERFLOW,
        BuiltInAnnotationKind.INTRINSIC,
        BuiltInAnnotationKind.WHEN,
        BuiltInAnnotationKind.FASTNATIVE,
        BuiltInAnnotationKind.FROZEN,
        BuiltInAnnotationKind.ANNOTATION,
        -> matchable
    }

    /** 将源/二进制注解身份投影到同一版本分类；未知身份对应官方 UNKNOWN kind。 */
    fun classification(
        identity: CangjieAnnotationIdentity?,
        languageVersion: LanguageVersion,
    ): CjmpAnnotationClassification = when (identity) {
        is CangjieAnnotationIdentity.LanguageBuiltIn -> classification(identity.kind, languageVersion)
        is CangjieAnnotationIdentity.PlatformDerived -> classification(identity.kind.officialKind, languageVersion)
        is CangjieAnnotationIdentity.PackageDirective -> when (identity.kind) {
            CangjiePackageDirectiveKind.NON_PRODUCT -> classification(BuiltInAnnotationKind.NON_PRODUCT, languageVersion)
        }
        CangjieAnnotationIdentity.Unknown -> unsupportedOnCommonSpecific
        is CangjieAnnotationIdentity.Custom,
        is CangjieAnnotationIdentity.SystemMacro,
        is CangjieAnnotationIdentity.SpecialExpression,
        null,
        -> matchable
    }

    fun isDisallowedOnCommonSpecific(
        identity: CangjieAnnotationIdentity?,
        languageVersion: LanguageVersion,
    ): Boolean = classification(identity, languageVersion).matchingDisposition ==
            CjmpAnnotationMatchingDisposition.UNSUPPORTED_ON_COMMON_SPECIFIC

    fun isHandledOutsideAnnotationMatching(
        kind: BuiltInAnnotationKind,
        languageVersion: LanguageVersion,
    ): Boolean = classification(kind, languageVersion).matchingDisposition != CjmpAnnotationMatchingDisposition.MATCH

    /** 未知注解使用官方 UNKNOWN 分类；其它自定义身份仍参与普通注解比较。 */
    fun isHandledOutsideAnnotationMatching(
        identity: CangjieAnnotationIdentity?,
        languageVersion: LanguageVersion,
    ): Boolean = classification(identity, languageVersion).matchingDisposition != CjmpAnnotationMatchingDisposition.MATCH
}
