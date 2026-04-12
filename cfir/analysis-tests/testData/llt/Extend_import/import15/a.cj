// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package test

import std.collection.ArrayList

public class Foo <: Equatable<Foo> {
    public let id: String

    init(id: String) {
        this.id = id
    }

    public operator func ==(that: Foo): Bool {
        return id == that.id
    }
}

public class Foos {
    public let foos: ArrayList<Foo> = ArrayList()
}
