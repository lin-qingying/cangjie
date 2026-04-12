// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC-2: %run %run_opt %output %run_args

class A<T> {
    static var a: Int64 = 1
}

main() {
    A<Int32>.a = 2
    return A<Int64>.a
}
