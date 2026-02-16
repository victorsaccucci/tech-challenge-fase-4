# Sistema de Feedback Serverless - Tech Challenge Fase 4

Sistema de gerenciamento de feedbacks de cursos online com arquitetura serverless na AWS.

## Visão Geral

Sistema desenvolvido para receber feedbacks de estudantes, enviar notificações automáticas para feedbacks críticos e gerar relatórios semanais de satisfação.

## Arquitetura

### Componentes AWS

- **API Gateway + Lambda**: Endpoint REST para receber feedbacks
- **RDS PostgreSQL**: Banco de dados relacional para persistência
- **SQS**: Fila para processamento assíncrono de feedbacks críticos
- **Lambda (Notificação)**: Processa feedbacks críticos e envia alertas via SNS
- **Lambda (Relatório)**: Gera relatórios semanais automaticamente
- **SNS**: Serviço de notificação por email
- **EventBridge**: Agendamento semanal de relatórios
- **CloudWatch**: Monitoramento e logs

### Fluxo de Dados

```
Cliente → API Gateway → Lambda/Spring Boot → RDS PostgreSQL
                                    ↓
                              SQS (se crítico)
                                    ↓
                         Lambda Notificação → SNS → Email Admin
                         
EventBridge (semanal) → Lambda Relatório → RDS → SNS → Email Admin
```

## Segurança

- **VPC Isolada**: Recursos em rede privada
- **Security Groups**: Controle de acesso por porta/protocolo
- **IAM Roles**: Princípio do menor privilégio
- **Secrets Manager**: Credenciais criptografadas
- **Encryption at Rest**: RDS com criptografia
- **HTTPS**: Comunicação segura via API Gateway

## Pré-requisitos

- Java 21
- Maven 3.8+
- Terraform 1.0+
- AWS CLI configurado
- Conta AWS com permissões adequadas

## Deploy

### 1. Build das Funções Lambda

```bash
# Notificação Crítica
cd serverless-functions/notification-lambda
mvn clean package
cd ../..

# Relatório Semanal
cd serverless-functions/weekly-report-lambda
mvn clean package
cd ../..
```

### 2. Configurar Terraform

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# Edite terraform.tfvars com suas configurações
```

### 3. Deploy da Infraestrutura

```bash
terraform init
terraform plan
terraform apply
```

### 4. Deploy da Aplicação Spring Boot

```bash
mvn clean package
# Deploy no AWS Elastic Beanstalk ou ECS
```

## Monitoramento

### CloudWatch Dashboards

- **Lambda Metrics**: Invocações, erros, duração
- **RDS Metrics**: CPU, memória, conexões
- **SQS Metrics**: Mensagens na fila, processamento

### Alarmes Configurados

- Erros em funções Lambda (> 5 em 5 minutos)
- Alta utilização de CPU no RDS (> 80%)
- Mensagens antigas na fila SQS (> 1 hora)

### Logs

```bash
# Logs da Lambda de Notificação
aws logs tail /aws/lambda/critical-notification-handler --follow

# Logs da Lambda de Relatório
aws logs tail /aws/lambda/weekly-report-handler --follow
```

## Configuração

### Variáveis de Ambiente

**Lambda Notificação:**
- `SNS_TOPIC_ARN`: ARN do tópico SNS

**Lambda Relatório:**
- `SNS_TOPIC_ARN`: ARN do tópico SNS
- `DB_URL`: URL de conexão do PostgreSQL
- `DB_USER`: Usuário do banco
- `DB_PASSWORD`: Senha do banco

## API Endpoints

### POST /avaliacao

Cria uma nova avaliação.

**Request:**
```json
{
  "description": "Excelente curso!",
  "score": 9
}
```

**Response (201):**
```json
{
  "id": 1,
  "description": "Excelente curso!",
  "score": 9,
  "urgencyLevel": "LOW",
  "createdAt": "2024-01-15T10:30:00"
}
```

**Validações:**
- `score`: deve estar entre 0 e 10
- `description`: obrigatório, máximo 500 caracteres

## Níveis de Urgência

- **HIGH** (0-3): Feedback crítico, notificação imediata
- **MEDIUM** (4-6): Feedback moderado
- **LOW** (7-10): Feedback positivo

## Notificações

### Email de Urgência (Automático)

Enviado imediatamente quando score ≤ 3:

```
ALERTA DE FEEDBACK CRÍTICO

Descrição: [descrição do feedback]
Urgência: HIGH
Data de envio: [timestamp]

Ação imediata necessária!
```

### Relatório Semanal (Segunda-feira 9h)

```
RELATÓRIO SEMANAL DE FEEDBACKS
Gerado em: [timestamp]

RESUMO GERAL
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Total de avaliações: 150
Média de avaliações: 7.5

AVALIAÇÕES POR URGÊNCIA
================================
Alta: 10
Média: 40
Baixa: 100

AVALIAÇÕES POR DIA
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[breakdown diário]
```

## Testes

```bash
# Testes unitários
mvn test

# Teste local da API
curl -X POST http://localhost:8080/avaliacao \
  -H "Content-Type: application/json" \
  -d '{"description":"Teste","score":5}'
```

## Estrutura do Projeto

```
tech-challenge-fase-4/
├── src/main/java/com/fiap/FeedbackServerlessApp/
│   ├── controllers/          # REST Controllers
│   ├── services/             # Lógica de negócio
│   ├── repositories/         # Acesso a dados
│   ├── entities/             # Entidades JPA
│   └── dtos/                 # Data Transfer Objects
├── serverless-functions/
│   ├── notification-lambda/  # Lambda de notificação
│   └── weekly-report-lambda/ # Lambda de relatório
├── terraform/                # Infraestrutura como código
├── docs/                     # Documentação adicional
└── Feedback_API.postman_collection.json
```

## CI/CD

Pipeline sugerido:

1. **Build**: Maven package
2. **Test**: Testes unitários e integração
3. **Security Scan**: SAST/DAST
4. **Deploy Lambda**: Upload para S3 + Update function
5. **Deploy App**: Elastic Beanstalk/ECS
6. **Smoke Tests**: Validação pós-deploy

## Estimativa de Custos (AWS)

- **RDS t3.micro**: ~$15/mês
- **Lambda**: ~$0.20/mês (free tier)
- **SQS**: ~$0.40/mês
- **SNS**: ~$0.50/mês
- **CloudWatch**: ~$5/mês

**Total estimado**: ~$21/mês

## Contribuindo

1. Fork o projeto
2. Crie uma branch (`git checkout -b feature/nova-funcionalidade`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/nova-funcionalidade`)
5. Abra um Pull Request

## Licença

Este projeto é parte do Tech Challenge da FIAP - Fase 4.

## Autores

- Equipe FIAP Tech Challenge

## Suporte

Para dúvidas ou problemas, abra uma issue no repositório.
