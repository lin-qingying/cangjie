// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

public class Test<T> {
    public var a: Int64 = 11
    private Test(x: T) {
        println("before")
    }
    public init(x: Int64) {
        Test<Bool>(true)
    }
}

main(): Int64 {
    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>Test<Int64><!>(1)
    return 0
}
