package org.cangnova.cangjie.analysis.api.permissions

public inline fun <T> allowAnalysisOnEdt(action: () -> T): T {
    val permissionRegistry = CaAnalysisPermissionRegistry.getInstance()
    if (permissionRegistry.isAnalysisAllowedOnEdt) return action()

    permissionRegistry.isAnalysisAllowedOnEdt = true
    try {
        return action()
    } finally {
        permissionRegistry.isAnalysisAllowedOnEdt = false
    }
}
public inline fun <R> forbidAnalysis(description: String, action: () -> R): R {
    val permissionRegistry = CaAnalysisPermissionRegistry.getInstance()
    if (permissionRegistry.explicitAnalysisRestriction != null) return action()

    permissionRegistry.explicitAnalysisRestriction =
        CaAnalysisPermissionRegistry.CaExplicitAnalysisRestriction(description)
    return try {
        action()
    } finally {
        permissionRegistry.explicitAnalysisRestriction = null
    }
}
public inline fun <T> allowAnalysisFromWriteAction(action: () -> T): T {
    val permissionRegistry = CaAnalysisPermissionRegistry.getInstance()
    if (permissionRegistry.isAnalysisAllowedInWriteAction) return action()

    permissionRegistry.isAnalysisAllowedInWriteAction = true
    try {
        return action()
    } finally {
        permissionRegistry.isAnalysisAllowedInWriteAction = false
    }
}
