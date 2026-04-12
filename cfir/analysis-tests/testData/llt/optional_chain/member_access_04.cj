// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct A {
    let b: Int64 = 0
}

main() {
    let a = Some(A())
    let x = a?.c
    match (x) {
        case Some(t) => t
        case None => 1
    }
    ()
}

/* SCAN
error: 'c' is not a member of struct 'A'
  ==> member_access_04.cj:15:16:
   | 
15 |     let x = a?.c
   |                ^ 
   | 

1 error generated, 1 error printed.
*/
