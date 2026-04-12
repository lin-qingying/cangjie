// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

class A<T> {}

extend A<Int64> {
    func foo() {
        11
    }
}

main() {
    if (A<Int64>().foo() != 11) {
        return 1
    }
    return 0
}
