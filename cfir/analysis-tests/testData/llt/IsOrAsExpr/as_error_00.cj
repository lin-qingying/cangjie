// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output
// ASSERT: regex expected 'Enum-Option<Generics-T>', found 'Enum-Option<Enum-Option<Generics-T>>'

func doSth<T>(): Option<T> {
    let a = 0
    a as Option<T>
}

main() {
    doSth<Float64>()
    0
}
