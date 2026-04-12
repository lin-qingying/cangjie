// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    var a: Int64 = 1
    static var b: Int64 = 1
    static init() {
        println(a)
    }
}

main() {
    return 0
}

/* SCAN
error: 'a' is non-static member, cannot be accessed by static function 'init'
  ==> static_init_03.cj:13:17:
   | 
13 |         println(a)
   |                 ^ 
   | 

1 error generated, 1 error printed.
*/
