// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output
// TIMEOUT: 10

enum E {
    A
}

main() {
    let x = ((((((((((((((((((((1 + true) + 1) + true) + 1) + true) + 1) + true) + 1) + true) + 1) + true) + 1) + true) +
        1) + true) + 1) + true) + 1) + true) + 1) + (A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
        A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
        A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
        A * A + A * A + A * A + A * A + A * A + A) + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 +
        true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 +
        true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 +
        true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 +
        true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 +
        true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + true + 1 + (true + (1 + (true +
        (1 + (true + (1 + (true + (1 + (true + (1 + (true + (1 + (true + (1 + (true + (1 + (true + (1 + (true + (1 + (true +
        (1 + (true + (1 + (true + (1 + (true + (1 + (true + (1 + (true + (1 + (true + (1 + true))))))))))))))))))))))))))))))))))
}

/* SCAN
error: invalid binary operator '+' on type 'Int64' and 'Bool'
  ==> timeout_01.cj:15:35:
   | 
15 |     let x = ((((((((((((((((((((1 + true) + 1) + true) + 1) + true) + 1) + true) + 1) + true) + 1) + true) + 1) + true) +
   |                                   ^ 
   | 
   # note: you may want to implement 'operator func +(right: Bool)' for type 'Int64'

1 error generated, 1 error printed.
*/
