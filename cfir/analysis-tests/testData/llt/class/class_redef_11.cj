// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    if (<!GENERIC_ARGUMENT_NO_MATCH!>Coo.foo<Int32>(1, 2)<!> != 2 || Boo<Int32>.foo<Int32>(3, 4) != 4) {
        return 1
    }
    return 0
}

open class Boo<T> {
    public static func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>V<!>>(x: T, y: V) where V <: Int32 {
        return y
    }
}

class Coo <: Boo<Int32> {
    public static redef func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>V<!>>(x: Int32, y: V) where V <: <!GENERIC_CONSTRAINT_NOT_LOOSER!>Int64<!> {
        return y
    }
}
