// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

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
