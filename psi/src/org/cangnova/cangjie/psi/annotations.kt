package org.cangnova.cangjie.psi

/**
 * 标记仓颉 PSI 的实现细节 API。
 *
 * 对齐 Kotlin `KtImplementationDetail`，用于声明仅供 PSI 实现模块内部使用的接口。
 */
@RequiresOptIn("Internal API which should not be used outside the Cangjie PSI implementation modules as it does not have any compatibility guarantees")
annotation class CjImplementationDetail

/**
 * 标记仓颉 PSI 的非公开 API。
 *
 * 对齐 Kotlin `KtNonPublicApi`，用于声明仅供 IDE 与框架内部消费的 PSI API。
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("Internal API which is used in projects developed around the Cangjie compiler and IDE")
annotation class CjNonPublicApi
