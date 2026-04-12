// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    static func f() {
        g()
    }

    func g(_: Array<Int64>) {}
}

main() {
    A.f()
}

/* SCAN
error: 'g' is non-static member, cannot be accessed by static function 'f'
  ==> variadic_err_static_call_non.cj:11:9:
   | 
11 |         g()
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
