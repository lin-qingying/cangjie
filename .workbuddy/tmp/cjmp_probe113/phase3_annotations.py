import os
import time

R = 'D:/code/intellij/cangjie'
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCommonSpecificChecker.kt'


def read(p):
    with open(p, 'rb') as f:
        return f.read().decode('utf-8')


def robust_write(path, text):
    data = text.encode('utf-8')
    tmp = path + '.cjmp.tmp'
    last = None
    for _ in range(20):
        try:
            with open(tmp, 'wb') as f:
                f.write(data)
            os.replace(tmp, path)
            return
        except OSError as e:
            last = e
            time.sleep(0.5)
    raise last


def replace_once(text, old, new, what):
    if text.count(old) == 1:
        return text.replace(old, new, 1)
    nl = '\r\n' if '\r\n' in text else '\n'
    old2, new2 = old.replace('\n', nl), new.replace('\n', nl)
    c = text.count(old2)
    assert c == 1, f"{what}: anchor count {c} (lf={text.count(old)})"
    return text.replace(old2, new2, 1)


t = read(p)

# 1) checkMemberAnnotationMatch：多重集一一对应（官方 MatchCJMPDeclAnnotations 判据）
old_call = '''        val specificAnnotationKeys = specificMember.annotationKeys(context.languageVersionSettings)
        val commonAnnotationKeys = commonMember.annotationKeys(context.languageVersionSettings)

        if (specificAnnotationKeys != commonAnnotationKeys) {'''
new_call = '''        // 官方判据（CheckCJMPAnnotations.cpp:253-296 MatchCJMPDeclAnnotations）：
        // 双向一一对应 + **按出现次数**（同一注解出现多次须逐次配对）；special-handled 注解
        //（@Deprecated / @Attribute / 非序列化家族 / 不支持家族）不参与该比较。
        val specificAnnotationKeys = specificMember.annotationKeys(context.languageVersionSettings)
        val commonAnnotationKeys = commonMember.annotationKeys(context.languageVersionSettings)

        if (!annotationKeyMultisetsEqual(specificAnnotationKeys, commonAnnotationKeys)) {'''
t = replace_once(t, old_call, new_call, 'multiset compare')

# 2) annotationKeys：Set → List + special-handled 过滤 + 多重集助手
old_helper_head = '''/**
 * 取得声明上的解析注解身份集合。
 *
 * common/specific 匹配必须比较 builtin kind 或 resolved ClassId；短名相同的两个
 * custom annotation 不能被视为同一注解。
 */
private fun CfirDeclaration.annotationKeys(
    settings: org.cangnova.cangjie.LanguageVersionSettings,
): Set<AnnotationMatchKey> =
    annotations.mapNotNull { annotation ->
        when {
            annotation.annotationKind != null &&
                ((annotation as? org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall)
                    ?.isSupportedBuiltinAnnotation(annotation.annotationKind!!, settings)
                    ?: settings.supportsBuiltinAnnotationKind(annotation.annotationKind!!)) ->
                AnnotationMatchKey.BuiltIn(annotation.annotationKind!!)
            annotation is org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall &&
                annotation.platformAnnotationKind != null &&
                annotation.annotationVersionSupport(settings) ==
                org.cangnova.cangjie.annotations.AnnotationVersionSupportStatus.SUPPORTED ->
                AnnotationMatchKey.Platform(annotation.platformAnnotationKind!!)
            annotation.annotationClassId != null -> AnnotationMatchKey.Resolved(annotation.annotationClassId!!)
            else -> null
        }
    }.toSet()'''
new_helper_head = '''/**
 * 取得声明上的解析注解身份列表（保留出现次数）。
 *
 * common/specific 匹配必须比较 builtin kind 或 resolved ClassId；短名相同的两个
 * custom annotation 不能被视为同一注解。**保留重复项**：官方按出现次数逐次配对
 *（CheckCJMPAnnotations.cpp:253-296）。
 *
 * special-handled 注解不参与该比较（官方 `IsSpecialHandledAnnotation`）：
 * @Deprecated / @Attribute / 非序列化家族（C、JAVA_HAS_DEFAULT、OBJ_C_MIRROR、
 * OBJ_C_INIT、OBJ_C_OPTIONAL）/ 不支持家族（JAVA、CALLING_CONV、FOREIGN_GETTER_NAME、
 * FOREIGN_SETTER_NAME、CONSTSAFE、ENSURE_PREPARED_TO_MOCK、NON_PRODUCT）——
 * 其中 @Deprecated 由 [checkMemberDeprecatedInherited] 单独负责。
 */
private fun CfirDeclaration.annotationKeys(
    settings: org.cangnova.cangjie.LanguageVersionSettings,
): List<AnnotationMatchKey> =
    annotations.mapNotNull { annotation ->
        when {
            annotation.annotationKind != null &&
                ((annotation as? org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall)
                    ?.isSupportedBuiltinAnnotation(annotation.annotationKind!!, settings)
                    ?: settings.supportsBuiltinAnnotationKind(annotation.annotationKind!!)) ->
                AnnotationMatchKey.BuiltIn(annotation.annotationKind!!)

            annotation is org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall &&
                annotation.platformAnnotationKind != null &&
                annotation.annotationVersionSupport(settings) ==
                org.cangnova.cangjie.annotations.AnnotationVersionSupportStatus.SUPPORTED ->
                AnnotationMatchKey.Platform(annotation.platformAnnotationKind!!)
            annotation.annotationClassId != null -> AnnotationMatchKey.Resolved(annotation.annotationClassId!!)
            else -> null
        }
    }.filterNot { key -> key.isSpecialHandledForCjmpMatch() }

/**
 * 官方 `IsSpecialHandledAnnotation`（CheckCJMPAnnotations.cpp:198-203）对位：
 * 这些注解不参与 common/specific 注解多重集比较。
 */
private fun AnnotationMatchKey.isSpecialHandledForCjmpMatch(): Boolean = when (this) {
    is AnnotationMatchKey.BuiltIn -> kind in SPECIAL_HANDLED_BUILTIN_ANNOTATIONS
    is AnnotationMatchKey.Platform -> kind in SPECIAL_HANDLED_PLATFORM_ANNOTATIONS
    is AnnotationMatchKey.Resolved -> false
}

/** 官方 non-serialized + unsupported + deprecated/attribute 家族（BuiltIn 侧对位）。 */
private val SPECIAL_HANDLED_BUILTIN_ANNOTATIONS: Set<BuiltInAnnotationKind> = setOf(
    BuiltInAnnotationKind.DEPRECATED,
    BuiltInAnnotationKind.ATTRIBUTE,
    BuiltInAnnotationKind.C,
    BuiltInAnnotationKind.JAVA_HAS_DEFAULT,
    BuiltInAnnotationKind.OBJ_C_MIRROR,
    BuiltInAnnotationKind.OBJ_C_INIT,
    BuiltInAnnotationKind.OBJ_C_OPTIONAL,
    BuiltInAnnotationKind.JAVA,
    BuiltInAnnotationKind.CALLING_CONV,
    BuiltInAnnotationKind.FOREIGN_GETTER_NAME,
    BuiltInAnnotationKind.FOREIGN_SETTER_NAME,
    BuiltInAnnotationKind.CONSTSAFE,
    BuiltInAnnotationKind.ENSURE_PREPARED_TO_MOCK,
    BuiltInAnnotationKind.NON_PRODUCT,
)

/** 平台注解身份侧的官方 special-handled 家族。 */
private val SPECIAL_HANDLED_PLATFORM_ANNOTATIONS: Set<CangjiePlatformAnnotationKind> = setOf(
    CangjiePlatformAnnotationKind.JAVA_HAS_DEFAULT,
    CangjiePlatformAnnotationKind.OBJ_C_MIRROR,
    CangjiePlatformAnnotationKind.OBJ_C_INIT,
    CangjiePlatformAnnotationKind.OBJ_C_OPTIONAL,
    CangjiePlatformAnnotationKind.FOREIGN_GETTER_NAME,
    CangjiePlatformAnnotationKind.FOREIGN_SETTER_NAME,
)

/**
 * 注解身份多重集相等：元素与出现次数同时相等（官方双向一一对应语义的等价判据）。
 */
private fun annotationKeyMultisetsEqual(a: List<AnnotationMatchKey>, b: List<AnnotationMatchKey>): Boolean =
    a.size == b.size && a.groupingBy { it }.eachCount() == b.groupingBy { it }.eachCount()'''
t = replace_once(t, old_helper_head, new_helper_head, 'annotation keys helper')

robust_write(p, t)
print('annotation multiset patch applied')
print('CangjiePlatformAnnotationKind imported:', 'CangjiePlatformAnnotationKind' in t)
