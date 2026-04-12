// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

let var285: (Option<Range<Int8>>) -> Range<UInt16> = func210;

func func210(param211: Option<Range<Int8>>): Range<UInt16> {
    0..1
}

func func210(param629: Array<UInt32>, param630: UInt32): Option<Array<Range<Int8>>> {
    Option<Array<Range<Int8>>>.None
}

public interface I {
    func test() {
        let var604: Range<Int64> = {
            =>
            func foo<T>() {}
            foo<Int64>()
            0..1
        }()
    }
}
