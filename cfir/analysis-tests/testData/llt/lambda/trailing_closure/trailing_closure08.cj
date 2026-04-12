// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main(): Int64 {
    func f(a : Int64) {
        func g(h : (Int64) -> Int64) {
            h(0) - a
        }
        g
    }
    var t = (if (true) {f(1)} else {f(2)}){ t => t + 1 }
    return 0
}

/* SCAN
error: trailing closure can only be used on function calls with function or variable names
  ==> trailing_closure08.cj:16:13:
   | 
16 |     var t = (if (true) {f(1)} else {f(2)}){ t => t + 1 }
   |             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
