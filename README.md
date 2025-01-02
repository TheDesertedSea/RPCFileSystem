# RPC 文件系统
通过动态链接的方式拦截用户在Linux操作系统上的文件操作系统调用，将其替换为远程过程调用，实现对远程服务器上文
件的操作。支持的调用函数包括：open, close, read, write, lseek, stat, unlink, getdirentries。 
主要工作： 
- 设计并使用C语言实现客户端interpose library 
- 使用C语言实现客户端与代理服务器间的RPC的client stub与server stub; 使用TCP进行网络传输 
- 设计并实现代理服务器的check-on-use的缓存策略与版本管理 
- 设计并实现文件存储服务器的文件存储 
- 使用Java RMI实现代理服务器与文件存储服务器间的RPC
