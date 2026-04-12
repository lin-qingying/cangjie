// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// RUN-EXEC-PIPE-0: %run %run_opt %output %run_args

type varr = VArray<Int64, $3>
type varr1_1<T> = VArray<Int64, $3>

main() {
    var a: varr = varr(repeat: 0)
    var d = varr(repeat: 0)
    //   var b: varr1<Int64> = varr1<Int64>(repeat: 0)
    var c: varr1_1<Rune> = varr1_1<Rune>(repeat: 0)
    var e = varr1_1<Rune>(repeat: 0)
}
