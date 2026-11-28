// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: NONE
// RUN_PIPELINE_TILL: FRONTEND
// 不存在 CJMP 编译模式时，普通 CFFI 注解目标检查仍应执行。
package e7_annotation_ordinary

@EnsurePreparedToMock
public func ordinary(): Int64 { 1 }

@EnsurePreparedToMock
public class Ordinary { public init() {} }

<!ONLY_CFUNC_CAN_USE_ANNOTATION!>@CallingConv[C]<!>
public func wrongConvention(): Int64 { 1 }
