// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args

let f1: CFunc<(VArray<Int8, $2>) -> Int8> = {arr => arr[0] + arr[1]}

@C
func f2(arr: VArray<Int8, $2>): Int8 {
    arr[0] + arr[1]
}

main() {
    var v1: VArray<Int8, $2> = [2, 3]
    var x = unsafe { f1(inout v1) }
    var y = unsafe { f2(inout v1) }
    if (x != 5 || y != 5) {
        return 1
    }
    return 0
}
