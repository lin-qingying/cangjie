// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args
internal import std.collection.*

func stridedSliceOp(t: Tensor, lowers: ArrayList<Int64>, uppers: ArrayList<Int64>, steps: ArrayList<Int64>) {
    // mock
    return t
}

struct Tensor {
    var shape: Array<Int64>

    init(shape_: Array<Int64>) {
        shape = shape_
    }

    operator func [](ranges: Array<Range<Int64>>) {
        var lowers: ArrayList<Int64> = ArrayList<Int64>(ranges.size)
        var uppers: ArrayList<Int64> = ArrayList<Int64>(ranges.size)
        var steps: ArrayList<Int64> = ArrayList<Int64>(ranges.size)

        for (range in ranges) {
            lowers.add(range.start)
            uppers.add(range.end)
            steps.add(range.step)
        }

        return stridedSliceOp(this, lowers, uppers, steps)
    }
}

var t: Tensor = Tensor([5, 5])

func foo(t: Tensor) {
    var t0 = t[[0..t.shape[0], 0..1]]
    var t1 = t[[0..t.shape[0], 1..2]]
    var t2 = t[[0..t.shape[0], 2..3]]
}

main() {
    foo(t)
    0
}
