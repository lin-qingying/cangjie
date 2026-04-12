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
    let x = A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
        A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
        A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
        A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
        A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
        A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
        A * A
}

/* SCAN
error: invalid binary operator '*' on type 'Enum-E' and 'Enum-E'
  ==> timeout_00.cj:15:19:
   | 
15 |     let x = A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
   |                   ^ 
   | 
   # note: you may want to implement 'operator func *(right: Enum-E)' for type 'Enum-E'

1 error generated, 1 error printed.
*/
