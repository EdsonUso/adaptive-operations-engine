@echo off

echo === LIMPANDO AMBIENTE ANTERIOR ===
docker-compose down > NUL 2>&1
docker stop port-blocker > NUL 2>&1
docker rm port-blocker > NUL 2>&1

echo.
echo === INICIANDO AMBIENTE DE DEMONSTRAÇÃO ===
docker-compose up --build -d

echo.
echo Aguardando serviços iniciarem... (30 segundos)
timeout /t 30 > NUL

cls
echo === AMBIENTE PRONTO E ESTÁVEL ===
echo O 'aoe-target-app' está rodando e acessível em http://localhost:8081
echo -----------------------------------------------------
docker ps --format "table {{.Names}}\t{{.Status}}" --filter name=aoe-target-app
echo -----------------------------------------------------

echo.
echo Pressione qualquer tecla para simular a falha (porta bloqueada + app offline)...
pause > NUL

cls
echo === SIMULANDO FALHA DUPLA ===
echo 1. Bloqueando a porta 9090 com um contêiner 'port-blocker'...
docker run --rm -d --name port-blocker -p 9090:80 nginx > NUL

echo 2. Derrubando o 'aoe-target-app'...
docker stop aoe-target-app > NUL

echo.
echo Aguardando o sistema de monitoramento (Prometheus) detectar as falhas e atualizar a Fact-Base... (20 segundos)
timeout /t 20 > NUL

cls
echo === CENÁRIO DE FALHA PRONTO ===
echo O sistema detectou os problemas e atualizou sua base de fatos (Redis).
echo O 'aoe-target-app' está offline e a porta 9090 está bloqueada.

echo.
echo --- Estado Atual dos Contêineres ---
docker ps --format "table {{.Names}}\t{{.Status}}" --filter name=aoe-target-app --filter name=port-blocker

echo.
echo --- Base de Fatos (Redis) ---
docker exec redis redis-cli HGETALL fact-base

echo.
echo ^>^>^> AÇÃO NECESSÁRIA ^<^<^<
echo Envie agora a requisição POST para o Planner via Insomnia, Postman ou curl.
echo A visualização de logs do executor iniciará em 5 segundos...
echo.
timeout /t 5 > NUL

cls
echo ================================================================================
echo    LOGS AO VIVO DO 'aoe-executor' --- ENVIE A REQUISIÇÃO AGORA
echo ================================================================================
echo.

docker-compose logs -f --tail="10" aoe-executor
