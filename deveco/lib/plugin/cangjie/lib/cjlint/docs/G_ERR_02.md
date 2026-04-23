G.ERR.02 防止通过异常抛出的内容泄露敏感信息

【级别】要求

【描述】

如果在传递异常的时候未对其中的敏感信息进行过滤，常常会导致信息泄露，而这可能帮助攻击者尝试发起进一步的攻击。攻击者可以通过构造恶意的输入参数来发掘应用的内部结构和机制。不管是异常中的文本消息，还是异常本身的类型都可能泄露敏感信息。因此，当异常被传递到信任边界以外时，必须同时对敏感的异常消息和敏感的异常类型进行过滤。

【正例】

```cangjie
func exceptionExample(path: String): Unit {
    var file: File
    if (!File.exists(path)) {
        // 安全策略
        println("Invalide file")
        return
    }
    file = File(path, Append)
    // CODE
}
```

```cangjie
func exceptionExample(index: Int32): Unit {
    var path: String
    var file: File
    // 限制输入
    match (index) {
        case 1 => path = "/home/test1"
        case 2 => path = "/home/test2"
        case _ => return
    }
    file = File(path, Append)

    // CODE
}
```

这个正确示例限制用户只能打开 /home/test1 与 /home/test2。同时，它也会过滤在 catch 块中捕获的异常中的敏感信息。

【反例】

```cangjie
func exceptionExample(path: String): Unit {
    var file: File
    if (!File.exists(path)) {
        // 异常消息和类型泄露敏感信息
        throw IOException("File does not exist")
    }
    file = File(path, Append)
    // CODE
}
```

当打开的源文件不存在时，程序会抛出 IOException 异常，并提示 “File does not exist”。这使得攻击者可以不断传入伪造的路径名称来重现出底层文件系统结构。

```cangjie
func exceptionExample(path: String): Unit {
    var file: File
    if (!File.exists(path)) {
        // 异常净化
        throw IOException()
    }
    file = File(path, Append)
    // CODE
}
```

此例中虽然报错信息并未透露错误原因，但是对于不同的错误原因仍会抛出不同类型的异常。攻击者可以根据程序的行为推断出有关文件系统的敏感信息。未对用户输入做限制，使得系统面临暴力攻击的风险，攻击者可以多次传入所有可能的文件名进行查询来发现有效文件。如果传入一个文件名后程序返回一个 IOException 异常，则表明该文件不存在，否则说明该文件是存在的。

【例外场景】

对出于问题定位目的，可将敏感异常信息记录到日志中，但必须做好日志的访问控制，防止日志被任意访问，导致敏感信息泄露给非授权用户。