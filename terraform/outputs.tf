output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = aws_db_instance.postgres.endpoint
}

output "sns_topic_arn" {
  description = "SNS Topic ARN"
  value       = aws_sns_topic.feedback_notifications.arn
}

output "sqs_queue_url" {
  description = "SQS Queue URL"
  value       = aws_sqs_queue.critical_feedbacks.url
}

output "lambda_notification_arn" {
  description = "Critical Notification Lambda ARN"
  value       = aws_lambda_function.critical_notification.arn
}

output "lambda_report_arn" {
  description = "Weekly Report Lambda ARN"
  value       = aws_lambda_function.weekly_report.arn
}

output "ec2_instance_profile" {
  description = "EC2 Instance Profile Name"
  value       = aws_iam_instance_profile.ec2_profile.name
}
