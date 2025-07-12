复现步骤

1. 启动close-wait-server
2. 通过telnet或者curl进行测试，配合tcp抓包工具。确保测试的步骤中，都是通过正常发送fin实现。避免如rst等命令

