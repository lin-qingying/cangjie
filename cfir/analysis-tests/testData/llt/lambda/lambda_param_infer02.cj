// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt -Woff unused %f -o %output
// EXEC: %run %run_opt %output %run_args

func app<T, R>(f: (T) -> R, arg: T) {
    f(arg)
}

func app(f: (String) -> String, arg: String) {
    f(arg)
}

main() {
    println([1, 2, 3][app({x => x + 1}, 1)])
}
