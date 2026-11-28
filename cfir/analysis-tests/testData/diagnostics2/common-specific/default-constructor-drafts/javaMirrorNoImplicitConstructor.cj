package default_constructor_java_mirror

// 故意不导入 interoplib.interop；即使存在互操作环境错误，Mirror 也不应获得默认 init。
@JavaMirror
public open class JavaWithoutInit {}

@JavaMirror
public open class JavaWithDeclaredInit {
    public init()
}
