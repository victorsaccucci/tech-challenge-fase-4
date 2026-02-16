variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-2"
}

variable "db_username" {
  description = "Database username"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "Database password"
  type        = string
  sensitive   = true
}

variable "admin_email" {
  description = "Admin email for notifications"
  type        = string
}
