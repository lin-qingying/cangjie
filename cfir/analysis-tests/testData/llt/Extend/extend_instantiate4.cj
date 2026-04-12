// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args

class A<T> {
    init(a: Array<T>) {}
    init(a: Collection<T>) {
        match (a) {
            case v: Array<T> => println("array")
            case _ => ()
        }
    }
}

extend<T> Array<T> {
    func test(): A<Array<T>> {
        return A<Array<T>>([this])
    }
}

main() {
    var a: A<Int64>
    0
}
