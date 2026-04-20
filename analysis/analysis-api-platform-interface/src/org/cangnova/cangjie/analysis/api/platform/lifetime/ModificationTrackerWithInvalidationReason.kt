/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.platform.lifetime

import com.intellij.openapi.util.ModificationTracker
import org.cangnova.cangjie.analysis.api.CaImplementationDetail

/**
 * A [ModificationTracker] that provides a reason for invalidation since its creation.
 */
@CaImplementationDetail
interface ModificationTrackerWithInvalidationReason : ModificationTracker {
    /**
     * Returns a human-readable representation of the invalidation reason if the tracker has been invalidated since its creation.
     *
     * @return the invalidation reason if it has been invalidated, or `null` otherwise.
     */
    fun getInvalidationReason(): String?
}
