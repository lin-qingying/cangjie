// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %noColor %f %error_count_limit_all 2>&1 | compare %f

struct A {
    public let a: Int64 = 1
    private let b: Int64 = 2
    public static let c: Int64 = 3
    private static let d: Int64 = 4
    public func m() {
        a // ok
        b // ok
        c // ok
        d // ok
        l() // ok
        o() // ok
    }
    private func l() {}
    public static func n() {
        a
        b
        c // ok
        d // ok
        l()
        m()
        o() // ok
    }
    private static func o() {}
    public operator func +(q: A) {
        a
        b
        c
        d
        l()
        m()
        n()
        o()
    }
    private operator func -(s: A) {}
}

main() {
    A().a // ok
    A.a

    A().b
    A.b

    A().c
    A.c // ok

    A().d
    A.d

    A().m()
    A.m()

    A().l()
    A.l()

    A().n()
    A.n() // ok

    A().o()
    A.o()

    A + A
    A() + A()

    A() - A()
    return 0
}

/* SCAN
error: 'l' is non-static member, cannot be accessed by static function 'n'
  ==> record_access_control.cj:28:9:
   | 
28 |         l()
   |         ^ 
   | 

error: 'm' is non-static member, cannot be accessed by static function 'n'
  ==> record_access_control.cj:29:9:
   | 
29 |         m()
   |         ^ 
   | 

error: 'a' is non-static member, cannot access by type name
  ==> record_access_control.cj:48:5:
   | 
48 |     A.a
   |     ^ 
   | 

error: 'b' is non-static member, cannot access by type name
  ==> record_access_control.cj:51:5:
   | 
51 |     A.b
   |     ^ 
   | 

error: object cannot access static member 'c'
  ==> record_access_control.cj:53:5:
   | 
53 |     A().c
   |     ^ 
   | 

error: object cannot access static member 'd'
  ==> record_access_control.cj:56:5:
   | 
56 |     A().d
   |     ^ 
   | 

error: 'm' is non-static member, cannot access by type name
  ==> record_access_control.cj:60:5:
   | 
60 |     A.m()
   |     ^ 
   | 

error: no matching function declaration for function call 'l'
  ==> record_access_control.cj:62:9:
   | 
62 |     A().l()
   |         ^ 
   | 

error: 'l' is non-static member, cannot access by type name
  ==> record_access_control.cj:63:5:
   | 
63 |     A.l()
   |     ^ 
   | 

error: object cannot access static member 'n'
  ==> record_access_control.cj:65:5:
   | 
65 |     A().n()
   |     ^ 
   | 

error: object cannot access static member 'o'
  ==> record_access_control.cj:68:5:
   | 
68 |     A().o()
   |     ^ 
   | 

error: no matching function declaration for function call 'o'
  ==> record_access_control.cj:69:7:
   | 
69 |     A.o()
   |       ^ 
   | 

error: invalid binary operator '+' on type 'Struct-A' and 'Struct-A'
  ==> record_access_control.cj:71:7:
   | 
71 |     A + A
   |       ^ 
   | 
   # note: you may want to implement 'operator func +(right: Struct-A)' for type 'Struct-A'

error: invalid binary operator '-' on type 'Struct-A' and 'Struct-A'
  ==> record_access_control.cj:74:9:
   | 
74 |     A() - A()
   |         ^ 
   | 
   # note: you may want to implement 'operator func -(right: Struct-A)' for type 'Struct-A'

error: 'a' is non-static member, cannot be accessed by static function 'n'
  ==> record_access_control.cj:24:9:
   | 
24 |         a
   |         ^ 
   | 

error: 'b' is non-static member, cannot be accessed by static function 'n'
  ==> record_access_control.cj:25:9:
   | 
25 |         b
   |         ^ 
   | 

error: can not access field 'b'
  ==> record_access_control.cj:50:5:
   | 
50 |     A().b
   |     ^ 
   | 

error: can not access field 'd'
  ==> record_access_control.cj:57:5:
   | 
57 |     A.d
   |     ^ 
   | 

error: expected member name or constructor call after 'A' type name
  ==> record_access_control.cj:71:5:
   | 
71 |     A + A
   |     ^ 
   | 
note: found candidate
 ==> record_access_control.cj:9:1:
  | 
9 | struct A {
  | ^ 
  | 

error: expected member name or constructor call after 'A' type name
  ==> record_access_control.cj:71:9:
   | 
71 |     A + A
   |         ^ 
   | 
note: found candidate
 ==> record_access_control.cj:9:1:
  | 
9 | struct A {
  | ^ 
  | 

20 errors generated, 20 errors printed.
*/
