G.CHK.04 禁止直接使用不可信数据构造正则表达式

【级别】要求

【描述】

正则表达式广泛用于匹配文本字符串。例如，POSIX 中 grep 实用程序支持用于查找指定文本中的模式的正则表达式。仓颉的 regex 包提供了 Regex 类，该类封装了一个编译过的正则表达式和一个 Matcher 类，通过 Matcher 类引擎，可以在字符串中进行匹配操作。

在仓颉中必须注意不能误用正则表达式的功能。攻击者可能会通过恶意构造的输入对初始化的正则表达式进行修改，比如导致正则表达式不符合程序规定要求。这种攻击称为正则注入 (regex injection)，可能会影响控制流，导致信息泄漏，或导致 ReDos 攻击。

以下是正则表达式可能被利用的方式：

**匹配标志**：不可信的输入可能覆盖匹配选项，然后有可能会被传给 Regex() 构造函数。

**贪婪**：一个非受信的输入可能试图注入一个正则表达式，通过它来改变初始的那个正则表达式，从而匹配尽可能多的字符串，导致暴露敏感信息。

**分组**：开发者会用括号包括一部分的正则表达式以完成一组动作中某些共同的部分。攻击者可能通过提供非受信的输入来改变这种分组。

非受信的输入应该在使用前净化，从而防止发生正则表达式注入。当用户必须指定正则表达式作为输入时，需要保证初始的正则表达式没有被无限制修改。在用户输入字符串提交给正则解析之前，进行白名单字符处理（比如字母和数字）是一个很好的输入净化策略。开发人员必须仅仅提供最有限的正则表达式功能给用户，从而减少被误用的可能。

ReDos 攻击是仓颉代码正则使用不当导致的常见安全风险。容易存在 ReDos 攻击的正则表达式主要有两类：

- 包含具有自我重复的重复性分组的正则，例如：

  ```text
  ^(\d+)+$
  ^(\d*)*$
  ^(\d+)*$
  ^(\d+|\s+)*$
  ```

- 包含替换的重复性分组，例如：

  ```text
  ^(\d|\d\d)+$
  ^(\d|\d?)+$
  ```

对于 ReDos 攻击的防护手段主要包括：

1. 进行正则匹配前，先对匹配的文本的长度进行校验；

2. 在编写正则时，尽量不要使用过于复杂的正则，尽量减少分组的使用，越复杂、分组越多越容易有缺陷，例如对于下面的正则：

   ```text
   ^(([a-z])+\.)+[A-Z]([a-z])+$
   ```

   存在 ReDos 风险，可以将多余的分组删除，这样在不改变检查规则的前提下消除了 ReDos 风险；

   ```text
   ^([a-z]+\.)+[A-Z][a-z]+$
   ```

   【正例】

   ```cangjie
   let REGEX_PATTER: Regex = Regex("a[bc]+d")
   func test(arg: String) {
      match (REGEX_PATTER.matches(arg)) {
          case None => ...
          case _ => ...
      }
   }
   ```

   【反例】

   ```cangjie
   let REGEX_PATTER: Regex = Regex("a(b|c+)+d")
   func test(arg: String) {
      match (REGEX_PATTER.matches(arg)) {
          case None => ...
          case _ => ...
      }
   }
   ```

3. 避免动态构建正则，当使用不可信数据构造正则时，要使用白名单进行严格校验。

      【正例】

   ```cangjie
   class LogSearch {
       func findLogEntry(search: String, log: String) {
           // Sanitize search string
           let ss = StringBuilder()
           for (i in search.runes()) {
               if (i.isLetter() || i.isNumber() || i == '_' || i =='\'') {
                   ss.append(i)
               }
           }
           let sanitized = ss.toString()
   
           // Construct regex dynamically from user string
           var regex: String = "(.*? +public\\[\\d+\\] +.*" + sanitized + ".*)"
           var logMatcher: Matcher = Regex(regex).matcher(log)
           ...
       }
   }
   ```

   【反例】

   ```cangjie
   class LogSearch {
       func findLogEntry(search: String, log: String) {
           // Construct regex dynamically from user string
           var regex: String = "(.*? +public\\[\\d+\\] +.*" + search + ".*)"
           var logMatcher: Matcher = Regex(regex).matcher(log)
           ...
        }
   }
   ```