// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

open class A {}

class E<X, Y> {
    public static func f(a: X, b: Y) {
        print("f(a: X, b: Y)")
    }
    public static func f(a: A, b: Int64) {
        print("f(a: X, b: Int64)")
    }
}

main() {
    E<A, Int64>.f(A(), 1)
    return 0;
}

/* SCAN
error: ambiguous match for function call 'f'
  ==> inst_dup_functions005.cj:21:17:
   | 
21 |     E<A, Int64>.f(A(), 1)
   |                 ^ 
   | 
note: found candidate
  ==> inst_dup_functions005.cj:12:24:
   | 
12 |     public static func f(a: X, b: Y) {
   |                        ^ 
   | 
note: found candidate
  ==> inst_dup_functions005.cj:15:24:
   | 
15 |     public static func f(a: A, b: Int64) {
   |                        ^ 
   | 

error: generic instantiation 'E<Class-A, Int64>' causes ambiguous function 'f'
  ==> inst_dup_functions005.cj:21:5:
   | 
21 |     E<A, Int64>.f(A(), 1)
   |     ^^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions005.cj:12:24:
   | 
12 |     public static func f(a: X, b: Y) {
   |                        ^ 
   | 
note: found candidate
  ==> inst_dup_functions005.cj:15:24:
   | 
15 |     public static func f(a: A, b: Int64) {
   |                        ^ 
   | 

2 errors generated, 2 errors printed.
*/
