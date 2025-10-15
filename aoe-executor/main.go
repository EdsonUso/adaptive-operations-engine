package main

import (
	"context"
	"encoding/json"
	"log"
	"os"

	"aoe-executor/executor"
	"aoe-executor/model"

	"github.com/go-redis/redis/v8"
	"github.com/rabbitmq/amqp091-go"
)

func failOnError(err error, msg string) {
	if err != nil {
		log.Panicf("%s: %s", msg, err)
	}
}

func main() {
	// --- 1. Conectar ao RabbitMQ ---
	// Pega a URL do RabbitMQ da variável de ambiente, com um fallback para localhost.
	rabbitURL := os.Getenv("RABBITMQ_URL")
	if rabbitURL == "" {
		rabbitURL = "amqp://guest:guest@localhost:5672/"
	}
	conn, err := amqp091.Dial(rabbitURL)
	failOnError(err, "Falha ao conectar com o RabbitMQ")
	defer conn.Close()

	ch, err := conn.Channel()
	failOnError(err, "Falha ao abrir um canal de comunicação")
	defer ch.Close()

	// --- 3. Configurar o Consumidor de Planos ---
	// Declara a exchange que o planner usa para publicar os planos.
	err = ch.ExchangeDeclare(
		"aoe.plans.exchange", // name
		"topic",              // type
		true,                 // durable
		false,                // auto-deleted
		false,                // internal
		false,                // no-wait
		nil,                  // arguments
	)
	failOnError(err, "Falha ao declarar a exchange de planos")

	// Declara a fila de onde os planos serão consumidos.
	q, err := ch.QueueDeclare(
		"plans_queue", // name
		true,          // durable
		false,         // delete when unused
		false,         // exclusive
		false,         // no-wait
		nil,           // arguments
	)
	failOnError(err, "Falha ao declarar a fila de planos")

	// Vincula a fila à exchange de planos.
	log.Printf("Vinculando a fila %s à exchange %s com a routing key %s", q.Name, "aoe.plans.exchange", "plan.new")
	err = ch.QueueBind(q.Name, "plan.new", "aoe.plans.exchange", false, nil)
	failOnError(err, "Falha ao vincular a fila de planos")

	// --- 4. Configurar o Publicador de Replanejamento ---
	replanExchange := "aoe.replan.exchange"
	replanRoutingKey := "replan.request"
	err = ch.ExchangeDeclare(replanExchange, "topic", true, false, false, false, nil)
	failOnError(err, "Falha ao declarar a exchange de replanejamento")

	// Cria uma função para publicar solicitações de replanejamento.
	replanPublisher := func(goal model.Goal) {
		body, err := json.Marshal(goal)
		if err != nil {
			log.Printf("Erro ao serializar o objetivo para replanejamento: %v", err)
			return
		}
		err = ch.PublishWithContext(context.Background(), replanExchange, replanRoutingKey, false, false, amqp091.Publishing{ContentType: "application/json", Body: body})
		if err != nil {
			log.Printf("Erro ao publicar solicitação de replanejamento: %v", err)
		}
	}

	// --- 2. Conectar ao Redis ---
	// Pega o endereço do Redis da variável de ambiente, com um fallback para localhost.
	redisAddr := os.Getenv("REDIS_ADDR")
	if redisAddr == "" {
		redisAddr = "localhost:6379"
	}
	rdb := redis.NewClient(&redis.Options{
		Addr:     redisAddr,
		Password: "", // sem senha
		DB:       0,  // usa o banco de dados padrão
	})

	// Verifica a conexão com o Redis
	_, err = rdb.Ping(context.Background()).Result()
	failOnError(err, "Falha ao conectar com o Redis")

	log.Println("✅ Conectado ao RabbitMQ e Redis. Aguardando planos...")

	// --- 5. Iniciar o Consumo de Mensagens ---
	msgs, err := ch.Consume(q.Name, "", false, false, false, false, nil)
	failOnError(err, "Falha ao registrar um consumidor")

	var forever chan struct{}

	go func() {
		for d := range msgs {
			var plan model.Plan
			err := json.Unmarshal(d.Body, &plan)
			if err != nil {
				log.Printf("❌ Erro ao decodificar o plano JSON: %s", err)
				d.Reject(false)
				continue
			}

			log.Printf("📥 Plano '%s' recebido com %d passo(s). Enviando para execução...", plan.TargetGoal.Name, len(plan.Steps))

			// Envia o plano para o executor, passando o cliente Redis e o publicador de replanejamento.
			if err := executor.ExecutePlan(plan, rdb, replanPublisher); err != nil {
				log.Printf("Execução do plano '%s' interrompida por falha.", plan.TargetGoal.Name)
			} else {
				log.Printf("Execução do plano '%s' concluída.", plan.TargetGoal.Name)
			}

			// Confirma o recebimento e processamento da mensagem.
			d.Ack(false)
		}
	}()

	log.Printf(" [*] Aguardando por mensagens. Pressione CTRL+C para sair.")
	<-forever
}
