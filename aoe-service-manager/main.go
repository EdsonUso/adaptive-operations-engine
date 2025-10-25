package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os/exec"
	"strconv"
	"strings"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

var (
	portInUse = prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "aoe_port_in_use",
		Help: "Indica se a porta 9090 está em uso por um processo bloqueador (1 para em uso, 0 para livre).",
	})
	cpuLoadHigh = prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "aoe_cpu_load_high",
		Help: "Indica se a carga de CPU do container alvo está alta (1 para alta, 0 para normal).",
	})
)

const CPU_THRESHOLD = 50.0

func init() {
	prometheus.MustRegister(portInUse)
	prometheus.MustRegister(cpuLoadHigh)
}

// Structs para as requisições JSON
type RestartContainerRequest struct {
	ContainerName string `json:"containerName"`
}

// recordMetrics verifica periodicamente o estado do sistema e atualiza as métricas do Prometheus.
func recordMetrics() {
	for {
		// Lógica para verificar se a porta está em uso
		cmdPortBlocker := exec.Command("docker", "ps", "-q", "--filter", "name=port-blocker")
		outputPortBlocker, _ := cmdPortBlocker.CombinedOutput()
		if len(outputPortBlocker) > 0 {
			portInUse.Set(1)
		} else {
			portInUse.Set(0)
		}

		// Lógica para verificar a CPU do aoe-target-app
		cmdStats := exec.Command("docker", "stats", "--no-stream", "--format", "{{.CPUPerc}}", "aoe-target-app")
		outputStats, err := cmdStats.CombinedOutput()
		if err != nil {
			// O container pode não estar rodando, o que é um estado válido
			cpuLoadHigh.Set(0)
		} else {
			cpuStr := strings.TrimSuffix(string(outputStats), "\n")
			cpuStr = strings.TrimSuffix(cpuStr, "%")
			cpuVal, err := strconv.ParseFloat(cpuStr, 64)
			if err == nil && cpuVal > CPU_THRESHOLD {
				cpuLoadHigh.Set(1)
			} else {
				cpuLoadHigh.Set(0)
			}
		}

		time.Sleep(5 * time.Second)
	}
}

func handleRestartContainer(w http.ResponseWriter, r *http.Request) {
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
}

func handleKillProcessOnPort(w http.ResponseWriter, r *http.Request) {
	const containerToKill = "port-blocker"
	log.Printf("Recebida solicitação para liberar a porta, parando o contêiner %s", containerToKill)
	cmd := exec.Command("docker", "stop", containerToKill)
	cmd.CombinedOutput() // Ignora o erro se o contêiner não existir
	log.Printf("Contêiner %s parado com sucesso.", containerToKill)
	w.WriteHeader(http.StatusOK)
}

// Struct para a requisição de remoção de contêineres maliciosos
type RemoveMaliciousRequest struct {
	Pattern string `json:"pattern"`
}

func handleRemoveMaliciousContainers(w http.ResponseWriter, r *http.Request) {
	var req RemoveMaliciousRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Corpo da requisição inválido", http.StatusBadRequest)
		return
	}
	log.Printf("Recebida solicitação para remover contêineres com padrão: %s", req.Pattern)

	// Listar containers com o padrão no nome
	cmdList := exec.Command("docker", "ps", "-q", "--filter", "name="+req.Pattern)
	output, err := cmdList.CombinedOutput()
	if err != nil {
		log.Printf("Erro ao listar contêineres com padrão %s: %s", req.Pattern, string(output))
		http.Error(w, string(output), http.StatusInternalServerError)
		return
	}

	containerIDs := strings.Split(strings.TrimSpace(string(output)), "\n")
	if len(containerIDs) == 1 && containerIDs[0] == "" {
		log.Printf("Nenhum contêiner encontrado com o padrão: %s", req.Pattern)
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("Nenhum contêiner malicioso encontrado."))
		return
	}

	log.Printf("Contêineres maliciosos encontrados: %v", containerIDs)
	for _, id := range containerIDs {
		if id == "" {
			continue
		}
		log.Printf("Removendo contêiner: %s", id)
		cmdRemove := exec.Command("docker", "rm", "-f", id)
		removeOutput, err := cmdRemove.CombinedOutput()
		if err != nil {
			log.Printf("Erro ao remover o contêiner %s: %s", id, string(removeOutput))
			// Não retorna erro aqui para tentar remover os outros
		} else {
			log.Printf("Contêiner %s removido com sucesso.", id)
		}
	}

	w.WriteHeader(http.StatusOK)
}

func handleDiagnostics(w http.ResponseWriter, r *http.Request) {
	log.Println("Recebida solicitação de diagnóstico. Retornando estado vazio.")
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte("{}")) // Return an empty JSON object
}

func main() {
	go recordMetrics()

	mux := http.NewServeMux()
	mux.HandleFunc("/actions/restart-container", handleRestartContainer)
	mux.HandleFunc("/actions/kill-process-on-port", handleKillProcessOnPort)
	mux.HandleFunc("/actions/remove-malicious-containers", handleRemoveMaliciousContainers)
	mux.HandleFunc("/diagnostics/check-port", handleDiagnostics)
	mux.Handle("/metrics", promhttp.Handler())

	log.Println("Service Manager iniciado na porta 8081...")
	if err := http.ListenAndServe(":8081", mux); err != nil {
		log.Fatalf("Erro ao iniciar o servidor: %s", err)
	}
}
