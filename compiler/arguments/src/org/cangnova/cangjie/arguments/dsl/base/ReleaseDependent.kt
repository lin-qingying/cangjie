package org.cangnova.cangjie.arguments.dsl.base

data class ReleaseDependent<T>(
    val current: T,
    val history: Map<CangJieReleaseVersion, T> = emptyMap()
)

fun <T> T.asReleaseDependent(): ReleaseDependent<T> = ReleaseDependent(this)
