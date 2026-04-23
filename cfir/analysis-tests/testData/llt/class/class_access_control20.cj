// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A {
    public static func f() {}
}

class B <: A {
    private static redef func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>f<!>() {
        print(1)
    }
}

main() {
}
