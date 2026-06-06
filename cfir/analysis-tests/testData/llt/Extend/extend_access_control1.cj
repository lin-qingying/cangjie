// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

// accessibility: functions in the extend body cannot access private members inside the class body

class A {}

extend A {
    private func test() {}
    func foo() {
        test()
        0
    }
}

main() {
    let a: A = A()
    a.<!sema_no_match_function_declaration_for_call!>t<!>est()
    a.foo()
    0
}
