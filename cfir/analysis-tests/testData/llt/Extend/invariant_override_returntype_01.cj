// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

public interface Tokens<T> {
    func dropFirst(): Tokens<T>
}

extend String <: Tokens<Rune> {
    public func dropFirst(): String {
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

/* SCAN
error: return type of 'dropFirst' can only be class/interface type which implements or inherits the interface type 'Interface-Tokens<Rune>'
  ==> invariant_override_returntype_01.cj:14:5:
   | 
14 |     public func dropFirst(): String {
   |     ^ 
   | 
note: cannot override/implement the following function
  ==> invariant_override_returntype_01.cj:10:5:
   | 
10 |     func dropFirst(): Tokens<T>
   |     ^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
