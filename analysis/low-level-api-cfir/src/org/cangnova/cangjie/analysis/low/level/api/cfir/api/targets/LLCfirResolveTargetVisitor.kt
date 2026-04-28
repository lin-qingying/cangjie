

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets

import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile

/**
 * This interface describes how to process nested declarations.
 *
 * @see LLCfirResolveTarget
 */
internal interface LLCfirResolveTargetVisitor {
    /**
     * Access to [CfirFile] declaration will be performed inside [action].
     */
    fun withFile(cfirFile: CfirFile, action: () -> Unit): Unit = action()

    /**
     * Access to elements inside [CfirClassLikeDeclaration] will be performed inside [action].
     * Will be called for each nested class-like declaration on the path.
     */
    fun withClassLike(cfirClassLike: CfirClassLikeDeclaration, action: () -> Unit): Unit = action()

    /**
     * 保留 regular class 专用入口，供仍然只接受 [CfirClass] 的旧路径复用。
     */
    fun withClass(cfirClass: CfirClass, action: () -> Unit): Unit = withClassLike(cfirClass, action)

    /**
     * Access to elements inside [CfirExtend] will be performed inside [action].
     * Will be called for each nested `extend` container on the path.
     */
    fun withExtend(cfirExtend: CfirExtend, action: () -> Unit): Unit = action()

    /**
     * This method will be performed on some target element depends on [LLCfirResolveTarget] implementation.
     */
    fun performAction(element: CfirElementWithResolveState)
}
