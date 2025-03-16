provider "aws" {
  region = "us-east-2"
}

data "aws_availability_zones" "available" {}

resource "aws_vpc" "main" {
  cidr_block = "10.0.0.0/16"
}

resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(aws_vpc.main.cidr_block, 8, count.index)
  map_public_ip_on_launch = true
  availability_zone       = element(data.aws_availability_zones.available.names, count.index)
}

resource "aws_subnet" "private" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(aws_vpc.main.cidr_block, 8, count.index + 2)
  availability_zone = element(data.aws_availability_zones.available.names, count.index)
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }
}

resource "aws_route_table_association" "public" {
  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# Security group for the load balancer
resource "aws_security_group" "lb" {
  name        = "lb-sg"
  description = "Security group for the load balancer"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

# Security group for the ECS tasks
resource "aws_security_group" "ecs" {
  name        = "ecs-sg"
  description = "Security group for the ECS tasks"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 4000
    to_port         = 4000
    protocol        = "tcp"
    security_groups = [aws_security_group.lb.id]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_ecs_cluster" "main" {
  name = "ecs-cluster"
}

##############################
# ECR and API Docker Image Build/Push
##############################

resource "aws_ecr_repository" "my_spring_app" {
  name = "my-spring-app"
}

resource "null_resource" "build_push_api" {
  # Update the relative file path to the Dockerfile (e.g., "../Dockerfile" if it's one level above the Terraform directory)
  triggers = {
    dockerfile_checksum = sha1(file("../Dockerfile"))
  }

  provisioner "local-exec" {
    command = <<EOT
      echo "Logging in to ECR..."
      aws ecr get-login-password --region us-east-2 | docker login --username AWS --password-stdin ${aws_ecr_repository.my_spring_app.repository_url}
      echo "Building API Docker image..."
      docker build -t my-spring-app -f ../Dockerfile .
      echo "Tagging API Docker image..."
      docker tag my-spring-app:latest ${aws_ecr_repository.my_spring_app.repository_url}:latest
      echo "Pushing API Docker image to ECR..."
      docker push ${aws_ecr_repository.my_spring_app.repository_url}:latest
    EOT
  }
}

##############################
# ECS Task Definitions
##############################

# Postgres Task Definition (using official image)
resource "aws_ecs_task_definition" "postgres" {
  family                   = "postgres-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"

  container_definitions = jsonencode([{
    name      = "api"
    image     = "${aws_ecr_repository.my_spring_app.repository_url}:latest"
    essential = true
    portMappings = [{
      containerPort = 4000
      hostPort      = 4000
    }]
    environment = [
      { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://postgres:5432/realtime-emergency-response-alert-system-db" },
      { name = "SPRING_DATASOURCE_USERNAME", value = "root" },
      { name = "SPRING_DATASOURCE_PASSWORD", value = "root" },
      { name = "SPRING_JPA_HIBERNATE_DDL_AUTO", value = "update" },
      { name = "SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT", value = "org.hibernate.dialect.PostgreSQLDialect" },
      { name = "SPRING_JPA_SHOW_SQL", value = "true" },
      { name = "SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS", value = "kafka:9092" }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = "app-log-group"
        awslogs-region        = "us-east-2"
        awslogs-stream-prefix = "app"
      }
    }
  }])
}

# Kafka Task Definition (using official image)
resource "aws_ecs_task_definition" "kafka" {
  family                   = "kafka-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"

  container_definitions = jsonencode([{
    name      = "api"
    image     = "${aws_ecr_repository.my_spring_app.repository_url}:latest"
    essential = true
    portMappings = [{
      containerPort = 4000
      hostPort      = 4000
    }]
    environment = [
      { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://postgres:5432/realtime-emergency-response-alert-system-db" },
      { name = "SPRING_DATASOURCE_USERNAME", value = "root" },
      { name = "SPRING_DATASOURCE_PASSWORD", value = "root" },
      { name = "SPRING_JPA_HIBERNATE_DDL_AUTO", value = "update" },
      { name = "SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT", value = "org.hibernate.dialect.PostgreSQLDialect" },
      { name = "SPRING_JPA_SHOW_SQL", value = "true" },
      { name = "SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS", value = "kafka:9092" }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = "app-log-group"
        awslogs-region        = "us-east-2"
        awslogs-stream-prefix = "app"
      }
    }
  }])
}

# API Task Definition (using the ECR image we pushed)
resource "aws_ecs_task_definition" "app" {
  family                   = "app-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([{
    name      = "api"
    image     = "${aws_ecr_repository.my_spring_app.repository_url}:latest"
    essential = true
    portMappings = [{
      containerPort = 4000
      hostPort      = 4000
    }]
    environment = [
      { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://postgres:5432/realtime-emergency-response-alert-system-db" },
      { name = "SPRING_DATASOURCE_USERNAME", value = "root" },
      { name = "SPRING_DATASOURCE_PASSWORD", value = "root" },
      { name = "SPRING_JPA_HIBERNATE_DDL_AUTO", value = "update" },
      { name = "SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT", value = "org.hibernate.dialect.PostgreSQLDialect" },
      { name = "SPRING_JPA_SHOW_SQL", value = "true" },
      { name = "SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS", value = "kafka:9092" }
    ]
    ,logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = "app-log-group"
        awslogs-region        = "us-east-2"
        awslogs-stream-prefix = "app"
      }
    }
  }])
}

##############################
# ECS Services
##############################

resource "aws_ecs_service" "postgres" {
  name            = "postgres-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.postgres.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = aws_subnet.public[*].id
    security_groups = [aws_security_group.ecs.id]
  }
}

resource "aws_ecs_service" "kafka" {
  name            = "kafka-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.kafka.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = aws_subnet.public[*].id
    security_groups = [aws_security_group.ecs.id]
  }
}

resource "aws_ecs_service" "app" {
  name            = "app-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = aws_subnet.public[*].id
    security_groups = [aws_security_group.ecs.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.main.arn
    container_name   = "api"
    container_port   = 4000
  }

  lifecycle {
    create_before_destroy = true
  }
}

##############################
# Load Balancer & Target Group
##############################

resource "aws_lb" "main" {
  name               = "app-lb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.lb.id]
  subnets            = aws_subnet.public[*].id

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_lb_target_group" "main" {
  name        = "app-tg"
  port        = 4000
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"
  health_check {
    path                = "/actuator/health"
    protocol            = "HTTP"
    healthy_threshold   = 5
    unhealthy_threshold = 2
    interval            = 60
    timeout             = 10
    matcher             = "200"
  }
}

resource "aws_lb_listener" "main" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.main.arn
  }
}

##############################
# EFS for PostgreSQL (if needed)
##############################

resource "aws_efs_file_system" "postgres" {
  creation_token = "postgres-efs"
}

resource "aws_efs_mount_target" "postgres" {
  count           = 2
  file_system_id  = aws_efs_file_system.postgres.id
  subnet_id       = element(aws_subnet.public[*].id, count.index)
  security_groups = [aws_security_group.ecs.id]
}
