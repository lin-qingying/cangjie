<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// accessibility: functions in the extend body cannot access private members inside the class body
class A {
    private func goo() {}
}

extend A {
    func foo() {
        <!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>oo()
        0
    }
}
