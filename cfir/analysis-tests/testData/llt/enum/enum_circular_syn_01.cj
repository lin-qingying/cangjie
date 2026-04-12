// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

var i16 = Array<Int64>(10, repeat: 5)
var cnt = 0

public enum Test<T> {
    T1

    func a16(x: Array<Int64>) {
        cnt++
        return x
    }
    public func test(x: T) {
        a16(i16);
        return 1
    }
}

main(): Int64 {
    let e = T1<Int64>
    if (e.a16(i16) != i16 || cnt != 1) {
        return 1
    }
    return 0
}
