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

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjieCallingConvention
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.session.CfirInteropTarget

/** ABI kind after applying the session's backend ABI policy. */
public enum class CfirAbiKind {
    CANGJIE,
    C,
    JAVA,
    OBJC,
    UNKNOWN,
}

/** Source-level ABI request before backend-dependent default selection. */
public data class CfirAbiRequest(
    /** `foreign` was present on the declaration. */
    val isForeign: Boolean,
    /** `@C` was explicitly present. */
    val hasExplicitC: Boolean,
    /** Explicit calling convention, if present. */
    val callingConvention: CangjieCallingConvention? = null,
    /** ABI 请求的宿主是否为函数；C struct 具有 C ABI 但不是 CFunc。 */
    val isFunction: Boolean = true,
)

/** Backend-independent result shape of resolving an ABI request. */
public data class CfirResolvedAbi(
    /** ABI selected for this session/backend profile. */
    val kind: CfirAbiKind,
    /** Whether the resulting function signature has C-function semantics. */
    val isCFunction: Boolean,
    /** Calling convention selected by the policy, if the policy can determine it. */
    val effectiveCallingConvention: CangjieCallingConvention? = null,
)

/** Java-specific semantic metadata; flags alone are intentionally insufficient. */
public data class CfirJavaInteropInfo(
    val isMirror: Boolean = false,
    val isMirrorSubtype: Boolean = false,
    val isImpl: Boolean = false,
    val hasDefault: Boolean = false,
    val isSyntheticWrapper: Boolean = false,
    val isApplication: Boolean = false,
    val isExtension: Boolean = false,
    val isInterfaceForward: Boolean = false,
    val isInterfaceDefault: Boolean = false,
    val externalName: String? = null,
    val generatedDeclarationIds: List<String> = emptyList(),
)

/** Objective-C-specific semantic metadata. */
public data class CfirObjCInteropInfo(
    val isMirror: Boolean = false,
    val isMirrorSubtype: Boolean = false,
    val isImpl: Boolean = false,
    val isInit: Boolean = false,
    val isOptional: Boolean = false,
    val isSyntheticWrapper: Boolean = false,
    val isInterfaceForward: Boolean = false,
    val externalName: String? = null,
    val generatedDeclarationIds: List<String> = emptyList(),
)

/** Common/specific mapping metadata retained independently from ordinary annotations. */
public enum class CfirCjmpDeclarationKind {
    CLASS,
    STRUCT,
    ENUM,
    INTERFACE,
    EXTEND,
}

/**
 * Common/specific mapping metadata retained independently from ordinary annotations.
 *
 * CJMapping is a compiler-derived attribute in the official frontend. [target] and
 * [declarationKind] therefore carry the identity that used to be guessed from a
 * source annotation name; the boolean compatibility properties below are only
 * read-only projections for existing consumers.
 */
public data class CfirCjmpMappingInfo(
    /** 由 session 的互操作目标配置选择的映射方向。 */
    val target: CfirInteropTarget,
    /** 被官方 parser/status 派生为 CJMapping 的声明种类。 */
    val declarationKind: CfirCjmpDeclarationKind,
    val isSupportedOnCommon: Boolean = true,
    val isSupportedOnSpecific: Boolean = true,
) {
    /** 兼容现有消费者的 Java 映射投影。 */
    public val isJavaMapping: Boolean
        get() = target == CfirInteropTarget.JAVA

    /** 兼容现有消费者的 Objective-C 映射投影。 */
    public val isObjCMapping: Boolean
        get() = target == CfirInteropTarget.OBJC
}

/**
 * Declaration-level interop snapshot.
 *
 * `isForeign` remains the declaration modifier in [CfirDeclarationStatus]; this snapshot
 * stores the derived and platform-dependent information so consumers do not rediscover it
 * from annotation names or source text.
 */
public data class CfirInteropInfo(
    val abiRequest: CfirAbiRequest,
    val resolvedAbi: CfirResolvedAbi,
    val foreignName: String? = null,
    val foreignGetterName: String? = null,
    val foreignSetterName: String? = null,
    val java: CfirJavaInteropInfo? = null,
    val objc: CfirObjCInteropInfo? = null,
    val cjmp: CfirCjmpMappingInfo? = null,
    val overflowStrategy: CangjieOverflowStrategy? = null,
    val isFastNative: Boolean = false,
    val isFrozen: Boolean = false,
    /** backend contract 使用的外部符号名，来源于解析结果和声明身份。 */
    val externalSymbolName: String? = null,
    /** FFI 注解的结构化官方身份；消费者不得从字符串名称重新判断语义。 */
    val ffiAnnotationKinds: Set<BuiltInAnnotationKind> = emptySet(),
    /** 仅用于 renderer/兼容 API 的显示名称投影。 */
    val ffiAnnotationNames: List<String> = emptyList(),
) {
    /** Whether this declaration currently has C-function ABI semantics. */
    public val isC: Boolean
        get() = resolvedAbi.kind == CfirAbiKind.C

    public companion object {
        /** Empty state for declarations without interop annotations. */
        public val NONE: CfirInteropInfo = CfirInteropInfo(
            abiRequest = CfirAbiRequest(isForeign = false, hasExplicitC = false),
            resolvedAbi = CfirResolvedAbi(CfirAbiKind.CANGJIE, isCFunction = false),
        )
    }
}

/** Attribute key used to attach the canonical interop snapshot to a declaration. */
private object CfirInteropInfoKey : CfirDeclarationDataKey()

/** Canonical declaration-owned interop snapshot. */
public var CfirDeclaration.interopInfo: CfirInteropInfo? by CfirDeclarationDataRegistry.data(CfirInteropInfoKey)

/** Read the declaration interop snapshot without manufacturing semantic state. */
public fun CfirDeclaration.resolvedInteropInfoOrNull(): CfirInteropInfo? = interopInfo
