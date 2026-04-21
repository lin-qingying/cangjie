G.OTH.02 禁止将敏感信息硬编码在程序中

【级别】要求

【描述】

如果将敏感信息（包括口令和加密密钥）硬编码在程序中，可能会将敏感信息暴露给攻击者。任何能够访问到二进制文件的人都可以反编译二进制文件并发现这些敏感信息。因此，不能将敏感信息硬编码在程序中。同时，硬编码敏感信息会增加代码管理和维护的难度。例如，在一个已经部署的程序中修改一个硬编码的口令需要发布一个补丁才能实现。

【正例】

```cangjie
class DataHandler {
    public func checkPwd() {
        let pwd = Array<UInt8>()
        let read_bytes: Int64
        let fs: File = File("serverpwd.txt", Open(true, true))
        read_bytes = fs.read(pwd)
        ...
        for (i in 0..pwd.size) {
            pwd[i] = 0
        }
        ...
    }
}
```

这个正确代码示例从一个安全目录下的外部文件获取密码信息，在其使用完后立即从内存中将其清除可以防止后续的信息泄露。

【反例】

```cangjie
class DataHandler {
    let pwd: String = "Huawei@123"
    ...
}
```