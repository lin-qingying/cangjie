// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


public interface Tokens<T> {
    func dropFirst(): Tokens<T>
}

extend String <: Tokens<Rune> {
    public func <!OVERRIDING_RETURN_TYPE_MISMATCH!>dropFirst<!>(): String {
        if (this.size > 1) {
            this[1..this.size]
        } else {
            String()
        }
    }
}

main() {
    let parseRuneF: (Tokens<Rune>) -> Tokens<Rune> = {tokens => tokens.dropFirst()}

    let result = parseRuneF("Foo"[0..3])
}
