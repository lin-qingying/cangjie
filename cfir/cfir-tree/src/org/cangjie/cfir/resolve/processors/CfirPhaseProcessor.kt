package org.cangjie.cfir.resolve.processors

import org.cangjie.cfir.resolve.CfirResolveProcessor

/**
 * Marker type for formal phase processors.
 *
 * Keeping this alias allows us to migrate processor implementations into
 * this package without breaking existing callers.
 */
typealias CfirPhaseProcessor = CfirResolveProcessor
