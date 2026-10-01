// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：同一个 enum 里定义两个同名构造器。官方 1.0.5 / 1.1.3 拒绝：带参构造器同名报
// "'A' is already exist in enum 'E'"（sema_duplicated_item_in_enum），无参构造器同名则按"声明重复定义"报 redefinition of declaration 'A'
// （本文件第二段）。合法枚举不能用同名构造器（官方也没有按参数列表重载枚举构造器）。
// 官方 cjc 1.0.5 / 1.1.3（第一段）："'A' is already exist in enum 'E'"（sema_duplicated_item_in_enum）。
// 官方 cjc 1.0.5 / 1.1.3（第二段）：sema_redefinition "redefinition of declaration 'A'"。
enum SameParamCtor {
    | A(Int64)
    | A(String)
}

enum SamePlainCtor {
    | B
    | B
}

