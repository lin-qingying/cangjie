// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A<T> {
    init(x: Bool, a: Array<T>) {}
    static func f(x: Array<T>) {
        x.size
    }
}

main() {
    let x = A<Int64>(true).f() == 0
}

/* SCAN
error: object cannot access static member 'f'
  ==> variadic_err_static.cj:17:13:
   | 
17 |     let x = A<Int64>(true).f() == 0
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
