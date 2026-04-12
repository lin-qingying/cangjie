// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

var v = 0

class Person <: Resource {
    var status = false
    public var name: String = "Shunshun"
    public init() {
        v++
    }
    public func isClosed(): Bool {
        v++
        var res = status
        status = true
        return res
    }
    public func close(): Unit {
        v++
    }
}

main(): Int64 {
    try (a = Person()) {
        v++
    } finally {
        v++
    }
    if (v != 5) {
        println(v)
        return 1
    }
    return 0
}
