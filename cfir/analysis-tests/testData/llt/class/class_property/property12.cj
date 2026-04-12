// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

main(): Int64 {
    var a = Doo()
    if (a.m != 10) {
        return 1
    }
    if (a.n != 20) {
        return 2
    }
    a.m = 1
    if (a.m != 10) {
        return 3
    }
    if (a.n != 20) {
        return 4
    }

    return 0
}

interface Boo {
    mut prop m: Int32 {
        get() {
            return 1
        }
        set(input) {
            ()
        }
    }

    prop n: Int32 {
        get() {
            return (2)
        }
    }
}

interface Coo <: Boo {
    mut prop m: Int32 {
        get() {
            return (10)
        }
        set(input) {
            ()
        }
    }
    prop n: Int32 {
        get() {
            return 20
        }
    }
}

class Doo <: Coo {}

/* SCAN
warning: unused variable:'input'
  ==> property12.cj:34:13:
   | 
34 |         set(input) {
   |             ^^^^^ unused variable
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused variable:'input'
  ==> property12.cj:51:13:
   | 
51 |         set(input) {
   |             ^^^^^ unused variable
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

2 warnings generated, 2 warnings printed.
*/
