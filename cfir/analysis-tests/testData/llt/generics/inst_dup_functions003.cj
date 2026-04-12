// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

class C<T, U> {
    public static func foo(a: T, b: U) {}
    public static func foo(a: U, b: U) {}
}

struct R<T, U> {
    public static func foo(a: T, b: U) {}
    public static func foo(a: U, b: U) {}
}

func bar<X>(a: X) {
    C<X, X>.foo(a, a)
    R<X, X>.foo(a, a)
}

main() {
    return 0;
}

/* SCAN
error: ambiguous match for function call 'foo'
  ==> inst_dup_functions003.cj:20:13:
   | 
20 |     C<X, X>.foo(a, a)
   |             ^ 
   | 
note: found candidate
  ==> inst_dup_functions003.cj:10:24:
   | 
10 |     public static func foo(a: T, b: U) {}
   |                        ^ 
   | 
note: found candidate
  ==> inst_dup_functions003.cj:11:24:
   | 
11 |     public static func foo(a: U, b: U) {}
   |                        ^ 
   | 

error: ambiguous match for function call 'foo'
  ==> inst_dup_functions003.cj:21:13:
   | 
21 |     R<X, X>.foo(a, a)
   |             ^ 
   | 
note: found candidate
  ==> inst_dup_functions003.cj:15:24:
   | 
15 |     public static func foo(a: T, b: U) {}
   |                        ^ 
   | 
note: found candidate
  ==> inst_dup_functions003.cj:16:24:
   | 
16 |     public static func foo(a: U, b: U) {}
   |                        ^ 
   | 

error: generic instantiation 'C<Generics-X, Generics-X>' causes ambiguous function 'foo'
  ==> inst_dup_functions003.cj:20:5:
   | 
20 |     C<X, X>.foo(a, a)
   |     ^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions003.cj:10:24:
   | 
10 |     public static func foo(a: T, b: U) {}
   |                        ^^^ 
   | 
note: found candidate
  ==> inst_dup_functions003.cj:11:24:
   | 
11 |     public static func foo(a: U, b: U) {}
   |                        ^^^ 
   | 

error: generic instantiation 'R<Generics-X, Generics-X>' causes ambiguous function 'foo'
  ==> inst_dup_functions003.cj:21:5:
   | 
21 |     R<X, X>.foo(a, a)
   |     ^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions003.cj:15:24:
   | 
15 |     public static func foo(a: T, b: U) {}
   |                        ^^^ 
   | 
note: found candidate
  ==> inst_dup_functions003.cj:16:24:
   | 
16 |     public static func foo(a: U, b: U) {}
   |                        ^^^ 
   | 

4 errors generated, 4 errors printed.
*/
