#!/bin/bash

# Cores para o output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== LIMPANDO AMBIENTE ANTERIOR ===${NC}"
docker-compose down > /dev/null 2>&1
docker stop port-blocker > /dev/null 2>&1
docker rm port-blocker > /dev/null 2>&1

echo -e "\n${BLUE}=== INICIANDO AMBIENTE DE DEMONSTRAÇÃO ===${NC}"
docker-compose up --build -d

echo -e "\n${YELLOW}Aguardando serviços iniciarem... (30 segundos)${NC}"
sleep 30

clear
echo -e "${GREEN}=== AMBIENTE PRONTO E ESTÁVEL ===${NC}"
echo "O 'aoe-target-app' está rodando e acessível em http://localhost:8081"
echo "-----------------------------------------------------"
docker ps --format "table {{.Names}}\t{{.Status}}" --filter name=aoe-target-app
echo "-----------------------------------------------------"

echo -e "\n${YELLOW}Pressione [ENTER] para simular a falha (porta bloqueada + app offline)...${NC}"
read

clear
echo -e "${YELLOW}=== SIMULANDO FALHA DUPLA ===${NC}"
echo "1. Bloqueando a porta 9090 com um contêiner 'port-blocker' நான"
docker run --rm -d --name port-blocker -p 9090:80 nginx > /dev/null

echo "2. Derrubando o 'aoe-target-app'..."
docker stop aoe-target-app > /dev/null

echo -e "\n${YELLOW}Aguardando o sistema de monitoramento (Prometheus) detectar as falhas e atualizar a Fact-Base... (20 segundos)${NC}"
sleep 20

clear
echo -e "${GREEN}=== CENÁRIO DE FALHA PRONTO ===${NC}"
echo "O sistema detectou os problemas e atualizou sua base de fatos (Redis)."
echo "O 'aoe-target-app' está offline e a porta 9090 está bloqueada."

echo "\n--- Estado Atual dos Contêineres ---"
docker ps --format "table {{.Names}}\t{{.Status}}" --filter name=aoe-target-app --filter name=port-blocker

echo "\n--- Base de Fatos (Redis) ---"
docker exec redis redis-cli HGETALL fact-base

echo -e "\n${YELLOW}>>> AÇÃO NECESSÁRIA <<< நான"
echo "Envie agora a requisição POST para o Planner via Insomnia, Postman ou curl:"
echo "-----------------------------------------------------"
echo "URL: POST http://localhost:8080/api/goals"
echo "Body:"
echo -e "{
    \"name\": \"ensure-payment-service-healthy\",
    \"priority\": 1,
    \"desiredState\": {
      \"service_web_healthy\": true
    }
}"
echo "-----------------------------------------------------"
echo -e "\nApós enviar a requisição, observe os logs do 'aoe-executor' para ver o plano de 2 passos sendo executado."
