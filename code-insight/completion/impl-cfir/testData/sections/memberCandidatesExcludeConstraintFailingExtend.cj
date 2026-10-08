package completion.sections

interface Shaped {}

class Box<T> {}

extend<T> Box<T> where T <: Shaped {
    func shapedOnly(): Int64 {
        1
    }
}

class Plain {}

// 约束满足：`shapedOnly` 必须出现。
func useSatisfied(box: Box<Shaped>): Int64 {
    box.satisfiedTarget
}

// 约束不满足（Plain 未实现 Shaped）：`shapedOnly` 不得出现。
func useFailing(box: Box<Plain>): Int64 {
    box.failingTarget
}
