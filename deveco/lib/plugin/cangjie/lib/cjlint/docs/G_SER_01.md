G.SER.01 禁止序列化未加密的敏感数据

【级别】要求

【描述】

虽然序列化可以将对象的状态保存为一个字节序列，之后通过反序列化将字节序列又能重新构造出原来的对象，但是它并没有提供一种机制来保证序列化数据的安全性。因此，敏感数据序列化之后是潜在对外暴露的，可访问序列化数据的攻击者可以借此获取敏感信息并确定对象的实现细节。永远不应该被序列化的敏感信息包括：密钥、数字证书以及那些在序列化时引用敏感数据的类，防止敏感数据被无意识的序列化导致敏感信息泄露。另外，声明了可序列化标识对象的所有字段在序列化时都会被输出为字节序列，能够解析这些字节序列的代码可以获取到这些数据的值，而不依赖于该字段在类中的可访问性。因此，若其中某些字段包含敏感信息，则会造成敏感信息泄露。

【正例】

```cangjie
class People <: Serializable {
    var name: String

    // 口令是敏感数据
    var password: String

    init(s: DataModelStruct) {
        name = String.deserialize(s.get("name"))
        password = ""
    }

    public func serialize(): DataModel {
        DataModelStruct().add(field<String>("name", name))
    }

    public static func deserialize(s: DataModel): People {
        let d = (s as DataModelStruct).getOrThrow()
        People(d)
    }
}
```

该正确示例在进行序列化和反序列化时跳过了 password 变量，避免了 password 信息被泄露。

【反例】

```cangjie
class People <: Serializable<People> {
    var name: String

    // 口令是敏感数据
    var password: String

    init(s: DataModelStruct) {
        name = String.deserialize(s.get("name"))
        password = String.deserialize(s.get("password"))
    }

    public func serialize(): DataModel {
        DataModelStruct().add(field<String>("name", name))
        DataModelStruct().add(field<String>("password", password))
    }

    public static func deserialize(s: DataModel): People {
        let d = (s as DataModelStruct).getOrThrow()
        People(d)
    }
}
```

该错误示例允许将敏感成员变量 password 进行序列化和反序列化，可能会导致 password 信息泄露。