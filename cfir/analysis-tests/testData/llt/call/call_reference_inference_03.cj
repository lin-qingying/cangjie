// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class A {
    func test<T>() {
        1
    }
    func test<T>(a: String) {
        0
    }
    private func test(a: Int64) {
        2
    }
    private func test(b: Rune) {
        3
    }
}

main() {
    let a = A()
    let b = a.test
    return 0
}

/* SCAN
error: type arguments needed for the generic function 'test'
  ==> call_reference_inference_03.cj:26:15:
   | 
26 |     let b = a.test
   |               ^^^^ cannot infer type arguments for the generic function
   | 

1 error generated, 1 error printed.
*/
