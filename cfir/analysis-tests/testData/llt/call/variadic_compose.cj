// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-4: %run %run_opt %output %run_args

func f(x: Array<Int8>) {
    x.size
}

func g(x: Int64) {
    x + 1
}

main() {
    (f ~> g)(1, 2, 3)
}
