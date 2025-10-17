package main

import (
	"log"
	"net/http"

	"github.com/prometheus/client_golang/prometheus/promhttp"
)

func main() {
	// Servir arquivos estáticos (CSS, JS, etc.) do diretório 'static'
	fs := http.FileServer(http.Dir("./static"))
	http.Handle("/static/", http.StripPrefix("/static/", fs))

	// Endpoint principal que serve a página HTML
	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "./static/index.html")
	})

	// Endpoint que o Prometheus usará para coletar métricas
	http.Handle("/metrics", promhttp.Handler())

	log.Println("AOE Target Application started on port 8080")
	log.Println("Access the UI at http://localhost:8081")
	log.Fatal(http.ListenAndServe(":8080", nil))
}
