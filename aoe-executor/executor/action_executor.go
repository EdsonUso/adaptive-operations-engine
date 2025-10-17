package executor

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strings"
	"time"

	"aoe-executor/model"

	"github.com/go-redis/redis/v8"
)

// ExecutePlan itera sobre as ações de um plano e as executa em sequência.
func ExecutePlan(plan model.Plan, rdb *redis.Client) error {
	log.Printf("🚀 Iniciando execução do plano '%s' com %d passo(s).", plan.TargetGoal.Name, len(plan.Steps))

	for _, action := range plan.Steps {
		discoveredFacts, err := executeAction(action)
		if err != nil {
			log.Printf("❌ Falha ao executar a ação '%s': %v", action.Name, err)
			// A lógica de replanejamento agora é tratada no loop principal.
			return err
		}

		log.Printf("✅ Ação '%s' executada com sucesso.", action.Name)
		applyEffects(action, discoveredFacts, rdb)
	}

	return nil
}

// CheckGoal verifica se o estado desejado do objetivo foi alcançado na base de fatos atual.
func CheckGoal(goal model.Goal, rdb *redis.Client) (bool, error) {
	currentState, err := rdb.HGetAll(context.Background(), "fact-base").Result()
	if err != nil {
		return false, fmt.Errorf("falha ao ler a base de fatos do Redis: %w", err)
	}

	for key, desiredValue := range goal.DesiredState {
		currentValue, ok := currentState[key]
		if !ok {
			// Se a chave do objetivo nem existe nos fatos, o objetivo não foi alcançado.
			return false, nil
		}

		// Compara os valores como strings para simplificar.
		if fmt.Sprintf("%v", desiredValue) != currentValue {
			return false, nil
		}
	}

	// Se todas as chaves do estado desejado correspondem, o objetivo foi alcançado.
	return true, nil
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

	var discoveredFacts map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&discoveredFacts); err != nil {
		return nil, nil
	}

	log.Printf("  -> Fatos descobertos via HTTP: %v", discoveredFacts)
	return discoveredFacts, nil
}

// applyEffects atualiza a Base de Fatos no Redis.
func applyEffects(action model.Action, discoveredFacts map[string]interface{}, rdb *redis.Client) {
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