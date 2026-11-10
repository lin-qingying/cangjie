// MODULE: first
// FILE: first.cj
package recovery.first

public class Widget {
    public func fromFirst(): Int64 {
        1
    }
}

// MODULE: second
// FILE: second.cj
package recovery.second

public class Widget {
    public func fromSecond(): Int64 {
        2
    }
}

// MODULE: main(first, second)
// MAIN_MODULE
// FILE: main.cj
package completion.sections

// 两个包各有同名类型：给不出确定的接收者类型，不得猜。
func site(): Int64 {
    Widget.completionTarget
}
