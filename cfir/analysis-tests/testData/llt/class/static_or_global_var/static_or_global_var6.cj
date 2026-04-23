// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    static var AFLAG: Bool
    static init() {
        AFLAG = B.<!USED_BEFORE_INITIALIZATION!>BFLAG<!>
    }
}

class B {
    public static let BFLAG: Bool = true
}

main(): Int64 {
    0
}
