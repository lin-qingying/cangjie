G.SER.03 保证序列化和反序列化的变量类型一致

【级别】要求

【描述】

仓颉不会对序列化和反序列化使用的数据进行类型检查，如果反序列化时使用的数据类型和序列化时传入数据类型不一致，则可能会造成数据错误。开发者需要保证序列化和反序列化时传入数据和接收数据的变量类型一致。

【正例】

```cangjie
class MySerializeDemo <: Serializable<MySerializeDemo> {
    var value: Int64
    var msg: String

    init(v: Int64) {
        value = v
        msg = match (value) {
            case 0x0 => "zero"
            case 0x7fffffff => "BIG INT"
            case _ => "DEFAULT"
        }
    }

    public func serialize(): DataModel {
        DataModelStruct().add(field<Int64>("value", value))
    }


    private init(s: DataModelStruct) {
        let v = Int64.deserialize(s.get("value"))
        value = v
        msg = match (v) {
            case 0x0 => "zero"
            case 0x7fffffff => "BIG INT"
            case _ => "DEFAULT"
        }
    }

    public static func deserialize(s: DataModel): MySerializeDemo {
        let d = (s as DataModelStruct).getOrThrow()
        MySerializeDemo(d)
    }
}
```

正确示例中序列化和反序列化使用的变量的类型一致，保证了反序列化后得到的对象数据符合预期。

【反例】

```cangjie
class MySerializeDemo <: Serializable<MySerializeDemo> {
    var value: Int64
    var msg: String

    init(v: Int64) {
        value = v
        msg = match (value) {
            case 0x0 => "zero"
            case 0x7fffffff => "BIG INT"
            case _ => "DEFAULT"
        }
    }

    public func serialize() : DataModel {
        DataModelStruct().add(field<Int64>("value", value))
    }


    private init(s: DataModelStruct) {
        let v = Int32.deserialize(s.get("value"))
        value = Int64(v)
        msg = match (v) {
            case 0x0 => "zero"
            case 0x7fffffff => "BIG INT"
            case _ => "DEFAULT"
        }
    }

    public static func deserialize(s: DataModel): MySerializeDemo {
        let d = (s as DataModelStruct).getOrThrow()
        MySerializeDemo(d)
    }
}
```

错误示例中序列化时传入的参数 value 是 Int64 类型，但是在接收的时候使用的是 Int32 类型的变量，因此会造成数据截断，导致反序列化的对象数据预期不一致。