// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    static var a: Int32 = 5
}

extend A {
    public static func <!STATIC_FUNCTION_OVERLOAD_CONFLICTS!>me<!>() {
        return 0
    }

    static func <!STATIC_FUNCTION_OVERLOAD_CONFLICTS!>me<!>(): Int64 {
        return Int64(a)
    }

    func me(): Int32 {
        return 0
    }
}

main(): Int64 {
    return 0
}
