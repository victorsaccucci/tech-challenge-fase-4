terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# VPC e Networking
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "feedback-vpc"
  }
}

# Internet Gateway
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "feedback-igw"
  }
}

# Route Table
resource "aws_route_table" "main" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "feedback-rt"
  }
}

resource "aws_subnet" "private_a" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.1.0/24"
  availability_zone = "${var.aws_region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "feedback-private-a"
  }
}

resource "aws_subnet" "private_b" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = "${var.aws_region}b"
  map_public_ip_on_launch = true

  tags = {
    Name = "feedback-private-b"
  }
}

# Associate Route Table
resource "aws_route_table_association" "a" {
  subnet_id      = aws_subnet.private_a.id
  route_table_id = aws_route_table.main.id
}

resource "aws_route_table_association" "b" {
  subnet_id      = aws_subnet.private_b.id
  route_table_id = aws_route_table.main.id
}

# Security Group para RDS
resource "aws_security_group" "rds" {
  name        = "feedback-rds-sg"
  description = "Security group for RDS PostgreSQL"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "feedback-rds-sg"
  }
}

# RDS PostgreSQL
resource "aws_db_subnet_group" "main" {
  name       = "feedback-db-subnet"
  subnet_ids = [aws_subnet.private_a.id, aws_subnet.private_b.id]

  tags = {
    Name = "feedback-db-subnet"
  }
}

resource "aws_db_instance" "postgres" {
  identifier             = "feedback-db"
  engine                 = "postgres"
  engine_version         = "15"
  instance_class         = "db.t3.micro"
  allocated_storage      = 20
  storage_encrypted      = true
  db_name                = "feedbackdb"
  username               = var.db_username
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  skip_final_snapshot    = true
  publicly_accessible    = true

  tags = {
    Name = "feedback-postgres"
  }
}

# SNS Topic para notificações
resource "aws_sns_topic" "feedback_notifications" {
  name = "feedback-critical-notifications"

  tags = {
    Name = "feedback-notifications"
  }
}

resource "aws_sns_topic_subscription" "email" {
  topic_arn = aws_sns_topic.feedback_notifications.arn
  protocol  = "email"
  endpoint  = var.admin_email
}

# SQS Queue para feedbacks críticos
resource "aws_sqs_queue" "critical_feedbacks" {
  name                       = "critical-feedbacks-queue"
  visibility_timeout_seconds = 300
  message_retention_seconds  = 86400

  tags = {
    Name = "critical-feedbacks-queue"
  }
}

# IAM Role para Lambda
resource "aws_iam_role" "lambda_role" {
  name = "feedback-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy" "lambda_policy" {
  name = "feedback-lambda-policy"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ]
        Resource = aws_sqs_queue.critical_feedbacks.arn
      },
      {
        Effect = "Allow"
        Action = [
          "sns:Publish"
        ]
        Resource = aws_sns_topic.feedback_notifications.arn
      },
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })
}

# Lambda - Notificação Crítica
resource "aws_lambda_function" "critical_notification" {
  filename         = "../serverless-functions/notification-lambda/target/notification-lambda-1.0.0.jar"
  function_name    = "critical-notification-handler"
  role            = aws_iam_role.lambda_role.arn
  handler         = "com.fiap.lambda.CriticalNotificationHandler::handleRequest"
  runtime         = "java21"
  timeout         = 60
  memory_size     = 512

  environment {
    variables = {
      SNS_TOPIC_ARN = aws_sns_topic.feedback_notifications.arn
    }
  }

  tags = {
    Name = "critical-notification-handler"
  }
}

resource "aws_lambda_event_source_mapping" "sqs_trigger" {
  event_source_arn = aws_sqs_queue.critical_feedbacks.arn
  function_name    = aws_lambda_function.critical_notification.arn
  batch_size       = 10
}

# Lambda - Relatório Semanal
resource "aws_lambda_function" "weekly_report" {
  filename         = "../serverless-functions/weekly-report-lambda/target/weekly-report-lambda-1.0.0.jar"
  function_name    = "weekly-report-handler"
  role            = aws_iam_role.lambda_role.arn
  handler         = "com.fiap.lambda.WeeklyReportHandler::handleRequest"
  runtime         = "java21"
  timeout         = 300
  memory_size     = 1024

  environment {
    variables = {
      SNS_TOPIC_ARN = aws_sns_topic.feedback_notifications.arn
      DB_URL        = "jdbc:postgresql://${aws_db_instance.postgres.endpoint}/feedbackdb"
      DB_USER       = var.db_username
      DB_PASSWORD   = var.db_password
    }
  }

  tags = {
    Name = "weekly-report-handler"
  }
}

# EventBridge Rule para executar semanalmente
resource "aws_cloudwatch_event_rule" "weekly_report" {
  name                = "weekly-report-schedule"
  description         = "Trigger weekly report every Monday at 9 AM"
  schedule_expression = "cron(0 9 ? * MON *)"
}

resource "aws_cloudwatch_event_target" "weekly_report" {
  rule      = aws_cloudwatch_event_rule.weekly_report.name
  target_id = "WeeklyReportLambda"
  arn       = aws_lambda_function.weekly_report.arn
}

resource "aws_lambda_permission" "allow_eventbridge" {
  statement_id  = "AllowExecutionFromEventBridge"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.weekly_report.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.weekly_report.arn
}

# IAM Role para EC2
resource "aws_iam_role" "ec2_role" {
  name = "ec2-sqs-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy" "ec2_policy" {
  name = "ec2-permissions"
  role = aws_iam_role.ec2_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:GetQueueUrl"
        ]
        Resource = aws_sqs_queue.critical_feedbacks.arn
      },
      {
        Effect = "Allow"
        Action = "lambda:InvokeFunction"
        Resource = aws_lambda_function.weekly_report.arn
      }
    ]
  })
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "ec2-sqs-profile"
  role = aws_iam_role.ec2_role.name
}

# CloudWatch Log Groups
resource "aws_cloudwatch_log_group" "critical_notification" {
  name              = "/aws/lambda/critical-notification-handler"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "weekly_report" {
  name              = "/aws/lambda/weekly-report-handler"
  retention_in_days = 7
}

# CloudWatch Alarms
resource "aws_cloudwatch_metric_alarm" "lambda_errors" {
  alarm_name          = "lambda-errors-alarm"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = "300"
  statistic           = "Sum"
  threshold           = "5"
  alarm_description   = "Alert when Lambda functions have errors"
  alarm_actions       = [aws_sns_topic.feedback_notifications.arn]
}
