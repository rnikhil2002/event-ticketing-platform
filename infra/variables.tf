variable "env" {
  type    = string
  default = "dev"
}

variable "region" {
  type    = string
  default = "us-east-1"
}

variable "image_tag" {
  description = "Docker image tag to deploy (CI passes the git SHA)"
  type        = string
  default     = "latest"
}

variable "desired_count" {
  type    = number
  default = 2
}

variable "db_host" {
  description = "Postgres host (e.g. an RDS endpoint) with databases users, events, bookings"
  type        = string
}

variable "redis_host" {
  description = "Redis host (e.g. an ElastiCache endpoint)"
  type        = string
}

variable "secrets_arn" {
  description = "Secrets Manager secret with JSON keys db_user, db_password, jwt_secret"
  type        = string
}
