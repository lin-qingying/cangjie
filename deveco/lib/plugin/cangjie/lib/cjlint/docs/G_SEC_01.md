G.SEC.01 进行安全检查的函数禁止声明为 `open`

【级别】建议

【描述】

实现安全检查功能的函数，如果可以被子类 override，恶意子类可以 override 安全检查函数，忽略这些安全检查，使安全检查失效。所以安全检查相关的函数禁止声明为 `open`，防止被 override。

【正例】

```cangjie
class SecurityCheck {
    ...
 
    public func requestPasswordAuthentication(protocol: String, prompt: String, scheme: String): Bool {
 
        if (checkProtocol(protocol) && checkPrompt(prompt) && checkScheme(scheme)) {
            ...
        }
    }
}
```

上述示例中，requestPasswordAuthentication 没有被声明为 open 类型，防止被子类覆写。

【反例】

```cangjie
class SecurityCheck {
    ...
 
    public open func requestPasswordAuthentication(protocol: String, prompt: String, scheme: String): Bool {
 
        if (checkProtocol(protocol) && checkPrompt(prompt) && checkScheme(scheme)) {
            ...
        }
    }
}
```

上述示例中，requestPasswordAuthentication 被声明为了 open 类型，攻击者可以构造恶意子类将该函数覆写，忽略其中的安全检查。