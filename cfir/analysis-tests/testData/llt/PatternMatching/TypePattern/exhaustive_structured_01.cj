// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func f<T>(x: Option<T>) {
    match (x) {
        case _: ToString => 0 // maybe reachable!
        case Some(_) => 1
        case None => 2
    }
}

main() {
    f(Some(0))
}
