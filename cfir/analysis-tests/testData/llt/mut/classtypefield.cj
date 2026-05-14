// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt 2>&1 | compare %f

class T {
    var i = 1
}

struct C {
    var a = T()
}

struct D {
    var d = C()
}

struct A {
    var a = T()
    var b = D()
    func foo() {
        a.i = 2 // ok
        <!CANNOT_MODIFY_VAR!>a<!> = T() // error
        <!CANNOT_MODIFY_VAR!>b<!>.d.a = T() // error
        b.d.a.i = 2 // ok
    }
}

main() {}
