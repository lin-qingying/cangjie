// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt -c %n.cj 2>&1 | compare %f



main() {
    var b = 1 + true
    var a = b

    a **= 1
    a *= true
    a /= true
    a %= true
    a += true
    a -= true
    a <<= true
    a >>= true
    a &&= true
    a ||= true
    a &= true
    a ^= true
    a |= true

    0
}

/* SCAN
[31merror[0m: invalid option: '-c'.
Invalid options. Try: 'cjc --help' for more information.
*/
