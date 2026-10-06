resource "aws_ecs_cluster" "main" {
  name = local.name
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

# Service discovery so booking-service can call event-service by name.
resource "aws_service_discovery_private_dns_namespace" "ns" {
  name = "${local.name}.local"
  vpc  = data.aws_vpc.default.id
}

resource "aws_service_discovery_service" "svc" {
  for_each = local.services
  name     = "${each.key}-service"
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.ns.id
    dns_records {
      type = "A"
      ttl  = 10
    }
  }
}

resource "aws_iam_role" "execution" {
  name               = "${local.name}-ecs-execution"
  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [{ Effect = "Allow", Principal = { Service = "ecs-tasks.amazonaws.com" }, Action = "sts:AssumeRole" }]
  })
}

resource "aws_iam_role_policy_attachment" "execution" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "read_secrets" {
  role   = aws_iam_role.execution.id
  policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [{ Effect = "Allow", Action = ["secretsmanager:GetSecretValue"], Resource = var.secrets_arn }]
  })
}

resource "aws_cloudwatch_log_group" "svc" {
  for_each          = toset(concat(keys(local.services), ["gateway"]))
  name              = "/ecs/${local.name}/${each.key}"
  retention_in_days = 14
}

locals {
  # Database URLs and the JWT secret come from one Secrets Manager secret (JSON keys).
  db_names  = { user = "users", event = "events", booking = "bookings" }
  extra_env = {
    user    = []
    event   = []
    booking = [
      { name = "REDIS_HOST", value = var.redis_host },
      { name = "EVENT_SERVICE_URL", value = "http://event-service.${local.name}.local:8080" },
    ]
  }
}

resource "aws_ecs_task_definition" "svc" {
  for_each                 = local.services
  family                   = "${local.name}-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.execution.arn

  container_definitions = jsonencode([{
    name         = each.key
    image        = "${aws_ecr_repository.svc[each.key].repository_url}:${var.image_tag}"
    essential    = true
    portMappings = [{ containerPort = 8080 }]
    environment  = concat([
      { name = "PORT", value = "8080" },
      { name = "DB_URL", value = "jdbc:postgresql://${var.db_host}:5432/${local.db_names[each.key]}" },
    ], local.extra_env[each.key])
    secrets = [
      { name = "DB_USER", valueFrom = "${var.secrets_arn}:db_user::" },
      { name = "DB_PASSWORD", valueFrom = "${var.secrets_arn}:db_password::" },
      { name = "JWT_SECRET", valueFrom = "${var.secrets_arn}:jwt_secret::" },
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options   = {
        awslogs-group         = aws_cloudwatch_log_group.svc[each.key].name
        awslogs-region        = var.region
        awslogs-stream-prefix = each.key
      }
    }
  }])
}

resource "aws_ecs_service" "svc" {
  for_each        = local.services
  name            = each.key
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.svc[each.key].arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.tasks.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.svc[each.key].arn
    container_name   = each.key
    container_port   = 8080
  }

  service_registries {
    registry_arn = aws_service_discovery_service.svc[each.key].arn
  }

  # Roll back automatically if a new version fails its health checks.
  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }
}

resource "aws_ecs_task_definition" "gateway" {
  family                   = "${local.name}-gateway"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.execution.arn
  container_definitions    = jsonencode([{
    name             = "gateway"
    image            = "${aws_ecr_repository.svc["gateway"].repository_url}:${var.image_tag}"
    essential        = true
    portMappings     = [{ containerPort = 80 }]
    logConfiguration = {
      logDriver = "awslogs"
      options   = {
        awslogs-group         = aws_cloudwatch_log_group.svc["gateway"].name
        awslogs-region        = var.region
        awslogs-stream-prefix = "gateway"
      }
    }
  }])
}

resource "aws_ecs_service" "gateway" {
  name            = "gateway"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.gateway.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.tasks.id]
    assign_public_ip = true
  }
  load_balancer {
    target_group_arn = aws_lb_target_group.gateway.arn
    container_name   = "gateway"
    container_port   = 80
  }
}
