package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.annotations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirAbiPolicy
import org.cangnova.cangjie.cfir.session.CfirInteropTarget
import org.cangnova.cangjie.cfir.session.interopSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.descriptors.Visibilities

/**
 * 从解析后的身份及参数发布 interop 快照。
 *
 * STATUS 只调用它发布不依赖实参的 ABI，BODY_RESOLVE 在映射完成后再次发布完整值。
 * 两次发布均消费同一 identity/mapping，没有从文本或 PSI 猜测注解。
 */
public fun CfirDeclaration.publishInteropInfo(session: CfirSession) {
    val member = this as? CfirMemberDeclaration ?: return
    val calls = annotations.filterIsInstance<CfirAnnotationCall>()
    fun annotation(kind: BuiltInAnnotationKind): CfirAnnotationCall? = calls.firstOrNull { it.annotationKind == kind }
    fun has(kind: BuiltInAnnotationKind): Boolean =
        annotation(kind)?.isSupportedBuiltinAnnotation(kind, session.languageVersionSettings) == true
    fun name(kind: BuiltInAnnotationKind): String? = annotation(kind)?.stringArgument("name")
    fun platformAnnotation(kind: CangjiePlatformAnnotationKind): CfirAnnotationCall? =
        calls.firstOrNull { it.platformAnnotationKind == kind }
    fun hasPlatform(kind: CangjiePlatformAnnotationKind): Boolean =
        platformAnnotation(kind)?.annotationVersionSupport(session.languageVersionSettings) ==
            AnnotationVersionSupportStatus.SUPPORTED
    fun platformName(kind: CangjiePlatformAnnotationKind): String? =
        platformAnnotation(kind)
            ?.takeIf { hasPlatform(kind) }
            ?.stringArgument("name")
    val serializedFacts = serializedInteropFacts
    val serializedJavaFacts = serializedFacts?.takeIf {
        session.languageVersionSettings.supportsFeature(LanguageFeature.JavaInteropAnnotations)
    }
    val serializedObjCFacts = serializedFacts?.takeIf {
        session.languageVersionSettings.supportsFeature(LanguageFeature.ObjCInteropAnnotations)
    }
    val conventionName = annotation(BuiltInAnnotationKind.CALLING_CONV)?.stringArgument("convention")
    val convention = CangjieCallingConvention.entries.firstOrNull { it.name == conventionName }
        ?: serializedFacts?.callingConvention
    // CJO stores the C ABI attribute separately from `Anno`; source `@C` and
    // that serialized fact are the same request, while a foreign declaration's
    // backend-default C ABI is not an explicit source `@C` request.
    val hasExplicitC = has(BuiltInAnnotationKind.C) || serializedFacts?.hasExplicitC == true
    val request = CfirAbiRequest(member.status.isForeign, hasExplicitC, convention, this is CfirFunction)
    val java = if (has(BuiltInAnnotationKind.JAVA) || hasPlatform(CangjiePlatformAnnotationKind.JAVA_MIRROR) ||
        hasPlatform(CangjiePlatformAnnotationKind.JAVA_IMPL) || hasPlatform(CangjiePlatformAnnotationKind.JAVA_HAS_DEFAULT) ||
        serializedJavaFacts?.let {
            it.isJavaMirror || it.isJavaMirrorSubtype || it.hasJavaDefault ||
                it.isJavaMirrorSyntheticWrapper || it.isJavaApplication || it.isJavaExtension ||
                it.isJavaCjMapping || it.isJavaInterfaceForward || it.isJavaInterfaceDefault
        } == true
    ) CfirJavaInteropInfo(
        isMirror = hasPlatform(CangjiePlatformAnnotationKind.JAVA_MIRROR) || serializedJavaFacts?.isJavaMirror == true,
        isMirrorSubtype = serializedJavaFacts?.isJavaMirrorSubtype == true,
        isImpl = hasPlatform(CangjiePlatformAnnotationKind.JAVA_IMPL),
        hasDefault = hasPlatform(CangjiePlatformAnnotationKind.JAVA_HAS_DEFAULT) || serializedJavaFacts?.hasJavaDefault == true,
        isSyntheticWrapper = serializedJavaFacts?.isJavaMirrorSyntheticWrapper == true,
        isApplication = serializedJavaFacts?.isJavaApplication == true,
        isExtension = serializedJavaFacts?.isJavaExtension == true,
        isInterfaceForward = serializedJavaFacts?.isJavaInterfaceForward == true,
        isInterfaceDefault = serializedJavaFacts?.isJavaInterfaceDefault == true,
        externalName = platformName(CangjiePlatformAnnotationKind.JAVA_MIRROR)
            ?: platformName(CangjiePlatformAnnotationKind.JAVA_IMPL)
            ?: name(BuiltInAnnotationKind.JAVA),
    ) else null
    val objc = if (hasPlatform(CangjiePlatformAnnotationKind.OBJ_C_MIRROR) || hasPlatform(CangjiePlatformAnnotationKind.OBJ_C_IMPL) ||
        hasPlatform(CangjiePlatformAnnotationKind.OBJ_C_INIT) || hasPlatform(CangjiePlatformAnnotationKind.OBJ_C_OPTIONAL) ||
        serializedObjCFacts?.let {
            it.isObjCMirror || it.isObjCMirrorSubtype || it.isObjCInit || it.isObjCOptional ||
                it.isObjCMirrorSyntheticWrapper || it.isObjCCjMapping || it.isObjCInterfaceForward
        } == true
    ) CfirObjCInteropInfo(
        isMirror = hasPlatform(CangjiePlatformAnnotationKind.OBJ_C_MIRROR) || serializedObjCFacts?.isObjCMirror == true,
        isMirrorSubtype = serializedObjCFacts?.isObjCMirrorSubtype == true,
        isImpl = hasPlatform(CangjiePlatformAnnotationKind.OBJ_C_IMPL),
        isInit = hasPlatform(CangjiePlatformAnnotationKind.OBJ_C_INIT) || serializedObjCFacts?.isObjCInit == true,
        isOptional = hasPlatform(CangjiePlatformAnnotationKind.OBJ_C_OPTIONAL) || serializedObjCFacts?.isObjCOptional == true,
        isSyntheticWrapper = serializedObjCFacts?.isObjCMirrorSyntheticWrapper == true,
        isInterfaceForward = serializedObjCFacts?.isObjCInterfaceForward == true,
        externalName = platformName(CangjiePlatformAnnotationKind.OBJ_C_MIRROR)
            ?: platformName(CangjiePlatformAnnotationKind.OBJ_C_IMPL),
    ) else null
    val abi = when {
        has(BuiltInAnnotationKind.JAVA) || java?.isMirror == true || java?.isImpl == true ->
            CfirResolvedAbi(CfirAbiKind.JAVA, false)
        objc?.isMirror == true || objc?.isImpl == true ->
            CfirResolvedAbi(CfirAbiKind.OBJC, false)
        else -> session.cfirAbiPolicy.resolve(request)
    }
    val foreignName = platformName(CangjiePlatformAnnotationKind.FOREIGN_NAME)
    val symbolName = (this as? CfirCallableDeclaration)?.symbol?.callableId?.callableName?.asString()
    val cjmp = deriveCjmpMappingInfo(session, member, calls, serializedFacts)
    val ffiAnnotationKinds = calls.asSequence()
        .mapNotNull { it.annotationKind }
        .filter { kind -> BuiltInAnnotationRegistry.languageBuiltIns.any { it.kind == kind && it.category == BuiltInAnnotationCategory.FFI } }
        .toMutableSet()
    if (hasExplicitC) ffiAnnotationKinds += BuiltInAnnotationKind.C
    if (serializedFacts?.callingConvention != null) ffiAnnotationKinds += BuiltInAnnotationKind.CALLING_CONV
    if (serializedFacts?.isFastNative == true) ffiAnnotationKinds += BuiltInAnnotationKind.FASTNATIVE
    val ffiAnnotationNames = ffiAnnotationKinds.mapNotNull { kind ->
        BuiltInAnnotationRegistry.languageBuiltIns.firstOrNull { it.kind == kind }?.sourceName
    }
    val platformKinds = calls.asSequence()
        .mapNotNull { it.platformAnnotationKind }
        .filter(::hasPlatform)
        .toSet()
    val platformNames = calls.asSequence()
        .filter { call -> call.platformAnnotationKind?.let(::hasPlatform) == true }
        .mapNotNull { it.platformAnnotationDescriptor?.sourceName }
        .distinct()
        .toList()
    interopInfo = CfirInteropInfo(
        abiRequest = request,
        resolvedAbi = abi,
        foreignName = foreignName,
        foreignGetterName = platformName(CangjiePlatformAnnotationKind.FOREIGN_GETTER_NAME),
        foreignSetterName = platformName(CangjiePlatformAnnotationKind.FOREIGN_SETTER_NAME),
        java = java,
        objc = objc,
        cjmp = cjmp,
        overflowStrategy = annotationInfo?.overflowStrategy,
        isFastNative = has(BuiltInAnnotationKind.FASTNATIVE) || serializedFacts?.isFastNative == true,
        isFrozen = has(BuiltInAnnotationKind.FROZEN),
        externalSymbolName = foreignName ?: java?.externalName ?: objc?.externalName ?: symbolName.takeIf { abi.kind == CfirAbiKind.C },
        ffiAnnotationKinds = ffiAnnotationKinds,
        ffiAnnotationNames = ffiAnnotationNames.distinct(),
        platformAnnotationKinds = platformKinds,
        platformAnnotationNames = platformNames,
    )
}

/**
 * 根据官方 parser 的配置规则派生 CJMapping 声明属性。
 *
 * CJMapping 不对应源码注解：只有启用 `--enable-interop-cjmapping` 后，
 * 公共 class/struct/enum/interface（以及 Java 目标下的 extend）才会被标记。
 * 已经显式声明 mirror/impl 的类型必须保持其原有互操作身份，不能再次派生 CJMapping。
 */
private fun deriveCjmpMappingInfo(
    session: CfirSession,
    declaration: CfirMemberDeclaration,
    calls: List<CfirAnnotationCall>,
    serializedFacts: CfirSerializedInteropFacts?,
): CfirCjmpMappingInfo? {
    // CJMapping is a language feature, not merely a session/backend option.
    // Apply the gate before consuming either source facts or serialized facts;
    // otherwise old-language sessions can publish a platform graph from CJO.
    if (!session.languageVersionSettings.supportsFeature(LanguageFeature.InteropCJMapping)) return null

    serializedFacts?.cjmpTarget?.let { serializedTarget ->
        val targetFeature = when (serializedTarget) {
            CfirInteropTarget.JAVA -> LanguageFeature.JavaInteropAnnotations
            CfirInteropTarget.OBJC -> LanguageFeature.ObjCInteropAnnotations
            CfirInteropTarget.NONE -> return null
        }
        if (!session.languageVersionSettings.supportsFeature(targetFeature)) return null
        return CfirCjmpMappingInfo(
            target = serializedTarget,
            declarationKind = declaration.cjmpDeclarationKindOrNull() ?: return null,
            isSupportedOnCommon = declaration.status.isCommon,
            isSupportedOnSpecific = declaration.status.isSpecific,
        )
    }

    val settings = session.interopSettings
    if (!settings.enableInteropCJMapping) return null
    if (settings.targetInteropLanguage == CfirInteropTarget.NONE) return null
    if (settings.targetInteropLanguage == CfirInteropTarget.JAVA &&
        !session.languageVersionSettings.supportsFeature(LanguageFeature.JavaInteropAnnotations)
    ) return null
    if (settings.targetInteropLanguage == CfirInteropTarget.OBJC &&
        !session.languageVersionSettings.supportsFeature(LanguageFeature.ObjCInteropAnnotations)
    ) return null

    fun hasPlatform(kind: CangjiePlatformAnnotationKind): Boolean = calls.any { call ->
        call.platformAnnotationKind == kind &&
            call.annotationVersionSupport(session.languageVersionSettings) ==
            AnnotationVersionSupportStatus.SUPPORTED
    }
    if (hasPlatform(CangjiePlatformAnnotationKind.JAVA_MIRROR) || hasPlatform(CangjiePlatformAnnotationKind.JAVA_IMPL) ||
        hasPlatform(CangjiePlatformAnnotationKind.OBJ_C_MIRROR) || hasPlatform(CangjiePlatformAnnotationKind.OBJ_C_IMPL)
    ) return null

    val declarationKind = when (declaration) {
        is CfirClass -> CfirCjmpDeclarationKind.CLASS.takeUnless { declaration.status.isAbstract }
        is CfirStruct -> CfirCjmpDeclarationKind.STRUCT
        is CfirEnum -> CfirCjmpDeclarationKind.ENUM
        is CfirInterface -> CfirCjmpDeclarationKind.INTERFACE
        is CfirExtend -> CfirCjmpDeclarationKind.EXTEND.takeIf {
            settings.targetInteropLanguage == CfirInteropTarget.JAVA
        }
        else -> null
    } ?: return null

    // The official parser requires PUBLIC for nominal CJMapping declarations.
    // Java extend is the explicit exception in CheckCJMappingAttr and is handled above.
    if (declaration !is CfirExtend && declaration.status.visibility != Visibilities.Public) return null

    return CfirCjmpMappingInfo(
        target = settings.targetInteropLanguage,
        declarationKind = declarationKind,
    )
}

/** 把成员声明归约为 CJMapping 声明种类；非 nominal/extend 声明返回 `null`。 */
private fun CfirMemberDeclaration.cjmpDeclarationKindOrNull(): CfirCjmpDeclarationKind? = when (this) {
    is CfirClass -> CfirCjmpDeclarationKind.CLASS
    is CfirStruct -> CfirCjmpDeclarationKind.STRUCT
    is CfirEnum -> CfirCjmpDeclarationKind.ENUM
    is CfirInterface -> CfirCjmpDeclarationKind.INTERFACE
    is CfirExtend -> CfirCjmpDeclarationKind.EXTEND
    else -> null
}
