// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

var i16 = Array<Int64>(10, repeat: 5)

public enum Test<T> {
    T1

    func a16(x: Array<T>) {
        return x
    }
}

main(): Int64 {
    let v = T1.a16(i16)
    if (T1.a16(i16) != i16) {
        return 1
    }
    return 0
}
