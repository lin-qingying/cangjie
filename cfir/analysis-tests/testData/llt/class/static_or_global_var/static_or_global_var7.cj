// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    static init() {
        C.fun()
    }
}

func getNum() {
    8
}

class C {
    static let NUM = getNum()
    public static func fun(): Int64 {
        println(<!USED_BEFORE_INITIALIZATION!>NUM<!>)
        <!USED_BEFORE_INITIALIZATION!>NUM<!>
    }
}

main(): Int64 {
    0
}
