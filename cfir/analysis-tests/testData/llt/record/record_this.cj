// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

internal import std.collection.*

struct Date {
    public let width: Int32
    public let length: Int32 = this.width
    public static var height: Int32 = 2
    public init() {
        this.width = 1
        this.length = 2
        this.f()
        this.f
        height = 3
    }
    public init(a: Int64) {
        this
        this(1)
    }
    public static func ff(): Date {
        return this
    }
    public func f(): Date {
        return this
    }
    public func g(l: ArrayList<Date>) {
        l._add(this)
    }
}

// ASSERT: scan :13:32:
// ASSERT: scan 'this' is only allowed to be used inside constructor or function for struct
// ASSERT: scan :17:9:
// ASSERT: scan cannot assign to immutable value
// ASSERT: scan :24:9:
// ASSERT: scan call to 'this' must be first expression in constructor of struct 'Date'
// ASSERT: scan :27:16:
// ASSERT: scan non-static variable 'this' cannot be referenced from a static context
// ASSERT: scan :33:11:
// ASSERT: scan '_add' is not a member of class 'ArrayList<Struct-Date>'
