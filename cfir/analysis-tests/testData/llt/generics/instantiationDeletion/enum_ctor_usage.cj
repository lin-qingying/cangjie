// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

var a = 7

public interface b<c> {
    func d(e: (Unit, UInt8, Int16, Unit) -> f, u!: Int64) {
        var g: Array<h<Int32, i, Range<Int64>>>
    }
}

public interface j<c> <: b<Range<UInt64>> {}

public interface k<c> <: j<Int32> {}

public interface i <: k<Range<Int32>> {}

public class f <: i {}

func l() {
    let m = (9, .3, f())
    match {
        case _ => m
    }[2]
}

func n(o!: Unit = (), p!: UInt8, v!: Int16 = 7, q!: Unit) {
    l()
}

enum h<c, r, s> {
    t(Range<UInt32>)
}

main() {
    n(p: 9, q: ()).d(n, u: a)
}
