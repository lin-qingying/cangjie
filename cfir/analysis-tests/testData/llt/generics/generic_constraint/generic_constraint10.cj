// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %output

interface A<T> {
    static func test(a: String): T
}

extend Int32 <: A<Int32> {
    public static func test(a: String): Int32 {
        return 123
    }
}

func getNumber<T>(data: String): T where T <: A<T> {
    test(T.test(data))
}

func test<T>(a: T): T where T <: A<T> {
    a
}

main() {
    var v: Int32 = getNumber("321")
    if (v != 123) {
        return 1
    }
    return 0
}
