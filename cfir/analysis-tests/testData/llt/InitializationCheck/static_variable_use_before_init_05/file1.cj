// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: file2.cj
// ERRCHECK: %compiler %cmp_opt %f file2.cj -o %output

// Test for sub class using parent class's static member in different file
// Circular initialization

class Foo <: Bar {
    static let y: Int64
    static init() {
        y = x
    }
}

main() {
    if (Foo.y == Bar.x && Bar.x == 2) {
        return 0
    }
    return 1
}

/* SCAN
error: variable 'y' is used before initialization
  ==> file2.cj:10:17:
   | 
10 |         x = Foo.y
   |                 ^ 
   | 

1 error generated, 1 error printed.
*/
