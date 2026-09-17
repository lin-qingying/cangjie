package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.annotations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirAbiPolicy
import org.cangnova.cangjie.cfir.session.CfirInteropTarget
import org.cangnova.cangjie.cfir.session.interopSettings
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
    fun has(kind: BuiltInAnnotationKind): Boolean = annotation(kind) != null
    fun name(kind: BuiltInAnnotationKind): String? = annotation(kind)?.stringArgument("name")
    val serializedFacts = serializedInteropFacts
    val conventionName = annotation(BuiltInAnnotationKind.CALLING_CONV)?.stringArgument("convention")
    val convention = CangjieCallingConvention.entries.firstOrNull { it.name == conventionName }
        ?: serializedFacts?.callingConvention
    // CJO stores the C ABI attribute separately from `Anno`; source `@C` and
    // that serialized fact are the same request, while a foreign declaration's
    // backend-default C ABI is not an explicit source `@C` request.
    val hasExplicitC = has(BuiltInAnnotationKind.C) || serializedFacts?.hasExplicitC == true
    val request = CfirAbiRequest(member.status.isForeign, hasExplicitC, convention, this is CfirFunction)
    val java = if (has(BuiltInAnnotationKind.JAVA) || has(BuiltInAnnotationKind.JAVA_MIRROR) ||
        has(BuiltInAnnotationKind.JAVA_IMPL) || has(BuiltInAnnotationKind.JAVA_HAS_DEFAULT) ||
        serializedFacts?.let {
            it.isJavaMirror || it.isJavaMirrorSubtype || it.hasJavaDefault ||
                it.isJavaMirrorSyntheticWrapper || it.isJavaApplication || it.isJavaExtension ||
                it.isJavaCjMapping || it.isJavaInterfaceForward || it.isJavaInterfaceDefault
        } == true
    ) CfirJavaInteropInfo(
        isMirror = has(BuiltInAnnotationKind.JAVA_MIRROR) || serializedFacts?.isJavaMirror == true,
        isMirrorSubtype = serializedFacts?.isJavaMirrorSubtype == true,
        isImpl = has(BuiltInAnnotationKind.JAVA_IMPL),
        hasDefault = has(BuiltInAnnotationKind.JAVA_HAS_DEFAULT) || serializedFacts?.hasJavaDefault == true,
        isSyntheticWrapper = serializedFacts?.isJavaMirrorSyntheticWrapper == true,
        isApplication = serializedFacts?.isJavaApplication == true,
        isExtension = serializedFacts?.isJavaExtension == true,
        isInterfaceForward = serializedFacts?.isJavaInterfaceForward == true,
        isInterfaceDefault = serializedFacts?.isJavaInterfaceDefault == true,
        externalName = name(BuiltInAnnotationKind.JAVA_MIRROR) ?: name(BuiltInAnnotationKind.JAVA_IMPL) ?: name(BuiltInAnnotationKind.JAVA),
    ) else null
    val objc = if (has(BuiltInAnnotationKind.OBJ_C_MIRROR) || has(BuiltInAnnotationKind.OBJ_C_IMPL) ||
        has(BuiltInAnnotationKind.OBJ_C_INIT) || has(BuiltInAnnotationKind.OBJ_C_OPTIONAL) ||
        serializedFacts?.let {
            it.isObjCMirror || it.isObjCMirrorSubtype || it.isObjCInit || it.isObjCOptional ||
                it.isObjCMirrorSyntheticWrapper || it.isObjCCjMapping || it.isObjCInterfaceForward
        } == true
    ) CfirObjCInteropInfo(
        isMirror = has(BuiltInAnnotationKind.OBJ_C_MIRROR) || serializedFacts?.isObjCMirror == true,
        isMirrorSubtype = serializedFacts?.isObjCMirrorSubtype == true,
        isImpl = has(BuiltInAnnotationKind.OBJ_C_IMPL),
        isInit = has(BuiltInAnnotationKind.OBJ_C_INIT) || serializedFacts?.isObjCInit == true,
        isOptional = has(BuiltInAnnotationKind.OBJ_C_OPTIONAL) || serializedFacts?.isObjCOptional == true,
        isSyntheticWrapper = serializedFacts?.isObjCMirrorSyntheticWrapper == true,
        isInterfaceForward = serializedFacts?.isObjCInterfaceForward == true,
        externalName = name(BuiltInAnnotationKind.OBJ_C_MIRROR) ?: name(BuiltInAnnotationKind.OBJ_C_IMPL),
    ) else null
    val abi = when {
        has(BuiltInAnnotationKind.JAVA) || java?.isMirror == true || java?.isImpl == true ->
            CfirResolvedAbi(CfirAbiKind.JAVA, false)
        objc?.isMirror == true || objc?.isImpl == true ->
            CfirResolvedAbi(CfirAbiKind.OBJC, false)
        else -> session.cfirAbiPolicy.resolve(request)
    }
    val foreignName = name(BuiltInAnnotationKind.FOREIGN_NAME)
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
    interopInfo = CfirInteropInfo(
        abiRequest = request,
        resolvedAbi = abi,
        foreignName = foreignName,
        foreignGetterName = name(BuiltInAnnotationKind.FOREIGN_GETTER_NAME),
        foreignSetterName = name(BuiltInAnnotationKind.FOREIGN_SETTER_NAME),
        java = java,
        objc = objc,
        cjmp = cjmp,
        overflowStrategy = annotationInfo?.overflowStrategy,
        isFastNative = has(BuiltInAnnotationKind.FASTNATIVE) || serializedFacts?.isFastNative == true,
        isFrozen = has(BuiltInAnnotationKind.FROZEN),
        externalSymbolName = foreignName ?: java?.externalName ?: objc?.externalName ?: symbolName.takeIf { abi.kind == CfirAbiKind.C },
        ffiAnnotationKinds = ffiAnnotationKinds,
        ffiAnnotationNames = ffiAnnotationNames.distinct(),
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
    serializedFacts?.cjmpTarget?.let { serializedTarget ->
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

    fun has(kind: BuiltInAnnotationKind): Boolean = calls.any { it.annotationKind == kind }
    if (has(BuiltInAnnotationKind.JAVA_MIRROR) || has(BuiltInAnnotationKind.JAVA_IMPL) ||
        has(BuiltInAnnotationKind.OBJ_C_MIRROR) || has(BuiltInAnnotationKind.OBJ_C_IMPL)
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

private fun CfirMemberDeclaration.cjmpDeclarationKindOrNull(): CfirCjmpDeclarationKind? = when (this) {
    is CfirClass -> CfirCjmpDeclarationKind.CLASS
    is CfirStruct -> CfirCjmpDeclarationKind.STRUCT
    is CfirEnum -> CfirCjmpDeclarationKind.ENUM
    is CfirInterface -> CfirCjmpDeclarationKind.INTERFACE
    is CfirExtend -> CfirCjmpDeclarationKind.EXTEND
    else -> null
}
