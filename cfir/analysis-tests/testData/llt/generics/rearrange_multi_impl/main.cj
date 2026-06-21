// FILE: ser.cj


package test

import std.collection.*

public abstract class DataModel {}

public class DataModelString <: DataModel {
    private var value: String

    public init(sv: String) {
        this.value = sv
    }

    public func getValue(): String {
        return value
    }
}

public class DataModelSeq <: DataModel {
    private var items: ArrayList<DataModel>

    public init() {
        items = ArrayList<DataModel>()
    }

    public init(list: ArrayList<DataModel>) {
        items = list
    }

    public func getItems(): ArrayList<DataModel> {
        return items
    }

    public func add(dm: DataModel) {
        items.add(dm)
    }
}

public class Field {
    private var name: String

    var data: DataModel

    public init(name: String, data: DataModel) {
        this.name = name
        this.data = data
    }

    public func getName(): String {
        return name
    }

    public func getData(): DataModel {
        return data
    }
}

func createDataModelSeq<T>(list: Collection<T>): DataModelSeq where T <: Serializable<T> {
    let dms: ArrayList<DataModel> = ArrayList<DataModel>(list.size)
    for (i in list) {
        dms.add(i.serialize())
    }
    return DataModelSeq(dms)
}

public func field<T>(name: String, data: T): Field where T <: Serializable<T> {
    return Field(name, data.serialize())
}

public class DataModelException <: Exception {
    public init() {
        super()
    }

    public init(message: String) {
        super(message)
    }

    protected override func getClassName(): String {
        return "DataModelException"
    }
}

// FILE: ser2.cj


package test

import std.collection.*

public interface Serializable<T> {
    func serialize(): DataModel
    static func deserialize(dm: DataModel): T
}

extend String <: Serializable<String> {
    public func serialize(): DataModel {
        return DataModelString(this)
    }

    public static func deserialize(dm: DataModel): String {
        return match (dm) {
            case dms: DataModelString => dms.getValue()
            case _ => throw DataModelException("this data is not DataModelString")
        }
    }
}

extend<T> ArrayList<T> <: Serializable<ArrayList<T>> where T <: Serializable<T> {
    public func serialize(): DataModel {
        return createDataModelSeq<T>(this)
    }

    public static func deserialize(dm: DataModel): ArrayList<T> {
        var data: ArrayList<DataModel> = match (dm) {
            case dms: DataModelSeq => dms.getItems()
            case _ => throw DataModelException("this data is not DataModelSeq")
        }
        var arr: ArrayList<T> = ArrayList<T>()
        for (i in data) {
            arr.add(T.deserialize(i))
        }
        return arr
    }
}

// FILE: main.cj



package test

import std.unittest.*
import std.collection.*

main(): Int64 {
    var l: ArrayList<String> = ArrayList<String>(["a", "b", "c"])
    var seris: DataModel = l.serialize()
    ArrayList<String>.deserialize(seris)
    return 0
}
