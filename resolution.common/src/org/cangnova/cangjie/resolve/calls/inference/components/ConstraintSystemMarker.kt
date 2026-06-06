

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.type.model.TypeSystemInferenceExtensionContext

/**
 * Serves as an identifier for a constraint system.
 * In general, [org.cangnova.cangjie.types.model.TypeSystemInferenceExtensionContext] is not an identifier,
 * as it may have singleton implementations (like `TypeComponents.typeContext`).
 *
 * [ConstraintSystemMarker] was introduced for inference logging and is used to
 * group together related constraints.
 */
interface ConstraintSystemMarker : TypeSystemInferenceExtensionContext
