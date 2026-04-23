G.SER.02 防止反序列化被利用来绕过构造函数中的安全操作

【级别】要求

【描述】

仓颉语言默认由开发者提供序列化和反序列化函数，开发者实现的反序列化函数中需要对各个字段进行校验。反序列化操作可以在绕过公开构造函数的情况下创建对象的实例，所以反序列化操作中的行为应该设计为与公开构造函数保持一致，这些行为包括：对参数的校验、对属性赋初始值等；否则，攻击者就可能会通过反序列化操作构造出与预期不符合的对象实例。仓颉语言使用反序列化功能时应关注此问题，需要在序列化和反序列化前后进行安全检查。

【正例】

```cangjie
class MySerializeDemo <: Serializable<MySerializeDemo> {

    var value: Int64

    init(v: Int64) {
        value = if (v >= 0) { v } else { 0 }
    }

    private init(s: DataModelStruct) {
        let v = Int64.deserialize(s.get("value"))
        value = if (v >= 0) { v } else { 0 }
    }

    public func serialize(): DataModel {
        return DataModelStruct().add(field<Int64>("value", value))
    }

    public static func deserialize(s: DataModel): MySerializeDemo {
        let d = (s as DataModelStruct).getOrThrow()
        MySerializeDemo(d)
    }
}
```

上述示例中， 反序列化操作中与构造函数中对 value 赋值操作保持一致，先检查后赋值。

【反例】

```cangjie
class MySerializeDemo <: Serializable<MySerializeDemo> {

    var value: Int64

    init(v: Int64) {
        value = if (v >= 0) { v } else { 0 }
    }

    private init(s: DataModelStruct) {
        value = Int64.deserialize(s.get("value"))
    }

    public func serialize(): DataModel {
        return DataModelStruct().add(field<Int64>("value", value))
    }

    public static func deserialize(s: DataModel): MySerializeDemo {
        let d = (s as DataModelStruct).getOrThrow()
        MySerializeDemo(d)
    }
}
```

上述示例中，构造函数会对参数进行检查，保证 value 的值为非负值，但通过反序列化操作可构造 value 值为负值的对象示例。