// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%compiler %cmp_opt %f -o %output
// EXEC:%run %run_opt %output %run_args

main() {
    var cycle: Float64 = 0.0
    var xx: Float64 = 1.0
    var x0: Float64 = 0.0
    var haltingProb: Float64 = 0.0

    while ((cycle < 2.0) && (haltingProb < 50.0)) {
        let (v0, v1, v2, v3) = (cycle + 1.0, haltingProb + (xx + 3.0) * 2.0 + 1.0, xx * 2.0 + 1.0, xx * 4.0 + 2.0 - x0)
        cycle = v0
        haltingProb = v1
        xx = v2
        x0 = v3
    }
    return 0
}
