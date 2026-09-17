package org.cangnova.cangjie.analysis.api.annotations

/** UNKNOWN 用于缺少新语义元信息的二进制，不能解释为没有注解参数。 */
enum class CaAnnotationResolutionStatus { UNKNOWN, TYPE_RESOLVED, ARGUMENTS_RESOLVED, RESOLVED, ERROR }
