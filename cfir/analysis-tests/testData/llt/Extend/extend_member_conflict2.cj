// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

extend A {
    public func x1() {}
    public static func x2() {}
    public func y1() {}
    public static func y2() {}
    public func f1() {}
    public static func f2() {}
    public func g1() {}
    public static func g2() {}
}

extend A {
    private func a() {}
    public func b() {}
    private func c() {}
    public func d() {}
    private func e() {}
}

class A {
    public static let x1: Int64 = 1
    public static let x2: Int64 = 1
    public let y1: Int64 = 2
    public let y2: Int64 = 2
    public static func f1(): Unit {}
    public static func f2(): Unit {}
    public func g1(): Unit {}
    public func g2(): Unit {}

    public func a() {}
    protected func b() {}
    protected func c() {}
    private func d() {}
    private func e() {}
}

/* SCAN
error: non-static member 'x1' cannot have the same name with static member in extended type
  ==> extend_member_conflict2.cj:10:5:
   | 
10 |     public func x1() {}
   |     ^ 
   | 

error: extend member 'x2' is not allowed to shadow members of 'Class-A'
  ==> extend_member_conflict2.cj:11:24:
   | 
11 |     public static func x2() {}
   |                        ^^ 
   | 
note: shadowed definition of 'x2' is here
  ==> extend_member_conflict2.cj:30:23:
   | 
30 |     public static let x2: Int64 = 1
   |                       ^^ 
   | 

error: extend member 'y1' is not allowed to shadow members of 'Class-A'
  ==> extend_member_conflict2.cj:12:17:
   | 
12 |     public func y1() {}
   |                 ^^ 
   | 
note: shadowed definition of 'y1' is here
  ==> extend_member_conflict2.cj:31:16:
   | 
31 |     public let y1: Int64 = 2
   |                ^^ 
   | 

error: static member 'y2' cannot have the same name with non-static member in extended type
  ==> extend_member_conflict2.cj:13:5:
   | 
13 |     public static func y2() {}
   |     ^ 
   | 

error: non-static member 'f1' cannot have the same name with static member in extended type
  ==> extend_member_conflict2.cj:14:5:
   | 
14 |     public func f1() {}
   |     ^ 
   | 

error: extend member 'f1' is not allowed to shadow members of 'Class-A'
  ==> extend_member_conflict2.cj:14:17:
   | 
14 |     public func f1() {}
   |                 ^^ 
   | 
note: shadowed definition of 'f1' is here
  ==> extend_member_conflict2.cj:33:24:
   | 
33 |     public static func f1(): Unit {}
   |                        ^^ 
   | 

error: extend member 'f2' is not allowed to shadow members of 'Class-A'
  ==> extend_member_conflict2.cj:15:24:
   | 
15 |     public static func f2() {}
   |                        ^^ 
   | 
note: shadowed definition of 'f2' is here
  ==> extend_member_conflict2.cj:34:24:
   | 
34 |     public static func f2(): Unit {}
   |                        ^^ 
   | 

error: extend member 'g1' is not allowed to shadow members of 'Class-A'
  ==> extend_member_conflict2.cj:16:17:
   | 
16 |     public func g1() {}
   |                 ^^ 
   | 
note: shadowed definition of 'g1' is here
  ==> extend_member_conflict2.cj:35:17:
   | 
35 |     public func g1(): Unit {}
   |                 ^^ 
   | 

error: static member 'g2' cannot have the same name with non-static member in extended type
  ==> extend_member_conflict2.cj:17:5:
   | 
17 |     public static func g2() {}
   |     ^ 
   | 

error: extend member 'g2' is not allowed to shadow members of 'Class-A'
  ==> extend_member_conflict2.cj:17:24:
   | 
17 |     public static func g2() {}
   |                        ^^ 
   | 
note: shadowed definition of 'g2' is here
  ==> extend_member_conflict2.cj:36:17:
   | 
36 |     public func g2(): Unit {}
   |                 ^^ 
   | 

error: extend member 'a' is not allowed to shadow members of 'Class-A'
  ==> extend_member_conflict2.cj:21:18:
   | 
21 |     private func a() {}
   |                  ^ 
   | 
note: shadowed definition of 'a' is here
  ==> extend_member_conflict2.cj:38:17:
   | 
38 |     public func a() {}
   |                 ^ 
   | 

error: extend member 'b' is not allowed to shadow members of 'Class-A'
  ==> extend_member_conflict2.cj:22:17:
   | 
22 |     public func b() {}
   |                 ^ 
   | 
note: shadowed definition of 'b' is here
  ==> extend_member_conflict2.cj:39:20:
   | 
39 |     protected func b() {}
   |                    ^ 
   | 

error: extend member 'c' is not allowed to shadow members of 'Class-A'
  ==> extend_member_conflict2.cj:23:18:
   | 
23 |     private func c() {}
   |                  ^ 
   | 
note: shadowed definition of 'c' is here
  ==> extend_member_conflict2.cj:40:20:
   | 
40 |     protected func c() {}
   |                    ^ 
   | 

error: 'main' is missing
 ==> extend_member_conflict2.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

14 errors generated, 14 errors printed.
*/
