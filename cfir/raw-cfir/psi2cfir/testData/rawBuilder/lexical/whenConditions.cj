// RUN_PIPELINE_TILL: FRONTEND
// 条件编译：@When 的四种条件形态（等值 / 与 / 或 / 不等值）放在类、成员函数、static 变量、接口、枚举、顶层函数上。
// 官方依据：Cangjie 文档《条件编译》；官方 Parse/ParseDecl.cpp 注解解析；cjc 1.0.5 / 1.1.3 零诊断。
@When[os == "Windows"]
class WindowsOnly {
    @When[arch == "x86_64"]
    public func w(): Int64 { return 1 }
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
    | A
}

@When[os != "Linux"]
func notLinux(): Int64 { return 1 }

@When[os == "Windows"]
func windowsOnly(): Int64 { return 2 }
