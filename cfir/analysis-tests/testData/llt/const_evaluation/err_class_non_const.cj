// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

var global = 0

func f() {
    global++
    global
}

class A {
    let x = f()
    const init() {}
}

main() {}

/* SCAN
error: expected 'const' expression guaranteed to be evaluated at compile time
  ==> err_class_non_const.cj:17:13:
   | 
17 |     let x = f()
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
