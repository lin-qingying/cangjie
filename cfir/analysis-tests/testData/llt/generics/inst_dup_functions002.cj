// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

class C<X, Y> {
    public static func f(a: X, b: Y) {}
    public static func f(a: Bool, b: Int64) {}
}

interface I<X, Y> {
    static func f(a: X, b: Y) {}
    static func f(a: Bool, b: Int64) {}
}

struct R<X, Y> {
    public static func f(a: X, b: Y) {}
    public static func f(a: Bool, b: Int64) {}
}

main() {
    C<Bool, Int64>.f(true, 1)
    I<Bool, Int64>.f(true, 1)
    R<Bool, Int64>.f(true, 1)
    return 0;
}

/* SCAN
error: ambiguous match for function call 'f'
  ==> inst_dup_functions002.cj:25:20:
   | 
25 |     C<Bool, Int64>.f(true, 1)
   |                    ^ 
   | 
note: found candidate
  ==> inst_dup_functions002.cj:10:24:
   | 
10 |     public static func f(a: X, b: Y) {}
   |                        ^ 
   | 
note: found candidate
  ==> inst_dup_functions002.cj:11:24:
   | 
11 |     public static func f(a: Bool, b: Int64) {}
   |                        ^ 
   | 

error: ambiguous match for function call 'f'
  ==> inst_dup_functions002.cj:26:20:
   | 
26 |     I<Bool, Int64>.f(true, 1)
   |                    ^ 
   | 
note: found candidate
  ==> inst_dup_functions002.cj:15:17:
   | 
15 |     static func f(a: X, b: Y) {}
   |                 ^ 
   | 
note: found candidate
  ==> inst_dup_functions002.cj:16:17:
   | 
16 |     static func f(a: Bool, b: Int64) {}
   |                 ^ 
   | 

error: ambiguous match for function call 'f'
  ==> inst_dup_functions002.cj:27:20:
   | 
27 |     R<Bool, Int64>.f(true, 1)
   |                    ^ 
   | 
note: found candidate
  ==> inst_dup_functions002.cj:20:24:
   | 
20 |     public static func f(a: X, b: Y) {}
   |                        ^ 
   | 
note: found candidate
  ==> inst_dup_functions002.cj:21:24:
   | 
21 |     public static func f(a: Bool, b: Int64) {}
   |                        ^ 
   | 

error: generic instantiation 'C<Bool, Int64>' causes ambiguous function 'f'
  ==> inst_dup_functions002.cj:25:5:
   | 
25 |     C<Bool, Int64>.f(true, 1)
   |     ^^^^^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions002.cj:10:24:
   | 
10 |     public static func f(a: X, b: Y) {}
   |                        ^ 
   | 
note: found candidate
  ==> inst_dup_functions002.cj:11:24:
   | 
11 |     public static func f(a: Bool, b: Int64) {}
   |                        ^ 
   | 

error: generic instantiation 'I<Bool, Int64>' causes ambiguous function 'f'
  ==> inst_dup_functions002.cj:26:5:
   | 
26 |     I<Bool, Int64>.f(true, 1)
   |     ^^^^^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions002.cj:15:17:
   | 
15 |     static func f(a: X, b: Y) {}
   |                 ^ 
   | 
note: found candidate
  ==> inst_dup_functions002.cj:16:17:
   | 
16 |     static func f(a: Bool, b: Int64) {}
   |                 ^ 
   | 

error: generic instantiation 'R<Bool, Int64>' causes ambiguous function 'f'
  ==> inst_dup_functions002.cj:27:5:
   | 
27 |     R<Bool, Int64>.f(true, 1)
   |     ^^^^^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions002.cj:20:24:
   | 
20 |     public static func f(a: X, b: Y) {}
   |                        ^ 
   | 
note: found candidate
  ==> inst_dup_functions002.cj:21:24:
   | 
21 |     public static func f(a: Bool, b: Int64) {}
   |                        ^ 
   | 

6 errors generated, 6 errors printed.
*/
