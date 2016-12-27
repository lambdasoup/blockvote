package backend

import "net/http"

func init() {
	http.HandleFunc("/", helloFunc)
}

func helloFunc(w http.ResponseWriter, r *http.Request) {
	w.Write([]byte("hello world"))
}
