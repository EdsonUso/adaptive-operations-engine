@echo off

echo === LIMPANDO AMBIENTE ANTERIOR (Cenário 3) ===
docker-compose down > NUL 2>&1

echo.
echo === INICIANDO AMBIENTE DE DEMONSTRAÇÃO ===
docker-compose up --build -d

echo.
echo Aguardando serviços iniciarem... (30 segundos)
timeout /t 30 > NUL

cls
echo === AMBIENTE PRONTO E ESTÁVEL ===
echo O 'aoe-target-app' está rodando normalmente em http://localhost:8081
echo -----------------------------------------------------
docker ps --format "table {{.Names}}\t{{.Status}}" --filter name=aoe-target-app
echo -----------------------------------------------------

echo.
echo Pressione qualquer tecla para simular a falha (Alta Carga de CPU interna)...
pause > NUL

cls
echo === SIMULANDO FALHA: ALTA CARGA DE CPU ===
echo 1. Acionando o endpoint /debug/burn-cpu no 'aoe-target-app' para simular um bug...
curl -s -o NUL http://localhost:8081/debug/burn-cpu

echo.
echo Aguardando o sistema de monitoramento (Prometheus) detectar a alta carga de CPU e a subsequente falha do 'aoe-target-app'... (75 segundos)
timeout /t 75 > NUL

cls
echo === CENÁRIO DE FALHA PRONTO ===
echo O sistema detectou os problemas e atualizou sua base de fatos (Redis).
echo O 'aoe-target-app' deve estar offline e a CPU sobrecarregada.

echo.
echo --- Estado Atual dos Contêineres ---
docker ps --format "table {{.Names}}\t{{.Status}}" --filter name=aoe-target-app

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
