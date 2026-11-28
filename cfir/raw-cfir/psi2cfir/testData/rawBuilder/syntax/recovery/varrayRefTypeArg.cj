// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：VArray 的元素类型是引用类型（接口 / 类）——VArray 元素类型必须是数值类型。
// 官方 cjc 1.0.5 / 1.1.3：sema_varray_arg_type_with_reftype。
interface Box<T> {
    func value(): T
}

func varrayRefTypeArg(): Unit {
    let a: VArray<Box<Int64>, $2> = [Box<Int64>(), Box<Int64>()]
}
