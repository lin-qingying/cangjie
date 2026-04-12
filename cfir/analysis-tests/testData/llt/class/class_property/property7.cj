// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

struct B {
    public var b = 3
}

class A {
    public mut prop a: B {
        get() {
            return B()
        }
        set(_) {
            return ()
        }
    }
    public func f() {
        return B()
    }
}

main() {
    var a = A()
    a.a.b
    if (a.a.b == 3) {
        print("${a.a.b}\n")
        return 0
    }
    a.a = B()
    a.f()
    return 1
}

/* SCAN
warning: unused expression
  ==> property7.cj:30:5:
   | 
30 |     a.a.b
   |     ^^^^^ unused expression
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

1 warning generated, 1 warning printed.
*/
