// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func g(a: Bool) {
    0
}

func g(a: Option<Bool>) {
    true
}

func call<T1, T2>(f: (T1) -> T2, x: T1) {
    f(x)
}

main() {
    return call(g, true)
}
