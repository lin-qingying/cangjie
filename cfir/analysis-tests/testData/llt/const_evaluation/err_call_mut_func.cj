// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct Foo {
    let a = 0
    var b = 0
    static const c = 0

    const init() {}

    func f1() {}
    const func f2() {}
    mut func f3() {
        b = 123
    }
}

main() {
    const v = Foo()
    print(v.a) // ok
    print(v.b) // ok
    print(Foo.c) // ok
    v.f1() // ok
    v.f2() // ok
    v.f3() // error, f3 is mut
    return 0
}

/* SCAN
error: cannot use mutable function on immutable value
  ==> err_call_mut_func.cj:30:5:
   | 
30 |     v.f3() // error, f3 is mut
   |     ^ ~~ is a mutable function
   |     |
   |     is immutable
   | 
note: 'v' is a variable declared with 'const'
  ==> err_call_mut_func.cj:24:11:
   | 
24 |     const v = Foo()
   |           ^ 
   | 

1 error generated, 1 error printed.
*/
