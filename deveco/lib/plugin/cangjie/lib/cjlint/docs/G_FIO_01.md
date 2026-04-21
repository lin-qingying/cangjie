G.FIO.01 临时文件使用完毕必须及时删除

【级别】要求

【描述】

程序运行时经常会需要创建临时文件。如果文件未被安全地创建或者用完后还是可访问的，具备本地文件系统访问权限的攻击者便可以利用临时文件进行恶意操作。删除已经不再需要的临时文件有助于对文件名和其他资源（如二级存储）进行回收利用。每一个程序在正常运行过程中都有责任确保已使用完毕的临时文件被删除。

【正例】

```cangjie
import std.fs.File
import std.fs.OpenOption

main() {
    let pathName = "/mytemp/doc.txt"
    let fs: File = File(pathName, CreateOrAppend)
    ...
    fs.flush()
    fs.close()
    print("This file already exists or created!")
    File.delete(pathName)
    ...
    return 0
}
```

这个正确示例代码在临时文件使用完毕之后、系统终止之前，显式地对其进行删除。

【反例】

```cangjie
import std.fs.File
import std.fs.OpenOption
 
main(){
    let pathName = "/mytemp/doc.txt";
    let fs: File = File(pathName, CreateOrAppend)
    ...
    fs.flush()
    fs.close()
    print("This file already exists or created!")
    ...
    return 0
}
```

这个错误示例代码在运行结束时未将临时文件删除。