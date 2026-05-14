// RUN_PIPELINE_TILL: FRONTEND
// 测试 extend 可访问性 — 泛型 extend 的跨包可见性
// FILE: generics.cj
package generics

public interface Comparable<T> {
    func compareTo(other: T): Int64 {
        let _ = other
        return 0
    }
}

public class Pair<A, B> {}

// 泛型 extend 不允许修饰符，默认 public
extend<A, B> Pair<A, B> <: Comparable<Pair<A, B>> {}

// FILE: main.cj
package localmain

import generics.*

func compareGenericExtend(p1: Pair<Int64, Int64>, p2: Pair<Int64, Int64>): Int64 {
    return p1.compareTo(p2)
}
