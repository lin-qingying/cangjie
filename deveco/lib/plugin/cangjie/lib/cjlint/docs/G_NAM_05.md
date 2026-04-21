G.NAM.05 const 变量的名称采用全大写

【级别】建议

【描述】

const 变量表示在编译时完成求值，并且在运行时不可改变的变量，使用下划线分隔的全大写单词来命名。

【正例】

```cangjie
// 符合：const 变量使用下划线分隔的全大写单词命名
const MAX_USER_NUM = 200

class Weight {
    static const GRAMS_PER_KG = 1000
}
```

【反例】

```cangjie
// 不符合：const 变量没有使用下划线分隔的全大写单词命名
const MAXUSERNUM = 200

class Weight {
    static const GramsPerKg = 1000
}
```