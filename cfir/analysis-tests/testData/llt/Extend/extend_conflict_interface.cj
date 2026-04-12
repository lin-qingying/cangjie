// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A {
    static var a: Int32 = 5
}

extend A {
    public static func me() {
        return 0
    }

    static func me(): Int64 {
        return Int64(a)
    }

    func me(): Int32 {
        return 0
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: overloaded functions 'me' cannot mix static and non-static
  ==> extend_conflict_interface.cj:18:17:
   | 
18 |     static func me(): Int64 {
   |                 ^^ 
   | 
note: non-static function is here
  ==> extend_conflict_interface.cj:22:10:
   | 
22 |     func me(): Int32 {
   |          ^^ 
   | 

error: overloaded functions 'me' cannot mix static and non-static
  ==> extend_conflict_interface.cj:14:24:
   | 
14 |     public static func me() {
   |                        ^^ 
   | 
note: non-static function is here
  ==> extend_conflict_interface.cj:22:10:
   | 
22 |     func me(): Int32 {
   |          ^^ 
   | 

2 errors generated, 2 errors printed.
*/
