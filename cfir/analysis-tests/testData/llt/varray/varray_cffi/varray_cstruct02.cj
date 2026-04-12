// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: %n.c
// EXEC: %clang %clang_opt %n.c -o %cffi_output
// EXEC: %compiler %cmp_opt %f %cffi-link -o %output
// RUN-EXEC-PIPE-0: %run %run_opt %output %run_args 2>&1 | compare %f
// ASSERT: scan a map: 1 2 1 2 1 2 1 2 1 2 1 2 1 2 1 2
// ASSERT: scan b[0]: 3

foreign func printS(s: S): Unit

@C
struct S {
    var a = VArray<VArray<VArray<VArray<Int32, $2>, $2>, $2>, $2>(repeat: [[[1, 2], [1, 2]], [[1, 2], [1, 2]]])
    var b = VArray<Int32, $3>(repeat: 3)
}

main() {
    var s: S = S()
    unsafe { printS(s) }
}
