package org.cangnova.cangjie.cfir

/**
 * 标记类不包含可变状态。
 *
 * 该注解用于框架审计和并发共享对象的约束说明。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class NoMutableState
