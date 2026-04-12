// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC-PIPE-0: %run %run_opt %output %run_args

let f1: CFunc<(CPointer<Int64>, Int64) -> Unit> = {
    x, size =>
    var i = 0;
    while (i < size) {
        unsafe { x.write(i, x.read(i) + i) }
        i++
    }
}

main() {
    var v1: VArray<Int64, $3> = [1, 2, 3]
    unsafe { f1(inout v1, 3) }
    if (1 == v1[0] && 3 == v1[1] && 5 == v1[2]) {
        return 0
    }
    return 1
}
