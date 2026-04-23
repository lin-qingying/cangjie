// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

let f = {x: String => x}

func f2<T, R>(x: T) {
    {x: R => x}
}

func f3(x: (String) -> Any) {
    x("s")
}

main() {
    let v: Any = f2("s")("s")
    f3(<!TYPE_MISMATCH!>f<!>)
    ()
}
