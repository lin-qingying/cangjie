// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：
//   `func g<T>(x: T) where T <: A & B`（A、B 互不相关）→ error: generic parameter 'T' cannot have two or more
//     class upper bounds 'Class-A, Class-B' without subtype relation（锚类型形参 `T`）
//     官方 kind sema_multiple_class_upperbounds（include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:96）
//   `func h<T>(t: T) where T <: I { t.missing() }` → error: 'missing' is not found for generic type 'T' in its
//     upper bounds（锚成员名 `missing`）→ 官方 kind sema_not_found_from_generic_upper_bounds（同文件 :68）
//   合法对照：单个类上界 + 成员存在时零诊断（接口成员不写显式 `public`，否则官方 cjc 会给 parse_redundant_modifier warning）。
class UpperA {
    public init() {}
}

class UpperB {
    public init() {}
}

interface Shape {
    // 接口成员隐式 public：官方 cjc 对接口成员上的显式 `public` 报 parse_redundant_modifier warning，此处不写。
    func area(): Int64
}

func conflictingClassBounds<<!MULTIPLE_CLASS_UPPER_BOUNDS!>T<!>>(x: T): T where T <: UpperA & UpperB {
    return x
}

func missingInUpperBounds<T>(t: T): Int64 where T <: Shape {
    return t.<!GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS!>missing<!>()
}

func presentInUpperBounds<T>(t: T): Int64 where T <: Shape {
    return t.area()
}
