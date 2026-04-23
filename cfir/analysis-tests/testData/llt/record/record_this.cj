// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

internal import std.collection.*

struct Date {
    public let width: Int32
    public let length: Int32 = <!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>.width
    public static var height: Int32 = 2
    public init() {
        this.width = 1
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.length<!> = 2
        this.f()
        this.f
        height = 3
    }
    public init(a: Int64) {
        <!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>
        <!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>(1)
    }
    public static func ff(): Date {
        return <!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>
    }
    public func f(): Date {
        return this
    }
    public func g(l: ArrayList<Date>) {
        l.<!NOT_MEMBER_OF!>_add<!>(this)
    }
}
