// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

func foo(x: Int32) {}

func foo(x: Int64) {}

func bar(x: Int64) {}

func bar(x: Int32) {}

func id<T>(x: T) {
    x
}

main() {
    foo(id(1))
    bar(id(1))
    ()
}
