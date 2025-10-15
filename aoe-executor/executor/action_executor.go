package executor

import (
	"aoe-executor/model"
	"context"
	"encoding/json"
	"fmt"
	"github.com/go-redis/redis/v8"
	"log"
	"net/http"
	"strings"
	"time"
)

// ExecutePlan itera sobre as ações de um plano e as executa em sequência.
func ExecutePlan(plan model.Plan, rdb *redis.Client, replanPublisher func(goal model.Goal)) error {
	log.Printf("🚀 Iniciando execução do plano '%s' com %d passo(s).", plan.TargetGoal.Name, len(plan.Steps))

	for _, action := range plan.Steps {
		// Executa a ação e obtém os fatos resultantes (se for uma ação de diagnóstico)
		discoveredFacts, err := executeAction(action)
		if err != nil {
			log.Printf("❌ Falha ao executar a ação '%s': %v", action.Name, err)

			// Lógica de replanejamento
			log.Println("  -> Atualizando a base de fatos com a falha.")
			rdb.HSet(context.Background(), "fact-base", "service_restart_failed", "true")

			log.Println("  -> Solicitando replanejamento para o objetivo original.")
			replanPublisher(plan.TargetGoal)

			return err // Interrompe a execução do plano atual para aguardar o novo.
		}

		log.Printf("✅ Ação '%s' executada com sucesso.", action.Name)

		// Aplica tanto os efeitos definidos na ação quanto os fatos descobertos pela execução
		applyEffects(action, discoveredFacts, rdb)
	}

	log.Printf("🎉 Plano '%s' concluído com sucesso!", plan.TargetGoal.Name)
	return nil
}

// executeAction executa uma única ação e retorna quaisquer fatos descobertos.
func executeAction(action model.Action) (map[string]interface{}, error) {
	switch action.Executor.Type {
	case "http":
		return executeHTTPAction(action)
	case "cli":
		log.Printf("Ação CLI ainda não implementada: %s", action.Name)
		return nil, nil // Simula sucesso por enquanto
	default:
		return nil, fmt.Errorf("tipo de executor desconhecido: %s", action.Executor.Type)
	}
}

// executeHTTPAction realiza uma chamada HTTP e retorna os fatos descobertos na resposta.
func executeHTTPAction(action model.Action) (map[string]interface{}, error) {
	executorInfo := action.Executor
	log.Printf("  -> Executando HTTP %s para %s", executorInfo.Method, executorInfo.URL)

	req, err := http.NewRequest(executorInfo.Method, executorInfo.URL, strings.NewReader(executorInfo.Body))
	if err != nil {
		return nil, fmt.Errorf("falha ao criar a requisição: %w", err)
	}

	for key, value := range executorInfo.Headers {
		req.Header.Set(key, value)
	}
	if executorInfo.Body != "" {
		req.Header.Set("Content-Type", "application/json")
	}

	client := &http.Client{Timeout: 10 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("falha ao executar a requisição: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return nil, fmt.Errorf("requisição falhou com status: %s", resp.Status)
	}

	// Decodifica o corpo da resposta para extrair fatos descobertos
	var discoveredFacts map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&discoveredFacts); err != nil {
		// Se não houver corpo ou o corpo não for um JSON de fatos, apenas ignore.
		return nil, nil
	}

	log.Printf("  -> Fatos descobertos via HTTP: %v", discoveredFacts)
	return discoveredFacts, nil
}

// applyEffects atualiza a Base de Fatos no Redis.
func applyEffects(action model.Action, discoveredFacts map[string]interface{}, rdb *redis.Client) {
	// Combina efeitos predefinidos com fatos descobertos
	allEffects := make(map[string]interface{})
	for k, v := range action.Effects {
		allEffects[k] = v
	}
	for k, v := range discoveredFacts {
		allEffects[k] = v
	}

	if len(allEffects) == 0 {
		return
	}

	log.Printf("  -> Aplicando efeitos da ação '%s' no Redis...", action.Name)
	for key, value := range allEffects {
		strValue := fmt.Sprintf("%v", value)
		err := rdb.HSet(context.Background(), "fact-base", key, strValue).Err()
		if err != nil {
			log.Printf("  -> ❗ Falha ao atualizar o fato '%s' no Redis: %v", key, err)
		} else {
			log.Printf("  -> Fato atualizado: {%s: %s}", key, strValue)
		}
	}
}
