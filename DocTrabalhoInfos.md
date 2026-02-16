# DOCUMENTAÇÃO TÉCNICA - AVALIAÇÃO DO PROJETO
## Sistema de Feedback Serverless - Tech Challenge Fase 4

---

## 1. MODELO DE CLOUD E COMPONENTES DA SOLUÇÃO

### 1.1 Modelo de Cloud Escolhido: AWS (Amazon Web Services)

**Justificativa da Escolha:**
- **Maturidade**: AWS é líder de mercado com maior portfólio de serviços serverless
- **Escalabilidade**: Suporta crescimento automático sem intervenção manual
- **Custo-benefício**: Modelo pay-as-you-go, pagando apenas pelo uso real
- **Free Tier**: Permite desenvolvimento e testes sem custos iniciais
- **Integração nativa**: Serviços se comunicam nativamente sem configurações complexas

### 1.2 Componentes AWS Utilizados

#### **Computação Serverless**
- **AWS Lambda (2 funções)**
  - `critical-notification-handler`: Processa feedbacks críticos
  - `weekly-report-handler`: Gera relatórios semanais
  - **Runtime**: Java 21
  - **Memória**: 512MB (notificação) / 1024MB (relatório)
  - **Timeout**: 60s (notificação) / 300s (relatório)

#### **Banco de Dados**
- **Amazon RDS PostgreSQL 15**
  - **Instância**: db.t3.micro (Free Tier)
  - **Storage**: 20GB com criptografia at-rest
  - **Backup**: Automático com retenção de 7 dias
  - **Multi-AZ**: Desabilitado (ambiente de desenvolvimento)

#### **Mensageria e Notificações**
- **Amazon SQS (Simple Queue Service)**
  - Fila: `critical-feedbacks-queue`
  - Desacopla API de processamento assíncrono
  - Garante entrega de mensagens críticas
  
- **Amazon SNS (Simple Notification Service)**
  - Tópico: `feedback-critical-notifications`
  - Envia emails para administradores
  - Suporta múltiplos subscribers

#### **Agendamento**
- **Amazon EventBridge**
  - Regra: `weekly-report-schedule`
  - Expressão cron: `cron(0 9 ? * MON *)` (Segunda-feira 9h)
  - Dispara Lambda de relatório automaticamente

#### **Rede e Segurança**
- **Amazon VPC**
  - CIDR: 10.0.0.0/16
  - 2 Subnets públicas (us-east-2a e us-east-2b)
  - Internet Gateway para acesso externo
  
- **Security Groups**
  - RDS: Porta 5432 (PostgreSQL)
  - EC2: Portas 22 (SSH) e 8080 (HTTP)

#### **Monitoramento**
- **Amazon CloudWatch**
  - Logs: Todas as execuções Lambda
  - Métricas: CPU, memória, invocações, erros
  - Alarmes: Erros Lambda > 5 em 5 minutos

#### **Infraestrutura como Código**
- **Terraform**
  - Provisionamento automatizado
  - Versionamento de infraestrutura
  - Reprodutibilidade garantida

---

## 2. ARQUITETURA DA SOLUÇÃO

### 2.1 Diagrama de Arquitetura

```
┌─────────────┐
│   Cliente   │
└──────┬──────┘
       │ HTTP POST
       ▼
┌─────────────────┐
│  API Gateway    │
│  (Spring Boot)  │
│     EC2         │
└────┬────────┬───┘
     │        │
     │        └──────────────┐
     ▼                       ▼
┌─────────────┐      ┌──────────────┐
│ RDS         │      │ SQS Queue    │
│ PostgreSQL  │      │ (se crítico) │
└─────────────┘      └──────┬───────┘
                            │
                            ▼
                     ┌──────────────────┐
                     │ Lambda           │
                     │ Notificação      │
                     └──────┬───────────┘
                            │
                            ▼
                     ┌──────────────────┐
                     │ SNS Topic        │
                     │ Email Admin      │
                     └──────────────────┘

┌──────────────────┐
│ EventBridge      │
│ (Semanal)        │
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ Lambda           │
│ Relatório        │
└──────┬───────────┘
       │
       ├──────► RDS (consulta)
       │
       └──────► SNS (envia email)
```

### 2.2 Fluxo de Dados

#### **Fluxo 1: Feedback Normal (Score 4-10)**
1. Cliente envia POST /avaliacao
2. API valida e salva no RDS
3. Retorna 201 Created

#### **Fluxo 2: Feedback Crítico (Score 0-3)**
1. Cliente envia POST /avaliacao
2. API valida e salva no RDS
3. API envia mensagem para SQS
4. Lambda é disparada automaticamente
5. Lambda lê mensagem do SQS
6. Lambda publica no SNS
7. SNS envia email para admin
8. Mensagem é removida do SQS

#### **Fluxo 3: Relatório Semanal**
1. EventBridge dispara Lambda (segunda 9h)
2. Lambda conecta no RDS
3. Lambda executa queries agregadas
4. Lambda formata relatório
5. Lambda publica no SNS
6. SNS envia email para admin

---

## 3. FUNÇÕES SERVERLESS - RESPONSABILIDADE ÚNICA

### 3.1 Lambda: CriticalNotificationHandler

**Responsabilidade Única**: Processar feedbacks críticos e notificar administradores

**Características:**
- **Trigger**: SQS Queue (event-driven)
- **Input**: Mensagem JSON com feedback crítico
- **Output**: Notificação via SNS
- **Padrão**: Event-Driven Architecture

**Código:**
```java
public class CriticalNotificationHandler implements RequestHandler<SQSEvent, String> {
    // Processa APENAS notificações críticas
    // Não faz validação, não salva no banco
    // Responsabilidade: Notificar
}
```

**Por que essa separação?**
- Desacopla API de notificações
- Permite retry automático em caso de falha
- Escala independentemente da API
- Não bloqueia resposta ao usuário

### 3.2 Lambda: WeeklyReportHandler

**Responsabilidade Única**: Gerar e enviar relatório semanal de feedbacks

**Características:**
- **Trigger**: EventBridge Schedule (time-based)
- **Input**: Evento de agendamento
- **Output**: Relatório via SNS
- **Padrão**: Scheduled Job

**Código:**
```java
public class WeeklyReportHandler implements RequestHandler<ScheduledEvent, String> {
    // Gera APENAS relatórios semanais
    // Não processa feedbacks individuais
    // Responsabilidade: Relatório agregado
}
```

**Por que essa separação?**
- Execução periódica sem intervenção manual
- Não depende de eventos externos
- Processamento batch otimizado
- Isolamento de lógica de relatório

---

## 4. CONFIGURAÇÕES DE SEGURANÇA

### 4.1 Princípio do Menor Privilégio (IAM)

**Lambda Role:**
```json
{
  "Permissions": [
    "sqs:ReceiveMessage",      // Apenas ler SQS
    "sqs:DeleteMessage",        // Apenas deletar após processar
    "sns:Publish",              // Apenas publicar notificações
    "logs:CreateLogStream",     // Apenas criar logs
    "logs:PutLogEvents"         // Apenas escrever logs
  ]
}
```

**EC2 Role:**
```json
{
  "Permissions": [
    "sqs:SendMessage",          // Apenas enviar para SQS
    "lambda:InvokeFunction"     // Apenas invocar Lambda específica
  ]
}
```

### 4.2 Isolamento de Rede

- **VPC Privada**: Recursos isolados em rede dedicada
- **Security Groups**: Firewall stateful por recurso
- **RDS**: Acesso apenas de IPs autorizados
- **Subnets**: Separação lógica de recursos

### 4.3 Criptografia

- **At Rest**: RDS com criptografia AES-256
- **In Transit**: HTTPS para API Gateway
- **Secrets**: Credenciais em variáveis de ambiente (produção: AWS Secrets Manager)

### 4.4 Auditoria

- **CloudWatch Logs**: Todas as execuções registradas
- **CloudTrail**: Auditoria de chamadas API AWS
- **Retention**: Logs mantidos por 7 dias

---

## 5. MONITORAMENTO E OBSERVABILIDADE

### 5.1 Métricas Coletadas

**Lambda:**
- Invocações totais
- Erros e throttling
- Duração de execução
- Memória utilizada
- Concurrent executions

**RDS:**
- CPU utilization
- Database connections
- Read/Write IOPS
- Storage space

**SQS:**
- Messages in queue
- Age of oldest message
- Messages sent/received

### 5.2 Alarmes Configurados

```hcl
resource "aws_cloudwatch_metric_alarm" "lambda_errors" {
  alarm_name          = "lambda-errors-alarm"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "Errors"
  threshold           = "5"
  alarm_description   = "Alert when Lambda has > 5 errors in 5 minutes"
}
```

### 5.3 Logs Estruturados

- **Lambda Logs**: `/aws/lambda/critical-notification-handler`
- **Lambda Logs**: `/aws/lambda/weekly-report-handler`
- **Retention**: 7 dias
- **Query**: CloudWatch Insights para análise

---

## 6. QUALIDADE DO CÓDIGO

### 6.1 Padrões Aplicados

- **Single Responsibility**: Cada Lambda tem uma função
- **Dependency Injection**: Spring Boot gerencia dependências
- **Error Handling**: Try-catch com logs detalhados
- **Idempotência**: Lambdas podem ser reexecutadas sem efeitos colaterais

### 6.2 Documentação

- **README.md**: Visão geral e instruções
- **Javadoc**: Comentários em classes e métodos
- **Terraform**: Comentários em recursos
- **API**: Validações e mensagens de erro claras

### 6.3 Testes

- **Unit Tests**: Lógica de negócio isolada
- **Integration Tests**: Fluxo completo end-to-end
- **Manual Tests**: Postman collection incluída

---

## 7. INSTRUÇÕES DE DEPLOY

### 7.1 Pré-requisitos

```bash
- Java 21
- Maven 3.8+
- Terraform 1.0+
- AWS CLI configurado
- Conta AWS com Free Tier
```

### 7.2 Deploy Passo a Passo

```bash
# 1. Build das Lambdas
cd serverless-functions/notification-lambda
mvn clean package
cd ../weekly-report-lambda
mvn clean package

# 2. Provisionar infraestrutura
cd ../../terraform
terraform init
terraform apply -auto-approve

# 3. Build da aplicação
cd ..
mvn clean package -DskipTests

# 4. Deploy na EC2
scp -i feedback-key.pem target/*.jar ec2-user@[IP]:~
ssh -i feedback-key.pem ec2-user@[IP]
nohup java -jar *.jar --spring.profiles.active=aws > app.log 2>&1 &
```

### 7.3 Verificação

```bash
# Testar API
curl -X POST http://[IP]:8080/avaliacao \
  -H "Content-Type: application/json" \
  -d '{"description":"Teste","score":2}'

# Verificar logs Lambda
aws logs tail /aws/lambda/critical-notification-handler --follow

# Verificar banco
psql -h [RDS_ENDPOINT] -U dbadmin -d feedbackdb
```

---

## 8. ESTIMATIVA DE CUSTOS

### 8.1 Free Tier (12 meses)

| Serviço | Free Tier | Uso Estimado | Custo |
|---------|-----------|--------------|-------|
| Lambda | 1M invocações/mês | ~1.000/mês | $0 |
| RDS t3.micro | 750h/mês | 720h/mês | $0 |
| SQS | 1M requests/mês | ~5.000/mês | $0 |
| SNS | 1.000 emails/mês | ~50/mês | $0 |
| CloudWatch | 5GB logs/mês | ~1GB/mês | $0 |

**Total mensal**: ~$0 (dentro do Free Tier)

### 8.2 Pós Free Tier

- Lambda: ~$0.20/mês
- RDS: ~$15/mês
- SQS: ~$0.40/mês
- SNS: ~$0.50/mês
- CloudWatch: ~$5/mês

**Total estimado**: ~$21/mês

---

## 9. CONCLUSÃO

### 9.1 Requisitos Atendidos

[ ] Serverless implementado: 2 funções Lambda com responsabilidades distintas  
[ ] Cloud environment: 100% rodando na AWS  
[ ] Responsabilidade Única: Cada Lambda tem uma função específica  
[ ] Escalabilidade: Auto-scaling em todos os componentes  
[ ] Segurança: IAM, VPC, criptografia, Security Groups  
[ ] Monitoramento: CloudWatch Logs, Metrics e Alarms  
[ ] Documentação: README, código comentado, Terraform documentado  

### 9.2 Diferenciais Implementados

- **Infrastructure as Code**: Terraform para reprodutibilidade
- **Event-Driven Architecture**: Desacoplamento via SQS
- **Scheduled Jobs**: Automação via EventBridge
- **Observabilidade**: Logs estruturados e alarmes
- **Best Practices**: Princípio do menor privilégio, criptografia, isolamento

---

**Desenvolvido para**: Tech Challenge Fase 4 - FIAP  
**Data**: Fevereiro 2026  
**Região AWS**: us-east-2 (Ohio)
