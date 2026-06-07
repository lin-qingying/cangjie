// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    if (B<Base>().a != "a") {
        return 2
    }
    return 0
}

class B<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: Base {
    public var b: String = ""
    public var a = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>.m2
}

enum Base {
    A | B

    public static prop m: String {
        get() {
            return "a"
        }
    }
}
