output "url" {
  value = "http://${aws_lb.main.dns_name}"
}

output "ecr_repositories" {
  value = { for k, r in aws_ecr_repository.svc : k => r.repository_url }
}

output "cluster" {
  value = aws_ecs_cluster.main.name
}
