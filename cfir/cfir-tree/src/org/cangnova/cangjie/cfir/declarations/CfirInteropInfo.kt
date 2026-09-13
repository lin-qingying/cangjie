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

import org.cangnova.cangjie.annotations.CangjieCallingConvention
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirDeclarationDataKey

/** ABI kind after applying the session's backend ABI policy. */
public enum class CfirAbiKind {
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
    val isImpl: Boolean = false,
    val hasDefault: Boolean = false,
    val externalName: String? = null,
    val generatedDeclarationIds: List<String> = emptyList(),
)

/** Objective-C-specific semantic metadata. */
public data class CfirObjCInteropInfo(
    val isMirror: Boolean = false,
    val isImpl: Boolean = false,
    val isInit: Boolean = false,
    val isOptional: Boolean = false,
    val externalName: String? = null,
    val generatedDeclarationIds: List<String> = emptyList(),
)

/** Common/specific mapping metadata retained independently from ordinary annotations. */
public data class CfirCjmpMappingInfo(
    val isJavaMapping: Boolean = false,
    val isObjCMapping: Boolean = false,
    val isSupportedOnCommon: Boolean = true,
    val isSupportedOnSpecific: Boolean = true,
)

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
) {
    /** Whether this declaration currently has C-function ABI semantics. */
    public val isC: Boolean
        get() = resolvedAbi.isCFunction

    public companion object {
        /** Empty state for declarations without interop annotations. */
        public val NONE: CfirInteropInfo = CfirInteropInfo(
            abiRequest = CfirAbiRequest(isForeign = false, hasExplicitC = false),
            resolvedAbi = CfirResolvedAbi(CfirAbiKind.UNKNOWN, isCFunction = false),
        )
    }
}

/** Attribute key used to attach the canonical interop snapshot to a declaration. */
private object CfirInteropInfoKey : CfirDeclarationDataKey()

/** Canonical declaration-owned interop snapshot. */
public var CfirDeclaration.interopInfo: CfirInteropInfo? by CfirDeclarationDataRegistry.data(CfirInteropInfoKey)

/** Read the declaration interop snapshot without manufacturing semantic state. */
public fun CfirDeclaration.resolvedInteropInfoOrNull(): CfirInteropInfo? = interopInfo
