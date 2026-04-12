// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f(c: Bool) {
    let a: Int64
    if (c) {
        try {
            a = Some(1) ?? throw Exception()
        } catch (_) {
        }
    } else {
        a = 2
    }
    return a
}

main() {
    f(true) - 1
}

/* SCAN
error: variable 'a' is used before initialization
  ==> variable_assignment_terminated_04.cj:19:12:
   | 
19 |     return a
   |            ^ 
   | 

1 error generated, 1 error printed.
*/
