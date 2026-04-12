// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

interface I {
    mut prop b: Int64 {
        get() {
            0
        }
        set(v) {
            v = v
        }
    }
}

main() {}

/* SCAN
error: cannot assign to immutable value
  ==> assign_009.cj:15:13:
   | 
15 |             v = v
   |             ^^^^^ 
   | 
note: parameter 'v' is immutable
  ==> assign_009.cj:14:13:
   | 
14 |         set(v) {
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
