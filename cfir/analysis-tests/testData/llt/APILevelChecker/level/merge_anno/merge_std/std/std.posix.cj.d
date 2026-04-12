// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.




package std.posix


/**
* Open file read-only.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_RDONLY: Int32 = 0x0

/**
* Open the file in read/write mode.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_RDWR: Int32 = 0x2

/**
* Open file write-only.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_WRONLY: Int32 = 0x1

/**
* Open the file in append mode.
* That is, the written data is appended to the end of the file.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_APPEND: Int32 = 0x400

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_APPEND: Int32 = 0x00000008

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_APPEND: Int32 = 0x8

/**
* Use of this flag is essential in some multithreaded programs since using a separate fcntl(2) F_SETFD operation to
* set the FD_CLOEXEC flag does not suffice to avoid race conditions where one thread opens a file descriptor at the
* same time as another thread does a fork(2) plus execve(2).
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_CLOEXEC: Int32 = 0x80000

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_CLOEXEC: Int32 = 0x01000000

/**
* If the file to be opened does not exist, it is automatically created.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_CREAT: Int32 = 0x40

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_CREAT: Int32 = 0x00000200

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_CREAT: Int32 = 0x100

/**
* If the file specified by pathname is not a directory, it fails to be opened.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_DIRECTORY: Int32 = 0x10000

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_DIRECTORY: Int32 = 0x00100000

/**
* Except there is no need to wait for any metadata changes that are not necessary to read the data just written.
* In practice, O_DSYNC means that the application does not need to wait for ancillary information,
* such as file modification time, to be written to disk.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_DSYNC: Int32 = 0x1000

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_DSYNC: Int32 = 0x400000

/**
* If O_CREAT is also set, this directive checks to see if the file exists. If the file does not exist, create the file.
* Otherwise, an error occurs in opening the file. In addition, if both O_CREAT
*            and O_EXCL are set and the file to be opened is a symbolic link, the file fails to be opened.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_EXCL: Int32 = 0x80

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_EXCL: Int32 = 0x00000800

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_EXCL: Int32 = 0x400

/**
* If the file to be opened is a terminal device, the terminal is not treated as a process control terminal.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_NOCTTY: Int32 = 0x100

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_NOCTTY: Int32 = 0x00020000

/**
* If the file specified by pathname is a one-symbol connection, the file fails to be opened.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_NOFOLLOW: Int32 = 0x20000

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_NOFOLLOW: Int32 = 0x00000100

/**
* Open a file in an unblockable manner, that is, immediately returns to the process with or without data read or waiting.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_NONBLOCK: Int32 = 0x800

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_NONBLOCK: Int32 = 0x00000004

/**
* Open the file synchronously.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_SYNC: Int32 = 0x101000

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_SYNC: Int32 = 0x0080

/**
* If a file exists and is opened writable, this flag clears the file length to 0,
* and the data previously stored in the file disappears.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_TRUNC: Int32 = 0x200

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_TRUNC: Int32 = 0x00000400

/**
* This flag only affects read operations and must be used in conjunction with O_SYNC or O_DSYNC.
* It causes the region () call to block until the data being read (and possibly metadata)
* has been flushed to disk, if necessary.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const O_RSYNC: Int32 = 0x101000

/**
* Indicates that the file owner has the read permission.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IRUSR: UInt32 = 0x100

/**
* Indicates that the file owner has the write permission.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IWUSR: UInt32 = 0x80

/**
* Indicates that the file owner has the execute permission.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IXUSR: UInt32 = 0x40

/**
* Indicates that the file owner has the read, write, and execute permissions.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IRWXU: UInt32 = 0x1C0

/**
* Indicates that the file user group has the read permission.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IRGRP: UInt32 = 0x20

/**
* Indicates that the file user group has the write permission.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IWGRP: UInt32 = 0x10

/**
* Indicates that the file user group has the execute permission.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IXGRP: UInt32 = 0x8

/**
* Indicates that the file user group has the read, write, and execute permissions.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IRWXG: UInt32 = 0x38

/**
* Read permission on behalf of other users.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IROTH: UInt32 = 0x4

/**
* Indicates that other users have the write permission.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IWOTH: UInt32 = 0x2

/**
* You have the execute permission on behalf of other users.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IXOTH: UInt32 = 0x1

/**
* The user has the read, write, and execute permissions on behalf of other users.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IRWXO: UInt32 = 0x7

/**
* The offset parameter indicates the new read/write position.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SEEK_SET: Int32 = 0x0

/**
* Add offsets to the current read/write position.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SEEK_CUR: Int32 = 0x1

/**
* Set the read/write position to the end of the file and add offsets.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SEEK_END: Int32 = 0x2

/**
* Open a file and returns a new file descriptor for it, or -1 on an error.
* When the O_CREAT parameter is present, the permission can be set via flag.
* Binding POSIX API is [open](https://man7.org/linux/man-pages/man2/open.2.html).
*
* @param path the file path.
* @param oflag it's the way to open,O_RDONLY,O_RDWR,O_WRONLY is a mutually exclusive condition
* and can be used together with other operations such as the O_APPEND process | operation.
* @param flag if O_CREAT is also set and a new file needs to be created, flag indicates the permission on the new file.
* @throws IllegalArgumentException while path contains null character.
* @return a file descriptor.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func `open`(path: String, oflag: Int32, flag: UInt32): Int32

/**
* Open a file and returns a new file descriptor for it, or -1 on error.
* Binding POSIX API is [open](https://man7.org/linux/man-pages/man2/open.2.html).
*
* @param path the file path.
* @param oflag it's the way to open,O_RDONLY,O_RDWR,O_WRONLY is a mutually exclusive condition
*        and can be used together with other operations such as the O_APPEND process | operation.
* @throws IllegalArgumentException while path contains null character.
* @return a file descriptor.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func `open`(path: String, oflag: Int32): Int32

/**
* Open a file and returns a new file descriptor for it, or -1 on an error.
* When the O_CREAT parameter is present, the permission can be set via flag.
* Binding POSIX API is [open](https://man7.org/linux/man-pages/man2/open.2.html).
*
* @param path the file path.
* @param oflag it's the way to open,O_RDONLY,O_RDWR,O_WRONLY is a mutually exclusive condition
*        and can be used together with other operations such as the O_APPEND process | operation.
* @param flag if O_CREAT is also set and a new file needs to be created, flag indicates the permission on the new file.
* @throws IllegalArgumentException while path contains null character.
* @return a file descriptor.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func open64(path: String, oflag: Int32, flag: UInt32): Int32

/**
* Open a file and returns a new file descriptor for it, or -1 on error.
* Binding POSIX API is [open](https://man7.org/linux/man-pages/man2/open.2.html).
*
* @param path the file path.
* @param oflag it's the way to open,O_RDONLY,O_RDWR,O_WRONLY is a mutually exclusive condition
*        and can be used together with other operations such as the O_APPEND process | operation.
* @throws IllegalArgumentException while path contains null character. 
* @return a file descriptor.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func open64(path: String, oflag: Int32): Int32

/**
* Open a file relative to a directory file descriptor,on error, -1 is returned and errno is set to indicate the error.
* Binding POSIX API is [openat](https://man7.org/linux/man-pages/man2/openat.2.html).
*
* @param fd a file descriptor.
* @param path the file path.
* @param oflag it's the way to open,O_RDONLY,O_RDWR,O_WRONLY is a mutually exclusive condition
*        and can be used together with other operations such as the O_APPEND process | operation.
* @param flag if O_CREAT is also set and a new file needs to be created, flag indicates the permission on the new file.
* @throws IllegalArgumentException while path contains null character. 
* @return a file descriptor.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func openat(fd: Int32, path: String, oflag: Int32, flag: UInt32): Int32

/**
* Open a file relative to a directory file descriptor,on error, -1 is returned and errno is set to indicate the error.
* Binding POSIX API is [openat](https://man7.org/linux/man-pages/man2/openat.2.html).
*
* @param fd a file descriptor.
* @param path the file path.
* @param oflag it's the way to open,O_RDONLY,O_RDWR,O_WRONLY is a mutually exclusive condition
*        and can be used together with other operations such as the O_APPEND process | operation.
* @throws IllegalArgumentException while path contains null character. 
* @return a file descriptor.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func openat(fd: Int32, path: String, oflag: Int32): Int32

/**
* Open a file relative to a directory file descriptor,on error, -1 is returned and errno is set to indicate the error.
* Binding POSIX API is [openat](https://man7.org/linux/man-pages/man2/openat.2.html).
*
* @param fd a file descriptor.
* @param path the file path.
* @param oflag it's the way to open,O_RDONLY,O_RDWR,O_WRONLY is a mutually exclusive condition
*        and can be used together with other operations such as the O_APPEND process | operation.
* @param flag if O_CREAT is also set and a new file needs to be created, flag indicates the permission on the new file.
* @throws IllegalArgumentException while path contains null character.
* @return a file descriptor.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func openat64(fd: Int32, path: String, oflag: Int32, flag: UInt32): Int32

/**
* Open a file relative to a directory file descriptor,on error, -1 is returned and errno is set to indicate the error.
* Binding POSIX API is [openat](https://man7.org/linux/man-pages/man2/openat.2.html).
*
* @param fd a file descriptor.
* @param path the file path.
* @param oflag it's the way to open,O_RDONLY,O_RDWR,O_WRONLY is a mutually exclusive condition
*        and can be used together with other operations such as the O_APPEND process | operation.
* @throws IllegalArgumentException while path contains null character.
* @return a file descriptor.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func openat64(fd: Int32, path: String, oflag: Int32): Int32

/**
* The pathname parameter points to the file path String to be created.
* Binding POSIX API is [creat](https://man7.org/linux/man-pages/man2/creat.2.html).
*
* @param path the file path.
* @param flag Indicates the permission to create a file.
* @throws IllegalArgumentException while path contains null character.
* @return a new file descriptor, and -1 if an error occurs.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func creat(path: String, flag: UInt32): Int32

/**
* If the file is no longer needed, close the file by using close().
* Close() causes the data to be written back to disk and frees the resources occupied by the file.
* Binding POSIX API is [close](https://man7.org/linux/man-pages/man2/close.2.html).
*
* @param fd a file descriptor.
* @return 0: success; –1: error.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func close(fd: Int32): Int32

/**
* When read() or write() is performed, the read/write location increases accordingly.
* Lseek() is used to control the read/write location of the file.
* The fildes parameter indicates the descriptor of an opened file, and the offset parameter indicates the number of offsets
*     for moving the read and write positions based on the whence parameter.
* If there is an error, -1 is returned.
* Binding POSIX API is [lseek](https://man7.org/linux/man-pages/man2/lseek.2.html).
*
* @param fd a file descriptor.
* @param offset the number of bits moved.
* @param whence three ways to move. There are SEEK_SET,SEEK_CUR,SEEK_END.
* @return  when the call is successful, the current read/write position is returned, that is, the number of bytes from the beginning of the file.
* if an error occurs, -1 is returned, and errno stores the error code.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func lseek(fd: Int32, offset: Int64, whence: Int32): Int64

/**
* Used to copy the file descriptor indicated by the oldfd parameter and return it.
* This new file descriptor and parameter oldfd refer to the same file, sharing all locks, reads, and writes.
* Location and authority or flag.
* Binding POSIX API is [dup](https://man7.org/linux/man-pages/man2/dup.2.html).
*
* @param fd a file descriptor.
* @return when the copy is successful, the minimum and unused file descriptors are returned. Returns -1 if there is an error.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func dup(fd: Int32): Int32

/**
* Copy the file descriptor specified by the oldfd parameter and return it to the next block of the newfd parameter.
* If the parameter newfd is an open file descriptor, the file specified by newfd will be closed first.
* Binding POSIX API is [dup2](https://man7.org/linux/man-pages/man2/dup2.2.html).
*
* @param fd a file descriptor.
* @param fd2 a file descriptor.
* @return @p fd2 file descriptor.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func dup2(fd: Int32, fd2: Int32): Int32

/**
* Read() transfers count bytes of the file indicated by the parameter fd to the memory indicated by the buffer pointer.
*        If count is 0, read() has no effect and returns 0.
* The returned value is the number of bytes actually read. If the returned value is 0,
*     the end of the file is reached or no data can be read. In addition, the read and write positions of the file change with the read bytes.
* Binding POSIX API is [read](https://man7.org/linux/man-pages/man2/read.2.html).
* Note: It is recommended that the size of count be the same as that of buf. You are advised to use safe usages such as buf.size.
* It is recommended that the size of a single buffer be less than or equal to 120000 bytes.
*
* @param fd a file descriptor.
* @param buffer reads data to the buffer container.
* @param nbyte size to read, buffer.size is recommended.
* @return number of characters actually read
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public unsafe func read(fd: Int32, buffer: CPointer<UInt8>, nbyte: UIntNative): IntNative

/**
* Read() transfers count bytes of the file specified by fd to the memory specified by the buffer pointer.
*        offset indicates the offset of the read position. If count is 0, read() does not take effect and returns 0.
* The returned value is the number of bytes actually read. If the returned value is 0, the end of the file
*     is reached or no data can be read. In addition, the read and write positions of the file change with the read bytes.
* Binding POSIX API is [pread](https://man7.org/linux/man-pages/man2/pread.2.html).
* Note: It is recommended that the size of count be the same as that of buf. You are advised to use safe usages such as buf.size.
* It is recommended that the size of a single buffer be less than or equal to 120000 bytes.
*
* @param fd a file descriptor.
* @param buffer reads data to the buffer container.
* @param nbyte size to read, buffer.size is recommended.
* @param offset Offset of the buffer.
* @return Actual Number of reads,on error, -1 is returned.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public unsafe func pread(fd: Int32, buffer: CPointer<UInt8>, nbyte: UIntNative, offset: Int32): IntNative

/**
* Write() writes count bytes of the memory specified by the parameter buf to the file specified by the parameter fd.
* In the file. Of course, the read and write locations of the file are moved.
* Returns -1 when an error occurs.
* Binding POSIX API is [write](https://man7.org/linux/man-pages/man2/write.2.html).
* Note: It is recommended that the size of count be the same as that of buf. You are advised to use safe usages such as buf.size.
* It is recommended that the size of a single buffer be less than or equal to 120000 bytes.
*
* @param fd a file descriptor.
* @param buffer write data to the buffer container.
* @param nbyte size to write, buffer.size is recommended.
* @return Actual Number of write,on error, -1 is returned.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public unsafe func write(fd: Int32, buffer: CPointer<UInt8>, nbyte: UIntNative): IntNative

/**
* Write() writes count bytes of the memory specified by the parameter buf to the file specified by the parameter fd.
* In the file. Of course, the read and write locations of the file are moved.
* offset indicates the offset of the write position.
* Binding POSIX API is [pwrite](https://man7.org/linux/man-pages/man2/pwrite.2.html).
* Note: It is recommended that the size of count be the same as that of buf. You are advised to use safe usages such as buf.size,
* It is recommended that the size of a single buffer be less than or equal to 120000 bytes.
*
* @param fd a file descriptor.
* @param buffer write data to the buffer container.
* @param nbyte size to write, buffer.size is recommended.
* @param offset offset of the buffer.
* @return actual Number of write,on error, -1 is returned.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public unsafe func pwrite(fd: Int32, buffer: CPointer<UInt8>, nbyte: UIntNative, offset: Int32): IntNative

/**< General documents. */
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IFREG: UInt32 = 0x8000

/**< Table of Contents. */
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IFDIR: UInt32 = 0x4000

/**< Character device. */
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IFCHR: UInt32 = 0x2000

/**< Block device. */
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IFBLK: UInt32 = 0x6000

/**< FIFO file. */
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IFIFO: UInt32 = 0x1000

/**< Soft connection. */
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IFLNK: UInt32 = 0xA000

/**< Socket file. */
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const S_IFSOCK: UInt32 = 0xC000

/**
* Check whether the file is a common file. The boolean type is returned.If true is returned, false is not returned.
* Different types are determined based on different values of mode.
* Binding POSIX API is [lstat](https://man7.org/linux/man-pages/man2/lstat.2.html).
*
* @param path the file path.
* @param mode differentiating Different Judgment Parameters
* @return if yes, true is returned. Otherwise, false is returned.
+
* @since 0.18.2
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func isType(path: String, mode: UInt32): Bool

/**
* Check whether the file is a common file. The boolean type is returned.
* Binding POSIX API is [lstat](https://man7.org/linux/man-pages/man2/lstat.2.html).
*
* @param path the file path.
* @return if yes, true is returned. Otherwise, false is returned.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func isReg(path: String): Bool

/**
* Check whether the directory is a directory. The boolean type is returned.
* Binding POSIX API is [lstat](https://man7.org/linux/man-pages/man2/lstat.2.html).
*
* @param path the file path.
* @return if yes, true is returned. Otherwise, false is returned.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func isDir(path: String): Bool

/**
* Check whether the device is a character device. The boolean type is returned.
* Binding POSIX API is [lstat](https://man7.org/linux/man-pages/man2/lstat.2.html).
*
* @param path the file path.
* @return if yes, true is returned. Otherwise, false is returned.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func isChr(path: String): Bool

/**
* Check whether the device is a block device and return the boolean type.
* Binding POSIX API is [lstat](https://man7.org/linux/man-pages/man2/lstat.2.html).
*
* @param path the file path.
* @return if yes, true is returned. Otherwise, false is returned.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func isBlk(path: String): Bool

/**
* Check whether the file is a FIFO file. The boolean type is returned.
* Binding POSIX API is [lstat](https://man7.org/linux/man-pages/man2/lstat.2.html).
*
* @param path the file path.
* @return if yes, true is returned. Otherwise, false is returned.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func isFIFO(path: String): Bool

/**
* Check whether the file is a socket file. The boolean type is returned.
* Binding POSIX API is [lstat](https://man7.org/linux/man-pages/man2/lstat.2.html).
*
* @param path the file path.
* @return if yes, true is returned. Otherwise, false is returned.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func isSock(path: String): Bool

/**
* Check whether the link is a soft link. The boolean type is returned.
* Binding POSIX API is [lstat](https://man7.org/linux/man-pages/man2/lstat.2.html).
*
* @param path the file path.
* @return if yes, true is returned. Otherwise, false is returned.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func isLnk(path: String): Bool

/**
* We can change the current working directory of the calling process by calling the
* chdir function.
* Binding POSIX API is [chdir](https://man7.org/linux/man-pages/man2/chdir.2.html).
*
* @param path the file path.
* @return If the operation succeeds, 0 is returned. If an error occurs, -1 is returned.
*
* @since 0.18.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func chdir(path: String): Int32

/**
* We can change the current working directory of the calling process by calling the
* fchdir function.
* Binding POSIX API is [fchdir](https://man7.org/linux/man-pages/man2/fchdir.2.html).
*
* @param fd a file descriptor.
* @return If the operation succeeds, 0 is returned. If an error occurs, -1 is returned.
*
* @since 0.18.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func fchdir(fd: Int32): Int32

/**
* Obtain the absolute path of the current execution.
* Binding POSIX API is [getcwd](https://man7.org/linux/man-pages/man2/getcwd.2.html).
*
* @return a String containing path information is returned if the operation succeeds. an empty String is returned if the operation fails.
*
* @since 0.18.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func getcwd(): String

/**
* Obtain the information about the Linux system from the /proc/version file.
*
* @return on success returns message.Example: Linux version 4.12.0-142-generic (buildd@lgw01-amd64-036)
* (gcc version 7.5.0 (Ubuntu 7.5.0-3ubuntu1-18.04)) #146-Ubuntu SMP Tue Apr 13 01:11:19 UTC 2021 on error,
* an empty String is returned.
*
* @since 0.18.4
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func getos(): String

/**
* Gethostname function to return only the name of the host.
* This name is usually the name of the host on a TCP/IP network.
* Binding POSIX API is [gethostname](https://man7.org/linux/man-pages/man2/gethostname.2.html).
*
* @return the tuple type. String stores hostname. Int32 stores Returns: 0 if OK, -1 on error.
*
* @since 0.18.4
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func gethostname(): String

/**
* The host name is set by the superuser using a similar function, sethostname.
* Binding POSIX API is [sethostname](https://man7.org/linux/man-pages/man2/sethostname.2.html).
*
* @param buf The buffer stores the host name to be set.
* @throws IllegalArgumentException while path contains null character.
* @return on success, zero is returned.  On error, -1 is returned, and errno is set to indicate the error.
*
* @since 0.18.4
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func sethostname(buf: String): Int32

/**
* The host name is set by the superuser using a similar function, sethostname.
* Binding POSIX API is .
*
* @return if the operation is successful, the login name of the String type is returned. An empty String is returned upon failure.
*
* @since 0.18.4
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func getlogin(): String

/**
* a file can have multiple directory entries pointing to its i-node.
* We can use either the link function to create a link to an existing file.
* Binding POSIX API is [link](https://man7.org/linux/man-pages/man2/link.2.html).
*
* @param path the file path.
* @param newpath other file path.if newpath exists, it will not be overwritten.
* @throws IllegalArgumentException while path or newpath contains null character.
* @return on success, zero is returned.  on error, -1 is returned, and errno is set to indicate the error.
*
* @since 0.18.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func link(path: String, newpath: String): Int32

/**
* deletes a name from the filesystem.
* If that name was the last link to a file and no processes have the file open, the file is deleted
*    and the space it was using is made available for reuse.
* If the name was the last link to a file but any processes still have the file open,
*    the file will remain in existence until the last file descriptor referring to it is closed.
* If the name referred to a symbolic link, the link is removed.
* If the name referred to a socket, FIFO, or device, the name for it is removed but processes which have the object open may continue to use it.
* Binding POSIX API is [unlink](https://man7.org/linux/man-pages/man2/unlink.2.html).
*
* @param path the file path.
* @throws IllegalArgumentException while path contains null character.
* @return on success, zero is returned.  on error, -1 is returned, and errno is set to indicate the error.
*
* @since 0.18.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func unlink(path: String): Int32

/**
* If oldpath is an empty string, create a link to the file referenced by olddirfd (which may have been obtained using
* the open(2) O_PATH flag).  In this case, olddirfd can refer to any type of file except a directory.
* This will generally not work if the file has a link count of zero (files created with O_TMPFILE and without O_EXCL are an exception).
* The caller must have the CAP_DAC_READ_SEARCH capability in order to use this flag.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const AT_EMPTY_PATH: Int32 = 0x1000

/**
* By default, linkat(), does not dereference oldpath if it is a symbolic link (like link()).
* The flag AT_SYMLINK_FOLLOW can be specified in flags to cause oldpath to be dereferenced if it is a symbolic link.
* If procfs is mounted, this can be used as an alternative to AT_EMPTY_PATH
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const AT_SYMLINK_FOLLOW :Int32 = 0x400

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const AT_SYMLINK_FOLLOW :Int32 = 0x040

/**
* fd The special value AT_FDCWD, then pathname is interpreted relative to the current working directory of the calling process.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const AT_FDCWD :Int32 = -0x64

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const AT_FDCWD :Int32 = -2

/**
* If oldpath is absolute, then olddirfd is ignored. The interpretation of newpath is as for oldpath, except that a relative
* pathname is interpreted relative to the directory referred to by the file descriptor newdirfd.
* Binding POSIX API is [linkat](https://man7.org/linux/man-pages/man2/linkat.2.html).
*
* @param fd a file descriptor.
* @param path the file path.
* @param nfd a file descriptor.
* @param newpath other file path.if newpath exists, it will not be overwritten.
* @param lflag AT_EMPTY_PATH or AT_SYMLINK_FOLLOW or 0
* @throws IllegalArgumentException while path or newPath contains null character.
* @return on success, zero is returned.  on error, -1 is returned, and errno is set to indicate the error.
*
* @since 0.18.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func linkat(fd: Int32, path: String, nfd: Int32, newPath: String, lflag: Int32): Int32

/**
* If the AT_REMOVEDIR flag is specified, then performs the equivalent of rmdir(2) on pathname.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const AT_REMOVEDIR :Int32 = 0x200

/**
* The unlinkat() system call operates in exactly the same way as either unlink() or rmdir(2) (depending on whether
*     or not flags includes the AT_REMOVEDIR flag) except for the differences described here.
* If the pathname given in pathname is relative, then it is interpreted relative to the directory referred to by the file descriptor dirfd.
* If the pathname given in pathname is relative and dirfd is the special value AT_FDCWD, then pathname is
*    interpreted relative to the current working directory of the calling process (like unlink() and rmdir(2)).
* If the pathname given in pathname is absolute, then dirfd is ignored.
* Binding POSIX API is [unlinkat](https://man7.org/linux/man-pages/man2/unlinkat.2.html).
*
* @param fd a file descriptor.
* @param path the file path.
* @param lflag AT_REMOVEDIR The superuser can call unlink with pathname specifying a directory if the filesystem supports it, or 0 .
* @throws IllegalArgumentException while path contains null character.
* @return on success, zero is returned.  on error, -1 is returned, and errno is set to indicate the error.
*
* @since 0.18.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func unlinkat(fd: Int32, path: String, ulflag: Int32): Int32

/**
* symlink() creates a symbolic link named linkpath which contains the string target.
* Symbolic links are interpreted at run time as if the contents of the link had been substituted into the path being followed to find a file or directory.
* Symbolic links may contain .. path components, which (if used at the start of the link) refer to the parent directories of that in which the link resides.
* A symbolic link (also known as a soft link) may point to an existing file or to a nonexistent one; the latter case is known as a dangling link.
* The permissions of a symbolic link are irrelevant; the ownership is ignored when following the link,
*     but is checked when removal or renaming of the link is requested and the link is in a directory with the sticky bit set.
* If linkpath exists, it will not be overwritten.
* Binding POSIX API is [symlink](https://man7.org/linux/man-pages/man2/symlink.2.html).
*
* @param path the file path.
* @param sympath the file path
* @throws IllegalArgumentException while path or symPath contains null character.
* @return on success, zero is returned.  on error, -1 is returned, and errno is set to indicate the error.
*
* @since 0.18.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func symlink(path: String, symPath: String): Int32

/**
* If the pathname given in linkpath is relative, then it is interpreted relative to the directory referred to
*    by the file descriptor newdirfd (rather than relative to the current working directory of the calling process).
* If linkpath is relative and newdirfd is the special value AT_FDCWD, then linkpath
*    is interpreted relative to the current working directory of the calling process.If linkpath is absolute, then newdirfd is ignored.
* Binding POSIX API is [symlinkat](https://man7.org/linux/man-pages/man2/symlinkat.2.html).
*
* @param path the file path.
* @param fd a file descriptor.
* @param sympath the file path.
* @throws IllegalArgumentException while path or symPath contains null character. 
* @return on success, zero is returned.  on error, -1 is returned, and errno is set to indicate the error.
*
* @since 0.18.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func symlinkat(path: String, fd: Int32, symPath: String): Int32

/**
* We can also unlink a file or a directory with the remove function. For a file,
* remove is identical to unlink. For a directory, remove is identical to rmdir.
* Binding POSIX API is [remove](https://man7.org/linux/man-pages/man3/remove.3.html).
*
* @param path the file path.
* @throws IllegalArgumentException while path contains null character.
* @return on success, zero is returned.  on error, -1 is returned, and errno is set to indicate the error.
*
* @since 0.18.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func remove(path: String): Int32

/**
* rename() renames a file, moving it between directories if required.  Any other hard links to the file
*          (as created using link(2)) are unaffected.  Open file descriptors for oldpath are also unaffected.
* Various restrictions determine whether or not the rename operation succeeds: see ERRORS below.
* If newpath already exists, it will be atomically replaced, so that there is no point at which another process
*    attempting to access newpath will find it missing.  However, there will probably be a window in which both oldpath and newpath refer to the file being renamed.
* If oldpath and newpath are existing hard links referring to the same file, then rename() does nothing, and returns a success status.
* If newpath exists but the operation fails for some reason,rename() guarantees to leave an instance of newpath in place.
* oldpath can specify a directory.  In this case, newpath must either not exist, or it must specify an empty directory.
* If oldpath refers to a symbolic link, the link is renamed; if newpath refers to a symbolic link, the link will be overwritten.
* Binding POSIX API is [rename](https://man7.org/linux/man-pages/man2/rename.2.html).
*
* @param oldName the file pathname.
* @param newName the file pathname.
* @throws IllegalArgumentException while oldName or newName contains null character.
* @return on success, zero is returned.  on error, -1 is returned, and errno is set to indicate the error.
*
* @since 0.18.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func rename(oldName: String, newName: String): Int32

/**
*The renameat() system call operates in exactly the same way as rename(), except for the differences described here.
*If the pathname given in oldpath is relative, then it is interpreted relative to the directory referred to by the file descriptor olddirfd
*If oldpath is relative and olddirfd is the special value  AT_FDCWD, then oldpath
*   is interpreted relative to the current working directory of the calling process (like rename()).If oldpath is absolute, then olddirfd is ignored.
*The interpretation of newpath is as for oldpath, except that a relative pathname is interpreted relative to the directory
*    referred to by the file descriptor newdirfd.
*
* @param oldfd a file descriptor.
* @param oldName the file pathname.
* @param newfd a file descriptor.
* @param newName other file pathname.
* @throws IllegalArgumentException while oldName or newName contains null character. 
* @return on success, zero is returned.  on error, -1 is returned, and errno is set to indicate the error.
*
* @since 0.18.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func renameat(oldfd: Int32, oldName: String, newfd: Int32, newName: String): Int32

/**
* The connection is disconnected; the default action is terminated.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGHUP: Int32 = 0x1

/**
* Terminal interrupt character; default action termination.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGINT: Int32 =  0x2

/**
* Terminal exit character; Default action termination + core.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGQUIT: Int32 = 0x3

/**
* Invalid hardware instruction. Default action termination + core.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGILL: Int32 = 0x4

/**
* Hardware fault; default action terminated + core.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGTRAP: Int32 = 0x5

/**
* Abnormal termination; default action termination + core.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGABRT: Int32 = 0x6

/**
* Hardware fault; default action terminated + core.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGIOT: Int32 = 0x6

/**
* Hardware fault; default action terminated + core.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGBUS: Int32 = 0x7

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGBUS: Int32 = 0xA

/**
* Arithmetic error; Default action termination + core.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGFPE: Int32 = 0x8

/**
* Terminate;Default Action Terminate.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGKILL: Int32 = 0x9

/**
* User Defined Signal; Default Action Termination.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGUSR1: Int32 = 0xA

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGUSR1: Int32 = 0x1E

/**
* Invalid memory reference; Default action termination + core.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGSEGV: Int32 = 0xB

/**
* User Defined Signal; Default Action Termination.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGUSR2: Int32 = 0xC

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGUSR2: Int32 = 0x1F

/**
* Pipelines written to unread processes; default action terminates.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGPIPE: Int32 = 0xD

/**
* The timer expires; the default action is terminated.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGALRM: Int32 = 0xE

/**
* Terminate;Default Action Terminate.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGTERM: Int32 = 0xF

/**
* Coprocessor stack failure; default action terminated.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGSTKFLT: Int32 = 0x10

/**
* Subprocess status changes; default action ignored.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGCHLD: Int32 = 0x11

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGCHLD: Int32 = 0x14

/**
* Continuation of the suspension process; Default Action Continue/Ignore.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGCONT: Int32 = 0x12

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGCONT: Int32 = 0x13

/**
* Stop;Default Action Stop.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGSTOP: Int32 = 0x13

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGSTOP: Int32 = 0x11

/**
* Terminal stop symbol; The default action stops the process.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGTSTP: Int32 = 0x14

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGTSTP: Int32 = 0x12

/**
* Background read control tty; the default action stops the process.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGTTIN: Int32 = 0x12

/**
* Background write control tty; the default action stops the process.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGTTOU: Int32 = 0x16

/**
* Emergency (socket); default action ignored.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGURG: Int32 =  0x17

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGURG: Int32 =  0x10

/**
* The CPU usage exceeds the upper limit. Default Action Termination or Termination + Core.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGXCPU: Int32 = 0x18

/**
* The file length exceeds the upper limit. Default Action Termination or Termination + Core.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGXFSZ: Int32 = 0x19

/**
* Virtual Time Alarm; Default Action Terminated.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGVTALRM: Int32 = 0x1A

/**
* Summary Timeout; Default Action Terminated.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGPROF: Int32 = 0x1B

/**
* Terminal window size changes; default action ignored.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGWINCH: Int32 = 0x1C

/**
* Asynchronous IO; Default Action Terminate/Ignore.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGIO: Int32 = 0x1D

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGIO: Int32 = 0x17

/**
* The power supply fails or restarts. Default Action Terminate/Ignore.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGPWR: Int32 = 0x1E

/**
* Invalid system call; Default action termination + core.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGSYS: Int32 = 0x1F

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const SIGSYS: Int32 = 0xC

/**
* Obtain the user group ID.
* Binding POSIX API is [dup2](https://man7.org/linux/man-pages/man2/getgid.2.html).
*
* @return the user group ID.
*
* @since 0.18.2
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func getgid(): UInt32

/**
* Returns the real user ID of the calling process.
* Binding POSIX API is [dup2](https://man7.org/linux/man-pages/man2/getuid.2.html).
*
* @return the user id.
*
* @since 0.18.2
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func getuid(): UInt32

/**
* Sets the effective group ID of the calling process,requires appropriate permissions.
* Binding POSIX API is [setgid](https://man7.org/linux/man-pages/man2/setgid.2.html).
*
* @param id to be set.
* @return if the setting is successful, 0 is returned. If the setting fails, -1 is returned.
*
* @since 0.18.2
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func setgid(id: UInt32): Int32

/**
* Sets the effective user ID of the calling process,requires appropriate permissions.
* Binding POSIX API is [setuid](https://man7.org/linux/man-pages/man2/setuid.2.html).
*
* @param id to be set.
* @return if the setting is successful, 0 is returned. If the setting fails, -1 is returned.
*
* @since 0.18.2
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func setuid(id: UInt32): Int32

/**
* Returns the PGID of the process specified by pid.If pid is zero, the process ID of the calling process is used.
* Binding POSIX API is [getpgid](https://man7.org/linux/man-pages/man2/getpgid.2.html).
*
* @param pid process id.
* @return if the execution is successful, the process group ID is returned. If an error occurs, -1 is returned.
*
* @since 0.18.2
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func getpgid(pid: Int32): Int32

/**
* Function description getgroup() is used to get the code of the group to which the current user belongs. The value of size is list.
* Number of gid_ts that [] can hold. If the parameter size has a value of zero, this function returns only
* Indicates the number of groups to which a user belongs.
* Binding POSIX API is [getgroups](https://man7.org/linux/man-pages/man2/getgroups.2.html).
*
* @param size number of data stored in the @p gidArray.
* @param gidArray stores group information.
* @return the group identifier, or -1 for errors.
*
* @since 0.18.2
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public unsafe func getgroups(size: Int32, gidArray: CPointer<UInt32>): Int32

/**
* Returns the process ID (PID) of the calling process.
* Binding POSIX API is [getpid](https://man7.org/linux/man-pages/man2/getpid.2.html).
*
* @return obtain the current process ID.
*
* @since 0.18.2
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func getpid(): Int32

/**
* Returns the process ID of the parent of the calling process.
* Binding POSIX API is [getppid](https://man7.org/linux/man-pages/man2/getppid.2.html).
*
* @return obtain the ID of the current parent process.
*
* @since 0.18.2
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func getppid(): Int32

/**
* Function description getpgrp() is used to get the ID of the group to which the current process belongs.
* This function is equivalent to callinggetpgid(0);The return value returns the ID of the group to which the current process belongs.
* Binding POSIX API is [getpgrp](https://man7.org/linux/man-pages/man2/getpgrp.2.html).
*
* @return obtains the ID of the group to which the current process belongs.
*
* @since 0.18.2
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func getpgrp(): Int32

/**
* Function description setpgrp() sets the group ID to which the current process belongs to the process ID of the current process.
* This function is equivalent to calling setpgid(0, 0).
* Binding POSIX API is [setpgrp](https://man7.org/linux/man-pages/man2/setpgrp.2.html).
*
* @return if the operation is successful, the group ID is returned. If there is an error, -1 is returned.
*
* @since 0.18.2
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func setpgrp(): Int32

/**
* Function description setpgid() sets the group ID specified by the parameter pid to the group ID specified by the parameter pgid.
* If the value of pid is 0,the group ID of the current process is used.
* Binding POSIX API is [setpgid](https://man7.org/linux/man-pages/man2/setpgid.2.html).
*
* @param pid process id.
* @param pgrp Process group ID.
* @return if the operation is successful, the group ID is returned. If there is an error, -1 is returned.
*
* @since 0.18.2
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func setpgid(pid: Int32, pgrp: Int32): Int32

/**
* Change the priority of the current thread.
* Only the super user can use a negative inc value, which indicates that the priority is first and the process executes faster.
* Binding POSIX API is [nice](https://man7.org/linux/man-pages/man2/nice.2.html).
*
* @param inc Priority of the current process,the range of the nice value is +19 (low priority) to -20.
* @return on success, the new nice value is returned (but see NOTES below),on error, -1 is returned, and errno is set to indicate the error.
* inc returns the maximum value 19 when the value is greater than 19.
*
* @since 0.18.2
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func nice(inc: Int32): Int32

/**
* System call can be used to send any signal to any process group or process.
* If pid is positive, then signal sig is sent to the process with the ID specified by pid.
* If pid equals 0, then sig is sent to every process in the process group of the calling process.
* If pid equals -1, then sig is sent to every process for which the calling process has permission to send signals,
* except for process 1 (init), but see below.
* If pid is less than -1, then sig is sent to every process in the process group whose ID is -pid.
* Binding POSIX API is [kill](https://man7.org/linux/man-pages/man2/kill.2.html).
*
* @param pid Process ID.
* @param sig indicates the signal ID
* @return if the execution is successful, 0 is returned. if there is an error, -1 is returned.
*
* @since 0.18.2
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func kill(pid: Int32, sig: Int32): Int32

/**
* Sends the signal sig to the process group pgrp. If pgrp is 0, killpg() sends the signal to the calling process's process group.
* Binding POSIX API is [killpg](https://man7.org/linux/man-pages/man2/killpg.2.html).
*
* @param pgid group ID
* @param sig indicates the signal ID.
* @return if the execution is successful, 0 is returned. if there is an error, -1 is returned.
*
* @since 0.18.2
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func killpg(pgid: Int32, sig: Int32): Int32

/**
* Test for read permission.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const R_OK: Int32 = 0x4

/**
* Test for write permission.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const W_OK: Int32 = 0x2

/**
* Test for execute permission.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const X_OK: Int32 = 0x1

/**
* Test for existence.
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public const F_OK: Int32 = 0x0

/**
* File access permission. The value is 0 for success and -1 for failure.
* Binding POSIX API is [access](https://man7.org/linux/man-pages/man2/access.2.html).
*
* @param path the file path.
* @param mode description of determining the read/write type, incoming Type R_OK, W_OK, X_OK, F_OK.
* @throws IllegalArgumentException while path contains null character.
* @return if all permissions to be checked pass the check, the value 0 is returned, indicating that the check is successful.
* if one permission is disabled, the value -1 is returned.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func access(path: String, mode: Int32): Int32

/**
* Viewing FD Access Rights.
* Binding POSIX API is [faccessat](https://man7.org/linux/man-pages/man2/faccessat.2.html).
*
* @param fd a file descriptor.
* @param path the file path.
* @param mode description of determining, incoming Type R_OK, W_OK, X_OK, F_OK.
* @param flag parameter.
* @throws IllegalArgumentException while path contains null character.
* @return if the operation succeeds, 0 is returned. Otherwise, all return -1.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func faccessat(fd: Int32, path: String, mode: Int32, flag: Int32): Int32

/**
* Set Default Permission Mask.
* Binding POSIX API is [umask](https://man7.org/linux/man-pages/man2/umask.2.html).
*
* @param cmask file Permission Parameters
* @return the previous value of the mask.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func umask(cmask: UInt32): UInt32

/**
* Modify the file owner and the group to which the file owner belongs.
* Binding POSIX API is [chown](https://man7.org/linux/man-pages/man2/chown.2.html).
*
* @param path the file path.
* @param owner uid parameter.
* @param group specifies the gid parameter.
* @throws IllegalArgumentException while path contains null character.
* @return 0 on success, -1 on error.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func chown(path: String, owner: UInt32, group: UInt32): Int32

/**
* Modify the fd file owner and the group to which the file owner belongs.
* If the operation succeeds, 0 is returned. If an error occurs, -1 is returned.
* Binding POSIX API is [fchown](https://man7.org/linux/man-pages/man2/fchown.2.html).
*
* @param fd a file descriptor.
* @param owner uid parameter.
* @param group specifies the gid parameter.
* @return succeeds 0 is returned. If an error occurs, -1 is returned.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func fchown(fd: Int32, owner: UInt32, group: UInt32): Int32

/**
* Modify the file owner and the group to which the file owner belongs.
* Changes a function that changes the ownership of the source file when the link is changed.
* Binding POSIX API is [lchown](https://man7.org/linux/man-pages/man2/lchown.2.html).
*
* @param path the file path.
* @param owner uid parameter.
* @param group specifies the gid parameter.
* @throws IllegalArgumentException while path contains null character.
* @return on success, zero is returned.  on error, -1 is returned, and errno is set to indicate the error.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func lchown(path: String, owner: UInt32, group: UInt32): Int32

/**
* Modify the fd file owner and the group to which the file owner belongs.
* Binding POSIX API is [fchownat](https://man7.org/linux/man-pages/man2/fchownat.2.html).
*
* @param fd a file descriptor.
* @param path the file path.
* @param owner uid parameter.
* @param group specifies the gid parameter.
* @param flag parameter.
* @throws IllegalArgumentException while path contains null character.
* @return on success, zero is returned.on error, -1 is returned, and errno is set to indicate the error.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func fchownat(fd: Int32, path: String, owner: UInt32, group: UInt32, flag: Int32): Int32

/**
* Modifying File Permissions.
* Binding POSIX API is [chmod](https://man7.org/linux/man-pages/man2/chmod.2.html).
*
* @param path the file path.
* @param mode description of determining.
* @throws IllegalArgumentException while path contains null character.
* @return on success,returns 0. on error, -1.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func chmod(path: String, mode: UInt32): Int32

/**
* Modifying fd File Permissions.
* Binding POSIX API is [fchmod](https://man7.org/linux/man-pages/man2/fchmod.2.html).
*
* @param fd a file descriptor.
* @param mode is description of determining.
* @return 0 on success, -1 on error
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func fchmod(fd: Int32, mode: UInt32): Int32

/**
* Modifying fd File Permissions.
* Binding POSIX API is [fchmodat](https://man7.org/linux/man-pages/man2/fchmodat.2.html).
*
* @param fd a file descriptor.
* @param path the file path.
* @param mode description of determining.
* @param flag parameter
* @throws IllegalArgumentException while path contains null character.
* @return on success returns 0. on error, -1.
*
* @since 0.16.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func fchmodat(fd: Int32, path: String, mode: UInt32, flag: Int32): Int32

/**
* Test whether a file descriptor refers to a terminal.
* Binding POSIX API is [isatty](https://man7.org/linux/man-pages/man3/isatty.3.html).
*
* @param fd a file descriptor.
* @return on success returns true, otherwise false is returned.
*
* @since 0.18.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func isatty(fd: Int32): Bool

/**
* Return name of a terminal.
* Binding POSIX API is [ttyname](https://man7.org/linux/man-pages/man3/ttyname.3.html).
*
* @param fd a file descriptor.
* @return pathname on success. On error, empty String is returned.
*
* @since 0.18.5
*/
@Deprecated
@!APILevel[since: "12", atomicservice : true]
public func ttyname(fd: Int32): String

