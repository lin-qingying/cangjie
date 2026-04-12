// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

main(): Int64 {
    return 0
}

open class Boo {
    public var a: Int64 = 1
    public static var b: Int64 = 1
    protected mut open prop a1: Int64 {
        get() {
            return (a + 1)
        }
        set(v) {
            a = v
        }
    }
}

class Coo <: Boo {
    private mut override prop a1: Int64 {
        get() {
            return (a + 2)
        }
        set(v) {
            a = v
        }
    }
}

/* SCAN
error: a deriving member must be at least as visible as its base member
  ==> property11.cj:27:31:
   | 
27 |     private mut override prop a1: Int64 {
   |                               ^^ the visibility of the deriving 'a1' is 'private'
   | 
note: the visibility of the base 'a1' is 'protected'
  ==> property11.cj:16:29:
   | 
16 |     protected mut open prop a1: Int64 {
   |                             ^^ 
   | 

1 error generated, 1 error printed.
*/
