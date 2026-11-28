// 注解：@Attribute 方括号形式、@Deprecated、@When 条件注解、自定义 @Annotation 与多注解叠加。
// 官方依据：Cangjie 文档《注解》《内置编译标记》《条件编译》；cjc 1.0.5 / 1.1.3 零诊断。
@Attribute[a, b, "s"]
class Marked {}

@Attribute["Binding"]
var bound: Int64 = 0

@Attribute[public, open]
class CtxMarked {}

@Deprecated
func oldApi(): Unit {}

@Deprecated[]
func oldApi2(): Unit {}

@Deprecated[message: "use newApi", since: "1.0.0", strict: true]
func oldApi3(): Unit {}

@When[cjc_version >= "1.0.0"]
class Versioned {}

@When[backend == "cjnative"]
func nativeOnly(): Int64 {
    return 1
}

@When[(os == "Windows" || os == "Linux") && !debug]
func combo(): Int64 {
    return 2
}

@Annotation
public class Marker {
    const init() {}
}

@Marker
class MarkedBare {}

@Marker[]
class MarkedEmpty {}

class Holder {
    @Attribute[State]
    private var state: Int64 = 0

    @Deprecated[message: "gone", since: "1.0.0"]
    public func legacy(): Unit {}
}

@When[!debug]
@Attribute[State]
@Deprecated["gone", since: "1.0.0"]
class Stacked {}
