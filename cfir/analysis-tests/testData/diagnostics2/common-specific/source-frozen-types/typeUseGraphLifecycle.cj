package s4_type_use_graph_lifecycle

// 单字段圆括号保留 Paren，双字段内层是 Tuple；Alias 使用由真正的 inner 节点选择。
public type LifecyclePair = (Int64, Int64)

public interface LifecycleFirst<T> {}
public interface LifecycleMiddle<T> {}
public interface LifecycleLast<T> {}

// 继承槽有序保存 T、U、T；三个接口分别绑定本声明自己的类型参数。
public interface LifecycleSource<T, U> <:
    LifecycleFirst<T> & LifecycleMiddle<U> & LifecycleLast<T> {
    func parenthesized(
        value: ((LifecyclePair, LifecyclePair))
    ): ((LifecyclePair, LifecyclePair)) {
        return value
    }

    func nestedCallback(
        callback: ((T, U, T) -> U)
    ): ((T, U, T) -> U) {
        return callback
    }

    // 源码没有原返回标注；完成签名有真实的推断返回类型。
    func inferredTuple(value: (Int64, Int64)) {
        return value
    }

    func implicitUnit() {}

    func explicitUnit(): Unit {}

    // 正常 std.core 导入提供 loader 创建的 COMPILER_ADDED Type 节点。
    func arrayUse(value: Array<Array<Int64>>): Array<Array<Int64>> {
        return value
    }
}
