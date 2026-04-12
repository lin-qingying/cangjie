// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

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
