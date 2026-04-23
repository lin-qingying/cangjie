// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct Base12 {
    public var du: Int64 = 1
    <!PROPERTY_MUST_HAVE_ACCESSORS!>public prop a: Int64 {
    }<!>
    public func f(): Int64 {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = 2
        return 0
    }
}

main() {
    var x = Base12()
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>x.a<!> = 4
    if (x.a == 4) {
        print("${x.a}")
        return 0
    }
    return 1
}
