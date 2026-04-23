G.NAM.01 包名采用全小写单词，允许包含数字和下划线

【级别】建议

【描述】

- 包名字母全小写，如果有多个单词使用下划线分隔；
- 包名允许有数字，例如 org.apache.commons.lang3；
- 带限定前缀的包名必须和当前包与源代码根目录的相对路径对应，建议以 Internet 域名反转的规则开头，再加上产品名称和模块名称。

【正例】

| 域名                   | 包名                   |
| ---------------------- | ---------------------- |
| my_product.example.com | com.example.my_product |
| my_product.example.org | org.example.my_product |