package org.cangnova.cangjie.analysis.api.impl.base.util

/**
 * 使用 PUBLICATION 线程安全模式创建 lazy 值。
 */
fun <T> lazyPub(initializer: () -> T) = lazy(LazyThreadSafetyMode.PUBLICATION, initializer)
