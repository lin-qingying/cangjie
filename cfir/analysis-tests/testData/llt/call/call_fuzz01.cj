// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-1: %compiler %cmp_opt %f -o %output

interface a<b, c> where b <: Int8, c <: d<UInt8, Range<UInt16>> {
    static func e<b, c>(f!: Float64) where b <: UInt64, c <: Int64 {
        {} is d<UInt8, Range<UInt16>>
    }
}
interface d<b, g16>>> {}
class i <: h<UInt16, UInt16> {
    static var m = i()
    var j: Int32 = 4
    static func k<b>(): Int32 {
        if (e(f: 52.0)~ {
            1
        } else {
            m.j
        }
    }
}
func l() {
    }
interface h<b, c> <: a<Int8, d<UInt8, Range<UInt> {i.k<Range<Int64>>
}

main() {}

