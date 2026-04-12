// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-PIPE-0: %run %run_opt %output

func f<T>(a: Option<T>) {}

main() {
    f<Int64>(None) // ok
    f(Option<Int64>.None) // ok

    f<Nothing>(None) // ok

    f(Option<Nothing>.None)
}
