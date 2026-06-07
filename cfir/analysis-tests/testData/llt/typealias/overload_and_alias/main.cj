// FILE: a/a.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package a

import std.time.DateTime
import std.collection.ArrayList

public type List<T> = MetaList<T>

public interface I0{}

public class MetaList<T> where T <: I0 {
    public init(elements: Array<T>) {
    }
}

extend DateTime <: I0{
}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


import std.time.DateTime
import a.*

public func maxDateTime(dateList:List<DateTime>):DateTime{
    return DateTime.now();
}

public func testFunc(tradeDate1: DateTime) {
    return List(maxDateTime(List(tradeDate1)))
}

main(): Int64 {
    return 0
}
