// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

interface I<T> {
    func foo(): Option<T>
}

extend<T> Option<T> <: I<Option<T>> {
    public func foo(): Option<Option<T>> {
        return Option<Option<T>>.None
    }
}

main() {}
