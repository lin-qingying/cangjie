package org.cangnova.cangjie.analysis.api.interop

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

interface CaInteropInfo : CaLifetimeOwner {
    val backends: List<CaInteropBackend>

    val isForeignDeclaration: Boolean

    val isFastNative: Boolean

    val externalName: String?

    val callingConvention: CaInteropCallingConvention?

    val ffiAnnotationNames: List<String>
}
