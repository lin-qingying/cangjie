// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let c: Nothing
    try {
        c = throw Exception()
    } catch (_) {}
    let b: Bool = c
}

/* SCAN
error: variable 'c' is used before initialization
  ==> variable_assignment_terminated_01.cj:14:19:
   | 
14 |     let b: Bool = c
   |                   ^ 
   | 

1 error generated, 1 error printed.
*/
