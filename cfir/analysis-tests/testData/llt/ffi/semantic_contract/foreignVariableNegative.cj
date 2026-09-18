// 官方允许顶层变量进入 foreign 语法，但 CFFI 预检查拒绝隐式 @C。
foreign var <!NATIVE_VAR_ERROR!>nativeValue<!>: Int64
