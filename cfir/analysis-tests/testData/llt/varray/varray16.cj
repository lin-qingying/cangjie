// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// RUN-EXEC-PIPE-0: %run %run_opt %output %run_args | compare %f

var a = VArray<Int64, $4_4>(repeat: 0) // 长度实际为 4，期望为 44
var b = VArray<Int64, $0b11>(repeat: 0) // 长度实际为 0，期望为 3

main() {
    println(a.size)
    println(b.size)
}

/* SCAN
44
3
*/
