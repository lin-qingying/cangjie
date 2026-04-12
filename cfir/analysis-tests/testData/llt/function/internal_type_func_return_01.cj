// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class C {}

public class A<T> {}

public var a = C()
public var a1 = (C(), C())
public var b: C = C()

public var (a2, b2): (C, C) = (C(), C())

public var (a3, b3) = (C(), C())

public func foo() {
    C()
}

public func foo1(): C {
    C()
}

public func foo2() {
    A<C>()
}

public func foo3(): A<C> {
    A<C>()
}

main() {
    return 0
}

/* SCAN
error: 'public' declaration uses 'internal' types
  ==> internal_type_func_return_01.cj:13:12:
   | 
13 | public var a = C()
   |            ^ inferred type 'Class-C' contains 'internal' type
   | 
note: the 'internal' type is 'Class-C'
 ==> internal_type_func_return_01.cj:9:7:
  | 
9 | class C {}
  |       ^ 
  | 

error: 'public' declaration uses 'internal' types
  ==> internal_type_func_return_01.cj:14:12:
   | 
14 | public var a1 = (C(), C())
   |            ^^ inferred type 'Tuple<Class-C, Class-C>' contains 'internal' type
   | 
note: the 'internal' type is 'Class-C'
 ==> internal_type_func_return_01.cj:9:7:
  | 
9 | class C {}
  |       ^ 
  | 

error: 'public' declaration uses 'internal' types
  ==> internal_type_func_return_01.cj:15:12:
   | 
15 | public var b: C = C()
   |            ^ 
   | 
note: type 'Class-C' contains 'internal' type
  ==> internal_type_func_return_01.cj:15:15:
   | 
15 | public var b: C = C()
   |               ^ 
   | 
note: the 'internal' type is 'Class-C'
 ==> internal_type_func_return_01.cj:9:7:
  | 
9 | class C {}
  |       ^ 
  | 

error: 'public' declaration uses 'internal' types
  ==> internal_type_func_return_01.cj:17:13:
   | 
17 | public var (a2, b2): (C, C) = (C(), C())
   |             ^^ inferred type 'Class-C' contains 'internal' type
   | 
note: the 'internal' type is 'Class-C'
 ==> internal_type_func_return_01.cj:9:7:
  | 
9 | class C {}
  |       ^ 
  | 

error: 'public' declaration uses 'internal' types
  ==> internal_type_func_return_01.cj:17:17:
   | 
17 | public var (a2, b2): (C, C) = (C(), C())
   |                 ^^ inferred type 'Class-C' contains 'internal' type
   | 
note: the 'internal' type is 'Class-C'
 ==> internal_type_func_return_01.cj:9:7:
  | 
9 | class C {}
  |       ^ 
  | 

error: 'public' declaration uses 'internal' types
  ==> internal_type_func_return_01.cj:19:13:
   | 
19 | public var (a3, b3) = (C(), C())
   |             ^^ inferred type 'Class-C' contains 'internal' type
   | 
note: the 'internal' type is 'Class-C'
 ==> internal_type_func_return_01.cj:9:7:
  | 
9 | class C {}
  |       ^ 
  | 

error: 'public' declaration uses 'internal' types
  ==> internal_type_func_return_01.cj:19:17:
   | 
19 | public var (a3, b3) = (C(), C())
   |                 ^^ inferred type 'Class-C' contains 'internal' type
   | 
note: the 'internal' type is 'Class-C'
 ==> internal_type_func_return_01.cj:9:7:
  | 
9 | class C {}
  |       ^ 
  | 

error: 'public' declaration uses 'internal' types
  ==> internal_type_func_return_01.cj:21:13:
   | 
21 | public func foo() {
   |             ^^^ inferred type 'Class-C' contains 'internal' type
   | 
note: the 'internal' type is 'Class-C'
 ==> internal_type_func_return_01.cj:9:7:
  | 
9 | class C {}
  |       ^ 
  | 

error: 'public' declaration uses 'internal' types
  ==> internal_type_func_return_01.cj:25:13:
   | 
25 | public func foo1(): C {
   |             ^^^^ 
   | 
note: type 'Class-C' contains 'internal' type
  ==> internal_type_func_return_01.cj:25:21:
   | 
25 | public func foo1(): C {
   |                     ^ 
   | 
note: the 'internal' type is 'Class-C'
 ==> internal_type_func_return_01.cj:9:7:
  | 
9 | class C {}
  |       ^ 
  | 

error: 'public' declaration uses 'internal' types
  ==> internal_type_func_return_01.cj:29:13:
   | 
29 | public func foo2() {
   |             ^^^^ inferred type 'Class-A<Class-C>' contains 'internal' type
   | 
note: the 'internal' type is 'Class-C'
 ==> internal_type_func_return_01.cj:9:7:
  | 
9 | class C {}
  |       ^ 
  | 

error: 'public' declaration uses 'internal' types
  ==> internal_type_func_return_01.cj:33:13:
   | 
33 | public func foo3(): A<C> {
   |             ^^^^ 
   | 
note: type 'Class-A<Class-C>' contains 'internal' type
  ==> internal_type_func_return_01.cj:33:21:
   | 
33 | public func foo3(): A<C> {
   |                     ^^^^ 
   | 
note: the 'internal' type is 'Class-C'
 ==> internal_type_func_return_01.cj:9:7:
  | 
9 | class C {}
  |       ^ 
  | 

11 errors generated, 11 errors printed.
*/
