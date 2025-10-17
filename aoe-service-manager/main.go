package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os/exec"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

var (
	portInUse = prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "aoe_port_in_use",
		Help: "Indica se a porta 9090 está em uso por um processo bloqueador (1 para em uso, 0 para livre).",
	})
)

func init() {
	prometheus.MustRegister(portInUse)
}

// RestartContainerRequest é a estrutura para o corpo da requisição de restart.
type RestartContainerRequest struct {
	ContainerName string `json:"containerName"`
}

// KillPortRequest é a estrutura para o corpo da requisição de matar processo na porta.
type KillPortRequest struct {
	Port int `json:"port"`
}

// recordMetrics verifica periodicamente o estado do sistema e atualiza as métricas do Prometheus.
func recordMetrics() {
	for {
		// Lógica para verificar se a porta está em uso
		cmdPortBlocker := exec.Command("docker", "ps", "-q", "--filter", "name=port-blocker")
		outputPortBlocker, err := cmdPortBlocker.CombinedOutput()
		if err != nil {
			log.Printf("Erro ao verificar o contêiner port-blocker para métricas: %s", string(outputPortBlocker))
		} else {
			if len(outputPortBlocker) > 0 {
				portInUse.Set(1)
			} else {
				portInUse.Set(0)
			}
		}

		time.Sleep(5 * time.Second)
	}
}

// handleRestartContainer lida com a lógica de reiniciar um contêiner Docker.
func handleRestartContainer(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Método não permitido", http.StatusMethodNotAllowed)
		return
	}

	var req RestartContainerRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Corpo da requisição inválido", http.StatusBadRequest)
		return
	}

	log.Printf("Recebida solicitação para reiniciar o contêiner: %s", req.ContainerName)
	cmd := exec.Command("docker", "restart", req.ContainerName)
	output, err := cmd.CombinedOutput()
	if err != nil {
		log.Printf("Erro ao reiniciar o contêiner %s: %s", req.ContainerName, string(output))
		http.Error(w, string(output), http.StatusInternalServerError)
		return
	}

	log.Printf("Contêiner %s reiniciado com sucesso.", req.ContainerName)
	w.WriteHeader(http.StatusOK)
	w.Write([]byte("Contêiner reiniciado com sucesso."))
}

// handleKillProcessOnPort lida com a lógica de parar o contêiner que bloqueia a porta.
// Para a PoC, ele para um contêiner com nome fixo "port-blocker".
func handleKillProcessOnPort(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Método não permitido", http.StatusMethodNotAllowed)
		return
	}

	var req KillPortRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Corpo da requisição inválido", http.StatusBadRequest)
		return
	}

	const containerToKill = "port-blocker"
	log.Printf("Recebida solicitação para liberar a porta %d, parando o contêiner %s", req.Port, containerToKill)

	cmd := exec.Command("docker", "stop", containerToKill)
	output, err := cmd.CombinedOutput()
	if err != nil {
		log.Printf("Erro ao parar o contêiner %s: %s", containerToKill, string(output))
		// Ignora o erro se o contêiner não existir, pois o objetivo (liberar a porta) foi alcançado.
	}

	log.Printf("Contêiner %s parado com sucesso.", containerToKill)
	w.WriteHeader(http.StatusOK)
	w.Write([]byte("Processo bloqueando a porta foi finalizado."))
}

func handleCheckPort(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Método não permitido", http.StatusMethodNotAllowed)
		return
	}

	// --- Verificação do Port Blocker ---
	cmdPortBlocker := exec.Command("docker", "ps", "-q", "--filter", "name=port-blocker")
	outputPortBlocker, err := cmdPortBlocker.CombinedOutput()
	if err != nil {
		log.Printf("Erro ao verificar o contêiner port-blocker: %s", string(outputPortBlocker))
		http.Error(w, string(outputPortBlocker), http.StatusInternalServerError)
		return
	}
	portInUse := len(outputPortBlocker) > 0

	// --- Verificação do App Alvo ---
	cmdTargetApp := exec.Command("docker", "ps", "-q", "--filter", "name=aoe-target-app")
	outputTargetApp, err := cmdTargetApp.CombinedOutput()
	if err != nil {
		log.Printf("Erro ao verificar o contêiner aoe-target-app: %s", string(outputTargetApp))
		http.Error(w, string(outputTargetApp), http.StatusInternalServerError)
		return
	}
	serviceHealthy := len(outputTargetApp) > 0

	log.Printf("Diagnóstico completo: port_9090_in_use=%t, service_web_healthy=%t", portInUse, serviceHealthy)

	// Retorna ambos os fatos descobertos
	response := map[string]bool{
		"port_9090_in_use":    portInUse,
		"service_web_healthy": serviceHealthy,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

func main() {
	go recordMetrics()

	mux := http.NewServeMux()
	mux.HandleFunc("/actions/restart-container", handleRestartContainer)
	mux.HandleFunc("/actions/kill-process-on-port", handleKillProcessOnPort)
	mux.HandleFunc("/diagnostics/check-port", handleCheckPort)
	mux.Handle("/metrics", promhttp.Handler())

	log.Println("Service Manager iniciado na porta 8081...")
	if err := http.ListenAndServe(":8081", mux); err != nil {
		log.Fatalf("Erro ao iniciar o servidor: %s", err)
	}
}
