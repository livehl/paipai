package main

import "net/http"
import "fmt"

func main() {
	fmt.Println("Server start")                            //最简单的日志提示已经开始工作
	http.Handle("/", http.FileServer(http.Dir("public/"))) // 设置静态文件路由
	http.ListenAndServe(":80", nil)                        // 开始http server
}
