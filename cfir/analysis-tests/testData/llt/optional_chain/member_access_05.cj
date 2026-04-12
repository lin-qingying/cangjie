// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct A {
    let b: B = B()
}

struct B {
    let c: Int64 = 0
}

main() {
    let a = Some(A())
    let x = a?.b?.c
}

/* SCAN
error: cannot use optional chaining
  ==> member_access_05.cj:19:13:
   | 
19 |     let x = a?.b?.c
   |             ^^^^ cannot use optional chaining on non-optional value of type 'Struct-B'
   | 

1 error generated, 1 error printed.
*/
