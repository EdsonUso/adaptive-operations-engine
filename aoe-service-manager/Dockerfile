# Dockerfile for aoe-service-manager

# --- Build Stage ---
FROM golang:1.24-alpine AS builder

WORKDIR /src

COPY go.mod ./
RUN go mod tidy

COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -o /app/aoe-service-manager .

# --- Final Stage ---
FROM alpine:latest

# Instala o cliente Docker
RUN apk --no-cache add docker-cli

WORKDIR /app

COPY --from=builder /app/aoe-service-manager .

CMD ["./aoe-service-manager"]
