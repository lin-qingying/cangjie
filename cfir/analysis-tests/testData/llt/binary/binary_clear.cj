// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%compiler %cmp_opt %f -o %output
// EXEC:%run %run_opt %output %run_args

func id<T>(x: T) {
    x
}

main() {
    let v = id(1) + id(2) + Int32(3)
}
