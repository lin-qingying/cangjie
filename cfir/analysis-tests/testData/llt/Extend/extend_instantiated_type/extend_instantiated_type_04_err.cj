// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A<T> {}

extend<T> A<T> {}

extend A<Int64> {
    func test() {
        println("A int64")
    }
}

main() {
    let a1 = A<Int64>()
    let a2 = A<Rune>()
    a1.test()
    a2.test()
}

/* SCAN
error: 'test' is not a member of class 'A<Rune>'
  ==> extend_instantiated_type_04_err.cj:23:8:
   | 
23 |     a2.test()
   |        ^^^^ 
   | 

1 error generated, 1 error printed.
*/
