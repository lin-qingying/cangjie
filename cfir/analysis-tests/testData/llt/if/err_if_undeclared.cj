// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f(a: Array<Int64>) {
    a.size
}

main() {
    f([if (true) {
        x = 2
        1
    } else {
        2
    }])
}

/* SCAN
error: undeclared identifier 'x'
  ==> err_if_undeclared.cj:15:9:
   | 
15 |         x = 2
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
