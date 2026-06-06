

package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

/**
 * [LLStatisticsOnlyApi] is applied to endpoints of regular (non-statistics) LL CFIR declarations. The annotation ensures that the specific
 * endpoint is only used for statistics and not for resolution purposes.
 */
@RequiresOptIn("This API is intended only for statistics collection. It must not be used for other purposes.")
annotation class LLStatisticsOnlyApi
