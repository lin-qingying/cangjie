package default_constructor_objc_mirror

// 故意不导入 interoplib.objc；验证已识别 Mirror 的默认 constructor 排除，不声称能链接运行。
@ObjCMirror
public open class ObjCWithoutInit {}

@ObjCMirror
public open class ObjCWithDeclaredInit {
    public init()
}
