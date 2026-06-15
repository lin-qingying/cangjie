// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


enum EE {
    A

    public prop a1: Int64 {
        get() {
            2
        }
    }
    public static prop a2: Int64 {
        get() {
            2
        }
    }
    public func test() {}
    public static func test1() {}
}

main(): Int64 {
    <!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>E<!>E.a1
    EE.A
    EE.a2
    <!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>E<!>E.test()
    <!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>E<!>E.test
    EE.test1
    EE.test1()
    0
}
