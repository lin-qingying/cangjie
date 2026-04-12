// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    // mut prop can be defined together with const init
    mut prop x: Int64 {
        get() {
            0
        }
        set(_) {}
    }

    const func f() {
        // but reference to the mut prop is not const expression
        x
    }

    const init() {}
}

main() {
    let a = A()
    a.x = 0
    a.x
}

/* SCAN
error: expected 'const' expression
  ==> err_class_mut_prop.cj:20:9:
   | 
20 |         x
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
