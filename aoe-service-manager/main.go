package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os/exec"
)

// RestartContainerRequest é a estrutura para o corpo da requisição de restart.
type RestartContainerRequest struct {
	ContainerName string `json:"containerName"`
}

// KillPortRequest é a estrutura para o corpo da requisição de matar processo na porta.
type KillPortRequest struct {
	Port int `json:"port"`
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

	// Para este cenário, o diagnóstico é saber se o port-blocker está rodando.
	cmd := exec.Command("docker", "ps", "-q", "--filter", "name=port-blocker")
	output, err := cmd.CombinedOutput()
	if err != nil {
		log.Printf("Erro ao verificar o contêiner port-blocker: %s", string(output))
		http.Error(w, string(output), http.StatusInternalServerError)
		return
	}

	portInUse := len(output) > 0 // Se o output tem algum conteúdo, o contêiner está rodando

	log.Printf("Diagnóstico da porta 9090: em_uso=%t (verificando por 'port-blocker')", portInUse)

	// Retorna o fato descoberto
	factKey := "port_9090_in_use"
	response := map[string]bool{factKey: portInUse}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

func main() {
	mux := http.NewServeMux()
	mux.HandleFunc("/actions/restart-container", handleRestartContainer)
	mux.HandleFunc("/actions/kill-process-on-port", handleKillProcessOnPort)
	mux.HandleFunc("/diagnostics/check-port", handleCheckPort)

	log.Println("Service Manager iniciado na porta 8081...")
	if err := http.ListenAndServe(":8081", mux); err != nil {
		log.Fatalf("Erro ao iniciar o servidor: %s", err)
	}
}
