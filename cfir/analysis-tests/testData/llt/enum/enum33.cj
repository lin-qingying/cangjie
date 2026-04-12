// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

enum E1 {
    e | e2

    public func foo() {
        e2
    } // Choose E1.e2
}

extend E1 {
    public func goo() {
        e2
    } // Choose E1.e2
}

enum E2 {
    e(Int64) | e2

    public func foo() {
        e
    } // Choose E1.e
}

extend E2 {
    public func goo() {
        e
    } // Choose E1.e
}

enum E<T> {
    e(T) | e(T, T)

    public func x(i: T) {
        e(i);
        e(i, i)
    } // Choose e in E<T>
}

enum F<T> {
    e(T) | e(T, T)

    public func x(i: T) {
        e(i);
        e(i, i)
    } // Choose e in F<T>
}

extend<T> F<T> {
    public func xx(i: T) {
        e(i);
        e(i, i)
    } // Choose e in F<T>
}

main(): Int64 {
    0
}

/* SCAN
error: enum constructor 'e' must be used with arguments
  ==> enum33.cj:27:9:
   | 
27 |         e
   |         ^ 
   | 
note: found candidate
  ==> enum33.cj:24:5:
   | 
24 |     e(Int64) | e2
   |     ^ 
   | 

error: enum constructor 'e' must be used with arguments
  ==> enum33.cj:33:9:
   | 
33 |         e
   |         ^ 
   | 
note: found candidate
  ==> enum33.cj:24:5:
   | 
24 |     e(Int64) | e2
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
