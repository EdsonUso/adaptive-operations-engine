package model

// ExecutorInfo contém as informações de como o Executor deve invocar uma ação.
type ExecutorInfo struct {
	Type    string            `json:"type"`
	Method  string            `json:"method"`
	URL     string            `json:"url"`
	Headers map[string]string `json:"headers"`
	Body    string            `json:"body"`
}

// Action representa uma única etapa em um plano.
type Action struct {
	Name          string                 `json:"name"`
	Preconditions map[string]interface{} `json:"preconditions"`
	Effects       map[string]interface{} `json:"effects"`
	Cost          int                    `json:"cost"`
	Executor      ExecutorInfo           `json:"executor"`
}

// Goal representa o objetivo que o plano está tentando alcançar.
// Por enquanto, vamos manter simples, mas pode ser expandido.
type Goal struct {
	Name         string                 `json:"name"`
	DesiredState map[string]interface{} `json:"desiredState"`
}

// Plan representa o plano de ações gerado pelo aoe-planner.
type Plan struct {
	TargetGoal Goal     `json:"targetGoal"`
	Steps      []Action `json:"steps"`
	TotalCost  int      `json:"totalCost"`
}
