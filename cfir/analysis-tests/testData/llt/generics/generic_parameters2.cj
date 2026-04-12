// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class A<X, Y> {}

open class B<X, Y> <: A<X, Y> {}

open class C<X, Y> <: A<X, Y> {}

func foo<Xot>() {}

open class A<X, Y> {}

open class B<X, Y> <: A<X, Y> {}

open class C<X, Y> <: A<X, Z, Y> {}

func foo<X, Y>(a: B<Y, X>, b: C<Y, X>): Int64 {
    0
}

main() {
    foo(B<Int32, Int64>(), C<Int32, Int64>())
}

/* SCAN
error: redefinition of declaration 'A'
  ==> generic_parameters2.cj:17:12:
   | 
17 | open class A<X, Y> {}
   |            ^ 
   | 
note: 'A' is previously declared here
 ==> generic_parameters2.cj:9:12:
  | 
9 | open class A<X, Y> {}
  |            ^ 
  | 

error: redefinition of declaration 'B'
  ==> generic_parameters2.cj:19:12:
   | 
19 | open class B<X, Y> <: A<X, Y> {}
   |            ^ 
   | 
note: 'B' is previously declared here
  ==> generic_parameters2.cj:11:12:
   | 
11 | open class B<X, Y> <: A<X, Y> {}
   |            ^ 
   | 

error: redefinition of declaration 'C'
  ==> generic_parameters2.cj:21:12:
   | 
21 | open class C<X, Y> <: A<X, Z, Y> {}
   |            ^ 
   | 
note: 'C' is previously declared here
  ==> generic_parameters2.cj:13:12:
   | 
13 | open class C<X, Y> <: A<X, Y> {}
   |            ^ 
   | 

error: ambiguous use of 'A'
  ==> generic_parameters2.cj:11:23:
   | 
11 | open class B<X, Y> <: A<X, Y> {}
   |                       ^^^^^^^ 
   | 
note: found candidate
 ==> generic_parameters2.cj:9:12:
  | 
9 | open class A<X, Y> {}
  |            ^ 
  | 
note: found candidate
  ==> generic_parameters2.cj:17:12:
   | 
17 | open class A<X, Y> {}
   |            ^ 
   | 

error: ambiguous use of 'A'
  ==> generic_parameters2.cj:13:23:
   | 
13 | open class C<X, Y> <: A<X, Y> {}
   |                       ^^^^^^^ 
   | 
note: found candidate
 ==> generic_parameters2.cj:9:12:
  | 
9 | open class A<X, Y> {}
  |            ^ 
  | 
note: found candidate
  ==> generic_parameters2.cj:17:12:
   | 
17 | open class A<X, Y> {}
   |            ^ 
   | 

error: ambiguous use of 'A'
  ==> generic_parameters2.cj:19:23:
   | 
19 | open class B<X, Y> <: A<X, Y> {}
   |                       ^^^^^^^ 
   | 
note: found candidate
 ==> generic_parameters2.cj:9:12:
  | 
9 | open class A<X, Y> {}
  |            ^ 
  | 
note: found candidate
  ==> generic_parameters2.cj:17:12:
   | 
17 | open class A<X, Y> {}
   |            ^ 
   | 

error: ambiguous use of 'A'
  ==> generic_parameters2.cj:21:23:
   | 
21 | open class C<X, Y> <: A<X, Z, Y> {}
   |                       ^^^^^^^^^^ 
   | 
note: found candidate
 ==> generic_parameters2.cj:9:12:
  | 
9 | open class A<X, Y> {}
  |            ^ 
  | 
note: found candidate
  ==> generic_parameters2.cj:17:12:
   | 
17 | open class A<X, Y> {}
   |            ^ 
   | 

error: undeclared type name 'Z'
  ==> generic_parameters2.cj:21:28:
   | 
21 | open class C<X, Y> <: A<X, Z, Y> {}
   |                            ^ 
   | 

15 errors generated, 8 errors printed.
*/
