# Adaptive Operations Engine (AOE)

[](https://www.google.com/search?q=https://github.com/seu-usuario/adaptive-operations-engine)

O **Adaptive Operations Engine (AOE)** é um projeto de prova de conceito que demonstra como o planejamento de IA, especificamente **GOAP (Goal-Oriented Action Planning)**, pode ser aplicado para criar sistemas de automação adaptativos e resilientes em ambientes corporativos, como operações de TI (DevOps), atendimento ao cliente e logística.

Este projeto foge dos tradicionais sistemas de workflow estático, propondo uma arquitetura onde o sistema **raciocina e cria planos dinamicamente** para atingir objetivos de negócio, adaptando-se a falhas e condições inesperadas em tempo real.

-----

## O Conceito Central: Planejamento Adaptativo vs. Workflow Estático

Muitas ferramentas de automação (como n8n, Zapier ou scripts de CI/CD) são baseadas em **workflows estáticos**. Elas são excelentes para executar uma sequência de passos pré-definida. Mas o que acontece quando um passo falha de uma forma inesperada? Geralmente, o processo para.

O AOE aborda o problema de uma perspectiva diferente, inspirada em sistemas de IA para jogos e robótica.

| Característica | **Orquestração de Workflow (Ex: n8n)** | **Planejamento Adaptativo (AOE)** |
| :--- | :--- | :--- |
| **Paradigma** | **Reativo:** "QUANDO X acontecer, FAÇA A, depois B, depois C." | **Proativo:** "Meu OBJETIVO é Y. Qual é o melhor plano para chegar lá AGORA?" |
| **Lógica** | O plano é fixo, definido por um humano. | O plano é dinâmico, calculado pelo sistema em tempo de execução. |
| **Resiliência** | Limitada a retentativas (`retries`). Não lida com falhas inesperadas. | **Alta.** Se um plano falha, o sistema analisa o novo estado e **replaneja** um novo caminho para o objetivo. |
| **Analogia** | Um trem seguindo um trilho fixo. | Um carro com Waze que recalcula a rota a cada novo obstáculo. |

O valor do AOE não está em *executar* tarefas, mas sim em **decidir inteligentemente qual a melhor sequência de tarefas a executar** com base no estado atual do ambiente.

## Arquitetura do Sistema

O sistema é composto por microsserviços que se comunicam de forma assíncrona, promovendo desacoplamento e escalabilidade.

```mermaid
graph TD;
    subgraph Ambiente de Demonstração (Docker)
        A(API Request) -->|1. Define Objetivo| Planner[🧠 aoe-planner (Java/Spring Boot)];
        Planner -->|2. Lê Estado| Redis[(💾 Redis - Base de Fatos)];
        Planner -->|3. Gera Plano A*| Planner;
        Planner -->|4. Publica Plano| RabbitMQ[🐇 RabbitMQ];
        RabbitMQ -->|5. Consome Plano| Executor[💪 aoe-executor (Go)];
        Executor -->|6. Executa Ações| TargetApp[🌐 aoe-target-app];
        Executor -->|7. Atualiza Estado| Redis;
        Executor -->|8. Falha? Sinaliza Replanejamento| RabbitMQ;
    end
```

  - **Planner (Java/Spring Boot):** O cérebro. Recebe objetivos via API REST, lê o estado atual do sistema no Redis e usa o algoritmo A\* para gerar o plano de ações de menor custo.
  - **Executor (Go):** As mãos. Consome os planos do RabbitMQ, executa cada ação (simulada ou real, como chamadas HTTP ou comandos CLI) e atualiza a "Base de Fatos" no Redis com os resultados.
  - **Redis:** A memória de curto prazo. Armazena o estado atual do ambiente ("Base de Fatos") de forma rápida e centralizada.
  - **RabbitMQ:** O sistema nervoso. Garante a comunicação assíncrona e resiliente entre o Planner e o Executor.
  - **Target App:** Um servidor web simples que serve como alvo para as ações do Executor, permitindo simular cenários de falha e recuperação.

## 🛠Tecnologias Utilizadas

  - **Backend (Planner):** Java 21, Spring Boot 3
  - **Backend (Executor):** Go
  - **Mensageria:** RabbitMQ
  - **Cache/Estado:** Redis
  - **Containerização:** Docker & Docker Compose

## Como Executar e Testar

Este projeto é totalmente containerizado. Tudo que você precisa é **Git** e **Docker** instalados.

### 1\. Pré-requisitos

  - [Git](https://git-scm.com/)
  - [Docker](https://www.docker.com/products/docker-desktop/) e Docker Compose

### 2\. Instalação

Clone o repositório e navegue até a pasta raiz:

```bash
git clone https://github.com/seu-usuario/adaptive-operations-engine.git
cd adaptive-operations-engine
```

### 3\. Subindo o Ambiente

Com um único comando, o Docker Compose irá construir as imagens e iniciar todos os contêineres (Planner, Executor, RabbitMQ, Redis e o App Alvo):

```bash
docker-compose up --build
```

Aguarde alguns instantes para que todos os serviços iniciem. Você pode acompanhar os logs em abas separadas do seu terminal com `docker logs -f <nome_do_container>`.

### Cenário de Demonstração: Recuperação de Serviço Web

Vamos ver o AOE em ação\!

#### Cenário 1: O Caminho Feliz (Recuperação Simples)

Primeiro, vamos "derrubar" nosso serviço web manualmente para que o AOE possa consertá-lo.

1.  **Pare o container do serviço alvo:**

    ```bash
    docker stop meuapp_container_demo
    ```

2.  **Envie o objetivo para o Planner:**
    Use `curl` ou um cliente de API para enviar o objetivo "quero que o serviço web esteja saudável" para a API do Planner.

    ```bash
    curl -X POST http://localhost:8080/api/goals \
         -H "Content-Type: application/json" \
         -d '{
               "name": "ensure-service-healthy",
               "priority": 1,
               "desiredState": {
                 "service_web_healthy": true
               }
             }'
    ```

3.  **Observe a Mágica:**

      - **No log do `aoe-planner`:** Você verá o recebimento do objetivo, a leitura do estado e a criação de um plano simples, como `Plano gerado com 1 ação: [RestartWebService]`.
      - **No log do `aoe-executor`:** Você verá o recebimento do plano e a execução da ação `RestartWebService`, que irá reiniciar o container `meuapp_container_demo`.
      - **Verificação:** Rode `docker ps` e você verá que o container `meuapp_container_demo` está de pé novamente.

#### Cenário 2: O Replanejamento Adaptativo (Porta Bloqueada)

Agora, vamos criar um problema mais complexo que exige raciocínio.

1.  **Simule o bloqueio da porta:**
    Vamos iniciar outro serviço (um Nginx simples) na mesma porta que nosso app alvo usa no host (porta 9090, conforme `docker-compose.yml`), para simular um conflito.

    ```bash
    docker run --rm -d --name port-blocker -p 9090:80 nginx
    ```

    Agora, mesmo que o AOE tente reiniciar `meuapp_container_demo`, ele não conseguirá subir corretamente devido ao conflito de porta.

2.  **Envie o mesmo objetivo novamente:**

    ```bash
    curl -X POST http://localhost:8080/api/goals \
         -H "Content-Type: application/json" \
         -d '{
               "name": "ensure-service-healthy",
               "priority": 1,
               "desiredState": {
                 "service_web_healthy": true
               }
             }'
    ```

3.  **Observe a Adaptação:**

      - **No log do `aoe-executor`:** A ação `RestartWebService` irá falhar ou não atingirá o efeito desejado. O Executor irá então diagnosticar a causa (porta bloqueada) e atualizar a Base de Fatos no Redis.
      - **No log do `aoe-planner`:** O Planner receberá um sinal de replanejamento. Ele lerá a nova Base de Fatos (`{ "port_9090_in_use": true }`) e criará um **novo plano**, como: `Novo plano gerado: [KillProcessBlockingPort, RestartWebService]`.
      - **No log do `aoe-executor`:** O executor irá agora executar o novo plano: primeiro remover o container `port-blocker` e depois reiniciar `meuapp_container_demo` com sucesso.

4.  **Limpeza:**
    Não se esqueça de parar o container de bloqueio se ele ainda estiver rodando:

    ```bash
    docker stop port-blocker
    ```

## 🗺️ Roadmap e Próximos Passos

  - [x] **Fase 1: MVP - Núcleo do Sistema**
  - [ ] **Fase 2: Aprimoramentos de Inteligência**
      - [ ] Implementar aprendizado adaptativo (ajustar custos de ações com base no sucesso/falha).
      - [ ] Integrar Behavior Trees no Executor para ações mais complexas e robustas.
      - [ ] Adicionar um painel de observabilidade simples.
  - [ ] **Fase 3: Casos de Uso Empresariais**
      - [ ] Detalhar o cenário de Gestão de Operações de TI.
      - [ ] Modelar um cenário de Roteamento de Tickets de Suporte.
  - [ ] **Fase 4: Open Source e Comunidade**
      - [ ] Melhorar a documentação e os comentários do código.
      - [ ] Adicionar testes unitários e de integração.

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](https://www.google.com/search?q=LICENSE) para mais detalhes