// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE)EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// (not CJNATIVE) EXEC: %compiler %f

interface P<T> {
    static prop p: T
}

class A <: P<A> {
    public static prop p: A {
        get() {
            return A()
        }
    }
}

open class B<T1> where T1 <: P<T1> {
    prop test: T1 {
        get() {
            return T1.p
        }
    }
}

main(): Int64 {
    let x = B<A>().test
    if (x is A) {
        return 0
    }
    return 1
}

/* SCAN
error: static member cannot depend on generic parameter 'Generics-T'
  ==> generic_upperbound_reference_04.cj:11:20:
   | 
11 |     static prop p: T
   |                    ^ 
   | 
note: generic argument declaration here
  ==> generic_upperbound_reference_04.cj:10:13:
   | 
10 | interface P<T> {
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
