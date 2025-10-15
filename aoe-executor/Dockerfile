# Dockerfile for aoe-executor

# --- Build Stage ---
# Usa uma imagem Go para compilar a aplicação
FROM golang:1.24-alpine AS builder

WORKDIR /src

# Copia os arquivos de gerenciamento de dependências e baixa as dependências
COPY go.mod go.sum ./
RUN go mod download

# Copia o resto do código fonte
COPY . .

# Compila a aplicação. CGO_ENABLED=0 e GOOS=linux garantem um binário estático para Linux.
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/aoe-executor .

# --- Final Stage ---
# Usa uma imagem Alpine mínima para a execução, resultando em uma imagem final menor.
FROM alpine:latest

WORKDIR /app

# Copia o binário compilado do estágio de build
COPY --from=builder /app/aoe-executor .

# Define o comando para executar a aplicação quando o contêiner iniciar
CMD ["./aoe-executor"]
