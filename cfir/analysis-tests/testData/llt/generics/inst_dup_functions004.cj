// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

class A<X, Y, Z> {
    public func test(a: A<X, Y, Z>, b: A<X, Y, Z>) {}
    public func test(a: A<X, Y, Int8>, b: A<X, Y, Z>) {}
    public func test(a: A<Int8, Int8, Int8>, b: A<X, Y, Z>) {}
}

interface B<X, Y, Z> {
    func test(a: B<X, Y, Z>, b: B<X, Y, Z>) {}
    func test(a: B<X, Y, Int8>, b: B<X, Y, Z>) {}
    func test(a: B<Int8, Int8, Int8>, b: B<X, Y, Z>) {}
}

struct C<X, Y, Z> {
    public func test(a: C<X, Y, Z>, b: C<X, Y, Z>) {}
    public func test(a: C<X, Y, Int8>, b: C<X, Y, Z>) {}
    public func test(a: C<Int8, Int8, Int8>, b: C<X, Y, Z>) {}
}

/* SCAN
error: generic instantiation 'A<Generics-X, Generics-Y, Generics-Z>' causes ambiguous function 'test'
  ==> inst_dup_functions004.cj:10:25:
   | 
10 |     public func test(a: A<X, Y, Z>, b: A<X, Y, Z>) {}
   |                         ^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:10:17:
   | 
10 |     public func test(a: A<X, Y, Z>, b: A<X, Y, Z>) {}
   |                 ^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:11:17:
   | 
11 |     public func test(a: A<X, Y, Int8>, b: A<X, Y, Z>) {}
   |                 ^^^^ 
   | 

error: generic instantiation 'A<Int8, Int8, Int8>' causes ambiguous function 'test'
  ==> inst_dup_functions004.cj:12:25:
   | 
12 |     public func test(a: A<Int8, Int8, Int8>, b: A<X, Y, Z>) {}
   |                         ^^^^^^^^^^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:10:17:
   | 
10 |     public func test(a: A<X, Y, Z>, b: A<X, Y, Z>) {}
   |                 ^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:11:17:
   | 
11 |     public func test(a: A<X, Y, Int8>, b: A<X, Y, Z>) {}
   |                 ^^^^ 
   | 

error: generic instantiation 'A<Generics-X, Generics-Y, Int8>' causes ambiguous function 'test'
  ==> inst_dup_functions004.cj:11:25:
   | 
11 |     public func test(a: A<X, Y, Int8>, b: A<X, Y, Z>) {}
   |                         ^^^^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:10:17:
   | 
10 |     public func test(a: A<X, Y, Z>, b: A<X, Y, Z>) {}
   |                 ^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:11:17:
   | 
11 |     public func test(a: A<X, Y, Int8>, b: A<X, Y, Z>) {}
   |                 ^^^^ 
   | 

error: generic instantiation 'B<Generics-X, Generics-Y, Generics-Z>' causes ambiguous function 'test'
  ==> inst_dup_functions004.cj:16:18:
   | 
16 |     func test(a: B<X, Y, Z>, b: B<X, Y, Z>) {}
   |                  ^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:16:10:
   | 
16 |     func test(a: B<X, Y, Z>, b: B<X, Y, Z>) {}
   |          ^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:17:10:
   | 
17 |     func test(a: B<X, Y, Int8>, b: B<X, Y, Z>) {}
   |          ^^^^ 
   | 

error: generic instantiation 'B<Int8, Int8, Int8>' causes ambiguous function 'test'
  ==> inst_dup_functions004.cj:18:18:
   | 
18 |     func test(a: B<Int8, Int8, Int8>, b: B<X, Y, Z>) {}
   |                  ^^^^^^^^^^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:16:10:
   | 
16 |     func test(a: B<X, Y, Z>, b: B<X, Y, Z>) {}
   |          ^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:17:10:
   | 
17 |     func test(a: B<X, Y, Int8>, b: B<X, Y, Z>) {}
   |          ^^^^ 
   | 

error: generic instantiation 'B<Generics-X, Generics-Y, Int8>' causes ambiguous function 'test'
  ==> inst_dup_functions004.cj:17:18:
   | 
17 |     func test(a: B<X, Y, Int8>, b: B<X, Y, Z>) {}
   |                  ^^^^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:16:10:
   | 
16 |     func test(a: B<X, Y, Z>, b: B<X, Y, Z>) {}
   |          ^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:17:10:
   | 
17 |     func test(a: B<X, Y, Int8>, b: B<X, Y, Z>) {}
   |          ^^^^ 
   | 

error: generic instantiation 'C<Generics-X, Generics-Y, Generics-Z>' causes ambiguous function 'test'
  ==> inst_dup_functions004.cj:22:25:
   | 
22 |     public func test(a: C<X, Y, Z>, b: C<X, Y, Z>) {}
   |                         ^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:22:17:
   | 
22 |     public func test(a: C<X, Y, Z>, b: C<X, Y, Z>) {}
   |                 ^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:23:17:
   | 
23 |     public func test(a: C<X, Y, Int8>, b: C<X, Y, Z>) {}
   |                 ^^^^ 
   | 

error: generic instantiation 'C<Int8, Int8, Int8>' causes ambiguous function 'test'
  ==> inst_dup_functions004.cj:24:25:
   | 
24 |     public func test(a: C<Int8, Int8, Int8>, b: C<X, Y, Z>) {}
   |                         ^^^^^^^^^^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:22:17:
   | 
22 |     public func test(a: C<X, Y, Z>, b: C<X, Y, Z>) {}
   |                 ^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:23:17:
   | 
23 |     public func test(a: C<X, Y, Int8>, b: C<X, Y, Z>) {}
   |                 ^^^^ 
   | 

error: generic instantiation 'C<Generics-X, Generics-Y, Int8>' causes ambiguous function 'test'
  ==> inst_dup_functions004.cj:23:25:
   | 
23 |     public func test(a: C<X, Y, Int8>, b: C<X, Y, Z>) {}
   |                         ^^^^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:22:17:
   | 
22 |     public func test(a: C<X, Y, Z>, b: C<X, Y, Z>) {}
   |                 ^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions004.cj:23:17:
   | 
23 |     public func test(a: C<X, Y, Int8>, b: C<X, Y, Z>) {}
   |                 ^^^^ 
   | 

error: 'main' is missing
 ==> inst_dup_functions004.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

10 errors generated, 10 errors printed.
*/
