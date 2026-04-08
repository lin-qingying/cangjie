package org.cangnova.cangjie.analysis.api.platform.compat

import org.cangnova.cangjie.analysis.api.platform.lifetime.CaLifetimeTokenFactory
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionChecker

/**
 * 为后续逐步收敛命名提供兼容别名。
 *
 * 当前仓库仍大量使用 `CaLifetimeTokenFactory` 与 `CaAnalysisPermissionChecker`，
 * 这里提供平台化命名的兼容别名，避免一次性打散整个调用栈。
 */
typealias CaPlatformLifetimeTokenFactory = CaLifetimeTokenFactory
typealias CaPlatformPermissionChecker = CaAnalysisPermissionChecker
