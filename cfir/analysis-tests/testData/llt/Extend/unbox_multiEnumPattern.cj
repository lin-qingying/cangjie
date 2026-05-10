// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class B {
    func g(): Int64 {
        println("func g in B!")
        10
    }
}

interface I {
    func f(): Int64
}

extend B <: I {
    func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>f<!>() {
        println("In extend B!");
        1
    }
}

main(): Int64 {
    var m: E = C(B())

    var res: Int64 = match (m) {
        case <!VAR_IN_OR_PATTERN!>C(x: B) | D(<!REDECLARATION!>x<!>: B)<!> => x.f();
        case _ => 0
    }

    var n: E = D(B())

    res = match (n) {
        case <!VAR_IN_OR_PATTERN!>C(x: B) | D(<!REDECLARATION!>x<!>: B)<!> => x.g()
        case _ => 0
    }
    res
}

enum E {
    C(I) | D(I)
}
