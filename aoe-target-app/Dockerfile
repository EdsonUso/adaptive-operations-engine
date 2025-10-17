# Dockerfile for aoe-target-app

# Build Stage
FROM golang:1.24-alpine AS builder
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/aoe-target-app .

# Final Stage
FROM alpine:latest
WORKDIR /app
# Copia os arquivos estáticos (HTML/CSS) do estágio de build
COPY --from=builder /src/static ./static
COPY --from=builder /app/aoe-target-app .
EXPOSE 8080
CMD ["./aoe-target-app"]
