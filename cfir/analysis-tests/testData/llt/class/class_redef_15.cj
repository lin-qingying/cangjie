// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class Boo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: Int64 {
    public static func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>V<!>>(x: T, y: V) where V <: Int32 {
        1
    }
}

class Coo <: Boo<Int64> {
    public static redef func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>V<!>>(x: Int64, y: V) where V <: Int32 {
        2
    }
}

main() {
    var c = Coo.foo<Int32>(0, 0)
    var b = Boo<Int64>.foo<Int32>(0, 0)
    if (c != 2 && b != 1) {
        return 1
    }
    return 0
}
