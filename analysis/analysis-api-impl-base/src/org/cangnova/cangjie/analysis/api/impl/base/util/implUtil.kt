package org.cangnova.cangjie.analysis.api.impl.base.util

fun <T> lazyPub(initializer: () -> T) = lazy(LazyThreadSafetyMode.PUBLICATION, initializer)
