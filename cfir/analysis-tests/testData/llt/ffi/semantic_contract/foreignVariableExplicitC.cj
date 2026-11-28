// cjc 1.1.3：显式 @C 的目标非法；没有缺少显式 @C 的 NATIVE_VAR_ERROR。
// 证据：review-20261003/foreign-variable-explicit-c，唯一 sema_illegal_use_of_annotation。
<!ILLEGAL_USE_OF_ANNOTATION!>@C<!>
foreign var nativeValue: Int64
