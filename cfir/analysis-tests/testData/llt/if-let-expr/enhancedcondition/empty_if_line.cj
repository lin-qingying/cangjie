// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func foo(c: ?Int) {
    if (let Some(_) <- c) {}
    else if (let None <- c) {
        ()
    }
}

main() {
    foo(3)
    foo(None)
}
