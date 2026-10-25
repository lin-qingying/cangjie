// 条件编译：@When 条件变量（os / arch）、组合条件与成员级条件。
// 官方依据：Cangjie 文档《条件编译》；cjc 1.0.5 / 1.1.3 零诊断。
@When[os == "Windows"]
class WindowsOnly {
    @When[arch == "x86_64"]
    public func w(): Int64 {
        return 1
    }

    public init() {}

    @When[os != "Linux"]
    static var v: Int64 = 0
}

@When[os == "Windows" && arch == "x86_64"]
interface WindowsArch {
    func f(): Int64
}

@When[os == "Windows" || os == "Linux"]
enum Desktop {
    | Home
    | Pro
}

@When[!debug]
func releaseOnly(): Int64 {
    return 2
}
