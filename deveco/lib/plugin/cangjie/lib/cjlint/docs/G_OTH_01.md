G.OTH.01 禁止在日志中保存口令、密钥和其他敏感数据

【级别】要求

【描述】

在日志中不能输出口令、密钥和其他敏感信息，口令包括明文口令和密文口令。对于敏感信息建议采取以下方法：

- 不在日志中打印敏感信息。
- 若因为特殊原因必须要打印日志，则用固定长度的星号（`*`）代替输出的敏感信息。

【正例】

```cangjie
func test() {
    let fs: File = File("xxx.log", CreateOrAppend)
    let logger = SimpleLogger("Login", LogLevel.INFO, fs)
    ...
    logger.info("Login success ,user is ${userName} and password is ****")
}
```

【反例】

```cangjie
func test() {
    let fs: File = File("xxx.log", CreateOrAppend)
    let logger = SimpleLogger("Login", LogLevel.INFO, fs)
    ...
    logger.info("Login success ,user is ${userName} and password is ${encrypt(pass)}")
}
```