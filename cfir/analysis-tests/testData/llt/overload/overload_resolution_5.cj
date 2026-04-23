// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func g(f: (Int) -> Int) {}

func g(f: (String) -> String) {}

main() {
    let _ = g(
        {
            x =>
            let v1 = x
            let v2 = v1
            g({y => v2})
            x.toString()
        }
    )
}
