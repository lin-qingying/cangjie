// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：enum 继承接口但没实现接口成员（官方 sema_need_member_implementation "implementation of function or property is needed in 'H'"，锚 enum 声明名）。
// 合法写法见 declarations/enumForms.cj（枚举成员函数自己实现接口的默认实现，或枚举体内提供实现）。
// 官方 cjc 1.0.5 / 1.1.3：sema_need_member_implementation。
enum H <: ToString {
    | A
}
