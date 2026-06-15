<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// class cannot access member functions defined in its extend
class A {
    func foo() {
        <!UNRESOLVED_REFERENCE!>g<!>o()
    }
}

extend A {
    func go() {}
}
