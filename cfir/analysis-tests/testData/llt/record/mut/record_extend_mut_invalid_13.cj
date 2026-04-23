// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    var b = R<!TOO_MANY_ARGUMENTS!>(a: 6)<!>
    b.c(7)
    return Int64(b.a - 7)
}

struct R {
    public var a: Int32 = 8
    public func cc() {
        <!UNRESOLVED_REFERENCE!>c<!>(1)
    }
}

extend R {
    public mut func c(b: Int32): Int32 {
        return 0
    }
}
