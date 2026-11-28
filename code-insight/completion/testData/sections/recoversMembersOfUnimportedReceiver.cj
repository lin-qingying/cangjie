// MODULE: dependency
// FILE: dependency.cj
package recovery.dependency

public class Widget {
    // 类型限定访问 `Widget.size` 走的是静态成员作用域，因此夹具必须是静态成员。
    public static func size(): Int64 {
        1
    }
}

// MODULE: main(dependency)
// MAIN_MODULE
// FILE: main.cj
package completion.sections

// Widget 未导入：类型本身在依赖模块里，成员候选不该因此消失。
func site(): Int64 {
    Widget.completionTarget
}
