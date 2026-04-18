

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets

import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirRegularClass

/**
 * This interface describes how to process nested declarations.
 *
 * @see LLCfirResolveTarget
 */
internal interface LLCfirResolveTargetVisitor {
    /**
     * Access to [CfirFile] declaration will be performed inside [action].
     */
    fun withFile(firFile: CfirFile, action: () -> Unit): Unit = action()

    /**
     * Access to elements inside [CfirRegularClass] will be performed inside [action].
     * Will be called for each nested [CfirRegularClass] on the path.
     */
    fun withRegularClass(firClass: CfirRegularClass, action: () -> Unit): Unit = action()

    /**
     * This method will be performed on some target element depends on [LLCfirResolveTarget] implementation.
     */
    fun performAction(element: CfirElementWithResolveState)
}
