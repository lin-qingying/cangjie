// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    func kiwi() {
        println('🥝')
    }

    const func f() {
        kiwi
    }

    const init() {}
}

main() {}

/* SCAN
error: expected 'const' expression
  ==> err_class_non_const_func.cj:15:9:
   | 
15 |         kiwi
   |         ^^^^ 
   | 

1 error generated, 1 error printed.
*/
