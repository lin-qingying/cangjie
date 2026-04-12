// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    init(a: Int64, c: Bool) {
        let b = 1
        if (c) {
            return
        }
        x = a
        println(x)
    }
    init() {
        x = 1
    }

    public var x: Int64
}

main() {
}

/* SCAN
error: the uninitialized member variable 'x' is not initialized in the constructor of class or struct
  ==> variable_assignment_terminated_in_ctor_02.cj:10:5:
   | 
10 |     init(a: Int64, c: Bool) {
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
