// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %output

internal import std.{collection.ArrayList, collection.map}

main() {
    let result = ArrayList([1, 2, 3]) |> map {
        item => item * item
    }
}
