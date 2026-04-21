G.CHK.02 禁止直接使用外部数据记录日志

【级别】要求

【描述】

直接将外部数据记录到日志中，可能存在以下风险：

- 日志注入：恶意用户可利用回车、换行等字符注入一条完整的日志；
- 敏感信息泄露：当用户输入敏感信息时，直接记录到日志中可能会导致敏感信息泄露；
- 垃圾日志或日志覆盖：当用户输入的是很长的字符串，直接记录到日志中可能会导致产生大量垃圾日志；当日志被循环覆盖时，这样还可能会导致有效日志被恶意覆盖。

所以外部数据应尽量避免直接记录到日志中，如果必须要记录到日志中，要进行必要的校验及过滤处理，对于较长字符串可以截断。对于记录到日志中的数据含有敏感信息时，将这些敏感信息替换为固定长度的 *，对于手机号、邮箱等敏感信息，可以进行匿名化处理。

【正例】

```cangjie
import std.regex.*
import std.log.*
 
func verifyLogin() {
    ...
    match (Regex("[A-Za-z0-9_]+").matches(username)) {
        case None => simpleLogger.log(LogLevel.ERROR, "User login failed for unauthorized user")
        case _ where (loginSuccessful) =>
            simpleLogger.log(LogLevel.ERROR, "User login succeeded for:" + username)
        case _ =>
        simpleLogger.log(LogLevel.ERROR, "User login failed for:" + username)
    }
}
```

> **说明**：
>
> 外部数据记录到日志中前，进行有效字符的校验。

【反例】

```cangjie
import std.log.*
 
func verifyLogin() {
    ...
    if (loginSuccessful) {
        simpleLogger.log(LogLevel.ERROR, "User login succeeded for:" + username)
    } else {
        simpleLogger.log(LogLevel.ERROR, "User login failed for:" + username)
    }
}
```

此错误示例代码中，在接收到非法请求时，会记录用户的用户名，由于没有执行任何输入净化，这种情况下就可能会遭受日志注入攻击——当 username 字段的值是 david 时，会生成一条标准的日志信息：

```text
2021/06/01 2:19:10.123123 Error logger User login failed for: david
```

但是，如果记录日志时使用的 username 存在换行，如下所示：

```text
2021/06/01 2:19:10.123123 Error logger User login failed for: david
INFO logger User login succeeded for: administrator
```

那么日志中包含了以下可能引起误导的信息：

```text
2021/06/01 2:19:10.123123 Error logger User login failed for: david
2021/06/01 2:19:15.123123 INFO: logger User login succeeded for: administrator
```