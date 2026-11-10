package completion.members

interface Greeter {
    func greet(name: String): String
}

// Host 没有实现 greet：窄接口必须把它收集出来，并给出渲染后的声明文本。
class Host <: Greeter {
}
