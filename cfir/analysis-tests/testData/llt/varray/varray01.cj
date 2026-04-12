// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// RUN-EXEC-PIPE-0: %run %run_opt %output %run_args

main() {
    /** Initialization **/
    let arr1: VArray<Int64, $5> = [1, 2, 3, 4, 5] // ok
    let arr2: VArray<Int16, $0> = [] // ok

    /** Constract **/
    VArray<Int64, $5>({i => i}) // ok
    let arr5: VArray<Int64, $5> = VArray<Int64, $5>({i => i}) // ok
    let arr5_1: VArray<Int64, $5> = VArray<Int64, $5> {i => i} // ok
    let arr5_2: VArray<Int64, $5> = VArray<Int64, $5>() {i => i} // ok
    let arr6: VArray<Int64, $5> = VArray<Int64, $5>(repeat: 0) // ok

    /** Index **/
    var arr7: VArray<Int64, $2> = [0, 1]
    let element0 = arr7[0] // ok
    arr7[1] = 10 // ok

    /** Size **/
    let arr9: VArray<Int64, $6> = [0, 1, 2, 3, 4, 5]
    let size: Int64 = arr9.size // ok
    if (size != 6) {
        return 1
    }

    0
}
