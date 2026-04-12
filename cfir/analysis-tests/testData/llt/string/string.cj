// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class student <: ToString {
    public var name = ""
    public init(name: String) {
        this.name = name
    }
    public func toString(): String {
        return this.name
    }
}

main(): Int64 {
    var s: student = student("Rune")
    print("${s}")
    return 0
}
