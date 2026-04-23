G.OTH.04 不要使用 String 存储敏感数据，敏感数据使用结束后应立即清零

【级别】建议

【描述】

仓颉中 `String` 是不可变对象（创建后无法更改）。如果使用 `String` 保存口令、秘钥等敏感信息时，这些敏感信息会一直在内存中直至被垃圾收集器回收，如果该进程的内存可 dump，这些敏感信息就可能被泄露。应使用可以主动立即将内容清除的数据结构存储敏感数据，如 `Array<Byte>` 等。敏感数据使用结束后立即将内容清除，可有效减少敏感数据在内存中的保留时间，降低敏感数据泄露的风险。

【正例】

```cangjie
func foo() {
    let password: Array<Rune> = getPassword()
    verifyPassword(password)
    for (i in 0..password.size) {
        password[i] = '\0'
    }
}

func verifyPassword(pwd: Array<Rune>): Bool {
    ...
}
```

上述正确示例中 password 被声明为了数组类型，并且在使用完毕后被清空，保证了后续 password 内容不会被泄露。

【反例】

```cangjie
func foo() {
    let password: String = getPassword()
    verifyPassword(password)
}

func verifyPassword(pwd: String): Bool {
    ...
}
```

上面的代码中，使用 `String` 保存密码信息，可能会导致敏感信息泄露。