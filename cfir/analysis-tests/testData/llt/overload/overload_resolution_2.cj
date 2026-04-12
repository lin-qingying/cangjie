// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler -Woff unused %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func f(a: Int8, b: Int8) {
    a + b
}

func f(a: Int16, b: Int16) {
    a + b
}

func f(a: Int32, b: Int32) {
    a + b
}

func f(a: Int64, b: Int64) {
    a + b
}

func f(a: UInt8, b: UInt8) {
    a + b
}

func f(a: UInt16, b: UInt16) {
    a + b
}

func f(a: UInt32, b: UInt32) {
    a + b
}

func f(a: UInt64, b: UInt64) {
    a + b
}

main() {
    f(f(f(f(f(f(f(f(f(f(f(f(f(f(f(f(f(f(f(f(f(1, 1), 1), 1), 1), 1), 1), 1), 1), 1), 1), 1), 1), 1), 1), 1), 1), 1), 1),
        1), 1), 1)
    ()
}
