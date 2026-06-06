import { defineConfig } from 'vitepress'

const cangjieLanguage = {
  name: 'cj',
  displayName: 'Cangjie',
  scopeName: 'source.cj',
  aliases: ['cangjie'],
  patterns: [
    { include: '#comments' },
    { include: '#strings' },
    { include: '#numbers' },
    { include: '#annotations' },
    { include: '#keywords' },
    { include: '#builtins' },
    { include: '#types' },
    { include: '#operators' }
  ],
  repository: {
    comments: {
      patterns: [
        { name: 'comment.line.double-slash.cj', match: '//.*$' },
        { name: 'comment.block.cj', begin: '/\\*', end: '\\*/' }
      ]
    },
    strings: {
      patterns: [
        {
          name: 'string.quoted.double.cj',
          begin: '"',
          end: '"',
          patterns: [
            { name: 'constant.character.escape.cj', match: '\\\\([btnfr"\'\\\\]|u\\{[0-9A-Fa-f]+\\})' },
            { name: 'variable.other.interpolation.cj', begin: '\\$\\{', end: '\\}', patterns: [{ include: '#keywords' }, { include: '#types' }, { include: '#numbers' }] }
          ]
        },
        {
          name: 'string.quoted.single.cj',
          begin: "'",
          end: "'",
          patterns: [
            { name: 'constant.character.escape.cj', match: '\\\\([btnfr"\'\\\\]|u\\{[0-9A-Fa-f]+\\})' },
            { name: 'variable.other.interpolation.cj', begin: '\\$\\{', end: '\\}', patterns: [{ include: '#keywords' }, { include: '#types' }, { include: '#numbers' }] }
          ]
        }
      ]
    },
    numbers: {
      patterns: [
        { name: 'constant.numeric.cj', match: '\\b(0[xX][0-9A-Fa-f_]+|0[bB][01_]+|[0-9][0-9_]*(\\.[0-9][0-9_]*)?([eE][+-]?[0-9][0-9_]*)?(f16|f32|f64|i8|i16|i32|i64|u8|u16|u32|u64)?|[0-9][0-9_]*(i8|i16|i32|i64|u8|u16|u32|u64))\\b' }
      ]
    },
    annotations: {
      patterns: [
        { name: 'entity.name.function.macro.cj', match: '@[A-Za-z_][A-Za-z0-9_]*' }
      ]
    },
    keywords: {
      patterns: [
        { name: 'keyword.control.cj', match: '\\b(as|break|case|catch|continue|do|else|finally|for|if|in|is|match|return|spawn|synchronized|throw|try|unsafe|where|while)\\b' },
        { name: 'keyword.declaration.cj', match: '\\b(class|const|enum|extend|foreign|func|import|init|interface|let|macro|main|mut|open|operator|override|package|private|protected|public|internal|static|struct|var)\\b' },
        { name: 'variable.language.cj', match: '\\b(this|super)\\b' }
      ]
    },
    builtins: {
      patterns: [
        { name: 'constant.language.cj', match: '\\b(true|false|None|Some)\\b' },
        { name: 'support.function.cj', match: '\\b(print|println)\\b' }
      ]
    },
    types: {
      patterns: [
        { name: 'support.type.cj', match: '\\b(Bool|Int8|Int16|Int32|Int64|IntNative|UInt8|UInt16|UInt32|UInt64|UIntNative|Float16|Float32|Float64|Rune|String|Unit|Nothing|Option|Array|Range|ArrayList|HashSet|HashMap|Exception|Error|Future|Thread|Mutex|Tokens|Token|CType|CPointer|CString)\\b' }
      ]
    },
    operators: {
      patterns: [
        { name: 'keyword.operator.cj', match: '(=>|\\?\\?|\\.\\.=|\\.\\.|==|!=|<=|>=|&&|\\|\\||\\+\\+|--|\\*\\*|[+\\-*/%<>=!&|?:])' }
      ]
    }
  }
} as const

const sidebar = [
  {
    text: '开始动手',
    items: [
      { text: '课程路线', link: '/' },
      { text: '准备工具链', link: '/setup/environment' },
      { text: '第一个命令行程序', link: '/setup/first-program' }
    ]
  },
  {
    text: '写出能运行的核心',
    items: [
      { text: '读懂源文件和入口', link: '/core/mental-model' },
      { text: '值、变量和类型边界', link: '/core/values-and-state' },
      { text: '表达式驱动流程', link: '/core/expressions' },
      { text: '集合组织数据', link: '/core/collections' }
    ]
  },
  {
    text: '把程序设计成模型',
    items: [
      { text: '函数和 Lambda', link: '/design/functions' },
      { text: 'struct 建模值', link: '/design/structs' },
      { text: 'class 与 interface 协作', link: '/design/classes-interfaces' },
      { text: 'enum、Option 与 match', link: '/design/enum-option-match' }
    ]
  },
  {
    text: '工程化',
    items: [
      { text: '泛型复用组件', link: '/engineering/generics' },
      { text: '包和模块拆分', link: '/engineering/packages' },
      { text: '异常和 I/O', link: '/engineering/errors-io' },
      { text: '并发任务', link: '/engineering/concurrency' }
    ]
  },
  {
    text: '边界能力',
    items: [
      { text: '宏的使用边界', link: '/boundary/macros' },
      { text: 'FFI 与 unsafe', link: '/boundary/ffi' },
      { text: '资料来源', link: '/appendix/sources' }
    ]
  }
]

export default defineConfig({
  lang: 'zh-CN',
  title: '仓颉语言教程',
  description: '用一个命令行任务程序串起仓颉语言核心能力',
  cleanUrls: true,
  lastUpdated: true,
  markdown: {
    theme: {
      light: 'github-light',
      dark: 'github-dark'
    },
    languages: [cangjieLanguage],
    lineNumbers: true,
    codeCopyButtonTitle: '复制代码'
  },
  themeConfig: {
    nav: [
      { text: '教程', link: '/' },
      { text: '资料来源', link: '/appendix/sources' }
    ],
    sidebar,
    outline: {
      level: [2, 3],
      label: '本页目录'
    },
    search: {
      provider: 'local'
    },
    footer: {
      message: '面向实践的仓颉语言教程。',
      copyright: 'Copyright © 2026'
    }
  }
})
